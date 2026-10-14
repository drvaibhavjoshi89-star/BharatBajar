package com.example.data.repository

import android.content.Context
import com.example.data.local.AlertEntity
import com.example.data.local.AppDatabase
import com.example.data.local.WatchlistEntity
import com.example.data.model.AlertCondition
import com.example.data.model.CandleStick
import com.example.data.model.Exchange
import com.example.data.model.MarketIndex
import com.example.data.model.OrderBook
import com.example.data.model.OrderBookEntry
import com.example.data.model.SectorPerformance
import com.example.data.model.Stock
import com.example.data.model.TimeFrame
import com.example.notification.StockNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.remote.YahooFinanceService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class MarketRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val db = AppDatabase.getDatabase(context)
    private val watchlistDao = db.watchlistDao()
    private val alertDao = db.alertDao()
    val notificationHelper = StockNotificationHelper(context)
    private val multiSourceService = com.example.data.remote.MultiSourceMarketService()

    // Stocks in-memory state
    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    // Indices in-memory state
    private val _indices = MutableStateFlow<List<MarketIndex>>(emptyList())
    val indices: StateFlow<List<MarketIndex>> = _indices.asStateFlow()

    // Sectoral performance
    private val _sectors = MutableStateFlow<List<SectorPerformance>>(emptyList())
    val sectors: StateFlow<List<SectorPerformance>> = _sectors.asStateFlow()

    // Simulation running state
    private val _isLiveSimulationActive = MutableStateFlow(true)
    val isLiveSimulationActive: StateFlow<Boolean> = _isLiveSimulationActive.asStateFlow()

    // Real-Time Internet Sync state
    private val _isInternetSyncing = MutableStateFlow(false)
    val isInternetSyncing: StateFlow<Boolean> = _isInternetSyncing.asStateFlow()

    private val _lastInternetSyncTime = MutableStateFlow<Long?>(null)
    val lastInternetSyncTime: StateFlow<Long?> = _lastInternetSyncTime.asStateFlow()

    private val _isInternetLiveConnected = MutableStateFlow(false)
    val isInternetLiveConnected: StateFlow<Boolean> = _isInternetLiveConnected.asStateFlow()

    private val _activeDataSource = MutableStateFlow("NSE India & Yahoo Finance")
    val activeDataSource: StateFlow<String> = _activeDataSource.asStateFlow()

    // Dynamic Fallback Cache: dynamically stores the latest live market values
    // received from internet sync, ensuring fallback always uses the newest details!
    private val fallbackStockDetails = java.util.concurrent.ConcurrentHashMap<String, Stock>()
    private val fallbackIndexDetails = java.util.concurrent.ConcurrentHashMap<String, MarketIndex>()

    private var tickerJob: Job? = null
    private var internetSyncJob: Job? = null

    init {
        initializeInitialData()
        coroutineScope.launch(Dispatchers.IO) {
            seedDefaultWatchlistsIfNeeded()
            syncLiveMarketDataFromInternet()
        }
        startLiveTicker()
        startPeriodicInternetSync()
    }

    private fun initializeInitialData() {
        val initialStocks = createSeedStocks()
        _stocks.value = initialStocks
        initialStocks.forEach { fallbackStockDetails[it.symbol] = it }

        val initialIndices = createSeedIndices()
        _indices.value = initialIndices
        initialIndices.forEach { fallbackIndexDetails[it.symbol] = it }

        updateSectoralPerformances(initialStocks)
    }

    private fun createSeedIndices(): List<MarketIndex> {
        return listOf(
            MarketIndex(
                symbol = "NIFTY 50",
                name = "NIFTY 50 (NSE Benchmark)",
                exchange = Exchange.NSE,
                currentValue = 24823.15,
                previousClose = 24687.40,
                dayHigh = 24885.60,
                dayLow = 24650.10,
                yearHigh = 26277.35,
                yearLow = 18837.85,
                advances = 34,
                declines = 16,
                unchanged = 0,
                sparklinePoints = generateSparkline(24687.40, 24823.15)
            ),
            MarketIndex(
                symbol = "SENSEX",
                name = "BSE SENSEX 30",
                exchange = Exchange.BSE,
                currentValue = 81381.36,
                previousClose = 80981.95,
                dayHigh = 81520.40,
                dayLow = 80890.25,
                yearHigh = 85978.25,
                yearLow = 63583.07,
                advances = 21,
                declines = 9,
                unchanged = 0,
                sparklinePoints = generateSparkline(80981.95, 81381.36)
            ),
            MarketIndex(
                symbol = "BANK NIFTY",
                name = "NIFTY Bank Index",
                exchange = Exchange.NSE,
                currentValue = 51940.60,
                previousClose = 51520.10,
                dayHigh = 52110.80,
                dayLow = 51430.20,
                yearHigh = 54467.35,
                yearLow = 42105.40,
                advances = 9,
                declines = 3,
                unchanged = 0,
                sparklinePoints = generateSparkline(51520.10, 51940.60)
            ),
            MarketIndex(
                symbol = "NIFTY IT",
                name = "NIFTY Information Tech",
                exchange = Exchange.NSE,
                currentValue = 42150.80,
                previousClose = 42580.40,
                dayHigh = 42600.10,
                dayLow = 41980.50,
                yearHigh = 44250.00,
                yearLow = 30500.00,
                advances = 2,
                declines = 8,
                unchanged = 0,
                sparklinePoints = generateSparkline(42580.40, 42150.80)
            ),
            MarketIndex(
                symbol = "NIFTY AUTO",
                name = "NIFTY Auto Sector",
                exchange = Exchange.NSE,
                currentValue = 25340.20,
                previousClose = 24780.50,
                dayHigh = 25410.00,
                dayLow = 24720.00,
                yearHigh = 26300.00,
                yearLow = 15800.00,
                advances = 12,
                declines = 3,
                unchanged = 0,
                sparklinePoints = generateSparkline(24780.50, 25340.20)
            ),
            MarketIndex(
                symbol = "NIFTY PHARMA",
                name = "NIFTY Healthcare & Pharma",
                exchange = Exchange.NSE,
                currentValue = 22750.40,
                previousClose = 22610.15,
                dayHigh = 22820.50,
                dayLow = 22550.00,
                yearHigh = 23400.00,
                yearLow = 14500.00,
                advances = 14,
                declines = 6,
                unchanged = 0,
                sparklinePoints = generateSparkline(22610.15, 22750.40)
            ),
            MarketIndex(
                symbol = "NIFTY METAL",
                name = "NIFTY Metal Sector",
                exchange = Exchange.NSE,
                currentValue = 9480.90,
                previousClose = 9320.40,
                dayHigh = 9520.10,
                dayLow = 9300.50,
                yearHigh = 10150.00,
                yearLow = 6200.00,
                advances = 10,
                declines = 5,
                unchanged = 0,
                sparklinePoints = generateSparkline(9320.40, 9480.90)
            ),
            MarketIndex(
                symbol = "BSE MIDCAP",
                name = "BSE MidCap Index",
                exchange = Exchange.BSE,
                currentValue = 46920.35,
                previousClose = 46410.80,
                dayHigh = 47050.20,
                dayLow = 46350.00,
                yearHigh = 49200.00,
                yearLow = 31200.00,
                advances = 78,
                declines = 42,
                unchanged = 5,
                sparklinePoints = generateSparkline(46410.80, 46920.35)
            )
        )
    }

    private fun generateSparkline(start: Double, end: Double, count: Int = 12): List<Double> {
        val list = mutableListOf<Double>()
        var curr = start
        val step = (end - start) / count
        for (i in 0 until count) {
            val jitter = (Random.nextDouble() - 0.48) * (abs(end - start) * 0.4 + 5.0)
            curr += step + jitter
            list.add(curr)
        }
        list.add(end)
        return list
    }

    private fun createSeedStocks(): List<Stock> {
        val raw = listOf(
            // Auto (Top Performing Sector)
            RawStock("TATAMOTORS", "Tata Motors Ltd.", Exchange.NSE, "Auto", 985.40, 952.10, 955.00, 992.50, 950.00, 1179.05, 608.20, 24500000, 362400.0, 10.8, 1.45, 0.61),
            RawStock("M&M", "Mahindra & Mahindra", Exchange.NSE, "Auto", 3045.80, 2960.00, 2975.00, 3070.00, 2965.00, 3222.00, 1495.00, 5400000, 378900.0, 31.4, 1.15, 0.72),
            RawStock("MARUTI", "Maruti Suzuki India", Exchange.NSE, "Auto", 12350.00, 12180.00, 12200.00, 12420.00, 12150.00, 13680.00, 9650.00, 1200000, 388200.0, 28.6, 0.78, 1.05),
            RawStock("BAJAJ-AUTO", "Bajaj Auto Ltd.", Exchange.BSE, "Auto", 10120.50, 9940.00, 9960.00, 10180.00, 9920.00, 12774.00, 5100.00, 890000, 286700.0, 34.2, 0.85, 0.80),
            RawStock("EICHERMOT", "Eicher Motors Ltd.", Exchange.NSE, "Auto", 4780.20, 4710.00, 4725.00, 4810.00, 4700.00, 5105.00, 3300.00, 1150000, 130800.0, 32.1, 0.92, 1.08),

            // Banking & Financials
            RawStock("HDFCBANK", "HDFC Bank Ltd.", Exchange.NSE, "Banking", 1682.40, 1665.10, 1668.00, 1690.00, 1662.00, 1794.00, 1363.55, 31200000, 1282000.0, 19.5, 1.08, 1.16),
            RawStock("ICICIBANK", "ICICI Bank Ltd.", Exchange.NSE, "Banking", 1245.90, 1228.40, 1232.00, 1252.00, 1226.00, 1300.85, 915.00, 22100000, 876500.0, 17.8, 1.12, 0.80),
            RawStock("SBIN", "State Bank of India", Exchange.NSE, "Banking", 798.50, 786.20, 788.00, 804.00, 784.00, 912.00, 555.00, 28500000, 712000.0, 10.4, 1.32, 1.72),
            RawStock("AXISBANK", "Axis Bank Ltd.", Exchange.BSE, "Banking", 1175.60, 1162.00, 1165.00, 1184.00, 1158.00, 1339.65, 960.00, 14200000, 363400.0, 13.9, 1.25, 0.09),
            RawStock("BAJFINANCE", "Bajaj Finance Ltd.", Exchange.NSE, "Financials", 6890.00, 6780.00, 6800.00, 6940.00, 6750.00, 8190.00, 6160.00, 2400000, 426100.0, 29.8, 1.40, 0.52),

            // Energy & Conglomerate
            RawStock("RELIANCE", "Reliance Industries", Exchange.NSE, "Energy", 2984.70, 2962.30, 2965.00, 2998.00, 2955.00, 3217.90, 2221.00, 14500000, 2018000.0, 28.2, 1.02, 0.34),
            RawStock("ONGC", "Oil & Natural Gas Corp", Exchange.BSE, "Energy", 295.40, 290.10, 291.00, 298.00, 289.00, 344.00, 179.00, 18900000, 371600.0, 7.8, 1.15, 4.15),
            RawStock("COALINDIA", "Coal India Ltd.", Exchange.NSE, "Energy", 498.20, 489.00, 490.00, 503.00, 487.00, 543.55, 295.00, 16200000, 307000.0, 8.2, 0.95, 5.12),
            RawStock("NTPC", "NTPC Limited", Exchange.NSE, "Power", 422.80, 416.00, 417.00, 426.00, 414.00, 448.45, 230.00, 19400000, 409900.0, 17.5, 1.10, 1.84),

            // IT & Tech (Lagging Sector Today)
            RawStock("INFY", "Infosys Ltd.", Exchange.NSE, "IT", 1885.60, 1930.00, 1920.00, 1928.00, 1878.00, 1991.45, 1358.35, 18200000, 782400.0, 28.9, 0.88, 2.44),
            RawStock("TCS", "Tata Consultancy Services", Exchange.BSE, "IT", 4120.00, 4185.00, 4170.00, 4180.00, 4105.00, 4592.25, 3313.00, 6200000, 1490000.0, 31.5, 0.76, 1.77),
            RawStock("WIPRO", "Wipro Limited", Exchange.NSE, "IT", 522.40, 534.80, 532.00, 535.00, 519.50, 584.00, 375.00, 11400000, 273500.0, 23.4, 1.05, 0.19),
            RawStock("HCLTECH", "HCL Technologies", Exchange.NSE, "IT", 1765.20, 1792.00, 1785.00, 1790.00, 1754.00, 1887.00, 1210.00, 5900000, 478900.0, 27.1, 0.82, 2.95),
            RawStock("TECHM", "Tech Mahindra Ltd.", Exchange.BSE, "IT", 1595.00, 1622.00, 1615.00, 1620.00, 1588.00, 1709.00, 1082.00, 4300000, 155800.0, 38.2, 1.18, 1.76),

            // Metals
            RawStock("TATASTEEL", "Tata Steel Ltd.", Exchange.NSE, "Metals", 158.40, 154.60, 155.00, 160.20, 154.00, 184.60, 118.00, 68000000, 197800.0, 24.5, 1.55, 2.27),
            RawStock("JSWSTEEL", "JSW Steel Ltd.", Exchange.NSE, "Metals", 995.80, 978.00, 980.00, 1004.00, 975.00, 1045.00, 740.00, 7800000, 243500.0, 26.8, 1.48, 0.73),
            RawStock("HINDALCO", "Hindalco Industries", Exchange.BSE, "Metals", 712.50, 698.20, 700.00, 718.00, 695.00, 745.00, 460.00, 8900000, 160100.0, 14.2, 1.38, 0.49),

            // Pharma
            RawStock("SUNPHARMA", "Sun Pharmaceutical", Exchange.NSE, "Pharma", 1875.00, 1850.00, 1855.00, 1884.00, 1848.00, 1960.00, 1080.00, 6200000, 449800.0, 39.5, 0.65, 0.72),
            RawStock("CIPLA", "Cipla Limited", Exchange.NSE, "Pharma", 1560.40, 1542.00, 1545.00, 1572.00, 1538.00, 1702.00, 1130.00, 4100000, 125900.0, 27.8, 0.58, 0.83),
            RawStock("DRREDDY", "Dr. Reddy's Labs", Exchange.BSE, "Pharma", 6650.00, 6580.00, 6600.00, 6710.00, 6560.00, 7104.00, 5200.00, 1300000, 111000.0, 19.8, 0.62, 0.60),

            // FMCG & Consumer
            RawStock("ITC", "ITC Limited", Exchange.NSE, "FMCG", 488.50, 482.10, 483.00, 492.00, 481.50, 528.50, 399.30, 24000000, 610000.0, 28.5, 0.68, 2.82),
            RawStock("TITAN", "Titan Company Ltd.", Exchange.NSE, "Consumer", 3480.00, 3445.00, 3450.00, 3510.00, 3430.00, 3886.95, 3055.00, 2100000, 308900.0, 84.5, 1.05, 0.32),
            RawStock("HINDUNILVR", "Hindustan Unilever", Exchange.BSE, "FMCG", 2690.00, 2715.00, 2705.00, 2720.00, 2675.00, 3035.00, 2170.00, 3200000, 632000.0, 58.2, 0.55, 1.56),

            // New-Age / Growth
            RawStock("ZOMATO", "Zomato Ltd.", Exchange.NSE, "Tech/Delivery", 268.40, 256.00, 258.00, 272.50, 255.00, 298.20, 105.00, 72000000, 237000.0, 112.0, 1.80, 0.00),
            RawStock("BHARTIARTL", "Bharti Airtel Ltd.", Exchange.NSE, "Telecom", 1695.50, 1680.00, 1682.00, 1710.00, 1675.00, 1779.00, 905.00, 9400000, 1012000.0, 62.4, 0.72, 0.47),
            RawStock("LT", "Larsen & Toubro Ltd.", Exchange.NSE, "Infra", 3640.00, 3585.00, 3590.00, 3660.00, 3570.00, 3948.00, 2880.00, 4800000, 500200.0, 34.0, 1.12, 0.93)
        )

        return raw.map { r ->
            val candles = generateHistoricalCandles(r.currentPrice, r.previousClose)
            val orderBook = generateOrderBook(r.currentPrice)
            Stock(
                symbol = r.symbol,
                companyName = r.companyName,
                exchange = r.exchange,
                sector = r.sector,
                currentPrice = r.currentPrice,
                previousClose = r.previousClose,
                openPrice = r.openPrice,
                dayHigh = r.dayHigh,
                dayLow = r.dayLow,
                yearHigh = r.yearHigh,
                yearLow = r.yearLow,
                volume = r.volume,
                marketCapCrores = r.marketCapCrores,
                peRatio = r.peRatio,
                beta = r.beta,
                dividendYield = r.dividendYield,
                historicalCandles = candles,
                orderBook = orderBook
            )
        }
    }

    private data class RawStock(
        val symbol: String,
        val companyName: String,
        val exchange: Exchange,
        val sector: String,
        val currentPrice: Double,
        val previousClose: Double,
        val openPrice: Double,
        val dayHigh: Double,
        val dayLow: Double,
        val yearHigh: Double,
        val yearLow: Double,
        val volume: Long,
        val marketCapCrores: Double,
        val peRatio: Double,
        val beta: Double,
        val dividendYield: Double
    )

    private fun generateHistoricalCandles(currentPrice: Double, previousClose: Double): Map<TimeFrame, List<CandleStick>> {
        val result = mutableMapOf<TimeFrame, List<CandleStick>>()
        val timeFrames = TimeFrame.values()

        val sdf1D = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdfDate = SimpleDateFormat("dd MMM", Locale.getDefault())
        val sdfYear = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        timeFrames.forEach { tf ->
            val count = when (tf) {
                TimeFrame.ONE_DAY -> 35 // 9:15 to 15:30 in ~10m bars
                TimeFrame.ONE_WEEK -> 28 // 7 days, 4 bars each
                TimeFrame.ONE_MONTH -> 30 // daily
                TimeFrame.ONE_YEAR -> 52 // weekly
                TimeFrame.FIVE_YEARS -> 60 // monthly
                TimeFrame.ALL -> 70
            }

            val candles = mutableListOf<CandleStick>()
            val now = System.currentTimeMillis()
            val totalSpanMillis = count.toLong() * tf.intervalMinutes * 60 * 1000L

            var runningClose = if (tf == TimeFrame.ONE_DAY) previousClose else currentPrice * (1.0 - (Random.nextDouble() - 0.3) * 0.25)
            val stepDrift = (currentPrice - runningClose) / count

            for (i in 0 until count) {
                val t = now - (count - i) * (totalSpanMillis / count)
                val label = when (tf) {
                    TimeFrame.ONE_DAY -> sdf1D.format(Date(t))
                    TimeFrame.ONE_WEEK, TimeFrame.ONE_MONTH -> sdfDate.format(Date(t))
                    else -> sdfYear.format(Date(t))
                }

                val open = runningClose
                val noise = (Random.nextDouble() - 0.48) * (currentPrice * 0.012)
                var close = if (i == count - 1) currentPrice else (open + stepDrift + noise)
                close = max(1.0, close)
                val high = max(open, close) + Random.nextDouble() * (currentPrice * 0.008)
                val low = max(1.0, min(open, close) - Random.nextDouble() * (currentPrice * 0.008))
                val vol = (Random.nextLong(15000, 250000) * (currentPrice / 1000.0)).toLong().coerceAtLeast(1000L)

                candles.add(
                    CandleStick(
                        timestamp = t,
                        timeLabel = label,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = vol
                    )
                )
                runningClose = close
            }
            result[tf] = candles
        }

        return result
    }

    private fun generateOrderBook(price: Double): OrderBook {
        val bids = mutableListOf<OrderBookEntry>()
        val asks = mutableListOf<OrderBookEntry>()

        var totalBuy = 0L
        var totalSell = 0L

        for (i in 1..5) {
            val bidPrice = price * (1.0 - (i * 0.0008))
            val bidQty = Random.nextInt(120, 4800) * 10
            val bidOrders = Random.nextInt(5, 55)
            bids.add(OrderBookEntry(bidOrders, bidQty, bidPrice))
            totalBuy += bidQty

            val askPrice = price * (1.0 + (i * 0.0008))
            val askQty = Random.nextInt(120, 4800) * 10
            val askOrders = Random.nextInt(5, 55)
            asks.add(OrderBookEntry(askOrders, askQty, askPrice))
            totalSell += askQty
        }

        return OrderBook(bids, asks, totalBuy, totalSell)
    }

    private fun updateSectoralPerformances(stocksList: List<Stock>) {
        val grouped = stocksList.groupBy { it.sector }
        val sectorsList = mutableListOf<SectorPerformance>()

        val sectorIndexMapping = mapOf(
            "Auto" to "NIFTY AUTO",
            "Banking" to "NIFTY BANK",
            "Financials" to "NIFTY FIN SERVICE",
            "Energy" to "NIFTY ENERGY",
            "Power" to "NIFTY POWER",
            "IT" to "NIFTY IT",
            "Metals" to "NIFTY METAL",
            "Pharma" to "NIFTY PHARMA",
            "FMCG" to "NIFTY FMCG",
            "Consumer" to "NIFTY CONSUMPTION",
            "Tech/Delivery" to "NIFTY NEXT 50",
            "Telecom" to "NIFTY TELECOM",
            "Infra" to "NIFTY INFRA"
        )

        for ((sector, stocksInSector) in grouped) {
            val avgChangePct = stocksInSector.map { it.changePercent }.average()
            val advances = stocksInSector.count { it.changePercent > 0 }
            val declines = stocksInSector.count { it.changePercent < 0 }

            val topGainers = stocksInSector.sortedByDescending { it.changePercent }.take(3)
            val topLosers = stocksInSector.sortedBy { it.changePercent }.take(3)

            val avgPrice = stocksInSector.map { it.currentPrice }.average()
            val avgChange = stocksInSector.map { it.change }.average()

            sectorsList.add(
                SectorPerformance(
                    sectorName = sector,
                    indexSymbol = sectorIndexMapping[sector] ?: "NIFTY $sector",
                    changePercent = avgChangePct,
                    currentPrice = avgPrice,
                    change = avgChange,
                    advances = advances,
                    declines = declines,
                    topGainers = topGainers,
                    topLosers = topLosers
                )
            )
        }

        // Sort sectors by change percent descending: index 0 is top gaining sector, last is top losing sector!
        _sectors.value = sectorsList.sortedByDescending { it.changePercent }
    }

    fun toggleSimulation() {
        _isLiveSimulationActive.value = !_isLiveSimulationActive.value
    }

    private fun startLiveTicker() {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(2000)
                if (_isLiveSimulationActive.value) {
                    processMarketTick()
                }
            }
        }
    }

    private fun startPeriodicInternetSync() {
        internetSyncJob?.cancel()
        internetSyncJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(7000)
                syncLiveMarketDataFromInternet()
            }
        }
    }

    suspend fun syncLiveMarketDataFromInternet() = withContext(Dispatchers.IO) {
        if (_isInternetSyncing.value) return@withContext
        _isInternetSyncing.value = true

        try {
            // Source 1: Direct NSE India Official API
            val nseQuotes = multiSourceService.fetchNseIndices()
            val nseMap = nseQuotes.associateBy { it.indexSymbol.uppercase() }

            // Source 2: Yahoo Finance for BSE Sensex & Equities
            val indexSymbols = listOf("^BSESN", "^NSEI", "^NSEBANK", "^CNXIT", "^CNXAUTO", "^CNXPHARMA", "^CNXMETAL")
            val yahooIndexQuotes = multiSourceService.fetchYahooQuotes(indexSymbols)

            val currentIndices = _indices.value
            val updatedIndices = currentIndices.map { idx ->
                val nseQuote = nseMap[idx.symbol.uppercase()]
                val yahooSym = when (idx.symbol) {
                    "NIFTY 50" -> "^NSEI"
                    "SENSEX" -> "^BSESN"
                    "BANK NIFTY" -> "^NSEBANK"
                    "NIFTY IT" -> "^CNXIT"
                    "NIFTY AUTO" -> "^CNXAUTO"
                    "NIFTY PHARMA" -> "^CNXPHARMA"
                    "NIFTY METAL" -> "^CNXMETAL"
                    else -> null
                }
                val yahooQuote = yahooSym?.let { yahooIndexQuotes[it] }

                val updatedIdx = if (nseQuote != null && nseQuote.lastPrice > 0) {
                    // Primary: Direct NSE Official Data
                    idx.copy(
                        currentValue = nseQuote.lastPrice,
                        previousClose = nseQuote.previousClose,
                        dayHigh = nseQuote.high,
                        dayLow = nseQuote.low,
                        yearHigh = nseQuote.yearHigh,
                        yearLow = nseQuote.yearLow,
                        advances = nseQuote.advances,
                        declines = nseQuote.declines
                    )
                } else if (yahooQuote != null && yahooQuote.regularMarketPrice > 0) {
                    // Secondary / BSE: Yahoo Finance
                    val sparkline = if (yahooQuote.candles.isNotEmpty()) {
                        yahooQuote.candles.map { it.close }
                    } else {
                        idx.sparklinePoints
                    }
                    idx.copy(
                        currentValue = yahooQuote.regularMarketPrice,
                        previousClose = yahooQuote.previousClose,
                        dayHigh = max(idx.dayHigh, yahooQuote.dayHigh),
                        dayLow = min(idx.dayLow, yahooQuote.dayLow),
                        yearHigh = yahooQuote.fiftyTwoWeekHigh,
                        yearLow = yahooQuote.fiftyTwoWeekLow,
                        sparklinePoints = sparkline
                    )
                } else {
                    fallbackIndexDetails[idx.symbol] ?: idx
                }
                fallbackIndexDetails[idx.symbol] = updatedIdx
                updatedIdx
            }
            _indices.value = updatedIndices

            // Fetch live stock quotes from Yahoo Finance for all stocks
            val stockSymbolsToFetch = listOf(
                "RELIANCE.NS", "TCS.NS", "HDFCBANK.NS", "INFY.NS", "ICICIBANK.NS",
                "TATAMOTORS.NS", "SBIN.NS", "ITC.NS", "BHARTIARTL.NS", "LT.NS",
                "MARUTI.NS", "M&M.NS", "ZOMATO.NS", "SUNPHARMA.NS", "TATASTEEL.NS",
                "AXISBANK.NS", "BAJFINANCE.NS", "WIPRO.NS", "HCLTECH.NS", "ONGC.NS",
                "COALINDIA.NS", "NTPC.NS", "TITAN.NS", "HINDUNILVR.NS", "JSWSTEEL.NS",
                "EICHERMOT.NS", "BAJAJ-AUTO.NS", "TECHM.NS", "HINDALCO.NS", "CIPLA.NS", "DRREDDY.NS"
            )
            val stockQuotes = multiSourceService.fetchYahooQuotes(stockSymbolsToFetch)

            if (stockQuotes.isNotEmpty()) {
                val currentStocks = _stocks.value
                val updatedStocks = currentStocks.map { stock ->
                    val yahooSym = "${stock.symbol}.NS"
                    val liveQuote = stockQuotes[yahooSym] ?: stockQuotes[stock.symbol]
                    if (liveQuote != null && liveQuote.regularMarketPrice > 0) {
                        val updatedCandles = if (liveQuote.candles.isNotEmpty()) {
                            val map = stock.historicalCandles.toMutableMap()
                            map[TimeFrame.ONE_DAY] = liveQuote.candles
                            map
                        } else {
                            stock.historicalCandles
                        }
                        val updatedStock = stock.copy(
                            currentPrice = liveQuote.regularMarketPrice,
                            previousClose = liveQuote.previousClose,
                            dayHigh = max(stock.dayHigh, liveQuote.dayHigh),
                            dayLow = if (stock.dayLow > 0) min(stock.dayLow, liveQuote.dayLow) else liveQuote.dayLow,
                            yearHigh = liveQuote.fiftyTwoWeekHigh,
                            yearLow = liveQuote.fiftyTwoWeekLow,
                            volume = liveQuote.volume,
                            historicalCandles = updatedCandles
                        )
                        // Make sure the new details are saved in fallback cache!
                        fallbackStockDetails[stock.symbol] = updatedStock
                        updatedStock
                    } else {
                        fallbackStockDetails[stock.symbol] ?: stock
                    }
                }
                _stocks.value = updatedStocks
                updateSectoralPerformances(updatedStocks)
                checkAlerts(updatedStocks)
                _isInternetLiveConnected.value = true
                _lastInternetSyncTime.value = System.currentTimeMillis()
                _activeDataSource.value = if (nseQuotes.isNotEmpty()) "NSE Direct + Yahoo Finance (Live • 7s)" else "Yahoo Finance (Live • 7s)"
            } else {
                // Keep the latest updated details from fallback cache
                val fallbackStocks = _stocks.value.map { fallbackStockDetails[it.symbol] ?: it }
                _stocks.value = fallbackStocks
                if (!_isInternetLiveConnected.value) {
                    _activeDataSource.value = "Market Simulator (NSE/BSE Fallback)"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MarketRepository", "Error syncing internet market data: ${e.message}")
            // Fallback gracefully without losing updated new prices
            val fallbackStocks = _stocks.value.map { fallbackStockDetails[it.symbol] ?: it }
            _stocks.value = fallbackStocks
            val fallbackIndices = _indices.value.map { fallbackIndexDetails[it.symbol] ?: it }
            _indices.value = fallbackIndices
            if (!_isInternetLiveConnected.value) {
                _activeDataSource.value = "Market Simulator (NSE/BSE Fallback)"
            }
        } finally {
            _isInternetSyncing.value = false
        }
    }

    private suspend fun processMarketTick() {
        val currentList = _stocks.value.ifEmpty { return }

        // Pick 4 to 8 random stocks to tick
        val tickCount = Random.nextInt(4, 9)
        val shuffledIndices = currentList.indices.shuffled().take(tickCount).toSet()

        val updatedStocks = currentList.mapIndexed { index, stock ->
            if (shuffledIndices.contains(index)) {
                // Determine tick direction (-0.15% to +0.15% gentle micro-tick)
                val deltaPercent = (Random.nextDouble() - 0.49) * 0.003
                val rawNewPrice = stock.currentPrice * (1.0 + deltaPercent)
                // Round to tick size of ₹0.05
                val newPrice = (rawNewPrice * 20.0).roundToInt() / 20.0
                val tickUp = newPrice >= stock.currentPrice

                val updatedHigh = max(stock.dayHigh, newPrice)
                val updatedLow = min(stock.dayLow, newPrice)
                val updatedVolume = stock.volume + Random.nextInt(200, 5000)

                // update order book
                val updatedBook = generateOrderBook(newPrice)

                val updatedStock = stock.copy(
                    currentPrice = newPrice,
                    dayHigh = updatedHigh,
                    dayLow = updatedLow,
                    volume = updatedVolume,
                    lastTickUp = tickUp,
                    orderBook = updatedBook
                )
                // Update fallback cache so new details are never lost
                fallbackStockDetails[stock.symbol] = updatedStock
                updatedStock
            } else {
                stock.copy(lastTickUp = null)
            }
        }

        _stocks.value = updatedStocks

        // Update indices based on live micro-movement without resetting to old seed values
        updateIndicesTick(updatedStocks)

        // Update sectors
        updateSectoralPerformances(updatedStocks)

        // Check alerts
        checkAlerts(updatedStocks)
    }

    private fun updateIndicesTick(stocksList: List<Stock>) {
        val currentIndices = _indices.value
        val updated = currentIndices.map { idx ->
            // Micro-tick strictly based on the current live value (e.g. 4.60% -> 4.61%),
            // NEVER resetting back to previousClose * (1 + tickRate/100) which caused the 1% glitch!
            val jitterPercent = (Random.nextDouble() - 0.49) * 0.0003 // +/- 0.015% realistic micro-movement
            val rawNewVal = idx.currentValue * (1.0 + jitterPercent)
            val newVal = (rawNewVal * 20.0).roundToInt() / 20.0

            val updatedSparkline = if (idx.sparklinePoints.isNotEmpty()) {
                val list = idx.sparklinePoints.toMutableList()
                if (list.size > 20) list.removeAt(0)
                list.add(newVal)
                list
            } else {
                idx.sparklinePoints
            }

            val updatedHigh = max(idx.dayHigh, newVal)
            val updatedLow = min(idx.dayLow, newVal)

            val updatedIdx = idx.copy(
                currentValue = newVal,
                dayHigh = updatedHigh,
                dayLow = updatedLow,
                sparklinePoints = updatedSparkline
            )
            fallbackIndexDetails[idx.symbol] = updatedIdx
            updatedIdx
        }
        _indices.value = updated
    }

    private suspend fun checkAlerts(stocksList: List<Stock>) {
        val activeAlerts = alertDao.getActiveAlertsList()
        if (activeAlerts.isEmpty()) return

        val stockMap = stocksList.associateBy { it.symbol }
        for (alert in activeAlerts) {
            val stock = stockMap[alert.symbol] ?: continue
            var triggered = false

            when (alert.condition) {
                AlertCondition.PRICE_ABOVE.name -> {
                    if (stock.currentPrice >= alert.targetPrice) triggered = true
                }
                AlertCondition.PRICE_BELOW.name -> {
                    if (stock.currentPrice <= alert.targetPrice) triggered = true
                }
                AlertCondition.PERCENT_CHANGE_UP.name -> {
                    if (stock.changePercent >= alert.targetPrice) triggered = true
                }
                AlertCondition.PERCENT_CHANGE_DOWN.name -> {
                    if (stock.changePercent <= -abs(alert.targetPrice)) triggered = true
                }
            }

            if (triggered) {
                val now = System.currentTimeMillis()
                alertDao.markAlertTriggered(alert.id, now)
                notificationHelper.sendStockAlertNotification(
                    alert = alert,
                    currentPrice = stock.currentPrice,
                    changePercent = stock.changePercent
                )
            }
        }
    }

    // --- Watchlists DB operations ---

    fun getAllWatchlists(): Flow<List<WatchlistEntity>> = watchlistDao.getAllWatchlists()

    suspend fun createWatchlist(name: String, symbols: List<String> = emptyList()): Long {
        return watchlistDao.insertWatchlist(
            WatchlistEntity(
                name = name,
                symbolsCsv = symbols.joinToString(","),
                isDefault = false
            )
        )
    }

    suspend fun addStockToWatchlist(watchlistId: Long, symbol: String) {
        val watchlist = watchlistDao.getWatchlistById(watchlistId) ?: return
        val currentSymbols = if (watchlist.symbolsCsv.isBlank()) {
            mutableListOf()
        } else {
            watchlist.symbolsCsv.split(",").filter { it.isNotBlank() }.toMutableList()
        }
        if (!currentSymbols.contains(symbol)) {
            currentSymbols.add(symbol)
            watchlistDao.updateWatchlist(watchlist.copy(symbolsCsv = currentSymbols.joinToString(",")))
        }
    }

    suspend fun removeStockFromWatchlist(watchlistId: Long, symbol: String) {
        val watchlist = watchlistDao.getWatchlistById(watchlistId) ?: return
        val currentSymbols = watchlist.symbolsCsv.split(",").filter { it.isNotBlank() && it != symbol }
        watchlistDao.updateWatchlist(watchlist.copy(symbolsCsv = currentSymbols.joinToString(",")))
    }

    suspend fun deleteWatchlist(watchlist: WatchlistEntity) {
        if (!watchlist.isDefault) {
            watchlistDao.deleteWatchlist(watchlist)
        }
    }

    private suspend fun seedDefaultWatchlistsIfNeeded() {
        if (watchlistDao.getCount() == 0) {
            watchlistDao.insertWatchlist(
                WatchlistEntity(
                    name = "Nifty Titans",
                    symbolsCsv = "RELIANCE,TCS,HDFCBANK,INFY,ICICIBANK,TATAMOTORS,ITC,SBIN",
                    isDefault = true
                )
            )
            watchlistDao.insertWatchlist(
                WatchlistEntity(
                    name = "Auto & Metals",
                    symbolsCsv = "TATAMOTORS,M&M,MARUTI,BAJAJ-AUTO,TATASTEEL,JSWSTEEL",
                    isDefault = false
                )
            )
            watchlistDao.insertWatchlist(
                WatchlistEntity(
                    name = "High Growth",
                    symbolsCsv = "ZOMATO,BHARTIARTL,BAJFINANCE,LT,SUNPHARMA",
                    isDefault = false
                )
            )
        }
    }

    // --- Alerts DB operations ---

    fun getAllAlerts(): Flow<List<AlertEntity>> = alertDao.getAllAlerts()
    fun getActiveAlerts(): Flow<List<AlertEntity>> = alertDao.getActiveAlerts()
    fun getTriggeredAlerts(): Flow<List<AlertEntity>> = alertDao.getTriggeredAlerts()

    suspend fun createAlert(
        symbol: String,
        companyName: String,
        targetPrice: Double,
        condition: AlertCondition,
        note: String
    ): Long {
        return alertDao.insertAlert(
            AlertEntity(
                symbol = symbol,
                companyName = companyName,
                targetPrice = targetPrice,
                condition = condition.name,
                note = note
            )
        )
    }

    suspend fun deleteAlert(alert: AlertEntity) {
        alertDao.deleteAlert(alert)
    }

    suspend fun clearTriggeredAlerts() {
        alertDao.clearTriggeredAlerts()
    }

    // Market status helper (IST Mon-Fri 09:15 to 15:30)
    fun getMarketStatusInfo(): MarketStatusInfo {
        val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val calendar = Calendar.getInstance(istTimeZone)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        // 09:15 is 555 min, 15:30 is 930 min
        val isOpen = !isWeekend && (totalMinutes in 555..930)
        val isPreMarket = !isWeekend && (totalMinutes in 540..554)

        val statusText = when {
            isWeekend -> "Market Closed (Weekend)"
            isPreMarket -> "Pre-Market Session (09:00 - 09:15)"
            isOpen -> "Market Open (NSE & BSE Live)"
            totalMinutes < 540 -> "Market Opens at 09:15 AM IST"
            else -> "Market Closed (Reopens 09:15 AM)"
        }

        return MarketStatusInfo(
            isOpen = isOpen,
            isPreMarket = isPreMarket,
            statusText = statusText,
            exchangeName = "NSE / BSE Live",
            nextSession = if (isOpen) "Closes 03:30 PM" else "Opens 09:15 AM IST"
        )
    }
}

data class MarketStatusInfo(
    val isOpen: Boolean,
    val isPreMarket: Boolean,
    val statusText: String,
    val exchangeName: String,
    val nextSession: String
)

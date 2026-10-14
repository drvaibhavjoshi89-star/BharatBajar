package com.example.data.model

enum class Exchange {
    NSE, BSE
}

enum class AlertCondition {
    PRICE_ABOVE,
    PRICE_BELOW,
    PERCENT_CHANGE_UP,
    PERCENT_CHANGE_DOWN
}

enum class TimeFrame(val label: String, val intervalMinutes: Int) {
    ONE_DAY("1D", 5),
    ONE_WEEK("1W", 30),
    ONE_MONTH("1M", 120),
    ONE_YEAR("1Y", 1440),
    FIVE_YEARS("5Y", 7200),
    ALL("ALL", 21600)
}

enum class ChartType {
    AREA_LINE,
    CANDLESTICK
}

data class CandleStick(
    val timestamp: Long,
    val timeLabel: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class OrderBookEntry(
    val orders: Int,
    val quantity: Int,
    val price: Double
)

data class OrderBook(
    val bids: List<OrderBookEntry>,
    val asks: List<OrderBookEntry>,
    val totalBuyQty: Long,
    val totalSellQty: Long
)

data class Stock(
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
    val marketCapCrores: Double, // in Crores (₹ Cr)
    val peRatio: Double,
    val beta: Double,
    val dividendYield: Double,
    val lastTickUp: Boolean? = null,
    val historicalCandles: Map<TimeFrame, List<CandleStick>> = emptyMap(),
    val orderBook: OrderBook? = null
) {
    val change: Double
        get() = currentPrice - previousClose

    val changePercent: Double
        get() = if (previousClose > 0) (change / previousClose) * 100 else 0.0

    val isPositive: Boolean
        get() = change >= 0
}

data class MarketIndex(
    val symbol: String,
    val name: String,
    val exchange: Exchange,
    val currentValue: Double,
    val previousClose: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val yearHigh: Double,
    val yearLow: Double,
    val advances: Int,
    val declines: Int,
    val unchanged: Int,
    val sparklinePoints: List<Double> = emptyList()
) {
    val change: Double
        get() = currentValue - previousClose

    val changePercent: Double
        get() = if (previousClose > 0) (change / previousClose) * 100 else 0.0

    val isPositive: Boolean
        get() = change >= 0
}

data class SectorPerformance(
    val sectorName: String,
    val indexSymbol: String,
    val changePercent: Double,
    val currentPrice: Double,
    val change: Double,
    val advances: Int,
    val declines: Int,
    val topGainers: List<Stock> = emptyList(),
    val topLosers: List<Stock> = emptyList()
)

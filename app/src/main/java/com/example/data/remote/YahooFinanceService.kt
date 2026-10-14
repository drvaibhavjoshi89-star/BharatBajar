package com.example.data.remote

import android.util.Log
import com.example.data.model.CandleStick
import com.example.data.model.Exchange
import com.example.data.model.MarketIndex
import com.example.data.model.Stock
import com.example.data.model.TimeFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

data class YahooStockQuote(
    val symbol: String,
    val regularMarketPrice: Double,
    val previousClose: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val volume: Long,
    val timestamp: Long,
    val candles: List<CandleStick>
)

class YahooFinanceService {

    private val okHttpClient = OkHttpClient.Builder()
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        val SYMBOL_MAP = mapOf(
            // Indices
            "NIFTY 50" to "^NSEI",
            "SENSEX" to "^BSESN",
            "BANK NIFTY" to "^NSEBANK",
            "NIFTY IT" to "^CNXIT",
            "NIFTY AUTO" to "^CNXAUTO",
            "NIFTY PHARMA" to "^CNXPHARMA",
            "NIFTY METAL" to "^CNXMETAL",

            // Major NSE & BSE Stocks
            "RELIANCE" to "RELIANCE.NS",
            "TCS" to "TCS.NS",
            "HDFCBANK" to "HDFCBANK.NS",
            "INFY" to "INFY.NS",
            "ICICIBANK" to "ICICIBANK.NS",
            "TATAMOTORS" to "TATAMOTORS.NS",
            "SBIN" to "SBIN.NS",
            "ITC" to "ITC.NS",
            "BHARTIARTL" to "BHARTIARTL.NS",
            "LT" to "LT.NS",
            "MARUTI" to "MARUTI.NS",
            "M&M" to "M&M.NS",
            "ZOMATO" to "ZOMATO.NS",
            "SUNPHARMA" to "SUNPHARMA.NS",
            "TATASTEEL" to "TATASTEEL.NS",
            "AXISBANK" to "AXISBANK.NS",
            "BAJFINANCE" to "BAJFINANCE.NS",
            "WIPRO" to "WIPRO.NS",
            "HCLTECH" to "HCLTECH.NS",
            "ONGC" to "ONGC.NS",
            "COALINDIA" to "COALINDIA.NS",
            "NTPC" to "NTPC.NS",
            "TITAN" to "TITAN.NS",
            "HINDUNILVR" to "HINDUNILVR.NS",
            "JSWSTEEL" to "JSWSTEEL.NS",
            "EICHERMOT" to "EICHERMOT.NS",
            "BAJAJ-AUTO" to "BAJAJ-AUTO.NS",
            "TECHM" to "TECHM.NS",
            "HINDALCO" to "HINDALCO.NS",
            "CIPLA" to "CIPLA.NS",
            "DRREDDY" to "DRREDDY.NS"
        )
    }

    suspend fun fetchQuote(yahooSymbol: String, range: String = "1d", interval: String = "15m"): YahooStockQuote? = withContext(Dispatchers.IO) {
        val encodedSymbol = try {
            java.net.URLEncoder.encode(yahooSymbol, "UTF-8")
        } catch (_: Exception) {
            yahooSymbol
        }
        val url = "$BASE_URL$encodedSymbol?range=$range&interval=$interval"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("YahooFinanceService", "Failed to fetch $yahooSymbol: HTTP ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val chart = root.optJSONObject("chart") ?: return@withContext null
            val resultArr = chart.optJSONArray("result") ?: return@withContext null
            val result = resultArr.optJSONObject(0) ?: return@withContext null

            val meta = result.optJSONObject("meta") ?: return@withContext null
            val regularMarketPrice = meta.optDouble("regularMarketPrice", 0.0)
            if (regularMarketPrice <= 0.0) return@withContext null

            val previousClose = meta.optDouble("chartPreviousClose", regularMarketPrice)
            val dayHigh = meta.optDouble("regularMarketDayHigh", max(regularMarketPrice, previousClose))
            val dayLow = meta.optDouble("regularMarketDayLow", min(regularMarketPrice, previousClose))
            val fiftyTwoWeekHigh = meta.optDouble("fiftyTwoWeekHigh", dayHigh * 1.2)
            val fiftyTwoWeekLow = meta.optDouble("fiftyTwoWeekLow", dayLow * 0.8)
            val volume = meta.optLong("regularMarketVolume", 100000L)
            val regularMarketTime = meta.optLong("regularMarketTime", System.currentTimeMillis() / 1000) * 1000L

            // Parse historical candles
            val candles = mutableListOf<CandleStick>()
            val timestamps = result.optJSONArray("timestamp")
            val indicators = result.optJSONObject("indicators")
            val quoteArr = indicators?.optJSONArray("quote")
            val quoteObj = quoteArr?.optJSONObject(0)

            val opens = quoteObj?.optJSONArray("open")
            val highs = quoteObj?.optJSONArray("high")
            val lows = quoteObj?.optJSONArray("low")
            val closes = quoteObj?.optJSONArray("close")
            val volumes = quoteObj?.optJSONArray("volume")

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

            if (timestamps != null && closes != null) {
                val len = min(timestamps.length(), closes.length())
                var lastValidClose = previousClose

                for (i in 0 until len) {
                    val t = timestamps.optLong(i) * 1000L
                    val c = closes.optDouble(i, Double.NaN)
                    if (c.isNaN()) continue

                    val o = opens?.optDouble(i, lastValidClose) ?: lastValidClose
                    val h = highs?.optDouble(i, max(o, c)) ?: max(o, c)
                    val l = lows?.optDouble(i, min(o, c)) ?: min(o, c)
                    val v = volumes?.optLong(i, 5000L) ?: 5000L

                    candles.add(
                        CandleStick(
                            timestamp = t,
                            timeLabel = sdf.format(Date(t)),
                            open = if (o.isNaN()) lastValidClose else o,
                            high = if (h.isNaN()) c else h,
                            low = if (l.isNaN()) c else l,
                            close = c,
                            volume = max(100L, v)
                        )
                    )
                    lastValidClose = c
                }
            }

            YahooStockQuote(
                symbol = yahooSymbol,
                regularMarketPrice = regularMarketPrice,
                previousClose = previousClose,
                dayHigh = dayHigh,
                dayLow = dayLow,
                fiftyTwoWeekHigh = fiftyTwoWeekHigh,
                fiftyTwoWeekLow = fiftyTwoWeekLow,
                volume = volume,
                timestamp = regularMarketTime,
                candles = candles
            )
        } catch (e: Exception) {
            Log.e("YahooFinanceService", "Error parsing Yahoo quote for $yahooSymbol: ${e.message}")
            null
        }
    }

    suspend fun fetchQuotesInParallel(symbols: List<String>): Map<String, YahooStockQuote> = coroutineScope {
        symbols.map { symbol ->
            async(Dispatchers.IO) {
                val quote = fetchQuote(symbol)
                if (quote != null) symbol to quote else null
            }
        }.awaitAll().filterNotNull().toMap()
    }
}

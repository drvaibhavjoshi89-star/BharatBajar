package com.example.data.remote

import android.util.Log
import com.example.data.model.CandleStick
import com.example.data.model.Exchange
import com.example.data.model.MarketIndex
import com.example.data.model.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

data class NseIndexQuote(
    val indexSymbol: String,
    val lastPrice: Double,
    val change: Double,
    val percentChange: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val previousClose: Double,
    val yearHigh: Double,
    val yearLow: Double,
    val advances: Int,
    val declines: Int
)

data class SourceHealthStatus(
    val nseApiLive: Boolean,
    val yahooFinanceLive: Boolean,
    val geminiFreeTierActive: Boolean,
    val lastSyncSource: String,
    val lastSyncTime: Long
)

class MultiSourceMarketService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val yahooFinanceService = YahooFinanceService()

    companion object {
        private const val NSE_ALL_INDICES_URL = "https://www.nseindia.com/api/allIndices"
        private const val NSE_MARKET_STATUS_URL = "https://www.nseindia.com/api/marketStatus"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    /**
     * Source 1: Official NSE India Direct Public API
     */
    suspend fun fetchNseIndices(): List<NseIndexQuote> = withContext(Dispatchers.IO) {
        val list = mutableListOf<NseIndexQuote>()
        try {
            val request = Request.Builder()
                .url(NSE_ALL_INDICES_URL)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://www.nseindia.com/")
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val dataArr = root.optJSONArray("data")
                if (dataArr != null) {
                    for (i in 0 until dataArr.length()) {
                        val item = dataArr.getJSONObject(i)
                        val name = item.optString("index", "")
                        val last = item.optDouble("last", 0.0)
                        if (last > 0 && name.isNotBlank()) {
                            list.add(
                                NseIndexQuote(
                                    indexSymbol = name,
                                    lastPrice = last,
                                    change = item.optDouble("variation", 0.0),
                                    percentChange = item.optDouble("percentChange", 0.0),
                                    open = item.optDouble("open", last),
                                    high = item.optDouble("high", last),
                                    low = item.optDouble("low", last),
                                    previousClose = item.optDouble("previousClose", last),
                                    yearHigh = item.optDouble("yearHigh", last * 1.2),
                                    yearLow = item.optDouble("yearLow", last * 0.8),
                                    advances = item.optString("advances", "0").toIntOrNull() ?: 0,
                                    declines = item.optString("declines", "0").toIntOrNull() ?: 0
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("MultiSourceMarket", "NSE Direct API call exception: ${e.message}")
        }
        list
    }

    /**
     * Source 2: Yahoo Finance for Stocks & BSE / NSE Historical Candles
     */
    suspend fun fetchYahooQuotes(symbols: List<String>): Map<String, YahooStockQuote> {
        return yahooFinanceService.fetchQuotesInParallel(symbols)
    }

    suspend fun fetchSingleQuote(yahooSymbol: String): YahooStockQuote? {
        return yahooFinanceService.fetchQuote(yahooSymbol)
    }
}

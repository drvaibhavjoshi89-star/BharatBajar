package com.example.data.model

enum class SentimentType {
    BULLISH,
    BEARISH,
    NEUTRAL
}

data class FinancialNewsItem(
    val id: String,
    val title: String,
    val source: String,
    val summary: String,
    val url: String,
    val timeAgo: String,
    val sentiment: SentimentType,
    val affectedIndices: List<String>,
    val category: String
)

data class MacroDriver(
    val title: String,
    val value: String,
    val impact: SentimentType,
    val description: String
)

data class MarketSentimentReport(
    val overallScore: Int, // 0-100 (0 = Extreme Fear / Bearish, 50 = Neutral, 100 = Bullish)
    val sentimentLabel: String,
    val summary: String,
    val niftyOutlook: String,
    val sensexOutlook: String,
    val lastUpdated: Long,
    val macroDrivers: List<MacroDriver>,
    val newsItems: List<FinancialNewsItem>,
    val isGoogleSearchGrounded: Boolean = true
)

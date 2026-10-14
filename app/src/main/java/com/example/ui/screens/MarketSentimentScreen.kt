package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinancialNewsItem
import com.example.data.model.MacroDriver
import com.example.data.model.MarketSentimentReport
import com.example.data.model.SentimentType
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSubtle
import com.example.ui.theme.NeutralAmber
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSentimentScreen(
    report: MarketSentimentReport,
    isLoading: Boolean,
    onRefreshNews: () -> Unit,
    errorMessage: String? = null,
    rateLimitResumeTime: Long? = null,
    onResumeNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    var secondsLeft by remember { mutableStateOf(0L) }
    LaunchedEffect(rateLimitResumeTime) {
        if (rateLimitResumeTime != null) {
            while (true) {
                val diff = (rateLimitResumeTime - System.currentTimeMillis()) / 1000
                secondsLeft = diff.coerceAtLeast(0)
                if (diff <= 0) break
                kotlinx.coroutines.delay(1000)
            }
        } else {
            secondsLeft = 0L
        }
    }

    val categories = remember(report.newsItems) {
        listOf("All") + report.newsItems.map { it.category }.distinct()
    }

    val filteredNews = remember(report.newsItems, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") report.newsItems
        else report.newsItems.filter { it.category == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // AI Quota & Auto-Resume Status Banner
        if (rateLimitResumeTime != null || errorMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (secondsLeft > 0) Color(0xFF2D1F08) else Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (secondsLeft > 0) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (secondsLeft > 0) Icons.Default.Info else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (secondsLeft > 0) Color(0xFFF59E0B) else Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (secondsLeft > 0) "Quota Cooldown • Auto-Resuming in ${secondsLeft}s" else "AI Usage Status",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (secondsLeft > 0) Color(0xFFFCD34D) else Color(0xFF93C5FD)
                                )
                            }

                            Button(
                                onClick = onResumeNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (secondsLeft > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (secondsLeft > 0) "Resume Now" else "Retry AI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "Rate limit reached (15 RPM). The app will automatically resume where it paused. Daily quota resets at 12:00 AM PT (12:30 PM IST).",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
        // Title Bar & Refresh Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Market Sentiment",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF4285F4).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Google Search Live",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Aggregated intelligence affecting Nifty 50 & Sensex",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                IconButton(
                    onClick = onRefreshNews,
                    enabled = !isLoading,
                    modifier = Modifier.testTag("refresh_news_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh News",
                            tint = TextPrimaryDark
                        )
                    }
                }
            }
        }

        // Sentiment Dial Meter Card
        item {
            SentimentGaugeCard(report = report)
        }

        // Nifty 50 & Sensex Outlook Briefs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IndexOutlookMiniCard(
                    indexName = "NIFTY 50",
                    outlook = report.niftyOutlook,
                    isPositive = report.overallScore >= 50,
                    modifier = Modifier.weight(1f)
                )
                IndexOutlookMiniCard(
                    indexName = "BSE SENSEX",
                    outlook = report.sensexOutlook,
                    isPositive = report.overallScore >= 50,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Macro Market Drivers
        item {
            Column {
                Text(
                    text = "Key Macro Drivers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(report.macroDrivers) { driver ->
                        MacroDriverCard(driver = driver)
                    }
                }
            }
        }

        // News Section Header & Category Filter Chips
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latest Financial Headlines (${filteredNews.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimaryDark
                    )
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = "Updated: ${sdf.format(Date(report.lastUpdated))}",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // News Items List
        items(filteredNews, key = { it.id }) { newsItem ->
            FinancialNewsCard(
                item = newsItem,
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.url))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            )
        }
    }
}

@Composable
fun SentimentGaugeCard(report: MarketSentimentReport, modifier: Modifier = Modifier) {
    val score = report.overallScore.coerceIn(0, 100)
    val sentimentColor = when {
        score >= 65 -> ProfitGreen
        score >= 45 -> NeutralAmber
        else -> LossRed
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dalal Street Sentiment Index",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = sentimentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = report.sentimentLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = sentimentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Semicircular Gauge Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.size(220.dp, 110.dp)) {
                    val strokeW = 16.dp.toPx()
                    val arcSize = Size(size.width - strokeW, (size.height * 2) - strokeW)
                    val arcTopLeft = Offset(strokeW / 2f, strokeW / 2f)

                    // Background Track
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Color Gradient Arc
                    val sweepProgress = 180f * (score / 100f)
                    drawArc(
                        brush = Brush.horizontalGradient(
                            listOf(LossRed, NeutralAmber, ProfitGreen)
                        ),
                        startAngle = 180f,
                        sweepAngle = sweepProgress,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Needle pointer
                    val angleRad = (180f + sweepProgress) * (PI / 180f)
                    val radius = (size.width - strokeW) / 2f
                    val needleLen = radius * 0.75f
                    val cx = size.width / 2f
                    val cy = size.height

                    val needleX = cx + needleLen * cos(angleRad).toFloat()
                    val needleY = cy + needleLen * sin(angleRad).toFloat()

                    drawLine(
                        color = Color.White,
                        start = Offset(cx, cy),
                        end = Offset(needleX, needleY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = Color(0xFF0F172A), radius = 3.dp.toPx(), center = Offset(cx, cy))
                }

                // Center Score Text
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "$score",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = sentimentColor
                    )
                    Text(
                        text = "out of 100",
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gauge labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 Bearish", fontSize = 10.sp, color = LossRed, fontWeight = FontWeight.Bold)
                Text("50 Neutral", fontSize = 10.sp, color = NeutralAmber, fontWeight = FontWeight.Bold)
                Text("100 Bullish", fontSize = 10.sp, color = ProfitGreen, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = CardBorderDark
            )

            // Executive Summary
            Text(
                text = report.summary,
                fontSize = 12.sp,
                color = TextPrimaryDark,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun IndexOutlookMiniCard(
    indexName: String,
    outlook: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (isPositive) ProfitGreen else LossRed

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(indexName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryDark)
                Icon(
                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = outlook,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                lineHeight = 15.sp,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun MacroDriverCard(driver: MacroDriver, modifier: Modifier = Modifier) {
    val impactColor = when (driver.impact) {
        SentimentType.BULLISH -> ProfitGreen
        SentimentType.BEARISH -> LossRed
        SentimentType.NEUTRAL -> NeutralAmber
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = modifier.width(170.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = driver.title,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(impactColor, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = driver.value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = driver.description,
                fontSize = 10.sp,
                color = TextSecondaryDark,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun FinancialNewsCard(
    item: FinancialNewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sentColor = when (item.sentiment) {
        SentimentType.BULLISH -> ProfitGreen
        SentimentType.BEARISH -> LossRed
        SentimentType.NEUTRAL -> NeutralAmber
    }
    val sentBg = when (item.sentiment) {
        SentimentType.BULLISH -> ProfitGreenSubtle
        SentimentType.BEARISH -> LossRedSubtle
        SentimentType.NEUTRAL -> NeutralAmber.copy(alpha = 0.15f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Source & Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.source,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD)
                    )
                    Text("•", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(item.timeAgo, fontSize = 11.sp, color = TextSecondaryDark)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = sentBg
                ) {
                    Text(
                        text = item.sentiment.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = sentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                lineHeight = 19.sp
            )

            if (item.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.summary,
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Affected Indices & External Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.affectedIndices.forEach { idx ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = idx,
                                fontSize = 9.sp,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Read Full",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketSentimentReport
import com.example.data.model.Exchange
import com.example.data.model.MarketIndex
import com.example.data.model.SectorPerformance
import com.example.data.model.Stock
import com.example.data.repository.MarketStatusInfo
import com.example.ui.components.MarketStatusHeader
import com.example.ui.components.StockRowCard
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSubtle
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketOverviewScreen(
    indices: List<MarketIndex>,
    stocks: List<Stock>,
    sectors: List<SectorPerformance>,
    statusInfo: MarketStatusInfo,
    isSimulationActive: Boolean,
    watchlistSymbols: Set<String>,
    activeAlertsCount: Int,
    sentimentReport: MarketSentimentReport,
    isInternetSyncing: Boolean = false,
    lastSyncTime: Long? = null,
    isInternetLive: Boolean = true,
    onToggleSimulation: () -> Unit,
    onManualSync: () -> Unit = {},
    onStockClick: (String) -> Unit,
    onWatchlistToggle: (String) -> Unit,
    onSetAlertClick: (String) -> Unit,
    onNavigateToSectors: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSentiment: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMoversTab by remember { mutableIntStateOf(0) } // 0: Top Gainers, 1: Top Losers, 2: Most Active

    val filteredStocks = remember(stocks, searchQuery) {
        if (searchQuery.isBlank()) stocks
        else stocks.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) ||
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.sector.contains(searchQuery, ignoreCase = true)
        }
    }

    val topGainers = remember(stocks) {
        stocks.sortedByDescending { it.changePercent }.take(6)
    }
    val topLosers = remember(stocks) {
        stocks.sortedBy { it.changePercent }.take(6)
    }
    val mostActive = remember(stocks) {
        stocks.sortedByDescending { it.volume }.take(6)
    }

    val displayedStocks = when (selectedMoversTab) {
        0 -> topGainers
        1 -> topLosers
        else -> mostActive
    }

    val bestSector = sectors.firstOrNull()
    val worstSector = sectors.lastOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // App Bar & Alerts Icon
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BHARAT BAJAR",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryDark,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                        ) {
                            Text(
                                text = "NSE • BSE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Real-time Indian Stock Market",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                IconButton(
                    onClick = onNavigateToAlerts,
                    modifier = Modifier.testTag("alerts_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (activeAlertsCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ) {
                                    Text("$activeAlertsCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Stock Alerts",
                            tint = TextPrimaryDark
                        )
                    }
                }
            }
        }

        // Live Market Status Header
        item {
            MarketStatusHeader(
                statusInfo = statusInfo,
                isSimulationActive = isSimulationActive,
                isInternetSyncing = isInternetSyncing,
                lastSyncTime = lastSyncTime,
                isInternetLive = isInternetLive,
                onToggleSimulation = onToggleSimulation,
                onManualSync = onManualSync,
                modifier = Modifier.testTag("market_status_header")
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search NSE/BSE stocks, sectors (e.g. RELIANCE, Auto)", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondaryDark
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = CardBorderDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stock_search_input")
            )
        }

        // If searching, show direct search results
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "Search Results (${filteredStocks.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
            }
            items(filteredStocks, key = { it.symbol }) { stock ->
                StockRowCard(
                    stock = stock,
                    isInWatchlist = watchlistSymbols.contains(stock.symbol),
                    onStockClick = { onStockClick(stock.symbol) },
                    onWatchlistToggle = { onWatchlistToggle(stock.symbol) },
                    onSetAlertClick = { onSetAlertClick(stock.symbol) }
                )
            }
        } else {
            // Live Indices Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Major Indian Indices",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "NSE & BSE",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(indices, key = { it.symbol }) { index ->
                            IndexCard(index = index)
                        }
                    }
                }
            }

            // Sectoral Highlights Banner (Top Sector vs Lagging Sector)
            item {
                Surface(
                    onClick = onNavigateToSectors,
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sectoral_movers_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sectoral Index Movers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Full Sector View",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Best Sector
                            if (bestSector != null) {
                                SectorMiniCard(
                                    title = "Top Sector",
                                    sector = bestSector,
                                    isGainer = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Worst Sector
                            if (worstSector != null) {
                                SectorMiniCard(
                                    title = "Lagging Sector",
                                    sector = worstSector,
                                    isGainer = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Market Sentiment Preview Card (Google Search Grounded)
            item {
                Surface(
                    onClick = onNavigateToSentiment,
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sentiment_overview_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Market Sentiment",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF4285F4).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Google Search Live",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Full Sentiment",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sentimentReport.overallScore >= 50) ProfitGreenSubtle else LossRedSubtle
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${sentimentReport.overallScore}/100",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (sentimentReport.overallScore >= 50) ProfitGreen else LossRed
                                    )
                                    Text(
                                        text = sentimentReport.sentimentLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sentimentReport.overallScore >= 50) ProfitGreen else LossRed
                                    )
                                }
                            }

                            val topArticle = sentimentReport.newsItems.firstOrNull()
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topArticle?.title ?: sentimentReport.summary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${topArticle?.source ?: "Financial Press"} • ${topArticle?.timeAgo ?: "Live"}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            // Market Movers Tabs
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Market Movers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark, RoundedCornerShape(10.dp))
                            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MoverTabButton(
                            title = "Top Gainers",
                            isSelected = selectedMoversTab == 0,
                            onClick = { selectedMoversTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        MoverTabButton(
                            title = "Top Losers",
                            isSelected = selectedMoversTab == 1,
                            onClick = { selectedMoversTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        MoverTabButton(
                            title = "Most Active",
                            isSelected = selectedMoversTab == 2,
                            onClick = { selectedMoversTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // List of Movers
            items(displayedStocks, key = { it.symbol }) { stock ->
                StockRowCard(
                    stock = stock,
                    isInWatchlist = watchlistSymbols.contains(stock.symbol),
                    onStockClick = { onStockClick(stock.symbol) },
                    onWatchlistToggle = { onWatchlistToggle(stock.symbol) },
                    onSetAlertClick = { onSetAlertClick(stock.symbol) }
                )
            }
        }
    }
}

@Composable
private fun IndexCard(index: MarketIndex, modifier: Modifier = Modifier) {
    val isPositive = index.isPositive
    val accentColor = if (isPositive) ProfitGreen else LossRed

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = modifier.width(180.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = index.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = index.exchange.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format("%,.2f", index.currentValue),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = String.format("%+,.2f (%+.2f%%)", index.change, index.changePercent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Advances / Declines ratio bar
            val total = index.advances + index.declines
            val advPct = if (total > 0) (index.advances.toFloat() / total) * 100f else 50f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(advPct.coerceAtLeast(1f))
                        .fillMaxSize()
                        .background(ProfitGreen)
                )
                Box(
                    modifier = Modifier
                        .weight((100f - advPct).coerceAtLeast(1f))
                        .fillMaxSize()
                        .background(LossRed)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Adv: ${index.advances}", fontSize = 9.sp, color = ProfitGreen)
                Text("Dec: ${index.declines}", fontSize = 9.sp, color = LossRed)
            }
        }
    }
}

@Composable
private fun SectorMiniCard(
    title: String,
    sector: SectorPerformance,
    isGainer: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isGainer) ProfitGreenSubtle else LossRedSubtle
    val color = if (isGainer) ProfitGreen else LossRed

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("%+.2f%%", sector.changePercent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sector.indexSymbol,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimaryDark
            )
            val topStock = if (isGainer) sector.topGainers.firstOrNull() else sector.topLosers.firstOrNull()
            if (topStock != null) {
                Text(
                    text = "${topStock.symbol}: ${String.format("%+.2f%%", topStock.changePercent)}",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

@Composable
private fun MoverTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondaryDark
            )
        }
    }
}

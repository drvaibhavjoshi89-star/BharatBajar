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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.SectorPerformance
import com.example.data.model.Stock
import com.example.ui.components.StockRowCard
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSubtle
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun SectoralMoversScreen(
    sectors: List<SectorPerformance>,
    allStocks: List<Stock>,
    watchlistSymbols: Set<String>,
    onStockClick: (String) -> Unit,
    onWatchlistToggle: (String) -> Unit,
    onSetAlertClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableIntStateOf(0) } // 0: Sector-Based Gainers & Losers, 1: Sector Heatmap Matrix, 2: All Market
    var selectedSectorFilter by remember { mutableStateOf<String?>(null) }

    val topGainerSector = sectors.firstOrNull()
    val topLoserSector = sectors.lastOrNull()

    val nearHighs = remember(allStocks) {
        allStocks.filter { (it.yearHigh - it.currentPrice) / it.yearHigh < 0.08 }
    }
    val nearLows = remember(allStocks) {
        allStocks.filter { (it.currentPrice - it.yearLow) / it.yearLow < 0.20 }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "Sectoral Indices & Movers",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Top gainers & losers driven by leading and lagging sectors",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // View Mode Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(10.dp))
                    .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { viewMode = 0 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewMode == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Leading Sectors",
                            fontSize = 12.sp,
                            fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == 0) Color.White else TextSecondaryDark
                        )
                    }
                }

                Surface(
                    onClick = { viewMode = 1 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewMode == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sector Matrix",
                            fontSize = 12.sp,
                            fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == 1) Color.White else TextSecondaryDark
                        )
                    }
                }

                Surface(
                    onClick = { viewMode = 2 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewMode == 2) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Market 52W",
                            fontSize = 12.sp,
                            fontWeight = if (viewMode == 2) FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == 2) Color.White else TextSecondaryDark
                        )
                    }
                }
            }
        }

        if (viewMode == 0) {
            // SECTION 1: TOP GAINER SECTOR & ITS GAINERS
            if (topGainerSector != null) {
                item {
                    SectorLeaderCard(
                        sector = topGainerSector,
                        isGainer = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Gainers in ${topGainerSector.sectorName} (${topGainerSector.indexSymbol})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ProfitGreen
                        )
                    }
                }

                items(topGainerSector.topGainers, key = { "gainer_${it.symbol}" }) { stock ->
                    StockRowCard(
                        stock = stock,
                        isInWatchlist = watchlistSymbols.contains(stock.symbol),
                        onStockClick = { onStockClick(stock.symbol) },
                        onWatchlistToggle = { onWatchlistToggle(stock.symbol) },
                        onSetAlertClick = { onSetAlertClick(stock.symbol) }
                    )
                }
            }

            // SECTION 2: TOP LOSER SECTOR & ITS LOSERS
            if (topLoserSector != null) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectorLeaderCard(
                        sector = topLoserSector,
                        isGainer = false
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Losers in ${topLoserSector.sectorName} (${topLoserSector.indexSymbol})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = LossRed
                        )
                    }
                }

                items(topLoserSector.topLosers, key = { "loser_${it.symbol}" }) { stock ->
                    StockRowCard(
                        stock = stock,
                        isInWatchlist = watchlistSymbols.contains(stock.symbol),
                        onStockClick = { onStockClick(stock.symbol) },
                        onWatchlistToggle = { onWatchlistToggle(stock.symbol) },
                        onSetAlertClick = { onSetAlertClick(stock.symbol) }
                    )
                }
            }

            // Quick sector filter chips
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explore Other Sectoral Constituents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sectors.take(5).forEach { s ->
                        val isSelected = selectedSectorFilter == s.sectorName
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSectorFilter = if (isSelected) null else s.sectorName
                            },
                            label = { Text(s.sectorName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (selectedSectorFilter != null) {
                val filteredSector = sectors.firstOrNull { it.sectorName == selectedSectorFilter }
                if (filteredSector != null) {
                    item {
                        Text(
                            text = "${filteredSector.sectorName} Stocks (${String.format("%+.2f%%", filteredSector.changePercent)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimaryDark
                        )
                    }
                    val stocksInSector = allStocks.filter { it.sector == filteredSector.sectorName }
                    items(stocksInSector, key = { "sector_${it.symbol}" }) { stock ->
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

        } else if (viewMode == 1) {
            // SECTOR MATRIX: Ranked leaderboard of all sectors
            item {
                Text(
                    text = "All Sectoral Indices Ranked by Performance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
            }

            items(sectors, key = { it.indexSymbol }) { sector ->
                val isPositive = sector.changePercent >= 0
                val color = if (isPositive) ProfitGreen else LossRed

                Surface(
                    onClick = {
                        selectedSectorFilter = sector.sectorName
                        viewMode = 0
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sector.indexSymbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = sector.sectorName,
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Adv: ${sector.advances} • Dec: ${sector.declines}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%+.2f%%", sector.changePercent),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }
        } else {
            // 52-WEEK HIGH / LOW BREAKOUTS
            item {
                Text(
                    text = "Stocks Near 52-Week High (Breakout Candidates)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ProfitGreen
                )
            }
            items(nearHighs, key = { "high_${it.symbol}" }) { stock ->
                StockRowCard(
                    stock = stock,
                    isInWatchlist = watchlistSymbols.contains(stock.symbol),
                    onStockClick = { onStockClick(stock.symbol) },
                    onWatchlistToggle = { onWatchlistToggle(stock.symbol) },
                    onSetAlertClick = { onSetAlertClick(stock.symbol) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Stocks Near 52-Week Low (Value Candidates)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LossRed
                )
            }
            items(nearLows, key = { "low_${it.symbol}" }) { stock ->
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
private fun SectorLeaderCard(
    sector: SectorPerformance,
    isGainer: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isGainer) ProfitGreenSubtle else LossRedSubtle
    val accentColor = if (isGainer) ProfitGreen else LossRed
    val badgeLabel = if (isGainer) "🔥 TOP GAINER SECTOR TODAY" else "❄️ TOP LAGGING SECTOR TODAY"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGainer) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%+.2f%%", sector.changePercent),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sector.indexSymbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Sector: ${sector.sectorName}",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Advances: ${sector.advances}",
                        fontSize = 11.sp,
                        color = ProfitGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Declines: ${sector.declines}",
                        fontSize = 11.sp,
                        color = LossRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

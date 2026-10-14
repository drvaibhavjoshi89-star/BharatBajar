package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleStick
import com.example.data.model.Stock
import com.example.data.model.TimeFrame
import com.example.ui.components.InteractiveStockChart
import com.example.ui.components.OrderBookDepthCard
import com.example.ui.components.StockFinancialMetricsCard
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stock: Stock,
    isInWatchlist: Boolean,
    onBackClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    onSetAlertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimeFrame by remember { mutableStateOf(TimeFrame.ONE_DAY) }

    val candles = remember(stock, selectedTimeFrame) {
        stock.historicalCandles[selectedTimeFrame] ?: emptyList()
    }

    val isPositive = stock.change >= 0
    val trendColor = if (isPositive) ProfitGreen else LossRed

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stock.symbol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryDark
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = stock.exchange.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = stock.companyName,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryDark
                    )
                }
            },
            actions = {
                IconButton(onClick = onSetAlertClick, modifier = Modifier.testTag("detail_set_alert_button")) {
                    Icon(
                        imageVector = Icons.Default.AddAlert,
                        contentDescription = "Set Alert",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onWatchlistToggle, modifier = Modifier.testTag("detail_toggle_watchlist_button")) {
                    Icon(
                        imageVector = if (isInWatchlist) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Watchlist",
                        tint = if (isInWatchlist) Color(0xFFFBBF24) else TextSecondaryDark
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0A0E17)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            // Live Price Display Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = String.format("₹%.2f", stock.currentPrice),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = trendColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%+,.2f (%+.2f%%)", stock.change, stock.changePercent),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = trendColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Today",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "Sector: ${stock.sector}",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Candlestick / Area Chart
            item {
                InteractiveStockChart(
                    candles = candles,
                    selectedTimeFrame = selectedTimeFrame,
                    onTimeFrameSelected = { selectedTimeFrame = it },
                    modifier = Modifier.testTag("interactive_stock_chart")
                )
            }

            // Action Quick Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSetAlertClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Price Alert", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onWatchlistToggle,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isInWatchlist) Color(0xFFFBBF24) else TextPrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isInWatchlist) "In Watchlist" else "Add Watchlist", fontSize = 12.sp)
                    }
                }
            }

            // Market Depth (Level 2 Order Book)
            if (stock.orderBook != null) {
                item {
                    OrderBookDepthCard(orderBook = stock.orderBook)
                }
            }

            // Financial Metrics & Fundamentals
            item {
                StockFinancialMetricsCard(stock = stock)
            }
        }
    }
}

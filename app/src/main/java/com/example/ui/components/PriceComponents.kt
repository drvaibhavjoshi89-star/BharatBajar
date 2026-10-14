package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exchange
import com.example.data.model.Stock
import com.example.data.repository.MarketStatusInfo
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSubtle
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun MarketStatusHeader(
    statusInfo: MarketStatusInfo,
    isSimulationActive: Boolean,
    isInternetSyncing: Boolean = false,
    lastSyncTime: Long? = null,
    isInternetLive: Boolean = true,
    dataSourceLabel: String = "NSE India & Yahoo Finance",
    onToggleSimulation: () -> Unit,
    onManualSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dotColor = if (isInternetLive || isSimulationActive) ProfitGreen else Color(0xFFEF4444)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isInternetLive) dataSourceLabel.uppercase() else "LIVE TICKER ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = dotColor,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF4285F4).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Multi-Source Live",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        val syncLabel = if (lastSyncTime != null) {
                            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            "Synced ${sdf.format(java.util.Date(lastSyncTime))} • ${statusInfo.statusText}"
                        } else {
                            "${statusInfo.statusText} (${statusInfo.nextSession})"
                        }
                        Text(
                            text = syncLabel,
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Yahoo Finance Sync & Simulation button
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onManualSync,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isInternetSyncing) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                    contentDescription = "Sync Yahoo Finance",
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "Sync",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleSimulation,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSimulationActive) ProfitGreenSubtle else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSimulationActive) ProfitGreen.copy(alpha = 0.5f) else CardBorderDark
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSimulationActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Toggle Ticker",
                                tint = if (isSimulationActive) ProfitGreen else TextSecondaryDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isSimulationActive) "Live" else "Pause",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSimulationActive) ProfitGreen else TextSecondaryDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockRowCard(
    stock: Stock,
    isInWatchlist: Boolean,
    onStockClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    onSetAlertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tick flash animation
    val tickColor by animateColorAsState(
        targetValue = when (stock.lastTickUp) {
            true -> ProfitGreen.copy(alpha = 0.25f)
            false -> LossRed.copy(alpha = 0.25f)
            null -> SurfaceDark
        },
        animationSpec = tween(durationMillis = 350),
        label = "tickFlash"
    )

    Surface(
        onClick = onStockClick,
        shape = RoundedCornerShape(12.dp),
        color = tickColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Symbol, Company Name, Sector Badge, Exchange
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stock.symbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark
                    )
                    // Exchange Tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Text(
                            text = stock.exchange.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    // Sector Tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = stock.sector,
                            fontSize = 9.sp,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stock.companyName,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    maxLines = 1
                )
            }

            // Right: Price and % Change Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("₹%.2f", stock.currentPrice),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    PriceChangePill(
                        change = stock.change,
                        changePercent = stock.changePercent
                    )
                }

                // Quick Watchlist Star
                IconButton(
                    onClick = onWatchlistToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isInWatchlist) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Watchlist",
                        tint = if (isInWatchlist) Color(0xFFFBBF24) else TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PriceChangePill(
    change: Double,
    changePercent: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = change >= 0
    val bgColor = if (isPositive) ProfitGreenSubtle else LossRedSubtle
    val contentColor = if (isPositive) ProfitGreen else LossRed

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = String.format("%+.2f%%", changePercent),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

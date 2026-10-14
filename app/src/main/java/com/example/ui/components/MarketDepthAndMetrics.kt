package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderBook
import com.example.data.model.Stock
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSubtle
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlin.math.max

@Composable
fun OrderBookDepthCard(
    orderBook: OrderBook,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Market Depth (L2 Order Book)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Top 5 Bids / Asks",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                // Bid side (Buy)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Orders", fontSize = 10.sp, color = TextSecondaryDark, modifier = Modifier.weight(1f))
                    Text("Qty", fontSize = 10.sp, color = TextSecondaryDark, modifier = Modifier.weight(1.2f))
                    Text("Bid (₹)", fontSize = 10.sp, color = ProfitGreen, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Ask side (Sell)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ask (₹)", fontSize = 10.sp, color = LossRed, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                    Text("Qty", fontSize = 10.sp, color = TextSecondaryDark, modifier = Modifier.weight(1.2f))
                    Text("Orders", fontSize = 10.sp, color = TextSecondaryDark, modifier = Modifier.weight(1f))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = CardBorderDark
            )

            // Rows
            val maxRowQty = max(
                orderBook.bids.maxOfOrNull { it.quantity } ?: 1,
                orderBook.asks.maxOfOrNull { it.quantity } ?: 1
            ).toFloat()

            val rowsCount = max(orderBook.bids.size, orderBook.asks.size)
            for (i in 0 until rowsCount) {
                val bid = orderBook.bids.getOrNull(i)
                val ask = orderBook.asks.getOrNull(i)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bid Row with subtle depth bar
                    Box(modifier = Modifier.weight(1f)) {
                        if (bid != null) {
                            val bidFraction = (bid.quantity / maxRowQty).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bidFraction)
                                    .height(18.dp)
                                    .align(Alignment.CenterEnd)
                                    .background(ProfitGreenSubtle, RoundedCornerShape(2.dp))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${bid.orders}", fontSize = 11.sp, color = TextSecondaryDark, modifier = Modifier.weight(1f))
                                Text("${bid.quantity}", fontSize = 11.sp, color = TextPrimaryDark, modifier = Modifier.weight(1.2f))
                                Text(String.format("%.2f", bid.price), fontSize = 11.sp, color = ProfitGreen, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Ask Row with subtle depth bar
                    Box(modifier = Modifier.weight(1f)) {
                        if (ask != null) {
                            val askFraction = (ask.quantity / maxRowQty).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(askFraction)
                                    .height(18.dp)
                                    .align(Alignment.CenterStart)
                                    .background(LossRedSubtle, RoundedCornerShape(2.dp))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(String.format("%.2f", ask.price), fontSize = 11.sp, color = LossRed, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
                                Text("${ask.quantity}", fontSize = 11.sp, color = TextPrimaryDark, modifier = Modifier.weight(1.2f))
                                Text("${ask.orders}", fontSize = 11.sp, color = TextSecondaryDark, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = CardBorderDark
            )

            // Total Volume Comparison Bar
            val total = orderBook.totalBuyQty + orderBook.totalSellQty
            val buyPct = if (total > 0) (orderBook.totalBuyQty.toFloat() / total) * 100f else 50f
            val sellPct = 100f - buyPct

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Buy: ${orderBook.totalBuyQty} (${String.format("%.1f", buyPct)}%)",
                        fontSize = 11.sp,
                        color = ProfitGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Total Sell: ${orderBook.totalSellQty} (${String.format("%.1f", sellPct)}%)",
                        fontSize = 11.sp,
                        color = LossRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(buyPct.coerceAtLeast(1f))
                            .fillMaxHeight()
                            .background(ProfitGreen)
                    )
                    Box(
                        modifier = Modifier
                            .weight(sellPct.coerceAtLeast(1f))
                            .fillMaxHeight()
                            .background(LossRed)
                    )
                }
            }
        }
    }
}

@Composable
fun RangeSliderBar(
    title: String,
    low: Double,
    current: Double,
    high: Double,
    modifier: Modifier = Modifier
) {
    val range = max(0.01, high - low)
    val fraction = ((current - low) / range).coerceIn(0.0, 1.0).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 12.sp, color = TextSecondaryDark, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(String.format("₹%.2f", low), fontSize = 11.sp, color = TextSecondaryDark)
            Text(String.format("₹%.2f", current), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Text(String.format("₹%.2f", high), fontSize = 11.sp, color = TextSecondaryDark)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Color(0xFF3B82F6), RoundedCornerShape(3.dp))
            )
            // Pin marker for current price
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.CenterStart)
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color(0xFF1D4ED8), CircleShape)
            )
        }
    }
}

@Composable
fun StockFinancialMetricsCard(
    stock: Stock,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Performance & Fundamentals",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            RangeSliderBar(
                title = "Today's Range",
                low = stock.dayLow,
                current = stock.currentPrice,
                high = stock.dayHigh
            )

            Spacer(modifier = Modifier.height(12.dp))

            RangeSliderBar(
                title = "52-Week Range",
                low = stock.yearLow,
                current = stock.currentPrice,
                high = stock.yearHigh
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = CardBorderDark
            )

            // 2x3 Grid of key metrics
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem("Open", String.format("₹%.2f", stock.openPrice), Modifier.weight(1f))
                MetricItem("Prev. Close", String.format("₹%.2f", stock.previousClose), Modifier.weight(1f))
                MetricItem("Volume", formatVol(stock.volume), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem("Mkt Cap", String.format("₹%.1f Cr", stock.marketCapCrores), Modifier.weight(1f))
                MetricItem("P/E Ratio", String.format("%.2f", stock.peRatio), Modifier.weight(1f))
                MetricItem("Div Yield", String.format("%.2f%%", stock.dividendYield), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = TextSecondaryDark)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
    }
}

private fun formatVol(vol: Long): String {
    return when {
        vol >= 10000000 -> String.format("%.2f Cr", vol / 10000000.0)
        vol >= 100000 -> String.format("%.2f L", vol / 100000.0)
        else -> vol.toString()
    }
}

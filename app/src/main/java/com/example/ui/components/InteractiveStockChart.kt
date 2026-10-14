package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleStick
import com.example.data.model.ChartType
import com.example.data.model.TimeFrame
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun InteractiveStockChart(
    candles: List<CandleStick>,
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    var chartType by remember { mutableStateOf(ChartType.AREA_LINE) }
    var showEma by remember { mutableStateOf(true) }
    var showVolume by remember { mutableStateOf(true) }
    var scrubbedCandle by remember { mutableStateOf<CandleStick?>(null) }
    var scrubX by remember { mutableStateOf<Float?>(null) }

    val isPositiveOverall = if (candles.size >= 2) {
        candles.last().close >= candles.first().open
    } else true

    val primaryColor = if (isPositiveOverall) ProfitGreen else LossRed

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Chart Header Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chart type switchers
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = { chartType = ChartType.AREA_LINE },
                    shape = RoundedCornerShape(8.dp),
                    color = if (chartType == ChartType.AREA_LINE) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Line Chart",
                            tint = if (chartType == ChartType.AREA_LINE) Color.White else TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = { chartType = ChartType.CANDLESTICK },
                    shape = RoundedCornerShape(8.dp),
                    color = if (chartType == ChartType.CANDLESTICK) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CandlestickChart,
                            contentDescription = "Candlestick Chart",
                            tint = if (chartType == ChartType.CANDLESTICK) Color.White else TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Indicator toggles
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = showEma,
                    onClick = { showEma = !showEma },
                    label = { Text("EMA 20", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = showVolume,
                    onClick = { showVolume = !showVolume },
                    label = { Text("VOL", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFA78BFA).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFA78BFA)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrubber Inspection Banner
        val activeCandle = scrubbedCandle ?: candles.lastOrNull()
        if (activeCandle != null) {
            val change = activeCandle.close - activeCandle.open
            val pct = if (activeCandle.open > 0) (change / activeCandle.open) * 100 else 0.0
            val candleColor = if (change >= 0) ProfitGreen else LossRed

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format("₹%.2f", activeCandle.close),
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%+.2f (%+.2f%%)", change, pct),
                            color = candleColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Time: ${activeCandle.timeLabel} • Vol: ${formatVolume(activeCandle.volume)}",
                        color = TextSecondaryDark,
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailPill("O", activeCandle.open)
                    DetailPill("H", activeCandle.high)
                    DetailPill("L", activeCandle.low)
                    DetailPill("C", activeCandle.close)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Chart Drawing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .pointerInput(candles) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            scrubX = offset.x
                            scrubbedCandle = getCandleAtX(offset.x, size.width.toFloat(), candles)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            scrubX = change.position.x
                            scrubbedCandle = getCandleAtX(change.position.x, size.width.toFloat(), candles)
                        },
                        onDragEnd = {
                            scrubX = null
                            scrubbedCandle = null
                        },
                        onDragCancel = {
                            scrubX = null
                            scrubbedCandle = null
                        }
                    )
                }
                .pointerInput(candles) {
                    detectTapGestures(
                        onPress = { offset ->
                            scrubX = offset.x
                            scrubbedCandle = getCandleAtX(offset.x, size.width.toFloat(), candles)
                            tryAwaitRelease()
                            scrubX = null
                            scrubbedCandle = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (candles.isEmpty()) return@Canvas

                val minPrice = candles.minOf { it.low } * 0.998
                val maxPrice = candles.maxOf { it.high } * 1.002
                val priceRange = max(0.01, maxPrice - minPrice)

                val maxVolume = candles.maxOfOrNull { it.volume } ?: 1L

                val chartHeight = if (showVolume) size.height * 0.78f else size.height
                val volumeHeight = if (showVolume) size.height * 0.20f else 0f
                val volumeTop = size.height - volumeHeight

                val count = candles.size
                val stepX = size.width / max(1, count)

                // Draw background grid lines (horizontal price lines)
                val gridLines = 4
                for (g in 0..gridLines) {
                    val y = chartHeight * (g.toFloat() / gridLines)
                    val priceVal = maxPrice - (g.toDouble() / gridLines) * priceRange
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Draw Volume bars
                if (showVolume && maxVolume > 0) {
                    for (i in candles.indices) {
                        val c = candles[i]
                        val x = i * stepX + stepX / 2f
                        val barHeight = (c.volume.toFloat() / maxVolume) * volumeHeight
                        val barTop = size.height - barHeight
                        val barColor = if (c.close >= c.open) ProfitGreen.copy(alpha = 0.35f) else LossRed.copy(alpha = 0.35f)
                        val barWidth = max(2f, stepX * 0.7f)

                        drawRect(
                            color = barColor,
                            topLeft = Offset(x - barWidth / 2f, barTop),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }

                // Render based on chart type
                if (chartType == ChartType.CANDLESTICK) {
                    for (i in candles.indices) {
                        val c = candles[i]
                        val x = i * stepX + stepX / 2f

                        val yOpen = (chartHeight - ((c.open - minPrice) / priceRange) * chartHeight).toFloat()
                        val yClose = (chartHeight - ((c.close - minPrice) / priceRange) * chartHeight).toFloat()
                        val yHigh = (chartHeight - ((c.high - minPrice) / priceRange) * chartHeight).toFloat()
                        val yLow = (chartHeight - ((c.low - minPrice) / priceRange) * chartHeight).toFloat()

                        val isUp = c.close >= c.open
                        val candleColor = if (isUp) ProfitGreen else LossRed

                        // Draw wick
                        drawLine(
                            color = candleColor,
                            start = Offset(x, yHigh),
                            end = Offset(x, yLow),
                            strokeWidth = 2f
                        )

                        // Draw body
                        val candleWidth = max(3f, stepX * 0.75f)
                        val top = min(yOpen, yClose)
                        val bodyHeight = max(2f, abs(yClose - yOpen))

                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x - candleWidth / 2f, top),
                            size = Size(candleWidth, bodyHeight)
                        )
                    }
                } else {
                    // Area & Line Chart
                    val linePath = Path()
                    val areaPath = Path()

                    for (i in candles.indices) {
                        val c = candles[i]
                        val x = i * stepX + stepX / 2f
                        val y = (chartHeight - ((c.close - minPrice) / priceRange) * chartHeight).toFloat()

                        if (i == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, chartHeight)
                            areaPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                    }

                    if (candles.isNotEmpty()) {
                        val lastX = (candles.size - 1) * stepX + stepX / 2f
                        areaPath.lineTo(lastX, chartHeight)
                        areaPath.close()

                        // Draw area gradient
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Draw line
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }
                }

                // Draw EMA 20 overlay line
                if (showEma && candles.size >= 5) {
                    val emaPeriod = min(20, candles.size)
                    val k = 2.0 / (emaPeriod + 1)
                    val emaValues = mutableListOf<Double>()
                    var ema = candles.take(emaPeriod).map { it.close }.average()
                    for (i in candles.indices) {
                        ema = candles[i].close * k + ema * (1 - k)
                        emaValues.add(ema)
                    }

                    val emaPath = Path()
                    for (i in emaValues.indices) {
                        val x = i * stepX + stepX / 2f
                        val y = (chartHeight - ((emaValues[i] - minPrice) / priceRange) * chartHeight).toFloat()
                        if (i == 0) emaPath.moveTo(x, y) else emaPath.lineTo(x, y)
                    }
                    drawPath(
                        path = emaPath,
                        color = Color(0xFF38BDF8),
                        style = Stroke(width = 2f, cap = StrokeCap.Round)
                    )
                }

                // Crosshair cursor & scrubber point
                scrubX?.let { sx ->
                    val clampedX = sx.coerceIn(0f, size.width)
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(clampedX, 0f),
                        end = Offset(clampedX, size.height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    scrubbedCandle?.let { sc ->
                        val y = (chartHeight - ((sc.close - minPrice) / priceRange) * chartHeight).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(clampedX, y)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5f,
                            center = Offset(clampedX, y)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Timeframe Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeFrame.values().forEach { tf ->
                val isSelected = tf == selectedTimeFrame
                Surface(
                    onClick = { onTimeFrameSelected(tf) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = tf.label,
                            color = if (isSelected) Color.White else TextSecondaryDark,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPill(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(text = String.format("%.1f", value), color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun getCandleAtX(x: Float, totalWidth: Float, candles: List<CandleStick>): CandleStick? {
    if (candles.isEmpty() || totalWidth <= 0f) return null
    val step = totalWidth / candles.size
    val index = (x / step).toInt().coerceIn(0, candles.size - 1)
    return candles[index]
}

private fun formatVolume(vol: Long): String {
    return when {
        vol >= 10000000 -> String.format("%.2f Cr", vol / 10000000.0)
        vol >= 100000 -> String.format("%.2f L", vol / 100000.0)
        vol >= 1000 -> String.format("%.1f K", vol / 1000.0)
        else -> vol.toString()
    }
}

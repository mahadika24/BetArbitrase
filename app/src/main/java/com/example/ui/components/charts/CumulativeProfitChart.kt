package com.example.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stats.ProfitPoint
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import kotlin.math.max
import kotlin.math.min

@Composable
fun CumulativeProfitChart(
    points: List<ProfitPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = QuantCyan,
    showScrubber: Boolean = true
) {
    if (points.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth().height(220.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No observations available to plot cumulative profit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val minProfit = min(0.0, points.minOf { it.cumulativeProfit })
    val maxProfit = max(0.0, points.maxOf { it.cumulativeProfit })
    val totalRange = if (maxProfit - minProfit < 1.0) 100.0 else maxProfit - minProfit
    val paddingBuffer = totalRange * 0.1
    val yMin = minProfit - paddingBuffer
    val yMax = maxProfit + paddingBuffer
    val ySpan = yMax - yMin

    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val gridColor = outlineColor.copy(alpha = 0.35f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cumulative_profit_chart_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cumulative Profit Curve (Chronological)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${points.size} observations | Strict chronological backtest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val finalPt = points.last()
                val isNetPositive = finalPt.cumulativeProfit >= 0
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isNetPositive) QuantEmerald.copy(alpha = 0.15f) else QuantRose.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${if (isNetPositive) "+" else ""}${String.format("$%.0f", finalPt.cumulativeProfit)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isNetPositive) QuantEmerald else QuantRose,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            if (showScrubber) {
                                detectTapGestures { offset ->
                                    val pct = (offset.x / size.width).coerceIn(0f, 1f)
                                    selectedIndex = (pct * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                }
                            }
                        }
                        .pointerInput(points) {
                            if (showScrubber) {
                                detectDragGestures { change, _ ->
                                    val pct = (change.position.x / size.width).coerceIn(0f, 1f)
                                    selectedIndex = (pct * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    fun xCoord(i: Int): Float {
                        return if (points.size <= 1) w / 2f else (i.toFloat() / (points.size - 1)) * w
                    }

                    fun yCoord(value: Double): Float {
                        val norm = (value - yMin) / ySpan
                        return (h - (norm * h)).toFloat()
                    }

                    // Draw 3 horizontal grid lines (yMin, 0 baseline, yMax)
                    val yZero = yCoord(0.0)
                    drawLine(
                        color = outlineColor.copy(alpha = 0.6f),
                        start = Offset(0f, yZero),
                        end = Offset(w, yZero),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    val yHalfTop = yCoord(maxProfit / 2.0)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, yHalfTop),
                        end = Offset(w, yHalfTop),
                        strokeWidth = 1f
                    )

                    // Path for the curve
                    val linePath = Path()
                    val areaPath = Path()

                    points.forEachIndexed { i, pt ->
                        val x = xCoord(i)
                        val y = yCoord(pt.cumulativeProfit)
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, yZero)
                            areaPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                    }

                    areaPath.lineTo(xCoord(points.size - 1), yZero)
                    areaPath.close()

                    // Fill gradient under curve
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f)),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Stroke line
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw scrubber if active
                    val activeIndex = selectedIndex
                    if (activeIndex != null && activeIndex in points.indices) {
                        val activeX = xCoord(activeIndex)
                        val activeY = yCoord(points[activeIndex].cumulativeProfit)

                        drawLine(
                            color = onSurfaceColor,
                            start = Offset(activeX, 0f),
                            end = Offset(activeX, h),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = Offset(activeX, activeY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(activeX, activeY)
                        )
                    }
                }

                // Scrubber readout tooltip
                val curIdx = selectedIndex
                if (curIdx != null && curIdx in points.indices) {
                    val p = points[curIdx]
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bet #${p.index} (${p.date}): ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (p.profit >= 0) "+" else ""}${String.format("$%.0f", p.profit)}  |  Total: ${if (p.cumulativeProfit >= 0) "+" else ""}${String.format("$%.0f", p.cumulativeProfit)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (p.cumulativeProfit >= 0) QuantEmerald else QuantRose,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stats.StrategyEvaluationReport
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import kotlin.math.abs
import kotlin.math.max

@Composable
fun StrategyComparisonBarChart(
    reports: List<StrategyEvaluationReport>,
    modifier: Modifier = Modifier
) {
    if (reports.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth().height(220.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No strategy evaluations available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // Determine max absolute ROI to center around 0%
    val maxAbsRoi = max(
        15.0,
        reports.maxOf { max(abs(it.roi), max(abs(it.bootstrapResult.lowerCi95), abs(it.bootstrapResult.upperCi95))) }
    ) * 1.15

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("strategy_comparison_chart_card"),
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
                        text = "Strategy ROI Comparison with 95% Bootstrap CI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Whiskers denote empirical 95% uncertainty interval",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val rowHeight = 36.dp
            val totalHeight = (rowHeight * reports.size) + 20.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val zeroX = w * 0.5f // center zero line

                    // Draw 0% center line
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(zeroX, 0f),
                        end = Offset(zeroX, h),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    fun xForRoi(roi: Double): Float {
                        val norm = (roi / maxAbsRoi).coerceIn(-1.0, 1.0)
                        return (zeroX + (norm * (w * 0.45f))).toFloat()
                    }

                    val barThickness = 14.dp.toPx()

                    reports.forEachIndexed { i, rep ->
                        val centerY = (i * rowHeight.toPx()) + (rowHeight.toPx() / 2f)
                        val barColor = if (rep.roi >= 0) QuantEmerald else QuantRose

                        // Bar from 0 to ROI
                        val roiX = xForRoi(rep.roi)
                        val leftX = minOf(zeroX, roiX)
                        val barWidth = abs(roiX - zeroX).coerceAtLeast(2f)

                        drawRect(
                            color = barColor.copy(alpha = 0.85f),
                            topLeft = Offset(leftX, centerY - (barThickness / 2f)),
                            size = Size(barWidth, barThickness)
                        )

                        // 95% Bootstrap CI Whisker
                        if (rep.sampleSize > 0) {
                            val ciLowerX = xForRoi(rep.bootstrapResult.lowerCi95)
                            val ciUpperX = xForRoi(rep.bootstrapResult.upperCi95)

                            // Error bar line
                            drawLine(
                                color = QuantCyan,
                                start = Offset(ciLowerX, centerY),
                                end = Offset(ciUpperX, centerY),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Left tick
                            drawLine(
                                color = QuantCyan,
                                start = Offset(ciLowerX, centerY - 6.dp.toPx()),
                                end = Offset(ciLowerX, centerY + 6.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Right tick
                            drawLine(
                                color = QuantCyan,
                                start = Offset(ciUpperX, centerY - 6.dp.toPx()),
                                end = Offset(ciUpperX, centerY + 6.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }

                // Strategy Title Labels overlay
                Column(modifier = Modifier.fillMaxSize()) {
                    reports.forEach { rep ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rep.strategyType.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${if (rep.roi >= 0) "+" else ""}${String.format("%.1f", rep.roi)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rep.roi >= 0) QuantEmerald else QuantRose
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

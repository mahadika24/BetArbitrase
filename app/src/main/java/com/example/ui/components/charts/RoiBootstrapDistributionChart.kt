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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stats.BootstrapResult
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import kotlin.math.max

@Composable
fun RoiBootstrapDistributionChart(
    bootstrap: BootstrapResult,
    modifier: Modifier = Modifier
) {
    val bins = bootstrap.histogramBins
    if (bins.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bootstrap data unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val maxPct = max(1.0, bins.maxOf { it.percentage })
    val minBinRoi = bins.first().binStart
    val maxBinRoi = bins.last().binEnd
    val roiSpan = if (maxBinRoi - minBinRoi <= 0.001) 1.0 else (maxBinRoi - minBinRoi)

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bootstrap_distribution_chart_card"),
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
                        text = "ROI Bootstrap Distribution (10,000 Resamples)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Text(
                        text = "Non-parametric empirical sampling | Seeded deterministic PRNG",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = QuantCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "95% CI: [${String.format("%.1f", bootstrap.lowerCi95)}%, ${String.format("%.1f", bootstrap.upperCi95)}%]",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = QuantCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val nBins = bins.size
                    val barSpacing = 2.dp.toPx()
                    val totalSpacing = barSpacing * (nBins - 1)
                    val barWidth = (w - totalSpacing) / nBins

                    fun xForRoi(roi: Double): Float {
                        val norm = ((roi - minBinRoi) / roiSpan).coerceIn(0.0, 1.0)
                        return (norm * w).toFloat()
                    }

                    // 1. Draw Shaded 95% Confidence Interval Background
                    val ciLeft = xForRoi(bootstrap.lowerCi95)
                    val ciRight = xForRoi(bootstrap.upperCi95)
                    drawRect(
                        color = QuantCyan.copy(alpha = 0.12f),
                        topLeft = Offset(ciLeft, 0f),
                        size = Size(ciRight - ciLeft, h)
                    )

                    // 2. Draw Histogram Bars
                    bins.forEachIndexed { i, bin ->
                        val barHeight = ((bin.percentage / maxPct) * h).toFloat()
                        val x = i * (barWidth + barSpacing)
                        val y = h - barHeight
                        val isInCi = bin.binEnd >= bootstrap.lowerCi95 && bin.binStart <= bootstrap.upperCi95
                        val isPositive = bin.binStart >= 0.0

                        val barColor = when {
                            !isPositive -> QuantRose.copy(alpha = 0.65f)
                            isInCi -> QuantCyan.copy(alpha = 0.85f)
                            else -> QuantCyan.copy(alpha = 0.4f)
                        }

                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )
                    }

                    // 3. Draw Null Hypothesis Line (ROI = 0%)
                    if (minBinRoi <= 0.0 && maxBinRoi >= 0.0) {
                        val xZero = xForRoi(0.0)
                        drawLine(
                            color = Color.White.copy(alpha = 0.85f),
                            start = Offset(xZero, 0f),
                            end = Offset(xZero, h),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // 4. Draw Observed ROI line
                    val xObs = xForRoi(bootstrap.observedRoi)
                    drawLine(
                        color = QuantEmerald,
                        start = Offset(xObs, 0f),
                        end = Offset(xObs, h),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend / Metrics Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Observed ROI: ${String.format("%.2f", bootstrap.observedRoi)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = QuantEmerald
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Bootstrap Median: ${String.format("%.2f", bootstrap.bootstrapMedianRoi)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "P(ROI > 0) = ${String.format("%.1f", bootstrap.probabilityPositiveRoi)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (bootstrap.probabilityPositiveRoi >= 95.0) QuantEmerald else QuantRose
                )
            }
        }
    }
}

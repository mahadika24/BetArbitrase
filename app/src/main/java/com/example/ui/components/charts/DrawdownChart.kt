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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stats.DrawdownAnalysis
import com.example.ui.theme.QuantRose
import kotlin.math.max

@Composable
fun DrawdownChart(
    analysis: DrawdownAnalysis,
    modifier: Modifier = Modifier
) {
    val points = analysis.cumulativeProfits
    if (points.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth().height(180.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No drawdown data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val maxDd = max(100.0, analysis.maxDrawdownAmount)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("drawdown_chart_card"),
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
                        text = "Underwater Drawdown Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Peak-to-trough equity drop | Chronological",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = QuantRose.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Max DD: -${String.format("$%.0f", analysis.maxDrawdownAmount)} (${String.format("%.1f", analysis.maxDrawdownPercent)}%)",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = QuantRose,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    fun xCoord(i: Int): Float {
                        return if (points.size <= 1) w / 2f else (i.toFloat() / (points.size - 1)) * w
                    }

                    fun yCoord(dd: Double): Float {
                        // Drawdown is positive drop from peak, so 0 is at top, maxDd is at bottom
                        val norm = (dd / maxDd).coerceIn(0.0, 1.0)
                        return (norm * (h - 10f)).toFloat()
                    }

                    // Baseline (0 drawdown line at top)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(0f, 0f),
                        end = Offset(w, 0f),
                        strokeWidth = 2f
                    )

                    val linePath = Path()
                    val areaPath = Path()

                    points.forEachIndexed { i, pt ->
                        val x = xCoord(i)
                        val y = yCoord(pt.drawdown)
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, 0f)
                            areaPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                    }

                    areaPath.lineTo(xCoord(points.size - 1), 0f)
                    areaPath.close()

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(QuantRose.copy(alpha = 0.35f), QuantRose.copy(alpha = 0.05f)),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = QuantRose,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Longest Losing Streak: ${analysis.longestLosingStreak} bets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Longest DD Phase: ${analysis.maxDrawdownDuration} bets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

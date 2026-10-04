package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResearchStrategyType
import com.example.ui.components.charts.CumulativeProfitChart
import com.example.ui.theme.QuantAmber
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import com.example.ui.viewmodel.ResearchUiState

@Composable
fun KnockoutAnalysisScreen(state: ResearchUiState) {
    val koReports = state.knockoutOnlyReports
    val koMatchesCount = state.matches.count { it.isKnockout }
    val totalMatches = state.matches.size

    val u35Report = koReports[ResearchStrategyType.UNDER_3_5]
    val o35Report = koReports[ResearchStrategyType.OVER_3_5]
    val u15HtReport = koReports[ResearchStrategyType.FIRST_HALF_UNDER_1_5]
    val drawReport = koReports[ResearchStrategyType.DRAW_1X2]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("knockout_analysis_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Knockout Isolation Header
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = QuantCyan.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = QuantCyan,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "KNOCKOUT-STAGE STRATEGY ISOLATION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Filtered strictly to Knockout Stage fixtures (UCL, Euros, World Cup). Non-knockout league games excluded to test whether cautious single-elimination tournament dynamics yield genuine structural market alpha.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Explicit sample size callout (satisfying prompt mandate)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Matches in Knockout Sample",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$koMatchesCount Matches ($totalMatches total in db)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "N = Observations",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuantCyan
                                )
                                Text(
                                    text = "N = ${u35Report?.sampleSize ?: 0} per strategy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = QuantCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Under 3.5 Core Hypothesis Deep Dive Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Primary Research Question: Under 3.5 Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Backing total match goals to be ≤ 3 in knockout ties.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (u35Report != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricBox(
                                title = "Knockout Win Rate",
                                value = "${String.format("%.1f", u35Report.winRate)}%",
                                sub = "Req break-even: ${String.format("%.1f", u35Report.portfolioBreakEvenRate)}%",
                                color = if (u35Report.winRate > u35Report.portfolioBreakEvenRate) QuantEmerald else QuantRose,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                title = "Knockout ROI",
                                value = "${if (u35Report.roi >= 0) "+" else ""}${String.format("%.2f", u35Report.roi)}%",
                                sub = "Total Net: ${if (u35Report.totalProfit >= 0) "+" else ""}${String.format("$%.0f", u35Report.totalProfit)}",
                                color = if (u35Report.roi >= 0) QuantEmerald else QuantRose,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricBox(
                                title = "Bootstrap 95% CI",
                                value = "[${String.format("%.1f", u35Report.bootstrapResult.lowerCi95)}%, ${String.format("%.1f", u35Report.bootstrapResult.upperCi95)}%]",
                                sub = if (u35Report.bootstrapResult.lowerCi95 > 0) "CI strictly positive" else "CI spans negative values",
                                color = if (u35Report.bootstrapResult.lowerCi95 > 0) QuantEmerald else QuantAmber,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                title = "Max Drawdown",
                                value = "-${String.format("$%.0f", u35Report.drawdownAnalysis.maxDrawdownAmount)}",
                                sub = "${String.format("%.1f", u35Report.drawdownAnalysis.maxDrawdownPercent)}% of stake",
                                color = QuantRose,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Knockout Strategy Comparison Table
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Knockout-Only Performance Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Evaluated exclusively across the $koMatchesCount knockout fixtures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Text("Strategy", modifier = Modifier.width(160.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("N", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Win %", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Break-Even", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Profit", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("ROI", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Status", modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        koReports.values.forEach { rep ->
                            val isPos = rep.roi >= 0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rep.strategyType.title, modifier = Modifier.width(160.dp), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("${rep.sampleSize}", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", rep.winRate)}%", modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                Text("${String.format("%.1f", rep.portfolioBreakEvenRate)}%", modifier = Modifier.width(90.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${if (isPos) "+" else ""}${String.format("$%.0f", rep.totalProfit)}", modifier = Modifier.width(90.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (isPos) QuantEmerald else QuantRose, style = MaterialTheme.typography.bodyMedium)
                                Text("${if (isPos) "+" else ""}${String.format("%.1f", rep.roi)}%", modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (isPos) QuantEmerald else QuantRose, style = MaterialTheme.typography.bodyMedium)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isPos) QuantEmerald.copy(alpha = 0.15f) else QuantRose.copy(alpha = 0.15f),
                                    modifier = Modifier.width(110.dp)
                                ) {
                                    Text(
                                        text = rep.classification.label.take(12),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPos) QuantEmerald else QuantRose,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Under 3.5 Knockout Cumulative Curve
        if (u35Report != null) {
            item {
                CumulativeProfitChart(
                    points = u35Report.drawdownAnalysis.cumulativeProfits,
                    lineColor = QuantCyan
                )
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    sub: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

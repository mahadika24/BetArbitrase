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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
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
import com.example.ui.components.charts.StrategyComparisonBarChart
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import com.example.ui.viewmodel.ResearchUiState

@Composable
fun SameSampleComparisonScreen(state: ResearchUiState) {
    val sameReports = state.sameSampleReports.values.toList()
    val sampleSizeN = sameReports.firstOrNull()?.sampleSize ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("same_sample_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Banner
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
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = QuantCyan,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "APPLE-TO-APPLE SAME-SAMPLE COMPARISON",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Eliminates survivorship and sample-selection bias by evaluating strategies strictly on the EXACT SAME identical set of matches ($sampleSizeN matches) where comparable observations exist across markets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = QuantCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Controlled Sample: Every strategy below has exactly N = $sampleSizeN observations.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Same-Sample Comparison Table
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Exact-Intersection Strategy Table (N = $sampleSizeN)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Text("Strategy", modifier = Modifier.width(150.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("N", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Win %", modifier = Modifier.width(75.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Avg Odds", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Profit", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("ROI", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Bootstrap 95% ROI CI", modifier = Modifier.width(170.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Max Drawdown", modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        sameReports.sortedByDescending { it.roi }.forEach { rep ->
                            val isPos = rep.roi >= 0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rep.strategyType.title, modifier = Modifier.width(150.dp), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("${rep.sampleSize}", modifier = Modifier.width(50.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Text("${String.format("%.1f", rep.winRate)}%", modifier = Modifier.width(75.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                Text(String.format("%.2f", rep.averageOdds), modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                Text("${if (isPos) "+" else ""}${String.format("$%.0f", rep.totalProfit)}", modifier = Modifier.width(85.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (isPos) QuantEmerald else QuantRose, style = MaterialTheme.typography.bodyMedium)
                                Text("${if (isPos) "+" else ""}${String.format("%.1f", rep.roi)}%", modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (isPos) QuantEmerald else QuantRose, style = MaterialTheme.typography.bodyMedium)
                                Text("[${String.format("%.1f", rep.bootstrapResult.lowerCi95)}%, ${String.format("%.1f", rep.bootstrapResult.upperCi95)}%]", modifier = Modifier.width(170.dp), fontFamily = FontFamily.Monospace, color = QuantCyan, style = MaterialTheme.typography.bodyMedium)
                                Text("-${String.format("$%.0f", rep.drawdownAnalysis.maxDrawdownAmount)}", modifier = Modifier.width(110.dp), fontFamily = FontFamily.Monospace, color = QuantRose, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // Same-Sample Comparison Bar Chart with Whiskers
        item {
            StrategyComparisonBarChart(reports = sameReports)
        }
    }
}

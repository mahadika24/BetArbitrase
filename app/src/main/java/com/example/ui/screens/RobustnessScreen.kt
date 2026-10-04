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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.stats.RobustnessScenario
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import com.example.ui.viewmodel.ResearchUiState

@Composable
fun RobustnessScreen(
    state: ResearchUiState,
    onStrategySelected: (ResearchStrategyType) -> Unit
) {
    val selectedStrategy = state.selectedStrategy
    val report = state.strategyReports[selectedStrategy]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("robustness_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Strategy Selection Chips
        item {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResearchStrategyType.values().forEach { type ->
                    FilterChip(
                        selected = selectedStrategy == type,
                        onClick = { onStrategySelected(type) },
                        label = { Text(type.title) }
                    )
                }
            }
        }

        if (report == null || report.sampleSize == 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Text("No data available for ${selectedStrategy.title} to test outlier robustness.")
                    }
                }
            }
            return@LazyColumn
        }

        val robustness = report.outlierRobustness

        // Outlier Header Card
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
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = QuantCyan,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "OUTLIER ROBUSTNESS & SENSITIVITY STRESS TEST",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Central Research Question: \"Does the apparent edge survive if unusually influential observations are removed?\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Many sports betting strategies appear profitable only because of 1 or 2 extreme freak longshots. If stripping the top 2 winners causes the strategy to collapse into negative returns, the historical edge was an outlier artifact rather than a repeatable market anomaly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Outlier Verdict Badge
        item {
            val survives = robustness.survivesOutlierRemoval
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (survives) QuantEmerald.copy(alpha = 0.15f) else QuantRose.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (survives) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (survives) QuantEmerald else QuantRose
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (survives) "Outlier Robustness Confirmed" else "Outlier Vulnerability Detected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (survives) QuantEmerald else QuantRose
                        )
                        Text(
                            text = if (survives)
                                "Strategy remains profitable (+${String.format("%.2f", robustness.minusTop2.roi)}% ROI) even when stripping the two most profitable match outcomes."
                            else
                                "Edge fails outlier test! Profitability drops to ${String.format("%.2f", robustness.minusTop2.roi)}% after removing top 2 outliers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Side-by-Side Scenarios Table
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Stress Test Scenarios Matrix (${selectedStrategy.title})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val scenarios = listOf(
                        robustness.baseline,
                        robustness.minusTop1,
                        robustness.minusTop2,
                        robustness.minusWorst1,
                        robustness.minusExtremeOdds
                    )

                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Text("Scenario", modifier = Modifier.width(170.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("N", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Stake", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Profit", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("ROI", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("ROI Impact", modifier = Modifier.width(95.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        scenarios.forEach { s ->
                            val isPos = s.roi >= 0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(170.dp)) {
                                    Text(s.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(s.description, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${s.sampleSize}", modifier = Modifier.width(50.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Text(String.format("$%.0f", s.totalStake), modifier = Modifier.width(80.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                Text("${if (s.totalProfit >= 0) "+" else ""}${String.format("$%.0f", s.totalProfit)}", modifier = Modifier.width(85.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (isPos) QuantEmerald else QuantRose, style = MaterialTheme.typography.bodyMedium)
                                Text("${if (isPos) "+" else ""}${String.format("%.2f", s.roi)}%", modifier = Modifier.width(85.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (isPos) QuantEmerald else QuantRose, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (s.deltaRoiFromBaseline == 0.0) "Baseline" else "${if (s.deltaRoiFromBaseline >= 0) "+" else ""}${String.format("%.2f", s.deltaRoiFromBaseline)}%",
                                    modifier = Modifier.width(95.dp),
                                    fontFamily = FontFamily.Monospace,
                                    color = if (s.deltaRoiFromBaseline >= 0) QuantEmerald else QuantRose,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

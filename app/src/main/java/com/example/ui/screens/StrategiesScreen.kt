package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import com.example.ui.components.charts.DrawdownChart
import com.example.ui.components.charts.RoiBootstrapDistributionChart
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import com.example.ui.viewmodel.ResearchUiState

@Composable
fun StrategiesScreen(
    state: ResearchUiState,
    onStrategySelected: (ResearchStrategyType) -> Unit
) {
    val selected = state.selectedStrategy
    val report = state.strategyReports[selected]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("strategies_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Strategy Selection Chips (Horizontally Scrollable)
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
                        selected = selected == type,
                        onClick = { onStrategySelected(type) },
                        label = { Text(type.title) },
                        modifier = Modifier.testTag("chip_${type.id}")
                    )
                }
            }
        }

        if (report == null || report.sampleSize == 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Text("No observations available for ${selected.title} under current filter.")
                    }
                }
            }
            return@LazyColumn
        }

        // Strategy Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = report.strategyType.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = QuantCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = report.strategyType.market,
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report.strategyType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Key Metrics 2x3 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Observations (N)",
                        value = "${report.sampleSize}",
                        subValue = "${report.matchesCount} unique matches",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Win Rate",
                        value = "${String.format("%.1f", report.winRate)}%",
                        subValue = "95% Wilson CI: [${String.format("%.1f", report.wilsonWinRateCi.lowerBound)}%, ${String.format("%.1f", report.wilsonWinRateCi.upperBound)}%]",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Average / Median Odds",
                        value = "${String.format("%.2f", report.averageOdds)} / ${String.format("%.2f", report.medianOdds)}",
                        subValue = "Break-even win rate: ${String.format("%.1f", report.portfolioBreakEvenRate)}%",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Total Profit & Stake",
                        value = "${if (report.totalProfit >= 0) "+" else ""}${String.format("$%.0f", report.totalProfit)}",
                        subValue = "Staked: ${String.format("$%.0f", report.totalStake)}",
                        valueColor = if (report.totalProfit >= 0) QuantEmerald else QuantRose,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Historical ROI",
                        value = "${if (report.roi >= 0) "+" else ""}${String.format("%.2f", report.roi)}%",
                        subValue = "Bootstrap Median: ${String.format("%.1f", report.bootstrapResult.bootstrapMedianRoi)}%",
                        valueColor = if (report.roi >= 0) QuantEmerald else QuantRose,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Max Drawdown / Losing Streak",
                        value = "-${String.format("$%.0f", report.drawdownAnalysis.maxDrawdownAmount)} (${String.format("%.1f", report.drawdownAnalysis.maxDrawdownPercent)}%)",
                        subValue = "Longest streak: ${report.drawdownAnalysis.longestLosingStreak} losses",
                        valueColor = QuantRose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Statistical Distinction Notice
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Statistical Distinction Note:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = QuantCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Win Rate 95% Confidence Interval [${String.format("%.1f", report.wilsonWinRateCi.lowerBound)}%, ${String.format("%.1f", report.wilsonWinRateCi.upperBound)}%] is calculated via Wilson Score.\n• ROI Uncertainty is calculated via 10,000 Bootstrap Resamples [${String.format("%.1f", report.bootstrapResult.lowerCi95)}%, ${String.format("%.1f", report.bootstrapResult.upperCi95)}%]. Win rate binomial intervals must NEVER be conflated with ROI confidence intervals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Chart 1: Cumulative Profit Curve
        item {
            CumulativeProfitChart(points = report.drawdownAnalysis.cumulativeProfits)
        }

        // Chart 2: Bootstrap ROI Histogram Distribution
        item {
            RoiBootstrapDistributionChart(bootstrap = report.bootstrapResult)
        }

        // Chart 3: Underwater Drawdown Profile
        item {
            DrawdownChart(analysis = report.drawdownAnalysis)
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    subValue: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

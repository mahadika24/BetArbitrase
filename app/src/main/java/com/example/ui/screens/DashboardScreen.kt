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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResearchStrategyType
import com.example.stats.StrategyEvaluationReport
import com.example.ui.components.charts.CumulativeProfitChart
import com.example.ui.components.charts.StrategyComparisonBarChart
import com.example.ui.theme.QuantAmber
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantIndigo
import com.example.ui.theme.QuantRose
import com.example.ui.viewmodel.ResearchUiState

enum class SortColumn {
    STRATEGY, N, WINS, LOSSES, WIN_RATE, AVG_ODDS, STAKE, PROFIT, ROI
}

@Composable
fun DashboardScreen(
    state: ResearchUiState,
    onStrategySelected: (ResearchStrategyType) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onStageFilterChanged: (String) -> Unit
) {
    var sortColumn by remember { mutableStateOf(SortColumn.ROI) }
    var sortAscending by remember { mutableStateOf(false) }

    val reports = state.strategyReports.values.toList()
    val sortedReports = remember(reports, sortColumn, sortAscending) {
        val comparator: Comparator<StrategyEvaluationReport> = when (sortColumn) {
            SortColumn.STRATEGY -> compareBy { it.strategyType.title }
            SortColumn.N -> compareBy { it.sampleSize }
            SortColumn.WINS -> compareBy { it.wins }
            SortColumn.LOSSES -> compareBy { it.losses }
            SortColumn.WIN_RATE -> compareBy { it.winRate }
            SortColumn.AVG_ODDS -> compareBy { it.averageOdds }
            SortColumn.STAKE -> compareBy { it.totalStake }
            SortColumn.PROFIT -> compareBy { it.totalProfit }
            SortColumn.ROI -> compareBy { it.roi }
        }
        if (sortAscending) reports.sortedWith(comparator) else reports.sortedWith(comparator.reversed())
    }

    // Main highlight metrics
    val bestRoiReport = reports.maxByOrNull { it.roi }
    val worstRoiReport = reports.minByOrNull { it.roi }
    val largestSampleReport = reports.maxByOrNull { it.sampleSize }
    val highestProfitReport = reports.maxByOrNull { it.totalProfit }
    val mostConsistentReport = reports.filter { it.sampleSize >= 20 }.minByOrNull { it.drawdownAnalysis.maxDrawdownPercent }

    val totalMatches = state.matches.size
    val knockoutMatches = state.matches.count { it.isKnockout }
    val competitionsCount = state.matches.map { it.competition }.distinct().size
    val dateRange = if (state.matches.isNotEmpty()) {
        val sortedDates = state.matches.map { it.date }.sorted()
        "${sortedDates.first()} → ${sortedDates.last()}"
    } else "N/A"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Hypothesis Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hypothesis_banner_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = QuantCyan.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Research Lab",
                                tint = QuantCyan,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CORE RESEARCH HYPOTHESIS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = QuantCyan,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "\"Does Under 3.5 goals in knockout-stage football matches show a genuine positive historical edge, or can the observed positive ROI be explained by small sample size and variance?\"",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cautionary Principle: A positive historical ROI is NEVER assumed to be a genuine edge without bootstrap confidence testing, sample size verification, and outlier stress analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Dataset Overview Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniCard(
                    title = "Total Matches",
                    value = "$totalMatches",
                    subtitle = "$knockoutMatches Knockouts",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "Observations",
                    value = "${state.observations.size}",
                    subtitle = "6 Active Strategies",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "Tournaments",
                    value = "$competitionsCount",
                    subtitle = dateRange.take(10),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stage Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Stage Filter:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = state.stageFilter == "ALL",
                    onClick = { onStageFilterChanged("ALL") },
                    label = { Text("All Matches (${state.matches.size})") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = state.stageFilter == "KNOCKOUT",
                    onClick = { onStageFilterChanged("KNOCKOUT") },
                    label = { Text("Knockouts Only ($knockoutMatches)") },
                    modifier = Modifier.testTag("filter_knockout")
                )
                FilterChip(
                    selected = state.stageFilter == "LEAGUE",
                    onClick = { onStageFilterChanged("LEAGUE") },
                    label = { Text("League Only (${totalMatches - knockoutMatches})") },
                    modifier = Modifier.testTag("filter_league")
                )
            }
        }

        // Research Highlights Banner Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Key Empirical Highlights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HighlightCard(
                        title = "Best Historical ROI",
                        strategy = bestRoiReport?.strategyType?.title ?: "N/A",
                        value = "${if ((bestRoiReport?.roi ?: 0.0) >= 0) "+" else ""}${String.format("%.2f", bestRoiReport?.roi ?: 0.0)}%",
                        color = QuantEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        title = "Worst Historical ROI",
                        strategy = worstRoiReport?.strategyType?.title ?: "N/A",
                        value = "${String.format("%.2f", worstRoiReport?.roi ?: 0.0)}%",
                        color = QuantRose,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HighlightCard(
                        title = "Highest Net Profit",
                        strategy = highestProfitReport?.strategyType?.title ?: "N/A",
                        value = "${if ((highestProfitReport?.totalProfit ?: 0.0) >= 0) "+" else ""}${String.format("$%.0f", highestProfitReport?.totalProfit ?: 0.0)}",
                        color = QuantCyan,
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        title = "Most Consistent (Lowest DD)",
                        strategy = mostConsistentReport?.strategyType?.title ?: "N/A",
                        value = "-${String.format("%.1f", mostConsistentReport?.drawdownAnalysis?.maxDrawdownPercent ?: 0.0)}% DD",
                        color = QuantIndigo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Strategy Comparison Data Table
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("strategy_table_card"),
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
                                text = "Strategy Comparison Matrix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap column header to sort | Default stake: $100/obs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontally scrollable professional data table
                    val horizontalScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScroll)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableHeaderCell("Strategy", 160.dp, SortColumn.STRATEGY, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                            TableHeaderCell("N", 60.dp, SortColumn.N, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                            TableHeaderCell("W-L", 80.dp, SortColumn.WINS, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                            TableHeaderCell("Win Rate", 85.dp, SortColumn.WIN_RATE, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                            TableHeaderCell("Avg Odds", 80.dp, SortColumn.AVG_ODDS, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                            TableHeaderCell("Stake", 90.dp, SortColumn.STAKE, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                            TableHeaderCell("Profit", 90.dp, SortColumn.PROFIT, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                            TableHeaderCell("ROI", 85.dp, SortColumn.ROI, sortColumn, sortAscending) { sortColumn = it; sortAscending = !sortAscending }
                        }

                        // Data Rows
                        sortedReports.forEach { rep ->
                            val isPos = rep.roi >= 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rep.strategyType.title,
                                    modifier = Modifier.width(160.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${rep.sampleSize}",
                                    modifier = Modifier.width(60.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${rep.wins}-${rep.losses}",
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", rep.winRate)}%",
                                    modifier = Modifier.width(85.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format("%.2f", rep.averageOdds),
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("$%.0f", rep.totalStake),
                                    modifier = Modifier.width(90.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${if (isPos) "+" else ""}${String.format("$%.0f", rep.totalProfit)}",
                                    modifier = Modifier.width(90.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isPos) QuantEmerald else QuantRose
                                )
                                Text(
                                    text = "${if (isPos) "+" else ""}${String.format("%.1f", rep.roi)}%",
                                    modifier = Modifier.width(85.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isPos) QuantEmerald else QuantRose
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visualizations
        item {
            StrategyComparisonBarChart(reports = sortedReports)
        }

        // Under 3.5 Specific Cumulative Curve
        item {
            val u35Report = state.strategyReports[ResearchStrategyType.UNDER_3_5]
            if (u35Report != null) {
                CumulativeProfitChart(
                    points = u35Report.drawdownAnalysis.cumulativeProfits,
                    lineColor = QuantCyan
                )
            }
        }

        // Quick Navigation Shortcuts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Research Modules",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToTab(3) }, // Knockout
                            modifier = Modifier.weight(1f).testTag("nav_btn_knockout")
                        ) {
                            Text("Knockout", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigateToTab(4) }, // Same Sample
                            modifier = Modifier.weight(1f).testTag("nav_btn_samesample")
                        ) {
                            Text("Same-Sample", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigateToTab(6) }, // Robustness
                            modifier = Modifier.weight(1f).testTag("nav_btn_robustness")
                        ) {
                            Text("Robustness", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    column: SortColumn,
    currentSort: SortColumn,
    sortAscending: Boolean,
    onClick: (SortColumn) -> Unit
) {
    Surface(
        onClick = { onClick(column) },
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.width(width)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (currentSort == column) QuantCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currentSort == column) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Sort direction",
                    tint = QuantCyan,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun HighlightCard(
    title: String,
    strategy: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = strategy,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

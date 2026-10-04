package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResearchStrategyType
import com.example.stats.SampleSplitter
import com.example.stats.SplitPartition
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantIndigo
import com.example.ui.theme.QuantRose
import com.example.ui.viewmodel.ResearchUiState
import kotlin.math.abs
import kotlin.math.max

@Composable
fun OutOfSampleScreen(
    state: ResearchUiState,
    onStrategySelected: (ResearchStrategyType) -> Unit
) {
    val selectedStrategy = state.selectedStrategy
    val report = state.strategyReports[selectedStrategy]

    var splitRatioPreset by remember { mutableStateOf("60-20-20") }
    val (trainRatio, valRatio) = when (splitRatioPreset) {
        "70-15-15" -> Pair(0.70, 0.15)
        "50-25-25" -> Pair(0.50, 0.25)
        else -> Pair(0.60, 0.20)
    }

    val observations = state.observations.filter { it.strategyType == selectedStrategy }
    val splitResult = remember(observations, trainRatio, valRatio) {
        SampleSplitter.splitChronological(observations, trainRatio, valRatio)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("out_of_sample_screen"),
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

        // Methodological Explanation Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CHRONOLOGICAL OUT-OF-SAMPLE PARTITIONING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To strictly avoid overfitting and lookahead bias, historical data is divided chronologically. Performance is evaluated across In-Sample calibration, Validation, and blind Out-of-Sample testing periods.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Partition Ratio:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterChip(
                            selected = splitRatioPreset == "60-20-20",
                            onClick = { splitRatioPreset = "60-20-20" },
                            label = { Text("60% / 20% / 20% (Default)") }
                        )
                        FilterChip(
                            selected = splitRatioPreset == "70-15-15",
                            onClick = { splitRatioPreset = "70-15-15" },
                            label = { Text("70% / 15% / 15%") }
                        )
                        FilterChip(
                            selected = splitRatioPreset == "50-25-25",
                            onClick = { splitRatioPreset = "50-25-25" },
                            label = { Text("50% / 25% / 25%") }
                        )
                    }
                }
            }
        }

        // Out-of-Sample Status Badge
        item {
            val oosProfitable = splitResult.isOosProfitable
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (oosProfitable) QuantEmerald.copy(alpha = 0.15f) else QuantRose.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (oosProfitable) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (oosProfitable) QuantEmerald else QuantRose
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (oosProfitable) "Out-of-Sample Validation Passed" else "Out-of-Sample Failure / Edge Decay",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (oosProfitable) QuantEmerald else QuantRose
                        )
                        Text(
                            text = if (oosProfitable)
                                "Edge successfully persisted into the forward test period (ROI: +${String.format("%.2f", splitResult.outOfSample.roi)}%)."
                            else
                                "Edge did not survive forward testing. Observed In-Sample returns may reflect historical curve-fitting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Partitions Side-by-Side Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PartitionCard(partition = splitResult.training, color = QuantCyan)
                PartitionCard(partition = splitResult.validation, color = QuantIndigo)
                PartitionCard(partition = splitResult.outOfSample, color = if (splitResult.isOosProfitable) QuantEmerald else QuantRose)
            }
        }

        // Training vs Validation vs OOS ROI Visual Comparison Chart
        item {
            TrainingValOosChart(
                train = splitResult.training,
                validation = splitResult.validation,
                oos = splitResult.outOfSample
            )
        }
    }
}

@Composable
fun PartitionCard(partition: SplitPartition, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = partition.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${partition.sampleSize} observations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${if (partition.roi >= 0) "+" else ""}${String.format("%.2f", partition.roi)}% ROI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (partition.roi >= 0) QuantEmerald else QuantRose
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Timeline: ${partition.dateRange}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Win Rate: ${String.format("%.1f", partition.winRate)}%  |  Profit: ${if (partition.totalProfit >= 0) "+" else ""}${String.format("$%.0f", partition.totalProfit)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TrainingValOosChart(
    train: SplitPartition,
    validation: SplitPartition,
    oos: SplitPartition
) {
    val partitions = listOf(train, validation, oos)
    val maxRoiAbs = max(10.0, partitions.maxOf { abs(it.roi) }) * 1.25

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Chronological ROI Transition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Comparing returns sequentially across training, validation, and OOS periods",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val zeroY = h * 0.5f

                    // Draw 0% baseline
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    val barWidth = 44.dp.toPx()
                    val gap = (w - (barWidth * 3)) / 4f

                    partitions.forEachIndexed { i, p ->
                        val x = gap + i * (barWidth + gap)
                        val norm = (p.roi / maxRoiAbs).toFloat().coerceIn(-1f, 1f)
                        val barH = norm * (h * 0.42f)
                        val color = if (p.roi >= 0) QuantEmerald else QuantRose

                        if (p.roi >= 0) {
                            drawRect(
                                color = color,
                                topLeft = Offset(x, zeroY - barH),
                                size = Size(barWidth, barH)
                            )
                        } else {
                            drawRect(
                                color = color,
                                topLeft = Offset(x, zeroY),
                                size = Size(barWidth, abs(barH))
                            )
                        }
                    }
                }

                // Partition labels below
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    partitions.forEach { p ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = p.name.take(12), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "${if (p.roi >= 0) "+" else ""}${String.format("%.1f", p.roi)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (p.roi >= 0) QuantEmerald else QuantRose
                            )
                        }
                    }
                }
            }
        }
    }
}

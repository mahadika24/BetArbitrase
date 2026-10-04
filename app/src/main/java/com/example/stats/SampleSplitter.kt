package com.example.stats

import com.example.data.model.BetObservation

data class SplitPartition(
    val name: String,
    val description: String,
    val observations: List<BetObservation>,
    val percentage: Double,
    val dateRange: String,
    val sampleSize: Int,
    val totalStake: Double,
    val totalProfit: Double,
    val roi: Double,
    val winRate: Double
)

data class ChronologicalSplitResult(
    val training: SplitPartition,
    val validation: SplitPartition,
    val outOfSample: SplitPartition,
    val isOosProfitable: Boolean
)

object SampleSplitter {

    /**
     * Splits observations strictly chronologically by date.
     * Prevents lookahead bias: future observations never affect earlier splits.
     * Default ratios: 60% Train, 20% Validation, 20% Out-of-Sample.
     */
    fun splitChronological(
        observations: List<BetObservation>,
        trainRatio: Double = 0.60,
        valRatio: Double = 0.20
    ): ChronologicalSplitResult {
        if (observations.isEmpty()) {
            val empty = SplitPartition("", "", emptyList(), 0.0, "N/A", 0, 0.0, 0.0, 0.0, 0.0)
            return ChronologicalSplitResult(empty, empty, empty, false)
        }

        // Strict chronological sort
        val sorted = observations.sortedBy { it.date }
        val n = sorted.size

        val trainEnd = (n * trainRatio).toInt().coerceIn(1, n)
        val valEnd = (trainEnd + (n * valRatio).toInt()).coerceIn(trainEnd, n)

        val trainList = sorted.subList(0, trainEnd)
        val valList = sorted.subList(trainEnd, valEnd)
        val oosList = sorted.subList(valEnd, n)

        val trainPartition = createPartition("Training (In-Sample)", "Model calibration & hypothesis formulation", trainList, trainRatio * 100.0)
        val valPartition = createPartition("Validation", "Hyperparameter & market stability check", valList, valRatio * 100.0)
        val oosPartition = createPartition("Out-of-Sample", "Strict blind forward testing", oosList, (1.0 - trainRatio - valRatio) * 100.0)

        return ChronologicalSplitResult(
            training = trainPartition,
            validation = valPartition,
            outOfSample = oosPartition,
            isOosProfitable = oosPartition.roi > 0.0 && oosPartition.sampleSize > 0
        )
    }

    private fun createPartition(
        name: String,
        description: String,
        items: List<BetObservation>,
        nominalPct: Double
    ): SplitPartition {
        val totalStake = items.sumOf { it.stake }
        val totalProfit = items.sumOf { it.profit }
        val roi = ProfitCalculator.calculateRoi(totalProfit, totalStake)
        val wins = items.count { it.outcome == com.example.data.model.Outcome.WIN }
        val winRate = if (items.isNotEmpty()) (wins.toDouble() / items.size) * 100.0 else 0.0
        val dateRange = if (items.isNotEmpty()) "${items.first().date} → ${items.last().date}" else "None"

        return SplitPartition(
            name = name,
            description = description,
            observations = items,
            percentage = nominalPct,
            dateRange = dateRange,
            sampleSize = items.size,
            totalStake = totalStake,
            totalProfit = totalProfit,
            roi = roi,
            winRate = winRate
        )
    }
}

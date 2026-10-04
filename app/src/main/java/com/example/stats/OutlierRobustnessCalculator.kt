package com.example.stats

import com.example.data.model.BetObservation

data class RobustnessScenario(
    val key: String,
    val title: String,
    val description: String,
    val sampleSize: Int,
    val totalStake: Double,
    val totalProfit: Double,
    val roi: Double,
    val deltaRoiFromBaseline: Double
)

data class OutlierRobustnessResult(
    val baseline: RobustnessScenario,
    val minusTop1: RobustnessScenario,
    val minusTop2: RobustnessScenario,
    val minusWorst1: RobustnessScenario,
    val minusExtremeOdds: RobustnessScenario,
    val survivesOutlierRemoval: Boolean // true if minusTop2 ROI is still > 0%
)

object OutlierRobustnessCalculator {

    /**
     * Calculates the sensitivity of strategy performance to highly influential observations.
     */
    fun analyzeOutlierRobustness(
        observations: List<BetObservation>,
        extremeOddsThreshold: Double = 3.5
    ): OutlierRobustnessResult {
        if (observations.isEmpty()) {
            val empty = RobustnessScenario("", "No data", "", 0, 0.0, 0.0, 0.0, 0.0)
            return OutlierRobustnessResult(empty, empty, empty, empty, empty, false)
        }

        // Scenario A: Full dataset
        val baselineStake = observations.sumOf { it.stake }
        val baselineProfit = observations.sumOf { it.profit }
        val baselineRoi = ProfitCalculator.calculateRoi(baselineProfit, baselineStake)
        val scenarioA = RobustnessScenario(
            key = "A",
            title = "Full Dataset",
            description = "All available observations without filtering",
            sampleSize = observations.size,
            totalStake = baselineStake,
            totalProfit = baselineProfit,
            roi = baselineRoi,
            deltaRoiFromBaseline = 0.0
        )

        // Sorted by profit descending (highest win first)
        val sortedByProfitDesc = observations.sortedByDescending { it.profit }

        // Scenario B: Minus top 1 win
        val minus1Obs = if (sortedByProfitDesc.size > 1) sortedByProfitDesc.drop(1) else emptyList()
        val sBStake = minus1Obs.sumOf { it.stake }
        val sBProfit = minus1Obs.sumOf { it.profit }
        val sBRoi = ProfitCalculator.calculateRoi(sBProfit, sBStake)
        val scenarioB = RobustnessScenario(
            key = "B",
            title = "Remove Top 1 Winner",
            description = "Excludes the single most profitable match result",
            sampleSize = minus1Obs.size,
            totalStake = sBStake,
            totalProfit = sBProfit,
            roi = sBRoi,
            deltaRoiFromBaseline = sBRoi - baselineRoi
        )

        // Scenario C: Minus top 2 wins
        val minus2Obs = if (sortedByProfitDesc.size > 2) sortedByProfitDesc.drop(2) else emptyList()
        val sCStake = minus2Obs.sumOf { it.stake }
        val sCProfit = minus2Obs.sumOf { it.profit }
        val sCRoi = ProfitCalculator.calculateRoi(sCProfit, sCStake)
        val scenarioC = RobustnessScenario(
            key = "C",
            title = "Remove Top 2 Winners",
            description = "Excludes the 2 most profitable outlier outcomes",
            sampleSize = minus2Obs.size,
            totalStake = sCStake,
            totalProfit = sCProfit,
            roi = sCRoi,
            deltaRoiFromBaseline = sCRoi - baselineRoi
        )

        // Scenario D: Minus single worst loser
        val sortedByProfitAsc = observations.sortedBy { it.profit }
        val minusWorstObs = if (sortedByProfitAsc.size > 1) sortedByProfitAsc.drop(1) else emptyList()
        val sDStake = minusWorstObs.sumOf { it.stake }
        val sDProfit = minusWorstObs.sumOf { it.profit }
        val sDRoi = ProfitCalculator.calculateRoi(sDProfit, sDStake)
        val scenarioD = RobustnessScenario(
            key = "D",
            title = "Remove Single Worst Outcome",
            description = "Excludes the biggest single loss/drawdown hit",
            sampleSize = minusWorstObs.size,
            totalStake = sDStake,
            totalProfit = sDProfit,
            roi = sDRoi,
            deltaRoiFromBaseline = sDRoi - baselineRoi
        )

        // Scenario E: Remove extreme odds (odds > threshold or top 5%)
        val oddsCutoff = if (observations.size >= 20) {
            val sortedOdds = observations.map { it.odds }.sorted()
            val p95Idx = (sortedOdds.size * 0.95).toInt().coerceAtMost(sortedOdds.size - 1)
            sortedOdds[p95Idx].coerceAtLeast(extremeOddsThreshold)
        } else {
            extremeOddsThreshold
        }
        val normalOddsObs = observations.filter { it.odds <= oddsCutoff }
        val sEStake = normalOddsObs.sumOf { it.stake }
        val sEProfit = normalOddsObs.sumOf { it.profit }
        val sERoi = ProfitCalculator.calculateRoi(sEProfit, sEStake)
        val scenarioE = RobustnessScenario(
            key = "E",
            title = "Remove Extreme Odds",
            description = "Excludes tail odds > ${String.format("%.2f", oddsCutoff)}",
            sampleSize = normalOddsObs.size,
            totalStake = sEStake,
            totalProfit = sEProfit,
            roi = sERoi,
            deltaRoiFromBaseline = sERoi - baselineRoi
        )

        val survives = scenarioC.sampleSize > 0 && scenarioC.roi > 0.0

        return OutlierRobustnessResult(
            baseline = scenarioA,
            minusTop1 = scenarioB,
            minusTop2 = scenarioC,
            minusWorst1 = scenarioD,
            minusExtremeOdds = scenarioE,
            survivesOutlierRemoval = survives
        )
    }
}

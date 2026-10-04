package com.example.stats

import com.example.data.model.BetObservation
import com.example.data.model.EvidenceClassification
import com.example.data.model.Outcome
import com.example.data.model.ResearchStrategyType
import com.example.data.model.VerificationRule

data class StrategyEvaluationReport(
    val strategyType: ResearchStrategyType,
    val sampleSize: Int, // N = observations
    val matchesCount: Int, // Unique matches count
    val wins: Int,
    val losses: Int,
    val voids: Int,
    val winRate: Double,
    val wilsonWinRateCi: ProportionConfidenceInterval,
    val averageOdds: Double,
    val medianOdds: Double,
    val portfolioBreakEvenRate: Double,
    val meanImpliedProbability: Double,
    val totalStake: Double,
    val totalProfit: Double,
    val roi: Double,
    val drawdownAnalysis: DrawdownAnalysis,
    val bootstrapResult: BootstrapResult,
    val outlierRobustness: OutlierRobustnessResult,
    val chronologicalSplit: ChronologicalSplitResult,
    val classification: EvidenceClassification,
    val verificationRules: List<VerificationRule>
)

object StrategyEvaluator {

    fun evaluate(
        strategyType: ResearchStrategyType,
        observations: List<BetObservation>,
        bootstrapIterations: Int = 10_000,
        bootstrapSeed: Long = 123456789L
    ): StrategyEvaluationReport {
        val n = observations.size
        val uniqueMatches = observations.map { it.matchId }.distinct().size

        if (n == 0) {
            val emptyDrawdown = DrawdownCalculator.analyzeDrawdown(emptyList())
            val emptyBootstrap = BootstrapCalculator.calculateRoiBootstrap(emptyList(), iterations = 100)
            val emptyRobustness = OutlierRobustnessCalculator.analyzeOutlierRobustness(emptyList())
            val emptySplit = SampleSplitter.splitChronological(emptyList())
            val emptyWilson = WilsonConfidenceInterval.calculateWilsonScore(0, 0)

            return StrategyEvaluationReport(
                strategyType = strategyType,
                sampleSize = 0,
                matchesCount = 0,
                wins = 0,
                losses = 0,
                voids = 0,
                winRate = 0.0,
                wilsonWinRateCi = emptyWilson,
                averageOdds = 0.0,
                medianOdds = 0.0,
                portfolioBreakEvenRate = 0.0,
                meanImpliedProbability = 0.0,
                totalStake = 0.0,
                totalProfit = 0.0,
                roi = 0.0,
                drawdownAnalysis = emptyDrawdown,
                bootstrapResult = emptyBootstrap,
                outlierRobustness = emptyRobustness,
                chronologicalSplit = emptySplit,
                classification = EvidenceClassification.INSUFFICIENT_DATA,
                verificationRules = emptyList()
            )
        }

        val wins = observations.count { it.outcome == Outcome.WIN }
        val losses = observations.count { it.outcome == Outcome.LOSS }
        val voids = observations.count { it.outcome == Outcome.VOID }
        val effectiveCount = wins + losses
        val winRate = if (effectiveCount > 0) (wins.toDouble() / effectiveCount) * 100.0 else 0.0

        val wilsonWinRateCi = WilsonConfidenceInterval.calculateWilsonScore(wins, effectiveCount)

        val oddsList = observations.map { it.odds }
        val averageOdds = if (oddsList.isNotEmpty()) oddsList.average() else 0.0
        val sortedOdds = oddsList.sorted()
        val medianOdds = if (sortedOdds.isNotEmpty()) {
            if (sortedOdds.size % 2 == 1) {
                sortedOdds[sortedOdds.size / 2]
            } else {
                (sortedOdds[sortedOdds.size / 2 - 1] + sortedOdds[sortedOdds.size / 2]) / 2.0
            }
        } else 0.0

        val stakes = observations.map { it.stake }
        val portfolioBreakEvenRate = BreakEvenCalculator.calculatePortfolioBreakEven(oddsList, stakes)
        val meanImpliedProbability = BreakEvenCalculator.calculateMeanImpliedProbability(oddsList)

        val totalStake = stakes.sum()
        val totalProfit = observations.sumOf { it.profit }
        val roi = ProfitCalculator.calculateRoi(totalProfit, totalStake)

        // Drawdown in chronological order
        val drawdownAnalysis = DrawdownCalculator.analyzeDrawdown(observations)

        // Bootstrap uncertainty
        val bootstrapResult = BootstrapCalculator.calculateRoiBootstrap(
            observations = observations,
            iterations = bootstrapIterations,
            seed = bootstrapSeed
        )

        // Outlier robustness
        val outlierRobustness = OutlierRobustnessCalculator.analyzeOutlierRobustness(observations)

        // Chronological out-of-sample split
        val chronologicalSplit = SampleSplitter.splitChronological(observations)

        // Scientific rule checks
        val rule1 = VerificationRule(
            title = "Statistical Sample Power",
            passed = n >= 50,
            valueDisplay = "N = $n observations ($uniqueMatches matches)",
            requirement = "N ≥ 50 observations",
            explanation = "Adequate sample size to guard against extreme small-sample noise."
        )

        val rule2 = VerificationRule(
            title = "Positive Observed ROI",
            passed = roi > 0.0,
            valueDisplay = "${if (roi > 0) "+" else ""}${String.format("%.2f", roi)}%",
            requirement = "Historical ROI > 0.0%",
            explanation = "Strategy must generate positive baseline net return over bookmaker margin."
        )

        val rule3 = VerificationRule(
            title = "Bootstrap 95% ROI Confidence",
            passed = bootstrapResult.lowerCi95 > 0.0,
            valueDisplay = "[${String.format("%.1f", bootstrapResult.lowerCi95)}%, ${String.format("%.1f", bootstrapResult.upperCi95)}%]",
            requirement = "95% CI Lower Bound > 0.0%",
            explanation = "Statistical test rejecting the null hypothesis of luck/variance at p < 0.05."
        )

        val rule4 = VerificationRule(
            title = "Outlier Resilience (Minus Top 2)",
            passed = outlierRobustness.minusTop2.roi > 0.0,
            valueDisplay = "${if (outlierRobustness.minusTop2.roi > 0) "+" else ""}${String.format("%.2f", outlierRobustness.minusTop2.roi)}%",
            requirement = "ROI > 0.0% without top 2 wins",
            explanation = "Verifies whether edge depends critically on rare freak longshot winners."
        )

        val rule5 = VerificationRule(
            title = "Chronological Out-of-Sample",
            passed = chronologicalSplit.outOfSample.roi > 0.0 && chronologicalSplit.outOfSample.sampleSize >= 5,
            valueDisplay = "${if (chronologicalSplit.outOfSample.roi > 0) "+" else ""}${String.format("%.2f", chronologicalSplit.outOfSample.roi)}% (N=${chronologicalSplit.outOfSample.sampleSize})",
            requirement = "Forward OOS ROI > 0.0%",
            explanation = "Checks if profitability persists forward into unobserved future periods."
        )

        val rule6 = VerificationRule(
            title = "Maximum Drawdown Exposure",
            passed = drawdownAnalysis.maxDrawdownPercent < 25.0,
            valueDisplay = "${String.format("%.1f", drawdownAnalysis.maxDrawdownPercent)}% (${String.format("$%.0f", drawdownAnalysis.maxDrawdownAmount)})",
            requirement = "Max Drawdown < 25% of turnover",
            explanation = "Ensures capital preservation and manageable downside volatility."
        )

        val rules = listOf(rule1, rule2, rule3, rule4, rule5, rule6)

        // Classify hypothesis
        val classification = when {
            n < 30 -> EvidenceClassification.INSUFFICIENT_DATA
            roi <= 0.0 -> EvidenceClassification.WEAK_UNSTABLE
            rule3.passed && rule4.passed && rule5.passed -> EvidenceClassification.ROBUST
            else -> EvidenceClassification.POSITIVE_INSUFFICIENT
        }

        return StrategyEvaluationReport(
            strategyType = strategyType,
            sampleSize = n,
            matchesCount = uniqueMatches,
            wins = wins,
            losses = losses,
            voids = voids,
            winRate = winRate,
            wilsonWinRateCi = wilsonWinRateCi,
            averageOdds = averageOdds,
            medianOdds = medianOdds,
            portfolioBreakEvenRate = portfolioBreakEvenRate,
            meanImpliedProbability = meanImpliedProbability,
            totalStake = totalStake,
            totalProfit = totalProfit,
            roi = roi,
            drawdownAnalysis = drawdownAnalysis,
            bootstrapResult = bootstrapResult,
            outlierRobustness = outlierRobustness,
            chronologicalSplit = chronologicalSplit,
            classification = classification,
            verificationRules = rules
        )
    }
}

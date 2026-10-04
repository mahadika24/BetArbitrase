package com.example.stats

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ProportionConfidenceInterval(
    val pointEstimate: Double, // in percentage
    val lowerBound: Double,    // in percentage
    val upperBound: Double,    // in percentage
    val confidenceLevel: Double = 0.95
)

object WilsonConfidenceInterval {

    /**
     * Calculates the 95% Wilson Score Confidence Interval for the Win Rate (binomial proportion).
     * This measures the statistical uncertainty in whether the strategy's observed hit rate is distinct
     * from random chance, specifically distinguishing it from ROI uncertainty.
     */
    fun calculateWilsonScore(
        wins: Int,
        total: Int,
        zScore: Double = 1.95996 // 95% two-tailed standard normal critical value
    ): ProportionConfidenceInterval {
        if (total <= 0) {
            return ProportionConfidenceInterval(0.0, 0.0, 0.0)
        }

        val n = total.toDouble()
        val p = wins.toDouble() / n
        val z2 = zScore * zScore

        val denominator = 1.0 + z2 / n
        val centerAdjusted = (p + z2 / (2.0 * n)) / denominator
        val margin = (zScore * sqrt((p * (1.0 - p) / n) + (z2 / (4.0 * n * n)))) / denominator

        val lower = max(0.0, centerAdjusted - margin) * 100.0
        val upper = min(1.0, centerAdjusted + margin) * 100.0
        val point = p * 100.0

        return ProportionConfidenceInterval(
            pointEstimate = point,
            lowerBound = lower,
            upperBound = upper
        )
    }
}

package com.example.stats

import com.example.data.model.BetObservation
import com.example.data.model.Outcome
import kotlin.math.max

data class ProfitPoint(
    val index: Int,
    val date: String,
    val matchId: String,
    val profit: Double,
    val cumulativeProfit: Double,
    val runningPeak: Double,
    val drawdown: Double
)

data class DrawdownAnalysis(
    val cumulativeProfits: List<ProfitPoint>,
    val maxDrawdownAmount: Double, // Maximum dollar peak-to-trough drop
    val maxDrawdownPercent: Double, // As percentage of total stake invested
    val maxDrawdownDuration: Int,  // Number of bets spent in the longest drawdown phase
    val longestLosingStreak: Int,   // Consecutive losses
    val currentLosingStreak: Int,
    val peakProfit: Double,
    val finalProfit: Double
)

object DrawdownCalculator {

    /**
     * Calculates drawdown and cumulative profit in strict chronological order.
     * Never reorders observations randomly.
     */
    fun analyzeDrawdown(observations: List<BetObservation>): DrawdownAnalysis {
        if (observations.isEmpty()) {
            return DrawdownAnalysis(
                cumulativeProfits = emptyList(),
                maxDrawdownAmount = 0.0,
                maxDrawdownPercent = 0.0,
                maxDrawdownDuration = 0,
                longestLosingStreak = 0,
                currentLosingStreak = 0,
                peakProfit = 0.0,
                finalProfit = 0.0
            )
        }

        // Ensure chronological ordering by date
        val chronological = observations.sortedBy { it.date }

        val points = ArrayList<ProfitPoint>(chronological.size)
        var cumulative = 0.0
        var runningPeak = 0.0
        var maxDrawdown = 0.0

        var longestStreak = 0
        var currentStreak = 0

        var currentDrawdownLength = 0
        var maxDrawdownDuration = 0

        var totalStake = 0.0

        for (i in chronological.indices) {
            val obs = chronological[i]
            cumulative += obs.profit
            totalStake += obs.stake

            if (cumulative > runningPeak) {
                runningPeak = cumulative
                currentDrawdownLength = 0
            } else {
                currentDrawdownLength++
                if (currentDrawdownLength > maxDrawdownDuration) {
                    maxDrawdownDuration = currentDrawdownLength
                }
            }

            val currentDrawdown = runningPeak - cumulative
            if (currentDrawdown > maxDrawdown) {
                maxDrawdown = currentDrawdown
            }

            if (obs.outcome == Outcome.LOSS) {
                currentStreak++
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                }
            } else if (obs.outcome == Outcome.WIN) {
                currentStreak = 0
            }

            points.add(
                ProfitPoint(
                    index = i + 1,
                    date = obs.date,
                    matchId = obs.matchId,
                    profit = obs.profit,
                    cumulativeProfit = cumulative,
                    runningPeak = runningPeak,
                    drawdown = currentDrawdown
                )
            )
        }

        val maxDdPercent = if (totalStake > 0.0) (maxDrawdown / totalStake) * 100.0 else 0.0

        return DrawdownAnalysis(
            cumulativeProfits = points,
            maxDrawdownAmount = maxDrawdown,
            maxDrawdownPercent = maxDdPercent,
            maxDrawdownDuration = maxDrawdownDuration,
            longestLosingStreak = longestStreak,
            currentLosingStreak = currentStreak,
            peakProfit = runningPeak,
            finalProfit = cumulative
        )
    }
}

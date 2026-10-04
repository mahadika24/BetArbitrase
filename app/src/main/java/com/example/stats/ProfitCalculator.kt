package com.example.stats

import com.example.data.model.Outcome

object ProfitCalculator {

    /**
     * Profit calculation for a single betting observation.
     * Default stake is $100.
     * Decimal odds:
     * - Winning bet: stake * (odds - 1)
     * - Losing bet: -stake
     * - Void bet: 0.0
     */
    fun calculateProfit(odds: Double, stake: Double = 100.0, outcome: Outcome): Double {
        return when (outcome) {
            Outcome.WIN -> stake * (odds - 1.0)
            Outcome.LOSS -> -stake
            Outcome.VOID, Outcome.PENDING -> 0.0
        }
    }

    /**
     * ROI must be: total profit / total amount staked.
     * Not calculated using the number of matches alone.
     * Returns ROI as percentage (e.g. 5.5 for +5.5%).
     */
    fun calculateRoi(totalProfit: Double, totalStake: Double): Double {
        if (totalStake <= 0.0) return 0.0
        return (totalProfit / totalStake) * 100.0
    }
}

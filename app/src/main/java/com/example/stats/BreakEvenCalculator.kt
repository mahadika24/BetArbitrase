package com.example.stats

object BreakEvenCalculator {

    /**
     * Calculates the aggregate portfolio break-even probability for varying decimal odds.
     * With stakes s_i and odds o_i:
     * Break-even requires: sum(p * s_i * (o_i - 1) - (1 - p) * s_i) = 0
     * => p * sum(s_i * o_i) = sum(s_i)
     * => p_break_even = sum(s_i) / sum(s_i * o_i)
     * For flat stakes (all equal):
     * p_break_even = N / sum(o_i) = 1 / mean(odds)
     */
    fun calculatePortfolioBreakEven(oddsList: List<Double>, stakes: List<Double> = emptyList()): Double {
        if (oddsList.isEmpty()) return 0.0
        val effectiveStakes = if (stakes.size == oddsList.size) stakes else List(oddsList.size) { 100.0 }
        
        var totalStake = 0.0
        var totalGrossReturnIfWon = 0.0
        for (i in oddsList.indices) {
            val odds = oddsList[i]
            if (odds > 0.0) {
                val s = effectiveStakes[i]
                totalStake += s
                totalGrossReturnIfWon += s * odds
            }
        }
        if (totalGrossReturnIfWon <= 0.0) return 0.0
        return (totalStake / totalGrossReturnIfWon) * 100.0 // in percentage
    }

    /**
     * Mean individual implied break-even probability:
     * (1/N) * sum(1 / odds_i)
     */
    fun calculateMeanImpliedProbability(oddsList: List<Double>): Double {
        if (oddsList.isEmpty()) return 0.0
        val validOdds = oddsList.filter { it > 0.0 }
        if (validOdds.isEmpty()) return 0.0
        val sumImplied = validOdds.sumOf { 1.0 / it }
        return (sumImplied / validOdds.size) * 100.0 // in percentage
    }
}

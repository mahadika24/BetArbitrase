package com.example

import com.example.data.csv.CsvParser
import com.example.data.model.BetObservation
import com.example.data.model.Outcome
import com.example.data.model.ResearchStrategyType
import com.example.stats.BootstrapCalculator
import com.example.stats.BreakEvenCalculator
import com.example.stats.DrawdownCalculator
import com.example.stats.OutlierRobustnessCalculator
import com.example.stats.ProfitCalculator
import com.example.stats.SampleSplitter
import com.example.stats.WilsonConfidenceInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticalCalculationsTest {

    @Test
    fun testProfitCalculation() {
        // Winning bet: stake * (odds - 1)
        val winProfit = ProfitCalculator.calculateProfit(odds = 2.50, stake = 100.0, outcome = Outcome.WIN)
        assertEquals(150.0, winProfit, 0.001)

        // Losing bet: -stake
        val lossProfit = ProfitCalculator.calculateProfit(odds = 2.50, stake = 100.0, outcome = Outcome.LOSS)
        assertEquals(-100.0, lossProfit, 0.001)

        // Void bet: 0.0
        val voidProfit = ProfitCalculator.calculateProfit(odds = 2.50, stake = 100.0, outcome = Outcome.VOID)
        assertEquals(0.0, voidProfit, 0.001)
    }

    @Test
    fun testRoiCalculation() {
        // total profit / total stake
        val roi = ProfitCalculator.calculateRoi(totalProfit = 50.0, totalStake = 500.0)
        assertEquals(10.0, roi, 0.001) // +10.0%

        val negativeRoi = ProfitCalculator.calculateRoi(totalProfit = -75.0, totalStake = 500.0)
        assertEquals(-15.0, negativeRoi, 0.001) // -15.0%

        val zeroStake = ProfitCalculator.calculateRoi(totalProfit = 10.0, totalStake = 0.0)
        assertEquals(0.0, zeroStake, 0.001)
    }

    @Test
    fun testBreakEvenCalculation() {
        // Constant odds of 2.0 -> break-even is 50%
        val be50 = BreakEvenCalculator.calculatePortfolioBreakEven(listOf(2.0, 2.0, 2.0))
        assertEquals(50.0, be50, 0.001)

        // Decimal odds 1.50 -> break even is 66.67%
        val be66 = BreakEvenCalculator.calculatePortfolioBreakEven(listOf(1.50))
        assertEquals(66.666, be66, 0.01)

        // Varying odds
        val beVarying = BreakEvenCalculator.calculatePortfolioBreakEven(listOf(1.50, 3.00))
        // Stakes = $100 each ($200). If won: 150 + 300 = 450. Break-even = 200 / 450 = 44.44%
        assertEquals(44.44, beVarying, 0.1)
    }

    @Test
    fun testWilsonConfidenceInterval() {
        // 50 wins out of 100 trials
        val ci = WilsonConfidenceInterval.calculateWilsonScore(wins = 50, total = 100)
        assertEquals(50.0, ci.pointEstimate, 0.01)
        assertTrue("Lower bound should be < 50%", ci.lowerBound < 50.0)
        assertTrue("Upper bound should be > 50%", ci.upperBound > 50.0)
        assertTrue("Lower bound reasonable (around 40%)", ci.lowerBound in 39.0..42.0)
        assertTrue("Upper bound reasonable (around 60%)", ci.upperBound in 58.0..61.0)
    }

    @Test
    fun testDrawdownAndLosingStreak() {
        val obs = listOf(
            BetObservation(matchId = "1", date = "2024-01-01", strategyType = ResearchStrategyType.UNDER_3_5, market = "TG", selection = "U3.5", odds = 2.0, outcome = Outcome.WIN, stake = 100.0, profit = 100.0),
            BetObservation(matchId = "2", date = "2024-01-02", strategyType = ResearchStrategyType.UNDER_3_5, market = "TG", selection = "U3.5", odds = 2.0, outcome = Outcome.WIN, stake = 100.0, profit = 100.0),
            BetObservation(matchId = "3", date = "2024-01-03", strategyType = ResearchStrategyType.UNDER_3_5, market = "TG", selection = "U3.5", odds = 2.0, outcome = Outcome.LOSS, stake = 100.0, profit = -100.0),
            BetObservation(matchId = "4", date = "2024-01-04", strategyType = ResearchStrategyType.UNDER_3_5, market = "TG", selection = "U3.5", odds = 2.0, outcome = Outcome.LOSS, stake = 100.0, profit = -100.0),
            BetObservation(matchId = "5", date = "2024-01-05", strategyType = ResearchStrategyType.UNDER_3_5, market = "TG", selection = "U3.5", odds = 2.0, outcome = Outcome.LOSS, stake = 100.0, profit = -100.0),
            BetObservation(matchId = "6", date = "2024-01-06", strategyType = ResearchStrategyType.UNDER_3_5, market = "TG", selection = "U3.5", odds = 2.0, outcome = Outcome.WIN, stake = 100.0, profit = 100.0)
        )

        val analysis = DrawdownCalculator.analyzeDrawdown(obs)
        // Peak profit was $200 at step 2. Trough was -$100 at step 5. Max drawdown = $300.
        assertEquals(300.0, analysis.maxDrawdownAmount, 0.001)
        // Longest losing streak was 3 consecutive losses (steps 3, 4, 5)
        assertEquals(3, analysis.longestLosingStreak)
        assertEquals(200.0, analysis.peakProfit, 0.001)
        assertEquals(0.0, analysis.finalProfit, 0.001)
    }

    @Test
    fun testBootstrapCalculationsAreDeterministic() {
        val obs = (1..50).map { i ->
            BetObservation(
                matchId = "m_$i",
                date = "2024-01-${String.format("%02d", i.coerceAtMost(28))}",
                strategyType = ResearchStrategyType.UNDER_3_5,
                market = "Total Goals",
                selection = "Under 3.5",
                odds = 1.50,
                outcome = if (i % 3 != 0) Outcome.WIN else Outcome.LOSS,
                stake = 100.0
            )
        }

        val run1 = BootstrapCalculator.calculateRoiBootstrap(obs, iterations = 2000, seed = 42L)
        val run2 = BootstrapCalculator.calculateRoiBootstrap(obs, iterations = 2000, seed = 42L)

        // Seeded generator guarantees reproducibility for data integrity
        assertEquals(run1.lowerCi95, run2.lowerCi95, 0.0001)
        assertEquals(run1.upperCi95, run2.upperCi95, 0.0001)
        assertEquals(run1.bootstrapMedianRoi, run2.bootstrapMedianRoi, 0.0001)
        assertTrue("Lower CI must be <= upper CI", run1.lowerCi95 <= run1.upperCi95)
    }

    @Test
    fun testChronologicalSampleSplitter() {
        val obs = (1..100).map { i ->
            BetObservation(
                matchId = "m_$i",
                date = "2024-01-${String.format("%02d", ((i - 1) % 28) + 1)}",
                strategyType = ResearchStrategyType.UNDER_3_5,
                market = "TG",
                selection = "U3.5",
                odds = 1.80,
                outcome = Outcome.WIN,
                stake = 100.0,
                profit = 80.0
            )
        }

        val split = SampleSplitter.splitChronological(obs, trainRatio = 0.60, valRatio = 0.20)
        assertEquals(60, split.training.sampleSize)
        assertEquals(20, split.validation.sampleSize)
        assertEquals(20, split.outOfSample.sampleSize)
        assertTrue(split.isOosProfitable)
    }

    @Test
    fun testOutlierRobustnessSensitivity() {
        // Create 10 bets: 9 lose $100 each (-$900), 1 wins at 15.00 odds (+$1400). Total profit = +$500.
        val normalLosers = (1..9).map { i ->
            BetObservation(
                matchId = "m_$i",
                date = "2024-01-01",
                strategyType = ResearchStrategyType.UNDER_3_5,
                market = "TG",
                selection = "U3.5",
                odds = 2.0,
                outcome = Outcome.LOSS,
                stake = 100.0,
                profit = -100.0
            )
        }
        val freakWinner = BetObservation(
            matchId = "m_10",
            date = "2024-01-02",
            strategyType = ResearchStrategyType.UNDER_3_5,
            market = "TG",
            selection = "U3.5",
            odds = 15.0,
            outcome = Outcome.WIN,
            stake = 100.0,
            profit = 1400.0
        )
        val dataset = normalLosers + freakWinner

        val robustness = OutlierRobustnessCalculator.analyzeOutlierRobustness(dataset)
        assertTrue("Baseline is positive (+50%)", robustness.baseline.roi > 0.0)
        // Removing single top winner leaves 9 losses -> ROI is -100%
        assertEquals(-100.0, robustness.minusTop1.roi, 0.001)
        assertFalse("Strategy fails outlier test because edge collapses", robustness.survivesOutlierRemoval)
    }

    @Test
    fun testCsvParserWithMissingTotalGoalsAndMalformedRows() {
        val csv = """
            date,home_team,away_team,home_goals,away_goals,market,selection,odds,result
            2024-05-01,Real Madrid,Bayern Munich,2,1,Total Goals,Under 3.5,1.50,WIN
            2024-05-02,Dortmund,PSG,1,0,Total Goals,Under 3.5,1.55,
            2024-05-03,Malformed Row Without Score
            2024-05-04,Arsenal,City,2,2,Total Goals,Under 3.5,,WIN
        """.trimIndent()

        val parsed = CsvParser.parse(csv)
        assertEquals(2, parsed.validRowsCount)
        assertEquals(2, parsed.errors.size) // 2 malformed rows detected with reasons
        assertEquals(3, parsed.matches.size) // 3 distinct matches encountered in file

        // Check derived outcome on second row (missing result column, but 1-0 score <= 3.5 -> WIN)
        val row2Obs = parsed.observations.firstOrNull { it.matchId.contains("Dortmund") }
        assertNotNull(row2Obs)
        assertEquals(Outcome.WIN, row2Obs?.outcome)
    }
}

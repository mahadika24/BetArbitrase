package com.example.stats

import com.example.data.model.BetObservation
import java.util.Random
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class HistogramBin(
    val binStart: Double,
    val binEnd: Double,
    val count: Int,
    val percentage: Double
)

data class BootstrapResult(
    val observedRoi: Double,
    val bootstrapMedianRoi: Double,
    val bootstrapMeanRoi: Double,
    val lowerCi95: Double,
    val upperCi95: Double,
    val iterations: Int,
    val probabilityPositiveRoi: Double, // % of bootstrap runs with ROI > 0
    val histogramBins: List<HistogramBin>
)

object BootstrapCalculator {

    private const val DEFAULT_ITERATIONS = 10_000
    private const val DEFAULT_SEED = 123456789L

    /**
     * Performs a deterministic non-parametric bootstrap resampling on the observations to estimate
     * ROI uncertainty.
     * Uses seeded pseudo-random generator to ensure exact reproducibility across runs.
     */
    fun calculateRoiBootstrap(
        observations: List<BetObservation>,
        iterations: Int = DEFAULT_ITERATIONS,
        seed: Long = DEFAULT_SEED,
        numBins: Int = 16
    ): BootstrapResult {
        if (observations.isEmpty()) {
            return BootstrapResult(
                observedRoi = 0.0,
                bootstrapMedianRoi = 0.0,
                bootstrapMeanRoi = 0.0,
                lowerCi95 = 0.0,
                upperCi95 = 0.0,
                iterations = iterations,
                probabilityPositiveRoi = 0.0,
                histogramBins = emptyList()
            )
        }

        val n = observations.size
        val totalStakeObserved = observations.sumOf { it.stake }
        val totalProfitObserved = observations.sumOf { it.profit }
        val observedRoi = ProfitCalculator.calculateRoi(totalProfitObserved, totalStakeObserved)

        // Seeded deterministic random generator for data integrity and reproducibility
        val random = Random(seed)

        // Pre-extract profit and stake arrays for fast in-memory execution of 10,000 resamples
        val profits = DoubleArray(n) { observations[it].profit }
        val stakes = DoubleArray(n) { observations[it].stake }

        val resampledRois = DoubleArray(iterations)
        var positiveCount = 0

        for (b in 0 until iterations) {
            var sumProfit = 0.0
            var sumStake = 0.0
            for (i in 0 until n) {
                val randomIndex = random.nextInt(n)
                sumProfit += profits[randomIndex]
                sumStake += stakes[randomIndex]
            }
            val resampledRoi = if (sumStake > 0.0) (sumProfit / sumStake) * 100.0 else 0.0
            resampledRois[b] = resampledRoi
            if (resampledRoi > 0.0) {
                positiveCount++
            }
        }

        resampledRois.sort()

        val lowerIdx = max(0, min(iterations - 1, floor(iterations * 0.025).toInt()))
        val upperIdx = max(0, min(iterations - 1, floor(iterations * 0.975).toInt()))
        val medianIdx = iterations / 2

        val lowerCi95 = resampledRois[lowerIdx]
        val upperCi95 = resampledRois[upperIdx]
        val medianRoi = resampledRois[medianIdx]
        val meanRoi = resampledRois.average()
        val probPositive = (positiveCount.toDouble() / iterations) * 100.0

        // Build histogram bins for visual distribution
        val minRoi = resampledRois.first()
        val maxRoi = resampledRois.last()
        val range = if (maxRoi - minRoi < 0.001) 1.0 else maxRoi - minRoi
        val binWidth = range / numBins

        val binCounts = IntArray(numBins)
        for (roi in resampledRois) {
            var bin = floor((roi - minRoi) / binWidth).toInt()
            if (bin >= numBins) bin = numBins - 1
            if (bin < 0) bin = 0
            binCounts[bin]++
        }

        val bins = ArrayList<HistogramBin>(numBins)
        for (i in 0 until numBins) {
            val start = minRoi + i * binWidth
            val end = start + binWidth
            val count = binCounts[i]
            val pct = (count.toDouble() / iterations) * 100.0
            bins.add(HistogramBin(start, end, count, pct))
        }

        return BootstrapResult(
            observedRoi = observedRoi,
            bootstrapMedianRoi = medianRoi,
            bootstrapMeanRoi = meanRoi,
            lowerCi95 = lowerCi95,
            upperCi95 = upperCi95,
            iterations = iterations,
            probabilityPositiveRoi = probPositive,
            histogramBins = bins
        )
    }
}

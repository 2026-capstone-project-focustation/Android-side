package net.focustation.myapplication.score

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

data class ScoringParams(
    // ── 조도 (EN 12464-1) ──
    val lightOptimalMin: Float = 300f,
    val lightOptimalMax: Float = 500f,
    val lightLevelSigma: Double = 300.0,
    val lightStabilitySigma: Double = 0.25,
    val lightLevelWeight: Double = 0.7,
    val lightStabilityWeight: Double = 0.3,

    // ── 소음 (ISO 1996-1) ──
    val noiseIdealDb: Double = 35.0,
    val noiseWorstDb: Double = 75.0,
    val noisePeakWorstDb: Double = 85.0,
    val noiseExceedThresholdDb: Double = 55.0,
    val noiseLevelWeight: Double = 0.6,
    val noisePeakWeight: Double = 0.2,
    val noiseExceedWeight: Double = 0.2,

    // ── 진동 (ISO 2631) ──
    val vibrationRmsMin: Double = 0.005,
    val vibrationRmsMax: Double = 0.1,
)

object ScoreCalculator {

    val DEFAULT_PARAMS = ScoringParams()

    fun calculateLightScore(
        samples: List<Float>,
        params: ScoringParams = DEFAULT_PARAMS,
    ): Double {
        if (samples.isEmpty()) return 0.0

        val mean = samples.map { it.toDouble() }.average()

        val deviation = when {
            mean < params.lightOptimalMin -> params.lightOptimalMin - mean
            mean > params.lightOptimalMax -> mean - params.lightOptimalMax
            else -> 0.0
        }
        val sigma = params.lightLevelSigma
        val fLevel = exp(-(deviation * deviation) / (2.0 * sigma * sigma))

        val fStability = if (samples.size < 2) {
            1.0
        } else {
            val stdDev = sqrt(samples.map { (it.toDouble() - mean).pow(2) }.average())
            val cv = if (mean > 0) stdDev / mean else 0.0
            val sigmaSt = params.lightStabilitySigma
            exp(-(cv * cv) / (2.0 * sigmaSt * sigmaSt))
        }

        return (100.0 * (params.lightLevelWeight * fLevel + params.lightStabilityWeight * fStability))
            .coerceIn(0.0, 100.0)
    }

    fun calculateNoiseScore(
        samples: List<Double>,
        params: ScoringParams = DEFAULT_PARAMS,
    ): Double {
        if (samples.isEmpty()) return 0.0

        val clamped = samples.map { it.coerceAtLeast(0.0) }
        val lAeq = calculateLAeq(clamped)

        val fLevel = when {
            lAeq <= params.noiseIdealDb -> 1.0
            lAeq >= params.noiseWorstDb -> 0.0
            else -> 1.0 - (lAeq - params.noiseIdealDb) / (params.noiseWorstDb - params.noiseIdealDb)
        }

        val peak = clamped.max()
        val fPeak = when {
            peak <= params.noiseIdealDb -> 1.0
            peak >= params.noisePeakWorstDb -> 0.0
            else -> 1.0 - (peak - params.noiseIdealDb) / (params.noisePeakWorstDb - params.noiseIdealDb)
        }

        val r = clamped.count { it > params.noiseExceedThresholdDb }.toDouble() / clamped.size

        return (100.0 * (params.noiseLevelWeight * fLevel + params.noisePeakWeight * fPeak + params.noiseExceedWeight * (1.0 - r)))
            .coerceIn(0.0, 100.0)
    }

    fun calculateLAeq(dbValues: List<Double>): Double {
        if (dbValues.isEmpty()) return 0.0
        val energyMean = dbValues.sumOf { 10.0.pow(it / 10.0) } / dbValues.size
        return 10.0 * log10(energyMean)
    }

    fun calculateVibrationScore(
        samples: List<Double>,
        params: ScoringParams = DEFAULT_PARAMS,
    ): Double {
        if (samples.isEmpty()) return 0.0

        val rms = sqrt(samples.sumOf { it * it } / samples.size)
        val range = params.vibrationRmsMax - params.vibrationRmsMin
        val rmsNorm = if (range > 0) {
            ((rms - params.vibrationRmsMin) / range).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        return (100.0 * (1.0 - rmsNorm)).coerceIn(0.0, 100.0)
    }

    fun calculateTotalScore(
        lightScore: Double,
        noiseScore: Double,
        vibrationScore: Double,
    ): Double = ((lightScore + noiseScore + vibrationScore) / 3.0).coerceIn(0.0, 100.0)

    // 권한 미허용 등으로 일부 센서만 유효할 때 사용
    fun calculateTotalScore(scores: List<Double>): Double {
        if (scores.isEmpty()) return 0.0
        return scores.average().coerceIn(0.0, 100.0)
    }
}

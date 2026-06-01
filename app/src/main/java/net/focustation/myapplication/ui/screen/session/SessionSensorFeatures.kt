package net.focustation.myapplication.ui.screen.session

import net.focustation.myapplication.data.repository.SensorSummaryPayload
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

private const val NOISE_SPIKE_MIN_DB = 55.0
private const val NOISE_SPIKE_DELTA_DB = 10.0
private const val VIBRATION_SPIKE_THRESHOLD = 0.08
private const val PHONE_MOVEMENT_THRESHOLD = 0.05

internal fun buildSensorSummaryPayload(
    noiseSamples: List<Double>,
    lightSamples: List<Float>,
    vibrationSamples: List<Double>,
    measurementDurationSec: Int,
): SensorSummaryPayload {
    val noise = summarize(noiseSamples)
    val light = summarize(lightSamples.map { it.toDouble() })
    val vibration = summarize(vibrationSamples)

    return SensorSummaryPayload(
        noiseMeanDb = noise.mean,
        noiseStdDb = noise.std,
        noiseMaxDb = noise.max,
        noiseP90Db = percentile(noiseSamples, 90.0),
        noiseSpikeCount = countNoiseSpikes(noiseSamples),
        lightMeanLux = light.mean,
        lightStdLux = light.std,
        lightMinLux = light.min,
        lightMaxLux = light.max,
        vibrationMean = vibration.mean,
        vibrationStd = vibration.std,
        vibrationMax = vibration.max,
        vibrationP95 = percentile(vibrationSamples, 95.0),
        vibrationSpikeCount = countThresholdCrossings(vibrationSamples, VIBRATION_SPIKE_THRESHOLD),
        measurementDurationSec = measurementDurationSec.coerceAtLeast(0),
        validSampleRatio = validDomainRatio(noiseSamples, lightSamples, vibrationSamples),
        phoneMovementRatio = movementRatio(vibrationSamples),
    )
}

internal fun buildSensorTargetV2ModelInput(
    surveyModelInput: Map<String, Any>,
    placeFeedback: Map<String, Any>,
    sensorSummary: SensorSummaryPayload,
): Map<String, Any> =
    linkedMapOf<String, Any>().apply {
        putAll(surveyModelInput)
        putAll(placeFeedback)
        putAll(sensorSummary.toFeatureMap())
    }

private data class NumericSummary(
    val mean: Double = 0.0,
    val std: Double = 0.0,
    val min: Double = 0.0,
    val max: Double = 0.0,
)

private fun summarize(samples: List<Double>): NumericSummary {
    if (samples.isEmpty()) return NumericSummary()

    val mean = samples.average()
    val std = sqrt(samples.map { (it - mean).pow(2) }.average())
    return NumericSummary(
        mean = mean,
        std = std,
        min = samples.min(),
        max = samples.max(),
    )
}

private fun percentile(
    samples: List<Double>,
    percentile: Double,
): Double {
    if (samples.isEmpty()) return 0.0

    val sorted = samples.sorted()
    val rank = (percentile.coerceIn(0.0, 100.0) / 100.0) * (sorted.lastIndex)
    val lowerIndex = floor(rank).toInt()
    val upperIndex = ceil(rank).toInt()
    if (lowerIndex == upperIndex) return sorted[lowerIndex]

    val weight = rank - lowerIndex
    return sorted[lowerIndex] * (1.0 - weight) + sorted[upperIndex] * weight
}

private fun countNoiseSpikes(samples: List<Double>): Int =
    samples
        .zipWithNext()
        .count { (previous, current) ->
            current >= NOISE_SPIKE_MIN_DB && current - previous >= NOISE_SPIKE_DELTA_DB
        }

private fun countThresholdCrossings(
    samples: List<Double>,
    threshold: Double,
): Int {
    var previousWasAbove = false
    var count = 0

    samples.forEach { sample ->
        val isAbove = sample >= threshold
        if (isAbove && !previousWasAbove) count += 1
        previousWasAbove = isAbove
    }

    return count
}

private fun validDomainRatio(
    noiseSamples: List<Double>,
    lightSamples: List<Float>,
    vibrationSamples: List<Double>,
): Double {
    val validDomains =
        listOf(
            noiseSamples.isNotEmpty(),
            lightSamples.isNotEmpty(),
            vibrationSamples.isNotEmpty(),
        ).count { it }

    return validDomains / 3.0
}

private fun movementRatio(samples: List<Double>): Double? {
    if (samples.isEmpty()) return null
    return samples.count { it >= PHONE_MOVEMENT_THRESHOLD }.toDouble() / samples.size
}

private fun SensorSummaryPayload.toFeatureMap(): Map<String, Any> =
    linkedMapOf<String, Any>(
        "noise_mean_db" to noiseMeanDb,
        "noise_std_db" to noiseStdDb,
        "noise_max_db" to noiseMaxDb,
        "noise_p90_db" to noiseP90Db,
        "noise_spike_count" to noiseSpikeCount,
        "light_mean_lux" to lightMeanLux,
        "light_std_lux" to lightStdLux,
        "light_min_lux" to lightMinLux,
        "light_max_lux" to lightMaxLux,
        "vibration_mean" to vibrationMean,
        "vibration_std" to vibrationStd,
        "vibration_max" to vibrationMax,
        "vibration_p95" to vibrationP95,
        "vibration_spike_count" to vibrationSpikeCount,
        "measurement_duration_sec" to measurementDurationSec,
    ).apply {
        validSampleRatio?.let { put("valid_sample_ratio", it) }
        phoneMovementRatio?.let { put("phone_movement_ratio", it) }
    }

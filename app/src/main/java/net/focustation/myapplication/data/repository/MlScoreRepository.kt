package net.focustation.myapplication.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.focustation.myapplication.BuildConfig
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

data class MlScoreRequest(
    val modelInput: Map<String, Any>,
)

data class MlScoreResponse(
    val score: Double,
)

class MlScoreRepository(
    private val baseUrl: String = BuildConfig.ML_SERVER_BASE_URL,
    private val apiKey: String = BuildConfig.ML_API_KEY,
) {
    suspend fun calculateScore(modelInput: Map<String, Any>): Result<MlScoreResponse> =
        runCatching {
            if (baseUrl.isBlank()) {
                error("ML_SERVER_BASE_URL이 설정되지 않았어요.")
            }
            if (apiKey.isBlank()) {
                error("ML_API_KEY가 설정되지 않았어요.")
            }

            withContext(Dispatchers.IO) {
                val endpoint = "${baseUrl.trimEnd('/')}/score"
                val connection = URI.create(endpoint).toURL().openConnection() as HttpURLConnection
                try {
                    val body = MlScoreRequest(modelInput).toJson().toString()
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.doOutput = true
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("X-API-Key", apiKey)

                    connection.outputStream.use { output ->
                        output.write(body.toByteArray(Charsets.UTF_8))
                    }

                    val responseCode = connection.responseCode
                    val responseBody =
                        if (responseCode in 200..299) {
                            connection.inputStream.bufferedReader().use { it.readText() }
                        } else {
                            connection.errorStream
                                ?.bufferedReader()
                                ?.use { it.readText() }
                                .orEmpty()
                        }

                    if (responseCode !in 200..299) {
                        throw IOException(
                            "ML score 요청 실패 ($responseCode): ${responseBody.take(MAX_ERROR_BODY_LENGTH)}",
                        )
                    }

                    MlScoreResponse(score = JSONObject(responseBody).getDouble("score"))
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun MlScoreRequest.toJson(): JSONObject {
        val json = JSONObject()
        stringFields.forEach { key ->
            json.put(key, modelInput[key]?.toString().orEmpty())
        }
        numberFields.forEach { key ->
            json.put(key, (modelInput[key] as? Number)?.toDouble() ?: 0.0)
        }
        return json
    }

    private companion object {
        private const val MAX_ERROR_BODY_LENGTH = 500

        private val stringFields =
            listOf(
                "user_type",
                "general_place_type",
                "general_task_type",
                "general_social_mode",
                "general_stay_duration",
                "general_time_slot",
                "place_type",
                "task_type",
                "group_size",
                "stay_duration",
                "time_slot",
                "day_type",
                "weather",
                "indoor_outdoor",
                "visit_frequency",
            )

        private val numberFields =
            listOf(
                "pref_quiet",
                "pref_light",
                "pref_low_crowd",
                "pref_privacy",
                "pref_outlet",
                "pref_distance",
                "pref_thermal_air",
                "pref_control",
                "pref_comfort",
                "pref_deepwork",
                "general_distraction_noise",
                "general_distraction_crowd",
                "general_distraction_visual",
                "general_distraction_temperature",
                "general_distraction_outlet",
                "general_distraction_distance",
                "general_priority_quiet",
                "general_priority_outlet",
                "general_priority_distance",
                "general_priority_comfort",
                "general_priority_privacy",
                "distance_minutes",
                "place_quiet",
                "place_light",
                "place_low_crowd",
                "place_low_visual_distraction",
                "place_control",
                "place_comfort",
                "place_outlet",
                "place_task_fit",
                "place_temperature_air",
                "place_seat_availability",
                "quiet_match",
                "light_match",
                "crowd_match",
                "privacy_match",
                "outlet_match",
                "thermal_air_match",
                "control_match",
                "comfort_match",
                "deepwork_task_match",
                "distance_penalty",
                "task_place_fit_match",
                "time_match",
            )
    }
}

package net.focustation.myapplication.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import net.focustation.myapplication.BuildConfig
import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.util.DebugLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

data class PlaceSnapshotPayload(
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val naverPlaceId: String? = null,
    val address: String? = null,
    val roadAddress: String? = null,
    val category: String? = null,
)

data class CreateSessionRequest(
    val durationSec: Int,
    val avgEnvironmentScore: Double,
    val avgNoise: Double,
    val avgIlluminance: Double,
    val avgVibration: Double,
    val focusTimeline: List<FocusDataPoint>,
    val placeSnapshot: PlaceSnapshotPayload,
    // context 10 + place 10 = 20개, snake_case. 라벨은 현재 계약에 없으므로 포함하지 않는다.
    val placeFeedback: Map<String, Any>,
)

data class CreateSessionResult(
    val sessionId: String,
    val mlScore: Double,
)

/**
 * Firebase Functions `POST /sessions` 클라이언트.
 * Functions가 설문 프로필(27)과 합쳐 ML 50 입력을 구성하고 Firestore에 세션 문서를 생성한다.
 * Android는 세션을 Firestore에 직접 쓰지 않는다.
 */
class SessionApiRepository(
    private val baseUrl: String = BuildConfig.FUNCTIONS_BASE_URL,
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) {
    suspend fun createSession(request: CreateSessionRequest): Result<CreateSessionResult> =
        runCatching {
            if (baseUrl.isBlank()) error("FUNCTIONS_BASE_URL이 설정되지 않았어요.")
            val token =
                authProvider()
                    .currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
                    ?: error("로그인 후 세션을 저장할 수 있어요.")

            withContext(Dispatchers.IO) {
                val endpoint = "${baseUrl.trimEnd('/')}/sessions"
                val connection = URI.create(endpoint).toURL().openConnection() as HttpURLConnection
                try {
                    val body = request.toJson().toString()
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.doOutput = true
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $token")

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
                        throw IOException(messageForError(responseCode, responseBody))
                    }

                    val json = JSONObject(responseBody)
                    CreateSessionResult(
                        sessionId = json.optString("sessionId"),
                        mlScore = json.optDouble("mlScore", -1.0),
                    )
                } finally {
                    connection.disconnect()
                }
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            DebugLog.e("[SessionApi][POST] 실패: ${error.message}", error)
        }

    private fun messageForError(
        responseCode: Int,
        responseBody: String,
    ): String {
        val code =
            runCatching {
                val json = JSONObject(responseBody)
                json.optString("error").ifBlank { json.optString("code") }
            }.getOrNull().orEmpty()

        return when (code) {
            "SURVEY_NOT_FOUND" -> "설문을 먼저 완료해주세요. 설문 응답이 없어 리포트를 만들 수 없어요."
            "UNAUTHENTICATED" -> "로그인이 만료됐어요. 다시 로그인해주세요."
            "INVALID_SESSION_PAYLOAD" -> "세션 데이터가 올바르지 않아요."
            else -> "세션 저장에 실패했어요. ($responseCode)"
        }
    }

    private fun CreateSessionRequest.toJson(): JSONObject {
        val timelineArray = JSONArray()
        focusTimeline.forEach { point ->
            timelineArray.put(
                JSONObject()
                    .put("timeLabel", point.timeLabel)
                    .put("focusScore", point.focusScore.toDouble()),
            )
        }

        val snapshotJson =
            JSONObject()
                .put("name", placeSnapshot.name)
                .put("latitude", placeSnapshot.latitude ?: JSONObject.NULL)
                .put("longitude", placeSnapshot.longitude ?: JSONObject.NULL)
        placeSnapshot.naverPlaceId?.takeIf { it.isNotBlank() }?.let { snapshotJson.put("naverPlaceId", it) }
        placeSnapshot.address?.takeIf { it.isNotBlank() }?.let { snapshotJson.put("address", it) }
        placeSnapshot.roadAddress?.takeIf { it.isNotBlank() }?.let { snapshotJson.put("roadAddress", it) }
        placeSnapshot.category?.takeIf { it.isNotBlank() }?.let { snapshotJson.put("category", it) }

        val feedbackJson = JSONObject()
        placeFeedback.forEach { (key, value) ->
            when (value) {
                is Float -> feedbackJson.put(key, value.toDouble())
                is Number -> feedbackJson.put(key, value)
                else -> feedbackJson.put(key, value.toString())
            }
        }

        return JSONObject()
            .put("durationSec", durationSec)
            .put("avgEnvironmentScore", avgEnvironmentScore)
            .put("avgNoise", avgNoise)
            .put("avgIlluminance", avgIlluminance)
            .put("avgVibration", avgVibration)
            .put("focusTimeline", timelineArray)
            .put("placeSnapshot", snapshotJson)
            .put("placeFeedback", feedbackJson)
    }
}

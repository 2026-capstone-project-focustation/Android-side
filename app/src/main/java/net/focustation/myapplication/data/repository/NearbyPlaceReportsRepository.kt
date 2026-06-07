package net.focustation.myapplication.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import net.focustation.myapplication.BuildConfig
import net.focustation.myapplication.util.DebugLog
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class PublicPlaceReportRecord(
    val placeKey: String,
    val placeName: String,
    val category: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceM: Double,
    val reportCount: Int,
    val mlScoreAvg: Double,
    val avgNoise: Double,
    val avgIlluminance: Double,
    val avgVibration: Double,
    val lastReportedAt: String?,
)

class NearbyPlaceReportsRepository(
    private val baseUrl: String = BuildConfig.FUNCTIONS_BASE_URL,
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) {
    suspend fun getNearbyPlaceReports(
        latitude: Double,
        longitude: Double,
        radiusM: Int = DEFAULT_RADIUS_M,
        limit: Int = DEFAULT_LIMIT,
    ): Result<List<PublicPlaceReportRecord>> =
        runCatching {
            if (baseUrl.isBlank()) error("FUNCTIONS_BASE_URL이 설정되지 않았어요.")
            val token =
                authProvider()
                    .currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
                    ?: error("로그인 후 공개 공간 요약을 볼 수 있어요.")

            withContext(Dispatchers.IO) {
                val endpoint =
                    "${baseUrl.trimEnd('/')}/nearbyPlaceReports" +
                        "?lat=${latitude.urlEncoded()}" +
                        "&lng=${longitude.urlEncoded()}" +
                        "&radiusM=${radiusM.coerceIn(1, MAX_RADIUS_M)}" +
                        "&limit=${limit.coerceIn(1, MAX_LIMIT)}"
                val connection = URI.create(endpoint).toURL().openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $token")

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
                        throw IOException("공개 공간 요약 조회 실패 ($responseCode): ${responseBody.take(500)}")
                    }

                    parseReports(responseBody)
                } finally {
                    connection.disconnect()
                }
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            DebugLog.e("[NearbyPlaceReports][GET] 실패: ${error.message}", error)
        }

    suspend fun getPlaceReportsInBounds(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        limit: Int = BOUNDS_DEFAULT_LIMIT,
    ): Result<List<PublicPlaceReportRecord>> =
        runCatching {
            if (baseUrl.isBlank()) error("FUNCTIONS_BASE_URL이 설정되지 않았어요.")
            val token =
                authProvider()
                    .currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
                    ?: error("로그인 후 공개 공간 요약을 볼 수 있어요.")

            withContext(Dispatchers.IO) {
                val endpoint =
                    "${baseUrl.trimEnd('/')}/placeReportsInBounds" +
                        "?north=${north.urlEncoded()}" +
                        "&south=${south.urlEncoded()}" +
                        "&east=${east.urlEncoded()}" +
                        "&west=${west.urlEncoded()}" +
                        "&limit=${limit.coerceIn(1, BOUNDS_MAX_LIMIT)}"
                val connection = URI.create(endpoint).toURL().openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $token")

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
                        throw IOException(messageForBoundsError(responseCode, responseBody))
                    }

                    parseReports(responseBody)
                } finally {
                    connection.disconnect()
                }
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            DebugLog.e("[PlaceReportsInBounds][GET] 실패: ${error.message}", error)
        }

    private fun parseReports(responseBody: String): List<PublicPlaceReportRecord> {
        val reports = JSONObject(responseBody).optJSONArray("reports") ?: return emptyList()
        return (0 until reports.length()).mapNotNull { index ->
            val item = reports.optJSONObject(index) ?: return@mapNotNull null
            val placeKey = item.optString("placeKey").trim()
            val placeName = item.optString("placeName").trim()
            val latitude = item.optDouble("latitude", Double.NaN)
            val longitude = item.optDouble("longitude", Double.NaN)
            if (placeKey.isBlank() || placeName.isBlank() || !latitude.isFinite() || !longitude.isFinite()) {
                return@mapNotNull null
            }
            PublicPlaceReportRecord(
                placeKey = placeKey,
                placeName = placeName,
                category = item.optString("category").ifBlank { null },
                latitude = latitude,
                longitude = longitude,
                distanceM = item.optDouble("distanceM", 0.0),
                reportCount = item.optInt("reportCount", 0),
                mlScoreAvg = item.optDouble("mlScoreAvg", 0.0),
                avgNoise = item.optDouble("avgNoise", 0.0),
                avgIlluminance = item.optDouble("avgIlluminance", 0.0),
                avgVibration = item.optDouble("avgVibration", 0.0),
                lastReportedAt = item.optString("lastReportedAt").ifBlank { null },
            )
        }
    }

    private fun Double.urlEncoded(): String = URLEncoder.encode(toString(), Charsets.UTF_8.name())

    private fun messageForBoundsError(
        responseCode: Int,
        responseBody: String,
    ): String {
        val code =
            runCatching {
                JSONObject(responseBody).optString("code")
            }.getOrNull().orEmpty()
        return when (code) {
            "BOUNDS_TOO_LARGE" -> "지도를 확대하면 이 영역의 공개 공간 요약을 볼 수 있어요."
            else -> "공개 공간 요약 조회 실패 ($responseCode): ${responseBody.take(500)}"
        }
    }

    private companion object {
        private const val DEFAULT_RADIUS_M = 1_500
        private const val DEFAULT_LIMIT = 30
        private const val MAX_RADIUS_M = 5_000
        private const val MAX_LIMIT = 50
        private const val BOUNDS_DEFAULT_LIMIT = 80
        private const val BOUNDS_MAX_LIMIT = 100
    }
}

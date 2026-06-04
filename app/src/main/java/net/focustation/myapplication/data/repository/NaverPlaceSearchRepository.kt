package net.focustation.myapplication.data.repository

import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.focustation.myapplication.BuildConfig
import net.focustation.myapplication.util.isPlausibleKoreanCoordinate
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class NaverPlaceSearchResult(
    val name: String,
    val category: String,
    val address: String,
    val roadAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

class NaverPlaceSearchRepository(
    private val clientId: String = BuildConfig.NAVER_SEARCH_CLIENT_ID,
    private val clientSecret: String = BuildConfig.NAVER_SEARCH_CLIENT_SECRET,
) {
    suspend fun searchPlaces(
        query: String,
        addressHint: String,
    ): Result<List<NaverPlaceSearchResult>> =
        runCatching {
            if (clientId.isBlank() || clientSecret.isBlank()) {
                error("NAVER_SEARCH_CLIENT_ID 또는 NAVER_SEARCH_CLIENT_SECRET이 설정되지 않았어요.")
            }

            val normalizedQuery =
                listOf(addressHint, query)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            if (normalizedQuery.isBlank()) return@runCatching emptyList()

            withContext(Dispatchers.IO) {
                val encodedQuery = URLEncoder.encode(normalizedQuery, Charsets.UTF_8.name())
                val endpoint =
                    "https://openapi.naver.com/v1/search/local.json" +
                        "?query=$encodedQuery&display=5&start=1&sort=random"
                val connection = URI.create(endpoint).toURL().openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("X-Naver-Client-Id", clientId)
                    connection.setRequestProperty("X-Naver-Client-Secret", clientSecret)

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
                            "Naver 장소 검색 실패 ($responseCode): ${responseBody.take(MAX_ERROR_BODY_LENGTH)}",
                        )
                    }

                    parsePlaces(responseBody)
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun parsePlaces(responseBody: String): List<NaverPlaceSearchResult> {
        val items = JSONObject(responseBody).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("title").cleanNaverText()
            if (name.isBlank()) return@mapNotNull null
            val latLng = item.toLatLngOrNull()

            NaverPlaceSearchResult(
                name = name,
                category = item.optString("category").cleanNaverText(),
                address = item.optString("address").cleanNaverText(),
                roadAddress = item.optString("roadAddress").cleanNaverText(),
                latitude = latLng?.latitude,
                longitude = latLng?.longitude,
            )
        }
    }

    private fun JSONObject.toLatLngOrNull() =
        runCatching {
            val mapX = optString("mapx").toDoubleOrNull() ?: return@runCatching null
            val mapY = optString("mapy").toDoubleOrNull() ?: return@runCatching null
            // 네이버 지역 검색 API는 mapx/mapy를 WGS84 경위도 × 10^7 정수로 반환한다.
            LatLng(mapY / 1e7, mapX / 1e7).takeIf {
                it.isValid() && isPlausibleKoreanCoordinate(it.latitude, it.longitude)
            }
        }.getOrNull()

    private fun String.cleanNaverText(): String =
        replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()

    private companion object {
        private const val MAX_ERROR_BODY_LENGTH = 500
    }
}

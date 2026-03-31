package net.focustation.myapplication.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import net.focustation.myapplication.data.model.FocusDataPoint
import java.util.Locale
import kotlin.math.abs

data class StudySessionSaveRequest(
    val totalFocusMinutes: Int,
    val avgEnvironmentScore: Float,
    val avgNoise: Float,
    val avgIlluminance: Float,
    val avgVibration: Double,
    val focusTimeline: List<FocusDataPoint>,
    val placeName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class SavedPlaceRequest(
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class StudySessionRecord(
    val sessionId: String,
    val endedAtEpochMillis: Long,
    val durationSec: Int,
    val focusScoreAvg: Float,
    val avgNoise: Float,
    val avgIlluminance: Float,
    val avgVibration: Double,
    val placeName: String,
    val focusTimeline: List<FocusDataPoint> = emptyList(),
)

class FirestoreStudyRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) {
    private val firestore by lazy { firestoreProvider() }
    private val auth by lazy { authProvider() }

    suspend fun saveStudySession(request: StudySessionSaveRequest): Result<String> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 기록을 저장할 수 있어요.")
            val sessionId = generateSessionId()
            val now = Timestamp.now()
            val totalDurationSec = request.totalFocusMinutes * 60
            val normalizedPlaceName = request.placeName.ifBlank { "장소 미지정" }

            val sessionPayload =
                hashMapOf(
                    "sessionId" to sessionId,
                    "startedAt" to Timestamp(now.seconds - totalDurationSec, now.nanoseconds),
                    "endedAt" to now,
                    "durationSec" to totalDurationSec,
                    "avgNoise" to request.avgNoise,
                    "avgIlluminance" to request.avgIlluminance,
                    "avgVibration" to request.avgVibration,
                    "focusScoreAvg" to request.avgEnvironmentScore,
                    "focusTimeline" to
                        request.focusTimeline.map {
                            mapOf(
                                "timeLabel" to it.timeLabel,
                                "focusScore" to it.focusScore,
                            )
                        },
                    "placeSnapshot" to
                        mapOf(
                            "name" to normalizedPlaceName,
                            "latitude" to request.latitude,
                            "longitude" to request.longitude,
                        ),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

            firestore
                .collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)
                .set(sessionPayload)
                .await()

            sessionId
        }

    suspend fun savePlace(request: SavedPlaceRequest): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 장소를 저장할 수 있어요.")
            if (request.name.isBlank()) error("장소 이름이 비어 있어요.")
            val placeId = buildStablePlaceId(request)
            val placePayload =
                hashMapOf(
                    "name" to request.name,
                    "latitude" to request.latitude,
                    "longitude" to request.longitude,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                )

            firestore
                .collection("users")
                .document(uid)
                .collection("savedPlaces")
                .document(placeId)
                .set(placePayload)
                .await()
        }

    suspend fun getStudySessions(limit: Long = 50): Result<List<StudySessionRecord>> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 기록을 불러올 수 있어요.")
            val snapshot =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("sessions")
                    .orderBy("endedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

            snapshot.documents.map { doc ->
                val placeSnapshot = doc.get("placeSnapshot") as? Map<*, *>
                StudySessionRecord(
                    sessionId = doc.getString("sessionId") ?: doc.id,
                    endedAtEpochMillis = doc.getTimestamp("endedAt")?.toDate()?.time ?: 0L,
                    durationSec = doc.getLong("durationSec")?.toInt() ?: 0,
                    focusScoreAvg = (doc.getDouble("focusScoreAvg") ?: 0.0).toFloat(),
                    avgNoise = (doc.getDouble("avgNoise") ?: 0.0).toFloat(),
                    avgIlluminance = (doc.getDouble("avgIlluminance") ?: 0.0).toFloat(),
                    avgVibration = doc.getDouble("avgVibration") ?: 0.0,
                    placeName = placeSnapshot?.get("name") as? String ?: "장소 미지정",
                    focusTimeline = parseFocusTimeline(doc.get("focusTimeline")),
                )
            }
        }

    suspend fun getStudySessionById(sessionId: String): Result<StudySessionRecord> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 기록을 불러올 수 있어요.")
            val document =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("sessions")
                    .document(sessionId)
                    .get()
                    .await()

            if (!document.exists()) error("선택한 세션 기록을 찾을 수 없어요.")

            val placeSnapshot = document.get("placeSnapshot") as? Map<*, *>
            StudySessionRecord(
                sessionId = document.getString("sessionId") ?: document.id,
                endedAtEpochMillis = document.getTimestamp("endedAt")?.toDate()?.time ?: 0L,
                durationSec = document.getLong("durationSec")?.toInt() ?: 0,
                focusScoreAvg = (document.getDouble("focusScoreAvg") ?: 0.0).toFloat(),
                avgNoise = (document.getDouble("avgNoise") ?: 0.0).toFloat(),
                avgIlluminance = (document.getDouble("avgIlluminance") ?: 0.0).toFloat(),
                avgVibration = document.getDouble("avgVibration") ?: 0.0,
                placeName = placeSnapshot?.get("name") as? String ?: "장소 미지정",
                focusTimeline = parseFocusTimeline(document.get("focusTimeline")),
            )
        }

    private fun parseFocusTimeline(raw: Any?): List<FocusDataPoint> {
        val points = raw as? List<*> ?: return emptyList()
        return points.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val timeLabel = (map["timeLabel"] as? String)?.trim().orEmpty()
            val score = (map["focusScore"] as? Number)?.toFloat() ?: return@mapNotNull null
            if (timeLabel.isBlank()) return@mapNotNull null
            FocusDataPoint(
                timeLabel = timeLabel,
                focusScore = score.coerceIn(0f, 100f),
            )
        }
    }

    private fun generateSessionId(): String =
        "${System.currentTimeMillis()}-${(1000..9999).random()}"

    private fun buildStablePlaceId(request: SavedPlaceRequest): String {
        val normalizedName = request.name.trim().lowercase(Locale.ROOT)
        val lat = request.latitude?.let { "%.4f".format(Locale.ROOT, it) } ?: "na"
        val lon = request.longitude?.let { "%.4f".format(Locale.ROOT, it) } ?: "na"
        val raw = "$normalizedName|$lat|$lon"
        return "place_${abs(raw.hashCode())}"
    }
}

package net.focustation.myapplication.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
                            "name" to request.placeName,
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

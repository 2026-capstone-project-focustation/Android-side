package net.focustation.myapplication.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.util.DebugLog
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

            DebugLog.d(
                "[Firestore][세션저장][요청] uid=${uidForLog(
                    uid,
                )}, sessionId=$sessionId, 분=${request.totalFocusMinutes}, 타임라인=${request.focusTimeline.size}개",
            )

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
                    // 소프트 삭제 기본값: 신규 문서는 항상 표시 상태로 저장
                    "isDeleted" to false,
                    "deletedAt" to null,
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

            DebugLog.d("[Firestore][세션저장][성공] uid=${uidForLog(uid)}, sessionId=$sessionId")

            sessionId
        }.onFailure { error ->
            DebugLog.e("[Firestore][세션저장][실패] ${error.message}", error)
        }

    suspend fun savePlace(request: SavedPlaceRequest): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 장소를 저장할 수 있어요.")
            if (request.name.isBlank()) error("장소 이름이 비어 있어요.")
            val placeId = buildStablePlaceId(request)
            val placeRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("savedPlaces")
                    .document(placeId)
            DebugLog.d(
                "[Firestore][장소저장][요청] uid=${uidForLog(uid)}, placeId=$placeId, name=${request.name}",
            )
            val placePayload =
                hashMapOf(
                    "name" to request.name,
                    "latitude" to request.latitude,
                    "longitude" to request.longitude,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            val exists = placeRef.get().await().exists()
            if (!exists) {
                placePayload["createdAt"] = FieldValue.serverTimestamp()
            }

            placeRef
                .set(placePayload, SetOptions.merge())
                .await()
            DebugLog.d("[Firestore][장소저장][성공] uid=${uidForLog(uid)}, placeId=$placeId")
        }.onFailure { error ->
            DebugLog.e("[Firestore][장소저장][실패] ${error.message}", error)
        }

    suspend fun getStudySessions(limit: Long = 50): Result<List<StudySessionRecord>> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 기록을 불러올 수 있어요.")
            DebugLog.d("[Firestore][목록조회][요청] uid=${uidForLog(uid)}, limit=$limit")
            val sessionsRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("sessions")
            val snapshot =
                try {
                    sessionsRef
                        .whereEqualTo("isDeleted", false)
                        .orderBy("endedAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                        .await()
                } catch (error: Exception) {
                    if (!isMissingIndexError(error)) throw error
                    DebugLog.w("[Firestore][목록조회][인덱스없음] 서버 인덱스 생성 전까지 폴백 쿼리로 조회합니다.")
                    sessionsRef
                        .orderBy("endedAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                        .await()
                }

            val records =
                snapshot.documents
                    .filterNot { doc -> doc.getBoolean("isDeleted") == true }
                    .map { doc ->
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
            DebugLog.d("[Firestore][목록조회][성공] uid=${uidForLog(uid)}, count=${records.size}")
            records
        }.onFailure { error ->
            DebugLog.e("[Firestore][목록조회][실패] ${error.message}", error)
        }

    suspend fun getStudySessionById(sessionId: String): Result<StudySessionRecord> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 기록을 불러올 수 있어요.")
            DebugLog.d("[Firestore][상세조회][요청] uid=${uidForLog(uid)}, sessionId=$sessionId")
            val document =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("sessions")
                    .document(sessionId)
                    .get()
                    .await()

            if (!document.exists() || document.getBoolean("isDeleted") == true) {
                error("선택한 세션 기록을 찾을 수 없어요.")
            }

            val placeSnapshot = document.get("placeSnapshot") as? Map<*, *>
            val record =
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
            DebugLog.d(
                "[Firestore][상세조회][성공] uid=${uidForLog(
                    uid,
                )}, sessionId=${record.sessionId}, 타임라인=${record.focusTimeline.size}개",
            )
            record
        }.onFailure { error ->
            DebugLog.e("[Firestore][상세조회][실패] sessionId=$sessionId, ${error.message}", error)
        }

    suspend fun deleteStudySession(sessionId: String): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 기록을 삭제할 수 있어요.")
            DebugLog.d("[Firestore][삭제][요청] uid=${uidForLog(uid)}, sessionId=$sessionId")
            val sessionRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("sessions")
                    .document(sessionId)

            val snapshot = sessionRef.get().await()
            if (!snapshot.exists()) {
                DebugLog.d("[Firestore][삭제][문서없음] uid=${uidForLog(uid)}, sessionId=$sessionId")
                error("삭제할 기록을 찾을 수 없어요.")
            }
            if (snapshot.getBoolean("isDeleted") == true) {
                DebugLog.d("[Firestore][삭제][이미삭제] uid=${uidForLog(uid)}, sessionId=$sessionId")
                error("이미 삭제된 기록이에요.")
            }

            sessionRef
                .update(
                    mapOf(
                        "isDeleted" to true,
                        "deletedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            DebugLog.d("[Firestore][삭제][소프트삭제성공] uid=${uidForLog(uid)}, sessionId=$sessionId")
        }.onFailure { error ->
            DebugLog.e("[Firestore][삭제][실패] sessionId=$sessionId, ${error.message}", error)
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

    private fun generateSessionId(): String = "${System.currentTimeMillis()}-${(1000..9999).random()}"

    private fun buildStablePlaceId(request: SavedPlaceRequest): String {
        val normalizedName = request.name.trim().lowercase(Locale.ROOT)
        val lat = request.latitude?.let { "%.4f".format(Locale.ROOT, it) } ?: "na"
        val lon = request.longitude?.let { "%.4f".format(Locale.ROOT, it) } ?: "na"
        val raw = "$normalizedName|$lat|$lon"
        return "place_${abs(raw.hashCode().toLong())}"
    }

    private fun uidForLog(uid: String): String = if (uid.length <= 6) uid else "${uid.take(6)}..."

    private fun isMissingIndexError(error: Throwable): Boolean {
        val firestoreError = error as? FirebaseFirestoreException ?: return false
        if (firestoreError.code != FirebaseFirestoreException.Code.FAILED_PRECONDITION) return false
        val message = firestoreError.message?.lowercase(Locale.ROOT).orEmpty()
        return message.contains("requires an index") || message.contains("create it here")
    }
}

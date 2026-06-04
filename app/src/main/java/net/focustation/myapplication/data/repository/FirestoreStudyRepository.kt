package net.focustation.myapplication.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.data.model.SensorTimelinePoint
import net.focustation.myapplication.data.model.SensorTimelines
import net.focustation.myapplication.util.DebugLog

data class SavedPlaceRecord(
    val id: String,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastUsedMillis: Long? = null,
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mlScore: Double? = null,
    val focusTimeline: List<FocusDataPoint> = emptyList(),
    val sensorTimelines: SensorTimelines = SensorTimelines(),
)

class FirestoreStudyRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) {
    private val firestore by lazy { firestoreProvider() }
    private val auth by lazy { authProvider() }

    // 세션 문서 생성은 Firebase Functions(POST /sessions)가 담당한다. Android는 직접 쓰지 않는다.

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
                sessionsRef
                    .orderBy("endedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

            val records =
                snapshot.documents
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
                            latitude = (placeSnapshot?.get("latitude") as? Number)?.toDouble(),
                            longitude = (placeSnapshot?.get("longitude") as? Number)?.toDouble(),
                            mlScore = doc.getDouble("mlScore"),
                            focusTimeline = parseFocusTimeline(doc.get("focusTimeline")),
                            sensorTimelines =
                                parseSensorTimelines(
                                    raw = doc.get("sensorTimelines"),
                                    noiseRaw = doc.get("noiseTimeline"),
                                    lightRaw = doc.get("lightTimeline"),
                                    vibrationRaw = doc.get("vibrationTimeline"),
                                ),
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

            if (!document.exists()) {
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
                    latitude = (placeSnapshot?.get("latitude") as? Number)?.toDouble(),
                    longitude = (placeSnapshot?.get("longitude") as? Number)?.toDouble(),
                    mlScore = document.getDouble("mlScore"),
                    focusTimeline = parseFocusTimeline(document.get("focusTimeline")),
                    sensorTimelines =
                        parseSensorTimelines(
                            raw = document.get("sensorTimelines"),
                            noiseRaw = document.get("noiseTimeline"),
                            lightRaw = document.get("lightTimeline"),
                            vibrationRaw = document.get("vibrationTimeline"),
                        ),
                )
            DebugLog.d(
                "[Firestore][상세조회][성공] uid=${uidForLog(
                    uid,
                )}, sessionId=${record.sessionId}, 센서추이=${record.sensorTimelines.totalCount()}개",
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

            // 새 계약: soft delete 폐기, 실제 문서를 hard delete 한다.
            sessionRef.delete().await()
            DebugLog.d("[Firestore][삭제][hard삭제성공] uid=${uidForLog(uid)}, sessionId=$sessionId")
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

    private fun parseSensorTimelines(
        raw: Any?,
        noiseRaw: Any? = null,
        lightRaw: Any? = null,
        vibrationRaw: Any? = null,
    ): SensorTimelines {
        val map = raw as? Map<*, *>
        return SensorTimelines(
            noise = parseSensorTimeline(map?.get("noise") ?: map?.get("noiseTimeline") ?: noiseRaw),
            light = parseSensorTimeline(map?.get("light") ?: map?.get("lightTimeline") ?: lightRaw),
            vibration = parseSensorTimeline(map?.get("vibration") ?: map?.get("vibrationTimeline") ?: vibrationRaw),
        )
    }

    private fun parseSensorTimeline(raw: Any?): List<SensorTimelinePoint> {
        val points = raw as? List<*> ?: return emptyList()
        return points.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val timeLabel = (map["timeLabel"] as? String)?.trim().orEmpty()
            val value = (map["value"] as? Number)?.toFloat() ?: return@mapNotNull null
            if (timeLabel.isBlank() || !value.isFinite()) return@mapNotNull null
            SensorTimelinePoint(
                timeLabel = timeLabel,
                value = value.coerceAtLeast(0f),
            )
        }
    }

    private fun SensorTimelines.totalCount(): Int = noise.size + light.size + vibration.size

    private fun uidForLog(uid: String): String = if (uid.length <= 6) uid else "${uid.take(6)}..."
}

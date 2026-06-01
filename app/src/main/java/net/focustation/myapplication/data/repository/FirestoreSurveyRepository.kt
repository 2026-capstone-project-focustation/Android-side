package net.focustation.myapplication.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import net.focustation.myapplication.survey.SurveyResponse
import net.focustation.myapplication.util.DebugLog

class FirestoreSurveyRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) {
    private val firestore by lazy { firestoreProvider() }
    private val auth by lazy { authProvider() }

    suspend fun saveSurveyResponse(response: SurveyResponse): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 설문을 저장할 수 있어요.")
            val responseId = generateResponseId()

            DebugLog.d(
                "[Firestore][설문저장][요청] uid=${uidForLog(
                    uid,
                )}, responseId=$responseId, input=${response.modelInput.size}, labels=${response.labels.size}",
            )

            val payload =
                hashMapOf(
                    "responseId" to responseId,
                    "schemaVersion" to SURVEY_SCHEMA_VERSION,
                    "source" to "android_app",
                    "modelInput" to response.modelInput,
                    "labels" to response.labels,
                    "submittedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

            val responseRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("surveyResponses")
                    .document("latest")

            responseRef
                .set(payload, SetOptions.merge())
                .await()

            DebugLog.d("[Firestore][설문저장][성공] uid=${uidForLog(uid)}, responseId=$responseId")
        }.onFailure { error ->
            if (error is CancellationException) throw error
            DebugLog.e("[Firestore][설문저장][실패] ${error.message}", error)
        }

    suspend fun loadLatestModelInput(): Result<Map<String, Any>> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("로그인 후 설문을 불러올 수 있어요.")
            val snapshot =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("surveyResponses")
                    .document("latest")
                    .get()
                    .await()

            val modelInput = snapshot.get("modelInput") as? Map<*, *> ?: emptyMap<Any, Any>()
            modelInput.toStringKeyPayload()
        }.onFailure { error ->
            if (error is CancellationException) throw error
            DebugLog.e("[Firestore][설문불러오기][실패] ${error.message}", error)
        }

    private fun generateResponseId(): String = "survey_${System.currentTimeMillis()}"

    private fun uidForLog(uid: String): String = if (uid.length <= 6) uid else "${uid.take(6)}..."

    private fun Map<*, *>.toStringKeyPayload(): Map<String, Any> =
        mapNotNull { (key, value) ->
            val safeKey = key as? String ?: return@mapNotNull null
            val safeValue =
                when (value) {
                    is String -> value
                    is Number -> value
                    is Boolean -> value
                    else -> null
                } ?: return@mapNotNull null

            safeKey to safeValue
        }.toMap()

    private companion object {
        private const val SURVEY_SCHEMA_VERSION = 2
    }
}

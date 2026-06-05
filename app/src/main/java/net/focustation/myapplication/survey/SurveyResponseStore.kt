package net.focustation.myapplication.survey

import net.focustation.myapplication.util.DebugLog

data class SurveyResponse(
    val modelInput: Map<String, Any>,
    val labels: Map<String, Any>,
)

object SurveyResponseStore {
    private var latestResponse: SurveyResponse? = null

    @Synchronized
    fun save(response: SurveyResponse) {
        latestResponse = response
        DebugLog.d(
            "[Survey][저장] input=${response.modelInput.size}개, labels=${response.labels.size}개",
        )
    }

    @Synchronized
    fun latest(): SurveyResponse? = latestResponse

    @Synchronized
    fun clear() {
        latestResponse = null
        DebugLog.d("[Survey][초기화] 저장된 설문 응답 제거")
    }
}

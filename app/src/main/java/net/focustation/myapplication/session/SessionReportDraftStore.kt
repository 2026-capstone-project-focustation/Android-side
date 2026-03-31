package net.focustation.myapplication.session

import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.util.DebugLog

data class SessionReportDraft(
    val totalFocusMinutes: Int,
    val avgEnvironmentScore: Float,
    val avgNoise: Float,
    val avgIlluminance: Float,
    val avgVibration: Double,
    val focusTimeline: List<FocusDataPoint>,
    val placeName: String = "",
    val placeLatitude: Double? = null,
    val placeLongitude: Double? = null,
)

object SessionReportDraftStore {
    private var latestDraft: SessionReportDraft? = null

    @Synchronized
    fun save(draft: SessionReportDraft) {
        DebugLog.d(
            "[DraftStore][저장] 분=${draft.totalFocusMinutes}, 평균점수=${draft.avgEnvironmentScore}, 타임라인=${draft.focusTimeline.size}개",
        )
        latestDraft = draft
    }

    @Synchronized
    fun consume(): SessionReportDraft? =
        latestDraft.also {
            if (it == null) {
                DebugLog.w("[DraftStore][소비] 저장된 draft가 없습니다.")
            } else {
                DebugLog.d(
                    "[DraftStore][소비] 분=${it.totalFocusMinutes}, 평균점수=${it.avgEnvironmentScore}, 타임라인=${it.focusTimeline.size}개",
                )
            }
            latestDraft = null
        }
}

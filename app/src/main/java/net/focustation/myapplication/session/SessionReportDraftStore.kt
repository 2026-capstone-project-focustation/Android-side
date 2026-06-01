package net.focustation.myapplication.session

import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.data.model.SensorTimelines
import net.focustation.myapplication.data.repository.SensorSummaryPayload
import net.focustation.myapplication.util.DebugLog

data class SessionReportDraft(
    val totalFocusMinutes: Int,
    val avgEnvironmentScore: Float,
    val avgNoise: Float,
    val avgIlluminance: Float,
    val avgVibration: Double,
    val focusTimeline: List<FocusDataPoint>,
    val sensorTimelines: SensorTimelines = SensorTimelines(),
    val placeName: String = "",
    val placeLatitude: Double? = null,
    val placeLongitude: Double? = null,
    val placeAddress: String = "",
    val placeCategory: String = "",
    val elapsedSeconds: Int = 0,
    val sessionEndEpochMillis: Long = 0L,
    val sensorSummary: SensorSummaryPayload = SensorSummaryPayload(),
)

object SessionReportDraftStore {
    private var latestDraft: SessionReportDraft? = null

    @Synchronized
    fun save(draft: SessionReportDraft) {
        DebugLog.d(
            "[DraftStore][저장] 분=${draft.totalFocusMinutes}, 센서추이=${draft.sensorTimelines.totalCount()}개",
        )
        latestDraft = draft
    }

    @Synchronized
    fun peek(): SessionReportDraft? = latestDraft

    @Synchronized
    fun clear() {
        latestDraft = null
    }

    @Synchronized
    fun clearIfCurrent(draft: SessionReportDraft) {
        if (latestDraft === draft) {
            latestDraft = null
        }
    }

    @Synchronized
    fun consume(): SessionReportDraft? =
        latestDraft.also {
            if (it == null) {
                DebugLog.w("[DraftStore][소비] 저장된 draft가 없습니다.")
            } else {
                DebugLog.d(
                    "[DraftStore][소비] 분=${it.totalFocusMinutes}, 센서추이=${it.sensorTimelines.totalCount()}개",
                )
            }
            latestDraft = null
        }
}

private fun SensorTimelines.totalCount(): Int = noise.size + light.size + vibration.size

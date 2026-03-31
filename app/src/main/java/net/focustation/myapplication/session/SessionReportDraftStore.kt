package net.focustation.myapplication.session

import net.focustation.myapplication.data.model.FocusDataPoint

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
        latestDraft = draft
    }

    @Synchronized
    fun consume(): SessionReportDraft? =
        latestDraft.also {
            latestDraft = null
        }
}

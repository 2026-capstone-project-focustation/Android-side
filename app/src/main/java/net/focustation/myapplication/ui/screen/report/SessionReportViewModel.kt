package net.focustation.myapplication.ui.screen.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.StudySessionRecord
import net.focustation.myapplication.util.DebugLog

data class StudyHistoryUiItem(
    val sessionId: String,
    val placeName: String,
    val focusScore: Int,
    val durationMinutes: Int,
    val endedAtEpochMillis: Long,
    val avgNoise: Float,
    val avgIlluminance: Float,
    val avgVibration: Double,
    val focusTimeline: List<FocusDataPoint> = emptyList(),
)

enum class ReportSortOption {
    LATEST,
    SCORE_HIGH,
    DURATION_LONG,
    PLACE_NAME,
}

data class SessionReportUiState(
    val isLoadingHistory: Boolean = false,
    val history: List<StudyHistoryUiItem> = emptyList(),
    val sortOption: ReportSortOption = ReportSortOption.LATEST,
    val historyErrorMessage: String? = null,
    val deletingSessionIds: Set<String> = emptySet(),
    val deleteFeedbackMessage: String? = null,
)

class SessionReportViewModel(
    private val repository: FirestoreStudyRepository = FirestoreStudyRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionReportUiState())
    val uiState: StateFlow<SessionReportUiState> = _uiState.asStateFlow()

    fun onScreenEntered() {
        DebugLog.d("[리포트][진입] 세션 보관함 조회")
        loadHistory()
    }

    fun hideHistoryItem(sessionId: String) {
        if (_uiState.value.deletingSessionIds.contains(sessionId)) return

        viewModelScope.launch {
            DebugLog.d("[리포트][삭제] 시작 sessionId=$sessionId")
            _uiState.update { state ->
                state.copy(
                    deletingSessionIds = state.deletingSessionIds + sessionId,
                    deleteFeedbackMessage = null,
                )
            }

            val result = repository.deleteStudySession(sessionId)
            result.fold(
                onSuccess = {
                    DebugLog.d("[리포트][삭제] 성공 sessionId=$sessionId")
                    _uiState.update { state ->
                        state.copy(
                            deletingSessionIds = state.deletingSessionIds - sessionId,
                            history = state.history.filterNot { it.sessionId == sessionId },
                            deleteFeedbackMessage = "기록을 숨김 처리했어요. (Firestore에는 보관돼요)",
                        )
                    }
                },
                onFailure = { error ->
                    DebugLog.e("[리포트][삭제] 실패 sessionId=$sessionId, ${error.message}", error)
                    val message =
                        when (error.message) {
                            "삭제할 기록을 찾을 수 없어요." -> "이미 없는 기록이에요. 목록을 새로고침해 주세요."
                            "이미 삭제된 기록이에요." -> "이미 숨김 처리된 기록이에요."
                            else -> error.message ?: "기록 삭제에 실패했어요."
                        }
                    _uiState.update { state ->
                        state.copy(
                            deletingSessionIds = state.deletingSessionIds - sessionId,
                            deleteFeedbackMessage = message,
                        )
                    }
                },
            )
        }
    }

    fun refreshHistory() {
        loadHistory()
    }

    fun setSortOption(option: ReportSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun consumeDeleteFeedbackMessage() {
        _uiState.update { it.copy(deleteFeedbackMessage = null) }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            DebugLog.d("[리포트][목록조회] 시작")
            _uiState.update { it.copy(isLoadingHistory = true, historyErrorMessage = null) }
            val result = repository.getStudySessions()
            result.fold(
                onSuccess = { records ->
                    val mapped = records.map(::toUiItem)
                    DebugLog.d("[리포트][목록조회] 성공: ${mapped.size}개")
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            history = mapped,
                            historyErrorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    DebugLog.e("[리포트][목록조회] 실패: ${error.message}", error)
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            history = emptyList(),
                            historyErrorMessage = error.message ?: "기록을 불러오지 못했어요.",
                        )
                    }
                },
            )
        }
    }

    private fun toUiItem(record: StudySessionRecord): StudyHistoryUiItem =
        StudyHistoryUiItem(
            sessionId = record.sessionId,
            placeName = record.placeName,
            focusScore = record.focusScoreAvg.toInt(),
            durationMinutes = (record.durationSec / 60).coerceAtLeast(1),
            endedAtEpochMillis = record.endedAtEpochMillis,
            avgNoise = record.avgNoise,
            avgIlluminance = record.avgIlluminance,
            avgVibration = record.avgVibration,
            focusTimeline = record.focusTimeline,
        )
}

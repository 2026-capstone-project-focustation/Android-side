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
import net.focustation.myapplication.data.repository.SavedPlaceRequest
import net.focustation.myapplication.data.repository.StudySessionRecord
import net.focustation.myapplication.data.repository.StudySessionSaveRequest
import net.focustation.myapplication.session.SessionReportDraftStore

data class StudyHistoryUiItem(
    val sessionId: String,
    val placeName: String,
    val focusScore: Int,
    val durationMinutes: Int,
    val endedAtEpochMillis: Long,
)

data class SessionReportUiState(
    val totalFocusMinutes: Int = 0,
    val avgEnvironmentScore: Float = 0f,
    val avgNoise: Float = 0f,
    val avgIlluminance: Float = 0f,
    val avgVibration: Double = 0.0,
    val focusTimeline: List<FocusDataPoint> = emptyList(),
    val placeSaved: Boolean = false,
    val isFromActiveSession: Boolean = true,
    val placeName: String = "",
    val placeLatitude: Double? = null,
    val placeLongitude: Double? = null,
    val isSavingSession: Boolean = false,
    val isSavingPlace: Boolean = false,
    val sessionSaved: Boolean = false,
    val errorMessage: String? = null,
    val isLoadingHistory: Boolean = false,
    val history: List<StudyHistoryUiItem> = emptyList(),
    val historyErrorMessage: String? = null,
)

class SessionReportViewModel(
    private val repository: FirestoreStudyRepository = FirestoreStudyRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionReportUiState())
    val uiState: StateFlow<SessionReportUiState> = _uiState.asStateFlow()

    private var sessionSaveAttempted = false

    init {
        loadHistory()
    }

    fun onScreenEntered(isFromActiveSession: Boolean) {
        _uiState.update { it.copy(isFromActiveSession = isFromActiveSession) }
        if (isFromActiveSession) {
            sessionSaveAttempted = false
            val draft = SessionReportDraftStore.consume()
            if (draft != null) {
                _uiState.update {
                    it.copy(
                        totalFocusMinutes = draft.totalFocusMinutes,
                        avgEnvironmentScore = draft.avgEnvironmentScore,
                        avgNoise = draft.avgNoise,
                        avgIlluminance = draft.avgIlluminance,
                        avgVibration = draft.avgVibration,
                        focusTimeline = draft.focusTimeline,
                        placeName = draft.placeName,
                        placeLatitude = draft.placeLatitude,
                        placeLongitude = draft.placeLongitude,
                        sessionSaved = false,
                        errorMessage = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "세션 실측 데이터를 찾지 못했어요. 다시 세션을 진행해 주세요.",
                    )
                }
            }
            saveSessionRecordIfNeeded()
        } else {
            loadHistory()
        }
    }

    fun saveSessionRecordIfNeeded() {
        if (sessionSaveAttempted) return

        val stateBeforeSave = _uiState.value
        if (stateBeforeSave.totalFocusMinutes <= 0 && stateBeforeSave.focusTimeline.isEmpty()) {
            return
        }

        sessionSaveAttempted = true

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingSession = true, errorMessage = null) }
            val state = _uiState.value
            val result =
                repository.saveStudySession(
                    StudySessionSaveRequest(
                        totalFocusMinutes = state.totalFocusMinutes,
                        avgEnvironmentScore = state.avgEnvironmentScore,
                        avgNoise = state.avgNoise,
                        avgIlluminance = state.avgIlluminance,
                        avgVibration = state.avgVibration,
                        focusTimeline = state.focusTimeline,
                        placeName = state.placeName,
                        latitude = state.placeLatitude,
                        longitude = state.placeLongitude,
                    ),
                )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSavingSession = false,
                            sessionSaved = true,
                            errorMessage = null,
                        )
                    }
                    loadHistory()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingSession = false,
                            errorMessage = error.message ?: "세션 기록 저장에 실패했어요.",
                        )
                    }
                },
            )
        }
    }

    fun savePlace() {
        if (_uiState.value.placeName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "장소 정보가 없어서 저장할 수 없어요.") }
            return
        }
        if (_uiState.value.placeSaved || _uiState.value.isSavingPlace) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPlace = true, errorMessage = null) }
            val state = _uiState.value
            val result =
                repository.savePlace(
                    SavedPlaceRequest(
                        name = state.placeName,
                        latitude = state.placeLatitude,
                        longitude = state.placeLongitude,
                    ),
                )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSavingPlace = false,
                            placeSaved = true,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingPlace = false,
                            errorMessage = error.message ?: "장소 저장에 실패했어요.",
                        )
                    }
                },
            )
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, historyErrorMessage = null) }
            val result = repository.getStudySessions()
            result.fold(
                onSuccess = { records ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            history = records.map(::toUiItem),
                            historyErrorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
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
        )
}

package net.focustation.myapplication.ui.screen.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.focustation.myapplication.data.model.SpaceRecord
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.StudySessionRecord
import kotlin.math.roundToInt

enum class SpaceSortOption { DATE, PLACE, SCORE }

data class SpaceHistoryUiState(
    val spaceRecords: List<SpaceRecord> = emptyList(),
    val sortOption: SpaceSortOption = SpaceSortOption.DATE,
    val isMapView: Boolean = true,
    val selectedSpaceId: String? = null,
    val filterMinScore: Int = 0,
    val filterMaxNoise: Float = 100f,
)

class SpaceHistoryViewModel(
    private val repository: FirestoreStudyRepository = FirestoreStudyRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpaceHistoryUiState())
    val uiState: StateFlow<SpaceHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSpaces()
    }

    fun setSortOption(option: SpaceSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
    }

    fun toggleView() {
        _uiState.value = _uiState.value.copy(isMapView = !_uiState.value.isMapView)
    }

    fun selectSpace(id: String?) {
        _uiState.value = _uiState.value.copy(selectedSpaceId = id)
    }

    private fun loadSpaces() {
        viewModelScope.launch {
            val sessions = repository.getStudySessions().getOrNull().orEmpty()
            _uiState.value = _uiState.value.copy(spaceRecords = sessions.toSpaceRecords())
        }
    }
}

// 위치 정보가 있는 세션 기록을 장소별로 묶어 지도 핀(SpaceRecord)으로 집계한다.
private fun List<StudySessionRecord>.toSpaceRecords(): List<SpaceRecord> =
    filter { it.latitude != null && it.longitude != null }
        .groupBy { it.placeName }
        .map { (placeName, group) ->
            val recent = group.maxByOrNull { it.endedAtEpochMillis } ?: group.first()
            SpaceRecord(
                id = placeName,
                name = placeName,
                latitude = recent.latitude ?: 0.0,
                longitude = recent.longitude ?: 0.0,
                avgFocusScore = group.map { it.focusScoreAvg }.average().roundToInt(),
                sessionCount = group.size,
                avgNoise = group.map { it.avgNoise }.average().toFloat(),
                avgIlluminance = group.map { it.avgIlluminance }.average().toFloat(),
                avgVibration = group.map { it.avgVibration }.average(),
                lastVisited = relativeVisitLabel(recent.endedAtEpochMillis),
            )
        }

private fun relativeVisitLabel(epochMillis: Long): String {
    if (epochMillis <= 0L) return "기록 없음"
    val dayMillis = 24L * 60L * 60L * 1000L
    val days = ((System.currentTimeMillis() - epochMillis) / dayMillis).toInt()
    return when {
        days <= 0 -> "오늘"
        days == 1 -> "어제"
        else -> "${days}일 전"
    }
}

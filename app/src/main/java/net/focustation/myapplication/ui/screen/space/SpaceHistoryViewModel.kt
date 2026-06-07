package net.focustation.myapplication.ui.screen.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.focustation.myapplication.data.model.SpaceRecord
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.NearbyPlaceReportsRepository
import net.focustation.myapplication.data.repository.PublicPlaceReportRecord
import net.focustation.myapplication.data.repository.StudySessionRecord
import net.focustation.myapplication.util.DebugLog
import net.focustation.myapplication.util.isPlausibleKoreanCoordinate
import kotlin.math.roundToInt

enum class SpaceSortOption { DATE, PLACE, SCORE }

data class SpaceHistoryUiState(
    val spaceRecords: List<SpaceRecord> = emptyList(),
    val publicSpaceRecords: List<SpaceRecord> = emptyList(),
    val showPublicReports: Boolean = false,
    val isLoadingPublicReports: Boolean = false,
    val sortOption: SpaceSortOption = SpaceSortOption.DATE,
    val isMapView: Boolean = true,
    val selectedSpaceId: String? = null,
    val filterMinScore: Int = 0,
    val filterMaxNoise: Float = 100f,
    val errorMessage: String? = null,
)

class SpaceHistoryViewModel(
    private val repository: FirestoreStudyRepository = FirestoreStudyRepository(),
    private val nearbyReportsRepository: NearbyPlaceReportsRepository = NearbyPlaceReportsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpaceHistoryUiState())
    val uiState: StateFlow<SpaceHistoryUiState> = _uiState.asStateFlow()
    private var lastVisibleBounds: VisibleMapBounds? = null
    private var boundsQueryJob: Job? = null

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

    fun setShowPublicReports(show: Boolean) {
        _uiState.value =
            _uiState.value.copy(
                showPublicReports = show,
                selectedSpaceId = null,
            )
        if (show) {
            lastVisibleBounds?.let { bounds ->
                schedulePublicReportsLoad(bounds, debounce = false)
            }
        }
    }

    fun updateVisibleBounds(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
    ) {
        val bounds = VisibleMapBounds(north = north, south = south, east = east, west = west)
        if (!bounds.isValid()) return
        lastVisibleBounds = bounds
        if (!_uiState.value.showPublicReports) return
        schedulePublicReportsLoad(bounds, debounce = true)
    }

    private fun loadSpaces() {
        viewModelScope.launch {
            repository.getStudySessions().fold(
                onSuccess = { sessions ->
                    _uiState.value =
                        _uiState.value.copy(
                            spaceRecords = sessions.toSpaceRecords(),
                            errorMessage = null,
                        )
                },
                onFailure = { error ->
                    DebugLog.e("[공간기록][조회] 실패: ${error.message}", error)
                    _uiState.value =
                        _uiState.value.copy(errorMessage = error.message ?: "공간 기록을 불러오지 못했어요.")
                },
            )
        }
    }

    private fun schedulePublicReportsLoad(
        bounds: VisibleMapBounds,
        debounce: Boolean,
    ) {
        boundsQueryJob?.cancel()
        boundsQueryJob =
            viewModelScope.launch {
                if (debounce) delay(PUBLIC_BOUNDS_DEBOUNCE_MS)
                loadPlaceReportsInBounds(bounds)
            }
    }

    private suspend fun loadPlaceReportsInBounds(bounds: VisibleMapBounds) {
        _uiState.value = _uiState.value.copy(isLoadingPublicReports = true)
        nearbyReportsRepository
            .getPlaceReportsInBounds(
                north = bounds.north,
                south = bounds.south,
                east = bounds.east,
                west = bounds.west,
            ).fold(
                onSuccess = { reports ->
                    _uiState.value =
                        _uiState.value.copy(
                            publicSpaceRecords = reports.toPublicSpaceRecords(),
                            isLoadingPublicReports = false,
                            errorMessage = null,
                        )
                },
                onFailure = { error ->
                    DebugLog.e("[공간기록][공개요약] 실패: ${error.message}", error)
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingPublicReports = false,
                            errorMessage = error.message ?: "근처 공개 공간 요약을 불러오지 못했어요.",
                        )
                },
            )
    }
}

data class VisibleMapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
) {
    fun isValid(): Boolean =
        north >= south &&
            east >= west &&
            isPlausibleKoreanCoordinate(north, east) &&
            isPlausibleKoreanCoordinate(south, west)
}

// 위치 정보가 있는 세션 기록을 장소별로 묶어 지도 핀(SpaceRecord)으로 집계한다.
private fun List<StudySessionRecord>.toSpaceRecords(): List<SpaceRecord> =
    filter { isPlausibleKoreanCoordinate(it.latitude, it.longitude) }
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

private fun List<PublicPlaceReportRecord>.toPublicSpaceRecords(): List<SpaceRecord> =
    filter { isPlausibleKoreanCoordinate(it.latitude, it.longitude) }
        .map { report ->
            SpaceRecord(
                id = "public:${report.placeKey}",
                name = report.placeName,
                latitude = report.latitude,
                longitude = report.longitude,
                avgFocusScore = report.mlScoreAvg.roundToInt(),
                sessionCount = report.reportCount,
                avgNoise = report.avgNoise.toFloat(),
                avgIlluminance = report.avgIlluminance.toFloat(),
                avgVibration = report.avgVibration,
                lastVisited = report.lastReportedAt ?: "공개 요약",
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

private const val PUBLIC_BOUNDS_DEBOUNCE_MS = 450L

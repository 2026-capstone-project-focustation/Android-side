package net.focustation.myapplication.ui.screen.space

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.focustation.myapplication.data.model.SpaceRecord

enum class SpaceSortOption { DATE, PLACE, SCORE }

data class SpaceHistoryUiState(
    val spaceRecords: List<SpaceRecord> =
        listOf(
            SpaceRecord("1", "중앙 도서관", 37.5012, 127.0396, 86, 12, 32f, 450f, 0.01, "오늘"),
            SpaceRecord("2", "카페 모카", 37.4990, 127.0371, 71, 5, 55f, 320f, 0.04, "어제"),
            SpaceRecord("3", "공대 열람실", 37.5035, 127.0441, 88, 20, 28f, 480f, 0.008, "2일 전"),
            SpaceRecord("4", "스터디 카페 A", 37.5021, 127.0388, 75, 8, 45f, 400f, 0.02, "3일 전"),
        ),
    val sortOption: SpaceSortOption = SpaceSortOption.DATE,
    val isMapView: Boolean = true,
    val selectedSpaceId: String? = null,
    val filterMinScore: Int = 0,
    val filterMaxNoise: Float = 100f,
)

class SpaceHistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SpaceHistoryUiState())
    val uiState: StateFlow<SpaceHistoryUiState> = _uiState.asStateFlow()

    fun setSortOption(option: SpaceSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
    }

    fun toggleView() {
        _uiState.value = _uiState.value.copy(isMapView = !_uiState.value.isMapView)
    }

    fun selectSpace(id: String?) {
        _uiState.value = _uiState.value.copy(selectedSpaceId = id)
    }
}

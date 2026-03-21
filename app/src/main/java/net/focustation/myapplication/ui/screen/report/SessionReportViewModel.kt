package net.focustation.myapplication.ui.screen.report

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.focustation.myapplication.data.model.FocusDataPoint

data class SessionReportUiState(
    val totalFocusMinutes: Int = 90,
    val avgEnvironmentScore: Float = 0f,
    val avgNoise: Float = 38.5f,
    val avgIlluminance: Float = 430f,
    val avgVibration: Double = 0.0,
    val focusTimeline: List<FocusDataPoint> =
        listOf(
            FocusDataPoint("0분", 70f),
            FocusDataPoint("15분", 80f),
            FocusDataPoint("30분", 86f),
            FocusDataPoint("45분", 76f),
            FocusDataPoint("60분", 90f),
            FocusDataPoint("75분", 84f),
            FocusDataPoint("90분", 92f),
        ),
    val placeSaved: Boolean = false,
    val isFromActiveSession: Boolean = true,
)

class SessionReportViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SessionReportUiState())
    val uiState: StateFlow<SessionReportUiState> = _uiState.asStateFlow()

    fun savePlace() {
        _uiState.value = _uiState.value.copy(placeSaved = true)
    }
}

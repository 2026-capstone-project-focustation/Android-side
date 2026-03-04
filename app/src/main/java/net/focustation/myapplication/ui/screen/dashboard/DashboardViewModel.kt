package net.focustation.myapplication.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.focustation.myapplication.data.model.EnvironmentSnapshot
import net.focustation.myapplication.data.model.SessionSummary
import net.focustation.myapplication.data.model.User

data class DashboardUiState(
    val user: User = User(),
    val todayAvgFocus: Int = 78,
    val todayWorkMinutes: Int = 142,
    val environmentSnapshot: EnvironmentSnapshot = EnvironmentSnapshot(),
    val recentSessions: List<SessionSummary> =
        listOf(
            SessionSummary("1", "오늘 14:30", "중앙 도서관", 82, 90, 4.1f),
            SessionSummary("2", "어제 10:00", "카페 모카", 71, 60, 3.7f),
            SessionSummary("3", "2일 전 16:00", "열람실 3층", 88, 120, 4.5f),
        ),
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
}

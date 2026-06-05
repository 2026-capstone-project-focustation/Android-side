package net.focustation.myapplication.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.focustation.myapplication.session.SessionPlaceSelectionStore
import net.focustation.myapplication.session.SessionReportDraftStore
import net.focustation.myapplication.survey.SurveyResponseStore

data class SettingsUiState(
    val userName: String = "",
    val userEmail: String = "",
)

class SettingsViewModel(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) : ViewModel() {
    private val auth by lazy { authProvider() }
    private val _uiState =
        MutableStateFlow(
            auth.currentUser?.let { user ->
                SettingsUiState(
                    userName = user.displayName?.takeIf { it.isNotBlank() } ?: "사용자",
                    userEmail = user.email.orEmpty(),
                )
            } ?: SettingsUiState(),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun signOut() {
        auth.signOut()
        SurveyResponseStore.clear()
        SessionReportDraftStore.clear()
        SessionPlaceSelectionStore.clear()
        _uiState.value = _uiState.value.copy(userName = "로그아웃됨", userEmail = "")
    }
}

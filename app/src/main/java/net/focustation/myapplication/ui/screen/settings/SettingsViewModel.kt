package net.focustation.myapplication.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val userName: String = "",
    val userEmail: String = "",
    val sensorSamplingSeconds: Int = 5,
    val focusDropAlertEnabled: Boolean = true,
    val sessionCompleteAlertEnabled: Boolean = true,
    val isDarkTheme: Boolean = false,
    val anonymousUploadEnabled: Boolean = false,
    val localStorageMb: Int = 42,
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

    fun setSamplingInterval(seconds: Int) {
        _uiState.value = _uiState.value.copy(sensorSamplingSeconds = seconds)
    }

    fun toggleFocusDropAlert(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(focusDropAlertEnabled = enabled)
    }

    fun toggleSessionCompleteAlert(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(sessionCompleteAlertEnabled = enabled)
    }

    fun toggleDarkTheme(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkTheme = enabled)
    }

    fun toggleAnonymousUpload(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(anonymousUploadEnabled = enabled)
    }

    fun clearLocalStorage() {
        _uiState.value = _uiState.value.copy(localStorageMb = 0)
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = _uiState.value.copy(userName = "로그아웃됨", userEmail = "")
    }
}

package net.focustation.myapplication.ui.screen.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val userName: String = "김예찬",
    val userEmail: String = "user@focustation.net",
    val sensorSamplingSeconds: Int = 5,
    val focusDropAlertEnabled: Boolean = true,
    val sessionCompleteAlertEnabled: Boolean = true,
    val isDarkTheme: Boolean = false,
    val anonymousUploadEnabled: Boolean = false,
    val localStorageMb: Int = 42,
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
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
}

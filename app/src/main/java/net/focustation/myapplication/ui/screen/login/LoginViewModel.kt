package net.focustation.myapplication.ui.screen.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.focustation.myapplication.session.SessionPlaceSelectionStore
import net.focustation.myapplication.session.SessionReportDraftStore
import net.focustation.myapplication.survey.SurveyResponseStore

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(
    private val firebaseAuthProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) : ViewModel() {
    private val firebaseAuth: FirebaseAuth? =
        runCatching { firebaseAuthProvider() }.getOrNull()
    private val authStateListener =
        FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        errorMessage = null,
                    )
            }
        }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        firebaseAuth?.let { auth ->
            auth.addAuthStateListener(authStateListener)

            if (auth.currentUser != null) {
                _uiState.value = _uiState.value.copy(isLoggedIn = true)
            }
        }
    }

    fun onGoogleLoginStarted() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    }

    fun onGoogleIdTokenReceived(idToken: String?) {
        val auth = firebaseAuth
        if (auth == null) {
            onGoogleLoginFailed("Firebase 설정이 아직 연결되지 않았어요. google-services.json 설정을 확인해주세요.")
            return
        }

        if (idToken.isNullOrBlank()) {
            onGoogleLoginFailed("Google ID 토큰을 가져오지 못했어요. 다시 시도해주세요.")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth
            .signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    clearPreviousUserCache()
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            errorMessage = null,
                        )
                } else {
                    val message = task.exception?.localizedMessage ?: "Firebase 로그인에 실패했어요."
                    onGoogleLoginFailed(message)
                }
            }
    }

    fun onGoogleLoginCanceled() {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
    }

    fun onGoogleLoginFailed(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // 새 사용자 로그인 시 이전 사용자의 인메모리 데이터가 남지 않도록 초기화한다.
    private fun clearPreviousUserCache() {
        SurveyResponseStore.clear()
        SessionReportDraftStore.clear()
        SessionPlaceSelectionStore.clear()
    }

    override fun onCleared() {
        firebaseAuth?.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}

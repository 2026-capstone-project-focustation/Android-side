package net.focustation.myapplication.ui.screen.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(
    private val firebaseAuthProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) : ViewModel() {
    private val firebaseAuth by lazy { firebaseAuthProvider() }
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
        firebaseAuth.addAuthStateListener(authStateListener)

        if (firebaseAuth.currentUser != null) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    fun onGoogleLoginStarted() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    }

    fun onGoogleIdTokenReceived(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            onGoogleLoginFailed("Google ID 토큰을 가져오지 못했어요. 다시 시도해주세요.")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth
            .signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
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

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}

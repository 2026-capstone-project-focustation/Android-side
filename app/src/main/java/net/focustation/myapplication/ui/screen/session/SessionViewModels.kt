package net.focustation.myapplication.ui.screen.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.focustation.myapplication.data.model.EnvironmentSnapshot

// ─── 환경 분석 세션 ───────────────────────────────────────────────────────────

data class EnvironmentSessionUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val totalSessionSeconds: Int = 300, // 5분
    val noiseHistory: List<Float> = emptyList(),
    val currentSnapshot: EnvironmentSnapshot = EnvironmentSnapshot(),
    val environmentScore: Float = 0f,
)

class EnvironmentSessionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EnvironmentSessionUiState())
    val uiState: StateFlow<EnvironmentSessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun startSession() {
        _uiState.value = _uiState.value.copy(isRunning = true, isPaused = false)
        timerJob =
            viewModelScope.launch {
                while (_uiState.value.elapsedSeconds < _uiState.value.totalSessionSeconds) {
                    delay(1000L)
                    val elapsed = _uiState.value.elapsedSeconds + 1
                    val noise = (30 + Math.random() * 20).toFloat()
                    val newHistory = (_uiState.value.noiseHistory + noise).takeLast(20)
                    _uiState.value =
                        _uiState.value.copy(
                            elapsedSeconds = elapsed,
                            noiseHistory = newHistory,
                            currentSnapshot =
                                EnvironmentSnapshot(
                                    noiseLevel = noise,
                                    illuminance = (380 + Math.random() * 100).toFloat(),
                                    temperature = (21 + Math.random() * 3).toFloat(),
                                ),
                            environmentScore = ((4.0 + Math.random()).toFloat()).coerceAtMost(5f),
                        )
                    if (!_uiState.value.isRunning) break
                }
            }
    }

    fun pauseSession() {
        _uiState.value = _uiState.value.copy(isRunning = false, isPaused = true)
        timerJob?.cancel()
    }

    fun stopSession() {
        timerJob?.cancel()
        _uiState.value = EnvironmentSessionUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

// ─── 집중 세션 ────────────────────────────────────────────────────────────────

data class FocusSessionUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val environmentFitScore: Float = 4.2f,
    val fitHistory: List<Float> = emptyList(),
)

class FocusSessionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun startSession() {
        _uiState.value = _uiState.value.copy(isRunning = true, isPaused = false)
        timerJob =
            viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    if (!_uiState.value.isRunning) break
                    val elapsed = _uiState.value.elapsedSeconds + 1
                    val fit = (3.5 + Math.random() * 1.5).toFloat()
                    val newHistory = (_uiState.value.fitHistory + fit).takeLast(30)
                    _uiState.value =
                        _uiState.value.copy(
                            elapsedSeconds = elapsed,
                            environmentFitScore = fit,
                            fitHistory = newHistory,
                        )
                }
            }
    }

    fun pauseSession() {
        _uiState.value = _uiState.value.copy(isRunning = false, isPaused = true)
        timerJob?.cancel()
    }

    fun stopSession() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false, isPaused = false)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

// ─── 피드백 세션 ──────────────────────────────────────────────────────────────

data class FeedbackUiState(
    val subjectiveScore: Int = 3,
    val question1: Int = 3, // 1=매우 불만족 ~ 5=매우 만족
    val question2: Int = 3,
    val question3: Int = 3,
    val submitted: Boolean = false,
)

class FeedbackSessionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun updateSubjectiveScore(score: Int) {
        _uiState.value = _uiState.value.copy(subjectiveScore = score)
    }

    fun updateQuestion1(score: Int) {
        _uiState.value = _uiState.value.copy(question1 = score)
    }

    fun updateQuestion2(score: Int) {
        _uiState.value = _uiState.value.copy(question2 = score)
    }

    fun updateQuestion3(score: Int) {
        _uiState.value = _uiState.value.copy(question3 = score)
    }

    fun submit() {
        _uiState.value = _uiState.value.copy(submitted = true)
    }
}

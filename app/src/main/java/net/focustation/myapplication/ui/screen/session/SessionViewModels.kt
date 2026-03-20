package net.focustation.myapplication.ui.screen.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.focustation.myapplication.data.model.EnvironmentSnapshot
import net.focustation.myapplication.score.ScoreCalculator
import net.focustation.myapplication.sensor.LightSensorManager
import net.focustation.myapplication.sensor.NoiseSensorManager
import net.focustation.myapplication.sensor.VibrationSensorManager

// ─── 환경 분석 세션 ───────────────────────────────────────────────────────────

data class EnvironmentSessionUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val totalSessionSeconds: Int = 300, // 5분
    val noiseHistory: List<Float> = emptyList(),
    val currentSnapshot: EnvironmentSnapshot = EnvironmentSnapshot(),
    val environmentScore: Float = 0f, // 0~100
)

class EnvironmentSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val lightManager = LightSensorManager(app)
    private val noiseManager = NoiseSensorManager()
    private val vibrationManager = VibrationSensorManager(app)

    private val _uiState = MutableStateFlow(EnvironmentSessionUiState())
    val uiState: StateFlow<EnvironmentSessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var noiseJob: Job? = null
    private var hasNoisePerm = false

    // 슬라이딩 윈도우 버퍼 (30샘플 ≈ 30초)
    private val lightBuf = ArrayDeque<Float>()
    private val noiseBuf = ArrayDeque<Double>()
    private val vibBuf = ArrayDeque<Double>()

    companion object {
        private const val WINDOW = 30
        private const val DISPLAY_HISTORY = 20
    }

    init {
        viewModelScope.launch {
            lightManager.getLightFlow().collect { lux ->
                lightBuf.addLast(lux)
                if (lightBuf.size > WINDOW) lightBuf.removeFirst()
                recalculate()
            }
        }
        viewModelScope.launch {
            vibrationManager.getVibrationFlow().collect { m ->
                vibBuf.addLast(m)
                if (vibBuf.size > WINDOW) vibBuf.removeFirst()
                recalculate()
            }
        }
    }

    /**
     * Begins collecting noise (dB) samples from the noise sensor and updates internal buffers and state.
     *
     * If noise collection is already active this function returns immediately. Otherwise it sets
     * `hasNoisePerm = true`, launches a coroutine saved in `noiseJob` that collects dB values from
     * `noiseManager.getNoiseFlow()`, appends them to the sliding `noiseBuf` (trimming to `WINDOW`),
     * and invokes `recalculate()` after each sample.
     */
    fun startNoiseCollection() {
        if (noiseJob != null) return
        hasNoisePerm = true
        noiseJob = viewModelScope.launch {
            noiseManager.getNoiseFlow().collect { db ->
                noiseBuf.addLast(db)
                if (noiseBuf.size > WINDOW) noiseBuf.removeFirst()
                recalculate()
            }
        }
    }

    /**
     * Computes component and total environment scores from the sliding sensor buffers and updates UI state.
     *
     * Calculates scores for light and vibration when their buffers contain samples, and includes noise scoring
     * only if noise permission has been granted and noise samples are available. Updates the current snapshot
     * with the most recent buffered values (preserving previous snapshot values when a buffer is empty),
     * refreshes the displayed noise history to the most recent samples, and sets the aggregated environment score.
     */
    private fun recalculate() {
        val lightScore = if (lightBuf.isNotEmpty()) ScoreCalculator.calculateLightScore(lightBuf.toList()) else null
        val noiseScore = if (hasNoisePerm && noiseBuf.isNotEmpty()) ScoreCalculator.calculateNoiseScore(noiseBuf.toList()) else null
        val vibScore = if (vibBuf.isNotEmpty()) ScoreCalculator.calculateVibrationScore(vibBuf.toList()) else null

        val total = ScoreCalculator.calculateTotalScore(listOfNotNull(lightScore, noiseScore, vibScore)).toFloat()

        _uiState.update { s ->
            s.copy(
                currentSnapshot = EnvironmentSnapshot(
                    noiseLevel = noiseBuf.lastOrNull()?.toFloat() ?: s.currentSnapshot.noiseLevel,
                    illuminance = lightBuf.lastOrNull() ?: s.currentSnapshot.illuminance,
                    vibration = vibBuf.lastOrNull() ?: s.currentSnapshot.vibration,
                ),
                noiseHistory = noiseBuf.takeLast(DISPLAY_HISTORY).map { it.toFloat() },
                environmentScore = total,
            )
        }
    }

    /**
     * Starts the session timer and marks the session as running.
     *
     * Cancels any existing timer, sets `isRunning = true` and `isPaused = false`, then launches a timer that increments `elapsedSeconds` by 1 every second until `elapsedSeconds` reaches `totalSessionSeconds` or the session is stopped/paused.
     */
    fun startSession() {
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.elapsedSeconds < _uiState.value.totalSessionSeconds) {
                delay(1000L)
                if (!_uiState.value.isRunning) break
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    /**
     * Pauses the active session.
     *
     * Cancels the running session timer (if any) and updates UI state to set `isRunning` to false and `isPaused` to true.
     */
    fun pauseSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    /**
     * Stops the current environment session and resets the UI session state to its default values.
     *
     * Cancels any running session timer and replaces the view-model UI state with a fresh EnvironmentSessionUiState.
     */
    fun stopSession() {
        timerJob?.cancel()
        _uiState.update { EnvironmentSessionUiState() }
    }

    /**
     * Performs cleanup when the ViewModel is being destroyed.
     *
     * Cancels the active session timer job if present.
     */
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
    val environmentFitScore: Float = 0f, // 0~100
    val fitHistory: List<Float> = emptyList(),
)

class FocusSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val lightManager = LightSensorManager(app)
    private val noiseManager = NoiseSensorManager()
    private val vibrationManager = VibrationSensorManager(app)

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var noiseJob: Job? = null
    private var hasNoisePerm = false

    private val lightBuf = ArrayDeque<Float>()
    private val noiseBuf = ArrayDeque<Double>()
    private val vibBuf = ArrayDeque<Double>()

    companion object {
        private const val WINDOW = 30
        private const val DISPLAY_HISTORY = 30
    }

    init {
        viewModelScope.launch {
            lightManager.getLightFlow().collect { lux ->
                lightBuf.addLast(lux)
                if (lightBuf.size > WINDOW) lightBuf.removeFirst()
                recalculate()
            }
        }
        viewModelScope.launch {
            vibrationManager.getVibrationFlow().collect { m ->
                vibBuf.addLast(m)
                if (vibBuf.size > WINDOW) vibBuf.removeFirst()
                recalculate()
            }
        }
    }

    /**
     * Begins collecting noise (dB) samples from the noise sensor and updates internal buffers and state.
     *
     * If noise collection is already active this function returns immediately. Otherwise it sets
     * `hasNoisePerm = true`, launches a coroutine saved in `noiseJob` that collects dB values from
     * `noiseManager.getNoiseFlow()`, appends them to the sliding `noiseBuf` (trimming to `WINDOW`),
     * and invokes `recalculate()` after each sample.
     */
    fun startNoiseCollection() {
        if (noiseJob != null) return
        hasNoisePerm = true
        noiseJob = viewModelScope.launch {
            noiseManager.getNoiseFlow().collect { db ->
                noiseBuf.addLast(db)
                if (noiseBuf.size > WINDOW) noiseBuf.removeFirst()
                recalculate()
            }
        }
    }

    /**
     * Recomputes the environment fit score from the current sensor buffers and updates the UI state.
     *
     * Computes component scores from available light, noise (only when permission granted), and vibration buffers,
     * aggregates them into a total fit score, then sets `environmentFitScore` and appends the value to `fitHistory`
     * (keeping only the last `DISPLAY_HISTORY` entries).
     */
    private fun recalculate() {
        val lightScore = if (lightBuf.isNotEmpty()) ScoreCalculator.calculateLightScore(lightBuf.toList()) else null
        val noiseScore = if (hasNoisePerm && noiseBuf.isNotEmpty()) ScoreCalculator.calculateNoiseScore(noiseBuf.toList()) else null
        val vibScore = if (vibBuf.isNotEmpty()) ScoreCalculator.calculateVibrationScore(vibBuf.toList()) else null

        val total = ScoreCalculator.calculateTotalScore(listOfNotNull(lightScore, noiseScore, vibScore)).toFloat()

        _uiState.update { s ->
            s.copy(
                environmentFitScore = total,
                fitHistory = (s.fitHistory + total).takeLast(DISPLAY_HISTORY),
            )
        }
    }

    /**
     * Starts the session timer and marks the session as running.
     *
     * Cancels any existing timer, sets `isRunning = true` and `isPaused = false`, then launches a job that increments `elapsedSeconds` by one every second while the session remains running.
     */
    fun startSession() {
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!_uiState.value.isRunning) break
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    /**
     * Pauses the active session.
     *
     * Cancels the running session timer (if any) and updates UI state to set `isRunning` to false and `isPaused` to true.
     */
    fun pauseSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    /**
     * Stops the active session and resets its running and paused state.
     *
     * Cancels any existing session timer and updates the UI state to set `isRunning = false`
     * and `isPaused = false`.
     */
    fun stopSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = false) }
    }

    /**
     * Performs cleanup when the ViewModel is being destroyed.
     *
     * Cancels the active session timer job if present.
     */
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

class FeedbackSessionViewModel : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    /**
     * Sets the user's overall subjective score in the feedback UI state.
     *
     * @param score The subjective score value where 1 = very dissatisfied and 5 = very satisfied.
     */
    fun updateSubjectiveScore(score: Int) {
        _uiState.update { it.copy(subjectiveScore = score) }
    }

    /**
     * Update the first feedback question response stored in the view model's UI state.
     *
     * @param score The selected response for question 1, where 1 = very dissatisfied and 5 = very satisfied (valid range: 1–5).
     */
    fun updateQuestion1(score: Int) {
        _uiState.update { it.copy(question1 = score) }
    }

    /**
     * Update the second questionnaire response in the feedback UI state.
     *
     * @param score The user-selected response for question 2 on a 1–5 scale (1 = very dissatisfied, 5 = very satisfied).
     */
    fun updateQuestion2(score: Int) {
        _uiState.update { it.copy(question2 = score) }
    }

    /**
     * Updates the stored response for question 3 in the feedback UI state.
     *
     * @param score Rating value from 1 (very dissatisfied) to 5 (very satisfied).
     */
    fun updateQuestion3(score: Int) {
        _uiState.update { it.copy(question3 = score) }
    }

    /**
     * Marks the feedback as submitted in the view model's UI state.
     */
    fun submit() {
        _uiState.update { it.copy(submitted = true) }
    }
}

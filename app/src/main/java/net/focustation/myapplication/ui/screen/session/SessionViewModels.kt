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

    // RECORD_AUDIO 권한 획득 후 호출. 중복 호출 방지됨
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

    fun pauseSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun stopSession() {
        timerJob?.cancel()
        _uiState.update { EnvironmentSessionUiState() }
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

    fun pauseSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun stopSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = false) }
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

class FeedbackSessionViewModel : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun updateSubjectiveScore(score: Int) {
        _uiState.update { it.copy(subjectiveScore = score) }
    }

    fun updateQuestion1(score: Int) {
        _uiState.update { it.copy(question1 = score) }
    }

    fun updateQuestion2(score: Int) {
        _uiState.update { it.copy(question2 = score) }
    }

    fun updateQuestion3(score: Int) {
        _uiState.update { it.copy(question3 = score) }
    }

    fun submit() {
        _uiState.update { it.copy(submitted = true) }
    }
}

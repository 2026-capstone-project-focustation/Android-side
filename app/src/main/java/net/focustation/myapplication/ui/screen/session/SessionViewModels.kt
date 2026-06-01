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
import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.data.repository.CreateSessionRequest
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.PlaceSnapshotPayload
import net.focustation.myapplication.data.repository.SavedPlaceRequest
import net.focustation.myapplication.data.repository.SessionApiRepository
import net.focustation.myapplication.score.ScoreCalculator
import net.focustation.myapplication.sensor.LightSensorManager
import net.focustation.myapplication.sensor.NoiseSensorManager
import net.focustation.myapplication.sensor.VibrationSensorManager
import net.focustation.myapplication.session.SessionPlaceSelectionStore
import net.focustation.myapplication.session.SessionReportDraft
import net.focustation.myapplication.session.SessionReportDraftStore
import net.focustation.myapplication.ui.screen.survey.placeRatingQuestions
import net.focustation.myapplication.util.DebugLog
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

// ─── 환경 분석 세션 ───────────────────────────────────────────────────────────

data class EnvironmentSessionUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val elapsedSeconds: Int = 0,
    val totalSessionSeconds: Int = 300, // 5분
    val noiseHistory: List<Float> = emptyList(),
    val currentSnapshot: EnvironmentSnapshot = EnvironmentSnapshot(),
    val environmentScore: Float = 0f, // 0~100
)

class EnvironmentSessionViewModel(
    app: Application,
) : AndroidViewModel(app) {
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
     * Starts collecting ambient noise (dB) samples and updates the view model's noise buffer and derived state.
     *
     * If noise collection is already active this is a no-op. Otherwise it sets `hasNoisePerm = true` and
     * launches a coroutine that collects values from `noiseManager`, appends them to `noiseBuf` (keeping at most
     * `WINDOW` most recent samples), and invokes `recalculate()` after each sample.
     */
    fun startNoiseCollection() {
        if (noiseJob != null) return
        hasNoisePerm = true
        DebugLog.d("[집중세션][소음측정] 마이크 소음 측정을 시작합니다.")
        noiseJob =
            viewModelScope.launch {
                noiseManager.getNoiseFlow().collect { db ->
                    noiseBuf.addLast(db)
                    if (noiseBuf.size > WINDOW) noiseBuf.removeFirst()
                    recalculate()
                }
            }
    }

    /**
     * Updates the UI state with component scores and an aggregated environment score derived from available sensor buffers.
     *
     * Computes light and vibration component scores when their buffers contain samples, and includes a noise component
     * only when noise permission has been granted and noise samples exist. Then updates the current snapshot using the
     * most recent buffered values (retaining previous snapshot values for any empty buffer), refreshes the displayed
     * noise history to the most recent samples, and sets the aggregated environment score.
     */
    private fun recalculate() {
        val lightScore = if (lightBuf.isNotEmpty()) ScoreCalculator.calculateLightScore(lightBuf.toList()) else null
        val noiseScore =
            if (hasNoisePerm &&
                noiseBuf.isNotEmpty()
            ) {
                ScoreCalculator.calculateNoiseScore(noiseBuf.toList())
            } else {
                null
            }
        val vibScore = if (vibBuf.isNotEmpty()) ScoreCalculator.calculateVibrationScore(vibBuf.toList()) else null

        val total = ScoreCalculator.calculateTotalScore(listOfNotNull(lightScore, noiseScore, vibScore)).toFloat()

        _uiState.update { s ->
            s.copy(
                currentSnapshot =
                    EnvironmentSnapshot(
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
        if (!_uiState.value.isPaused) {
            lightBuf.clear()
            noiseBuf.clear()
            vibBuf.clear()
            _uiState.update {
                it.copy(
                    isRunning = true,
                    isPaused = false,
                    isCompleted = false,
                    elapsedSeconds = 0,
                    noiseHistory = emptyList(),
                    currentSnapshot = EnvironmentSnapshot(),
                    environmentScore = 0f,
                )
            }
        } else {
            _uiState.update { it.copy(isRunning = true, isPaused = false, isCompleted = false) }
        }
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (_uiState.value.elapsedSeconds < _uiState.value.totalSessionSeconds) {
                    delay(1000L)
                    if (!_uiState.value.isRunning) break
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
    }

    fun completeSession() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isCompleted = true,
                elapsedSeconds = it.elapsedSeconds.coerceAtMost(it.totalSessionSeconds),
            )
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
        lightBuf.clear()
        noiseBuf.clear()
        vibBuf.clear()
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
        noiseJob?.cancel()
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

class FocusSessionViewModel(
    app: Application,
) : AndroidViewModel(app) {
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

    private var lightSum = 0.0
    private var lightCount = 0
    private var noiseSum = 0.0
    private var noiseCount = 0
    private var vibrationSum = 0.0
    private var vibrationCount = 0

    companion object {
        private const val WINDOW = 30
    }

    init {
        viewModelScope.launch {
            lightManager.getLightFlow().collect { lux ->
                lightBuf.addLast(lux)
                if (lightBuf.size > WINDOW) lightBuf.removeFirst()
                if (_uiState.value.isRunning) {
                    lightSum += lux
                    lightCount += 1
                }
                recalculate()
            }
        }
        viewModelScope.launch {
            vibrationManager.getVibrationFlow().collect { m ->
                vibBuf.addLast(m)
                if (vibBuf.size > WINDOW) vibBuf.removeFirst()
                if (_uiState.value.isRunning) {
                    vibrationSum += m
                    vibrationCount += 1
                }
                recalculate()
            }
        }
    }

    /**
     * Starts collecting ambient noise (dB) samples and updates the view model's noise buffer and derived state.
     *
     * If noise collection is already active this is a no-op. Otherwise it sets `hasNoisePerm = true` and
     * launches a coroutine that collects values from `noiseManager`, appends them to `noiseBuf` (keeping at most
     * `WINDOW` most recent samples), and invokes `recalculate()` after each sample.
     */
    fun startNoiseCollection() {
        if (noiseJob != null) return
        hasNoisePerm = true
        noiseJob =
            viewModelScope.launch {
                noiseManager.getNoiseFlow().collect { db ->
                    noiseBuf.addLast(db)
                    if (noiseBuf.size > WINDOW) noiseBuf.removeFirst()
                    if (_uiState.value.isRunning) {
                        noiseSum += db
                        noiseCount += 1
                    }
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
        val noiseScore =
            if (hasNoisePerm &&
                noiseBuf.isNotEmpty()
            ) {
                ScoreCalculator.calculateNoiseScore(noiseBuf.toList())
            } else {
                null
            }
        val vibScore = if (vibBuf.isNotEmpty()) ScoreCalculator.calculateVibrationScore(vibBuf.toList()) else null

        val total = ScoreCalculator.calculateTotalScore(listOfNotNull(lightScore, noiseScore, vibScore)).toFloat()

        _uiState.update { s ->
            s.copy(
                environmentFitScore = total,
                fitHistory =
                    if (s.isRunning) {
                        s.fitHistory + total
                    } else {
                        s.fitHistory
                    },
            )
        }
    }

    /**
     * Starts the session timer and marks the session as running.
     *
     * Cancels any existing timer, sets `isRunning = true` and `isPaused = false`, then launches a job that increments `elapsedSeconds` by one every second while the session remains running.
     */
    fun startSession() {
        if (!_uiState.value.isPaused) {
            lightBuf.clear()
            noiseBuf.clear()
            vibBuf.clear()
            resetAggregates()
            _uiState.update {
                it.copy(
                    elapsedSeconds = 0,
                    fitHistory = emptyList(),
                    environmentFitScore = 0f,
                )
            }
        }
        DebugLog.d("[집중세션][시작] 세션을 시작합니다. (재개=${_uiState.value.isPaused})")
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
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
        DebugLog.d("[집중세션][일시정지] 경과=${_uiState.value.elapsedSeconds}초")
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

        val state = _uiState.value
        val timeline = buildFocusTimeline(state.fitHistory, state.elapsedSeconds)
        val avgScore =
            if (state.fitHistory.isNotEmpty()) {
                state.fitHistory.average().toFloat()
            } else {
                state.environmentFitScore
            }
        val avgNoise = if (noiseCount > 0) (noiseSum / noiseCount).toFloat() else 0f
        val avgIlluminance = if (lightCount > 0) (lightSum / lightCount).toFloat() else 0f
        val avgVibration = if (vibrationCount > 0) vibrationSum / vibrationCount else 0.0
        val selectedPlace = SessionPlaceSelectionStore.consume()

        DebugLog.d(
            "[집중세션][종료] 경과=${state.elapsedSeconds}초, 히스토리=${state.fitHistory.size}개, 타임라인=${timeline.size}개",
        )
        DebugLog.d(
            "[집중세션][종료] 평균점수=$avgScore, 평균소음=$avgNoise, 평균조도=$avgIlluminance, 평균진동=$avgVibration",
        )

        SessionReportDraftStore.save(
            SessionReportDraft(
                totalFocusMinutes =
                    if (state.elapsedSeconds > 0) {
                        (state.elapsedSeconds + 59) / 60
                    } else {
                        0
                    },
                avgEnvironmentScore =
                avgScore,
                avgNoise = avgNoise,
                avgIlluminance = avgIlluminance,
                avgVibration = avgVibration,
                focusTimeline = timeline,
                placeName = selectedPlace?.name.orEmpty(),
                placeLatitude = selectedPlace?.latitude,
                placeLongitude = selectedPlace?.longitude,
                placeAddress = selectedPlace?.address.orEmpty(),
                placeCategory = selectedPlace?.category.orEmpty(),
                elapsedSeconds = state.elapsedSeconds,
                sessionEndEpochMillis = System.currentTimeMillis(),
            ),
        )
        DebugLog.d("[집중세션][종료] draft 저장 완료")

        lightBuf.clear()
        noiseBuf.clear()
        vibBuf.clear()
        resetAggregates()
        _uiState.update { FocusSessionUiState() }
    }

    private fun resetAggregates() {
        lightSum = 0.0
        lightCount = 0
        noiseSum = 0.0
        noiseCount = 0
        vibrationSum = 0.0
        vibrationCount = 0
    }

    private fun buildFocusTimeline(
        fitHistory: List<Float>,
        elapsedSeconds: Int,
    ): List<FocusDataPoint> {
        if (fitHistory.isEmpty()) return emptyList()

        val maxPoints = 24
        val indices =
            if (fitHistory.size <= maxPoints) {
                fitHistory.indices.toList()
            } else {
                (0 until maxPoints)
                    .map { i ->
                        ((i.toDouble() * (fitHistory.size - 1)) / (maxPoints - 1)).roundToInt()
                    }.distinct()
            }

        return indices.map { index ->
            val secondAtPoint =
                if (fitHistory.size <= 1 || elapsedSeconds <= 0) {
                    0
                } else {
                    ((index.toDouble() / (fitHistory.size - 1)) * elapsedSeconds).toInt()
                }
            FocusDataPoint(
                timeLabel = "${secondAtPoint / 60}분",
                focusScore = fitHistory[index].coerceIn(0f, 100f),
            )
        }
    }

    /**
     * Performs cleanup when the ViewModel is being destroyed.
     *
     * Cancels the active session timer job if present.
     */
    override fun onCleared() {
        super.onCleared()
        DebugLog.d("[집중세션][정리] ViewModel 해제")
        timerJob?.cancel()
        noiseJob?.cancel()
    }
}

// ─── 피드백 세션 ──────────────────────────────────────────────────────────────

// placeFeedback = context 10 + place 10 = 20개. 라벨은 현재 POST /sessions 계약에 없으므로 수집하지 않는다.
data class FeedbackUiState(
    // context (사용자 입력 또는 네이버 메타 seed)
    val placeType: String = "cafe",
    val taskType: String = "deep_study",
    val groupSize: String = "solo",
    val weather: String = "clear",
    val distanceMinutes: Float = 10f,
    val visitFrequency: String = "first_time",
    val indoorOutdoor: String = "indoor",
    // place 평가 10개 (Likert 1~5)
    val placeRatings: Map<String, Int> = defaultPlaceFeedbackRatings,
    val isSaving: Boolean = false,
    val saveErrorMessage: String? = null,
    val submitted: Boolean = false,
)

private val defaultPlaceFeedbackRatings: Map<String, Int> = placeRatingQuestions.associate { it.key to 3 }

class FeedbackSessionViewModel(
    private val sessionApi: SessionApiRepository = SessionApiRepository(),
    private val studyRepository: FirestoreStudyRepository = FirestoreStudyRepository(),
) : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(seedInitialState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    // 장소 선택 시 잡힌 네이버 카테고리로 place_type을 미리 채운다(사용자가 수정 가능).
    private fun seedInitialState(): FeedbackUiState {
        val category = SessionReportDraftStore.peek()?.placeCategory.orEmpty()
        val placeType = mapNaverCategoryToPlaceType(category)
        return FeedbackUiState(
            placeType = placeType,
            indoorOutdoor = indoorOutdoorForPlaceType(placeType),
        )
    }

    fun setPlaceType(value: String) {
        _uiState.update { it.copy(placeType = value, indoorOutdoor = indoorOutdoorForPlaceType(value)) }
    }

    fun setTaskType(value: String) {
        _uiState.update { it.copy(taskType = value) }
    }

    fun setGroupSize(value: String) {
        _uiState.update { it.copy(groupSize = value) }
    }

    fun setWeather(value: String) {
        _uiState.update { it.copy(weather = value) }
    }

    fun setDistanceMinutes(value: Float) {
        _uiState.update { it.copy(distanceMinutes = value.coerceIn(1f, 60f)) }
    }

    fun setVisitFrequency(value: String) {
        _uiState.update { it.copy(visitFrequency = value) }
    }

    fun setIndoorOutdoor(value: String) {
        _uiState.update { it.copy(indoorOutdoor = value) }
    }

    fun setPlaceRating(
        key: String,
        value: Int,
    ) {
        _uiState.update { state ->
            state.copy(placeRatings = state.placeRatings + (key to value.coerceIn(1, 5)))
        }
    }

    fun submit() {
        if (_uiState.value.isSaving || _uiState.value.submitted) return

        val draft = SessionReportDraftStore.peek()
        if (draft == null) {
            DebugLog.w("[Feedback][Save] 세션 draft 없음")
            _uiState.update {
                it.copy(saveErrorMessage = "세션 데이터를 찾지 못했어요. 저장하지 않고 나갈 수 있어요.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
            val request = buildRequest(draft, _uiState.value)
            val result = sessionApi.createSession(request)
            val created = result.getOrNull()
            if (created != null) {
                DebugLog.d("[Feedback][Save] 성공 sessionId=${created.sessionId}, mlScore=${created.mlScore}")
                // 사용한 장소를 savedPlaces에 남겨 다음 세션의 '최근 공간'으로 노출한다.
                persistPlaceIfNeeded(draft)
                SessionReportDraftStore.clearIfCurrent(draft)
                _uiState.update {
                    it.copy(isSaving = false, saveErrorMessage = null, submitted = true)
                }
            } else {
                val error = result.exceptionOrNull()
                DebugLog.e("[Feedback][Save] POST /sessions 실패: ${error?.message}", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = error?.message ?: "세션 기록 저장에 실패했어요.",
                    )
                }
            }
        }
    }

    private suspend fun persistPlaceIfNeeded(draft: SessionReportDraft) {
        val name = draft.placeName.trim()
        val lat = draft.placeLatitude
        val lng = draft.placeLongitude
        if (name.isBlank() || name == "장소 미지정" || lat == null || lng == null) return
        studyRepository
            .savePlace(SavedPlaceRequest(name = name, latitude = lat, longitude = lng))
            .onFailure { error ->
                DebugLog.e("[Feedback][장소저장] 실패: ${error.message}", error)
            }
    }

    private fun buildRequest(
        draft: SessionReportDraft,
        state: FeedbackUiState,
    ): CreateSessionRequest {
        val endTime =
            if (draft.sessionEndEpochMillis > 0L) {
                Instant
                    .ofEpochMilli(draft.sessionEndEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            } else {
                LocalDateTime.now()
            }
        val placeFeedback =
            linkedMapOf<String, Any>(
                "place_type" to state.placeType,
                "task_type" to state.taskType,
                "group_size" to state.groupSize,
                "stay_duration" to stayDurationForMinutes(draft.elapsedSeconds / 60),
                "time_slot" to timeSlotForHour(endTime.hour),
                "day_type" to dayTypeForDayOfWeek(endTime.dayOfWeek),
                "distance_minutes" to state.distanceMinutes.toDouble(),
                "weather" to state.weather,
                "indoor_outdoor" to state.indoorOutdoor,
                "visit_frequency" to state.visitFrequency,
            )
        placeRatingQuestions.forEach { question ->
            placeFeedback[question.key] = state.placeRatings[question.key] ?: 3
        }

        return CreateSessionRequest(
            durationSec = draft.elapsedSeconds,
            avgEnvironmentScore = draft.avgEnvironmentScore.toDouble(),
            avgNoise = draft.avgNoise.toDouble(),
            avgIlluminance = draft.avgIlluminance.toDouble(),
            avgVibration = draft.avgVibration,
            focusTimeline = draft.focusTimeline,
            placeSnapshot =
                PlaceSnapshotPayload(
                    name = draft.placeName.ifBlank { "장소 미지정" },
                    latitude = draft.placeLatitude,
                    longitude = draft.placeLongitude,
                    address = draft.placeAddress.ifBlank { null },
                    category = draft.placeCategory.ifBlank { null },
                ),
            placeFeedback = placeFeedback,
        )
    }
}

package net.focustation.myapplication.ui.screen.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.focustation.myapplication.data.model.EnvironmentSnapshot
import net.focustation.myapplication.data.model.SessionSummary
import net.focustation.myapplication.data.model.User
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.StudySessionRecord
import net.focustation.myapplication.sensor.LightSensorManager
import net.focustation.myapplication.sensor.NoiseSensorManager
import net.focustation.myapplication.sensor.VibrationSensorManager
import net.focustation.myapplication.util.DebugLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class DashboardUiState(
    val user: User = User(id = "", name = "사용자", email = ""),
    val todayAvgFocus: Int = 0,
    val todayWorkMinutes: Int = 0,
    val hasTodaySessions: Boolean = false,
    val hasEnvironmentSnapshot: Boolean = false,
    val environmentSnapshot: EnvironmentSnapshot =
        EnvironmentSnapshot(
            noiseLevel = 0f,
            illuminance = 0f,
            vibration = 0.0,
        ),
    val recentSessions: List<SessionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class DashboardViewModel(
    app: Application,
    private val repository: FirestoreStudyRepository = FirestoreStudyRepository(),
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) : AndroidViewModel(app) {
    constructor(app: Application) : this(
        app = app,
        repository = FirestoreStudyRepository(),
        authProvider = { FirebaseAuth.getInstance() },
    )

    private val auth by lazy { authProvider() }
    private val lightManager = LightSensorManager(app)
    private val noiseManager = NoiseSensorManager()
    private val vibrationManager = VibrationSensorManager(app)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var lightJob: Job? = null
    private var noiseJob: Job? = null
    private var vibrationJob: Job? = null

    init {
        refresh()
    }

    fun startRealtimeEnvironmentMonitoring(hasNoisePermission: Boolean) {
        if (lightJob == null) {
            lightJob =
                viewModelScope.launch {
                    runCatching {
                        lightManager.getLightFlow().collect { lux ->
                            updateEnvironmentSnapshot(illuminance = lux)
                        }
                    }.onFailure { error ->
                        DebugLog.e("[대시보드][실시간조도] 실패: ${error.message}", error)
                    }
                }
        }

        if (vibrationJob == null) {
            vibrationJob =
                viewModelScope.launch {
                    runCatching {
                        vibrationManager.getVibrationFlow().collect { vibration ->
                            updateEnvironmentSnapshot(vibration = vibration)
                        }
                    }.onFailure { error ->
                        DebugLog.e("[대시보드][실시간진동] 실패: ${error.message}", error)
                    }
                }
        }

        if (hasNoisePermission && noiseJob == null) {
            noiseJob =
                viewModelScope.launch {
                    runCatching {
                        noiseManager.getNoiseFlow().collect { db ->
                            updateEnvironmentSnapshot(noise = db.toFloat())
                        }
                    }.onFailure { error ->
                        DebugLog.e("[대시보드][실시간소음] 실패: ${error.message}", error)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                DebugLog.w("[대시보드][조회] 실패: 로그인 유저 없음")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recentSessions = emptyList(),
                        errorMessage = "로그인 후 내 데이터를 불러올 수 있어요.",
                    )
                }
                return@launch
            }

            val user =
                User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "사용자",
                    email = firebaseUser.email.orEmpty(),
                )

            val result = repository.getStudySessions(limit = 100)
            result.fold(
                onSuccess = { records ->
                    val sorted = records.sortedByDescending { it.endedAtEpochMillis }
                    val recentTop3 = sorted.take(3).map(::toSessionSummary)
                    val todayRecords = filterTodayRecords(sorted)

                    val todayAvgFocus =
                        if (todayRecords.isNotEmpty()) {
                            todayRecords.map { it.focusScoreAvg }.average().roundToInt()
                        } else {
                            0
                        }

                    val todayWorkMinutes =
                        todayRecords.sumOf { record ->
                            (record.durationSec / 60).coerceAtLeast(1)
                        }

                    val snapshot =
                        EnvironmentSnapshot(
                            noiseLevel = _uiState.value.environmentSnapshot.noiseLevel,
                            illuminance = _uiState.value.environmentSnapshot.illuminance,
                            vibration = _uiState.value.environmentSnapshot.vibration,
                        )

                    DebugLog.d(
                        "[대시보드][조회] 성공 records=${sorted.size}, recent=${recentTop3.size}, today=${todayRecords.size}",
                    )
                    _uiState.update {
                        it.copy(
                            user = user,
                            todayAvgFocus = todayAvgFocus,
                            todayWorkMinutes = todayWorkMinutes,
                            hasTodaySessions = todayRecords.isNotEmpty(),
                            hasEnvironmentSnapshot = snapshot.hasAnyReading(),
                            environmentSnapshot = snapshot,
                            recentSessions = recentTop3,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    DebugLog.e("[대시보드][조회] 실패: ${error.message}", error)
                    _uiState.update {
                        it.copy(
                            user = user,
                            hasTodaySessions = false,
                            hasEnvironmentSnapshot = false,
                            isLoading = false,
                            recentSessions = emptyList(),
                            errorMessage = error.message ?: "홈 데이터를 불러오지 못했어요.",
                        )
                    }
                },
            )
        }
    }

    private fun updateEnvironmentSnapshot(
        noise: Float? = null,
        illuminance: Float? = null,
        vibration: Double? = null,
    ) {
        _uiState.update { state ->
            val nextSnapshot =
                state.environmentSnapshot.copy(
                    noiseLevel = noise ?: state.environmentSnapshot.noiseLevel,
                    illuminance = illuminance ?: state.environmentSnapshot.illuminance,
                    vibration = vibration ?: state.environmentSnapshot.vibration,
                )
            state.copy(
                hasEnvironmentSnapshot = nextSnapshot.hasAnyReading(),
                environmentSnapshot = nextSnapshot,
            )
        }
    }

    private fun toSessionSummary(record: StudySessionRecord): SessionSummary =
        SessionSummary(
            id = record.sessionId,
            date = formatRelativeDate(record.endedAtEpochMillis),
            place = record.placeName,
            focusScore = record.focusScoreAvg.roundToInt().coerceIn(0, 100),
            totalMinutes = (record.durationSec / 60).coerceAtLeast(1),
            avgEnvironmentScore = record.focusScoreAvg,
        )

    private fun filterTodayRecords(records: List<StudySessionRecord>): List<StudySessionRecord> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return records.filter { record ->
            if (record.endedAtEpochMillis <= 0L) return@filter false
            val day = Instant.ofEpochMilli(record.endedAtEpochMillis).atZone(zone).toLocalDate()
            day == today
        }
    }

    private fun formatRelativeDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return "날짜 미상"
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val today = LocalDate.now(zone)
        val target = dateTime.toLocalDate()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return when {
            target == today -> "오늘 ${dateTime.format(timeFormatter)}"
            target == today.minusDays(1) -> "어제 ${dateTime.format(timeFormatter)}"
            else -> dateTime.format(DateTimeFormatter.ofPattern("MM.dd HH:mm"))
        }
    }
}

private fun EnvironmentSnapshot.hasAnyReading(): Boolean = noiseLevel > 0f || illuminance > 0f || vibration > 0.0

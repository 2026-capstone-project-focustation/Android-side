package net.focustation.myapplication.ui.screen.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.score.ScoreCalculator
import net.focustation.myapplication.ui.components.FocusScreenBackground
import net.focustation.myapplication.ui.components.MainBottomDestination
import net.focustation.myapplication.ui.components.MainBottomNavigationBar
import net.focustation.myapplication.ui.components.StatCard
import net.focustation.myapplication.ui.screen.onboarding.OnboardingStore
import net.focustation.myapplication.ui.theme.FocusBlue
import net.focustation.myapplication.ui.theme.FocusCanvas
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocusLine
import net.focustation.myapplication.ui.theme.FocusMuted
import net.focustation.myapplication.ui.theme.FocusSurface
import net.focustation.myapplication.ui.theme.FocusYellow
import net.focustation.myapplication.ui.theme.FocustationTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onStartSession: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToSpaceHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRetakeSurvey: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // 온보딩 직후 대시보드 첫 진입 시 한 번만 시스템 권한 다이얼로그를 띄운다.
    val context = LocalContext.current
    val onboardingStore = remember(context) { OnboardingStore(context) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {}
    LaunchedEffect(Unit) {
        if (!onboardingStore.permissionsRequested()) {
            onboardingStore.markPermissionsRequested()
            permissionLauncher.launch(firstLaunchPermissions())
        }
    }

    Scaffold(
        containerColor = FocusCanvas,
        bottomBar = {
            MainBottomNavigationBar(
                selected = MainBottomDestination.HOME,
                onTabClick = { destination ->
                    when (destination) {
                        MainBottomDestination.HOME -> Unit
                        MainBottomDestination.REPORT -> onNavigateToReport()
                        MainBottomDestination.MAP -> onNavigateToSpaceHistory()
                        MainBottomDestination.SETTINGS -> onNavigateToSettings()
                    }
                },
            )
        },
    ) { paddingValues ->
        FocusScreenBackground {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 44.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    DashboardHero(name = uiState.user.name)
                }

                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            value = "${uiState.todayAvgFocus.coerceAtLeast(0)}",
                            label = "오늘 점수",
                            icon = Icons.Filled.Insights,
                            iconColor = FocusBlue,
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            value = uiState.todayWorkMinutes.formatWorkTime(),
                            label = "작업 시간",
                            icon = Icons.Outlined.Schedule,
                            iconColor = FocusYellow,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    SurveyRetakeCard(
                        onRetakeSurvey = onRetakeSurvey,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(title = "지금 공간 상태", subtitle = "최근 측정값 기준")
                        Spacer(Modifier.height(18.dp))
                        DashboardEnvironmentSummary(
                            noise = uiState.environmentSnapshot.noiseLevel,
                            illuminance = uiState.environmentSnapshot.illuminance,
                            vibration = uiState.environmentSnapshot.vibration,
                            onStartSession = onStartSession,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyRetakeCard(
    onRetakeSurvey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = FocusSurface,
        border = BorderStroke(1.dp, FocusLine),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FocusYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Insights,
                    contentDescription = null,
                    tint = FocusInk,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "맞춤 설문",
                    style = MaterialTheme.typography.labelLarge,
                    color = FocusInk,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "선호가 바뀌면 집중 기준을 다시 조정해요",
                    style = MaterialTheme.typography.labelSmall,
                    color = FocusMuted,
                )
            }
            OutlinedButton(
                onClick = onRetakeSurvey,
                shape = CircleShape,
                border = BorderStroke(1.2.dp, FocusInk),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = FocusInk,
                    ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "다시하기",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DashboardHero(name: String) {
    val weekSlots = remember { currentWeekSlots() }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(FocusInk)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "사용자 프로필",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = name.dashboardDisplayName(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "오늘의 집중 환경 기록을 확인해요",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.60f),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(FocusYellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = FocusInk,
                    )
                }
            }

            DashboardCalendarStrip(days = weekSlots)
        }
    }
}

@Composable
private fun DashboardCalendarStrip(days: List<WeekDaySlot>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(62.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = if (day.selected) 0.14f else 0.07f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = day.weekday,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.62f),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (day.selected) FocusYellow else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.day,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (day.selected) FocusInk else Color.White.copy(alpha = 0.76f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardEnvironmentSummary(
    noise: Float,
    illuminance: Float,
    vibration: Double,
    onStartSession: () -> Unit,
) {
    val totalScore =
        remember(noise, illuminance, vibration) {
            calculateCurrentEnvironmentScore(
                noise = noise,
                illuminance = illuminance,
                vibration = vibration,
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TotalScoreGoalCard(
                score = totalScore,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(232.dp),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(232.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniSensorCard(
                    label = "조도",
                    value = "%.0f".format(illuminance.coerceAtLeast(0f)),
                    unit = "lux",
                    icon = Icons.Filled.WbSunny,
                    containerColor = FocusYellow,
                    contentColor = FocusInk,
                    modifier = Modifier.weight(1f),
                )
                MiniSensorCard(
                    label = "소음",
                    value = "%.0f".format(noise.coerceAtLeast(0f)),
                    unit = "dB",
                    icon = Icons.Filled.GraphicEq,
                    containerColor = FocusInk,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                )
                MiniSensorCard(
                    label = "진동",
                    value = "%.3f".format(vibration.coerceAtLeast(0.0)),
                    unit = "m/s²",
                    icon = Icons.Filled.Sensors,
                    containerColor = FocusSurface,
                    contentColor = FocusInk,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, FocusLine),
                )
            }
        }

        EnvironmentCheckButton(onStartSession = onStartSession)
    }
}

@Composable
private fun MiniSensorCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = containerColor,
        border = border,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = if (contentColor == Color.White) 0.16f else 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.78f),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = contentColor,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 3.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TotalScoreGoalCard(
    score: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = FocusBlue,
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BlueWaveBackground(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f),
            )
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = "총 점수",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "점",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.padding(bottom = 10.dp),
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = environmentScoreLabel(score),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlueWaveBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val values = listOf(0.28f, 0.58f, 0.52f, 0.72f, 0.36f, 0.66f, 0.42f)
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val points =
            values.mapIndexed { i, v ->
                Offset(x = step * i, y = size.height * (1f - v.coerceIn(0f, 1f)))
            }
        val linePath =
            Path().apply {
                moveTo(points.first().x, points.first().y)
                for (idx in 1 until points.size) {
                    val prev = points[idx - 1]
                    val curr = points[idx]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }
        val fillPath =
            Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(fillPath, Color.White.copy(alpha = 0.12f))
        drawPath(
            path = linePath,
            color = Color.White.copy(alpha = 0.42f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun EnvironmentCheckButton(onStartSession: () -> Unit) {
    OutlinedButton(
        onClick = onStartSession,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        shape = CircleShape,
        border = BorderStroke(1.4.dp, FocusInk),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = FocusInk,
            ),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = FocusInk,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "환경체크 시작",
            style = MaterialTheme.typography.labelLarge,
            color = FocusInk,
        )
    }
}

private fun firstLaunchPermissions(): Array<String> =
    buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

private fun calculateCurrentEnvironmentScore(
    noise: Float,
    illuminance: Float,
    vibration: Double,
): Int {
    if (noise <= 0f && illuminance <= 0f && vibration <= 0.0) return 0

    val scores =
        listOfNotNull(
            illuminance
                .takeIf { it > 0f }
                ?.let { ScoreCalculator.calculateLightScore(listOf(it)) },
            noise
                .takeIf { it > 0f }
                ?.let { ScoreCalculator.calculateNoiseScore(listOf(it.toDouble())) },
            vibration
                .takeIf { it > 0.0 }
                ?.let { ScoreCalculator.calculateVibrationScore(listOf(it)) },
        )

    return ScoreCalculator
        .calculateTotalScore(scores)
        .roundToInt()
        .coerceIn(0, 100)
}

private fun environmentScoreLabel(score: Int): String =
    when {
        score >= 80 -> "집중하기 좋은 공간"
        score >= 60 -> "무난한 집중 환경"
        score > 0 -> "조정이 필요한 공간"
        else -> "측정 전 대기"
    }

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FocusInk,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = FocusMuted,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    FocustationTheme {
        DashboardScreen(
            onStartSession = {},
            onNavigateToReport = {},
            onNavigateToSpaceHistory = {},
            onNavigateToSettings = {},
            onRetakeSurvey = {},
        )
    }
}

private data class WeekDaySlot(
    val weekday: String,
    val day: String,
    val selected: Boolean,
)

private fun currentWeekSlots(): List<WeekDaySlot> {
    val today = LocalDate.now()
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val weekdayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    return weekdayLabels.mapIndexed { index, label ->
        val date = startOfWeek.plusDays(index.toLong())
        WeekDaySlot(
            weekday = label,
            day = date.dayOfMonth.toString(),
            selected = date == today,
        )
    }
}

private fun String.dashboardDisplayName(): String =
    trim()
        .ifBlank { "사용자" }

private fun Int.formatWorkTime(): String {
    val safeMinutes = coerceAtLeast(0)
    return "${safeMinutes / 60}h ${safeMinutes % 60}m"
}

package net.focustation.myapplication.ui.screen.session

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.MiniLineGraph
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.ColorLight
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.ColorVibration
import net.focustation.myapplication.ui.theme.FocustationTheme

/**
 * Displays the environment analysis session UI and controls for a timed monitoring session.
 *
 * @param onSessionComplete Callback invoked when the session is completed or explicitly ended by the user.
 * @param onBack Callback invoked when the user navigates back from the screen.
 * @param viewModel ViewModel that provides UI state and session control actions; defaults to a local instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentSessionScreen(
    onSessionComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: EnvironmentSessionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                viewModel.startNoiseCollection()
                viewModel.startSession()
            } else {
                Toast
                    .makeText(
                        context,
                        "마이크 권한이 필요해요. 소음 측정 없이 환경 측정을 시작할 수 없어요.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }

    val startSession = {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startNoiseCollection()
            viewModel.startSession()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.elapsedSeconds) {
        if (uiState.elapsedSeconds >= uiState.totalSessionSeconds && uiState.isRunning) {
            viewModel.completeSession()
        }
    }

    EnvironmentSessionContent(
        uiState = uiState,
        onBack = onBack,
        onStart = startSession,
        onPause = viewModel::pauseSession,
        onResume = viewModel::startSession,
        onFinish = {
            viewModel.completeSession()
        },
        onRetry = {
            viewModel.stopSession()
            startSession()
        },
        onContinue = onSessionComplete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvironmentSessionContent(
    uiState: EnvironmentSessionUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
) {
    val remaining = (uiState.totalSessionSeconds - uiState.elapsedSeconds).coerceAtLeast(0)
    val progress =
        if (uiState.totalSessionSeconds <= 0) {
            0f
        } else {
            (uiState.elapsedSeconds.toFloat() / uiState.totalSessionSeconds).coerceIn(0f, 1f)
        }

    Scaffold(
        containerColor = ReferenceDesignTokens.Screen,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ReferenceDesignTokens.Screen)
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EnvironmentTopBar(onBack = onBack)
            if (uiState.isCompleted) {
                EnvironmentResultPanel(
                    score = uiState.predictionScore,
                    isLoading = uiState.isPredicting,
                    errorMessage = uiState.predictionErrorMessage,
                    onRetry = onRetry,
                    onContinue = onContinue,
                )
            } else {
                EnvironmentTimerCard(
                    remainingSeconds = remaining,
                    progress = progress,
                    isRunning = uiState.isRunning,
                    isPaused = uiState.isPaused,
                    isCompleted = uiState.isCompleted,
                )
                EnvironmentControlButtons(
                    isRunning = uiState.isRunning,
                    isPaused = uiState.isPaused,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onFinish = onFinish,
                )
                EnvironmentMetricGrid(
                    noise = uiState.currentSnapshot.noiseLevel,
                    illuminance = uiState.currentSnapshot.illuminance,
                    vibration = uiState.currentSnapshot.vibration,
                )
                EnvironmentSensorGraphCard(
                    title = "소음 추이",
                    history = uiState.noiseHistory,
                    statusText = sensorGraphStatusLabel(uiState),
                    lineColor = ColorNoise,
                    minValue = 20f,
                    maxValue = 90f,
                )
                EnvironmentSensorGraphCard(
                    title = "조도 추이",
                    history = uiState.lightHistory,
                    statusText = sensorGraphStatusLabel(uiState),
                    lineColor = ColorLight,
                    minValue = 0f,
                    maxValue = 1000f,
                )
                EnvironmentSensorGraphCard(
                    title = "진동 추이",
                    history = uiState.vibrationHistory,
                    statusText = sensorGraphStatusLabel(uiState),
                    lineColor = ColorVibration,
                    minValue = 0f,
                    maxValue = 0.2f,
                )
            }
        }
    }
}

@Composable
private fun EnvironmentTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onBack,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape),
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = ReferenceDesignTokens.WhiteCard,
                    contentColor = ReferenceDesignTokens.TextPrimary,
                ),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "환경 측정",
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                text = "소음, 조도, 진동을 기록해요",
                color = ReferenceDesignTokens.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.size(42.dp))
    }
}

@Composable
private fun EnvironmentTimerCard(
    remainingSeconds: Int,
    progress: Float,
    isRunning: Boolean,
    isPaused: Boolean,
    isCompleted: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignTokens.PhoneRadius),
        color = ReferenceDesignTokens.Dark,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    ReferenceDesignTokens.Dark,
                                    ReferenceDesignTokens.DarkAlt,
                                ),
                        ),
                    ).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatusPill(
                        label = environmentStatusLabel(isRunning, isPaused, isCompleted),
                        color =
                            when {
                                isCompleted -> ReferenceDesignTokens.Blue
                                isRunning -> ReferenceDesignTokens.Yellow
                                isPaused -> ReferenceDesignTokens.Blue
                                else -> Color.White.copy(alpha = 0.24f)
                            },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = formatRemainingTime(remainingSeconds),
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    )
                    Text(
                        text = "5분 동안 센서값을 실시간으로 기록해요",
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                color = ReferenceDesignTokens.Yellow,
                trackColor = Color.White.copy(alpha = 0.14f),
            )
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color,
) {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun EnvironmentMetricGrid(
    noise: Float,
    illuminance: Float,
    vibration: Double,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = "실시간 환경값",
            color = ReferenceDesignTokens.TextPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            EnvironmentMetricCard(
                label = "소음",
                value = "%.0f".format(noise),
                unit = "dB",
                icon = Icons.Outlined.GraphicEq,
                accentColor = ColorNoise,
                modifier = Modifier.weight(1f),
            )
            EnvironmentMetricCard(
                label = "조도",
                value = "%.0f".format(illuminance),
                unit = "lux",
                icon = Icons.Outlined.LightMode,
                accentColor = ColorLight,
                modifier = Modifier.weight(1f),
            )
            EnvironmentMetricCard(
                label = "진동",
                value = "%.3f".format(vibration),
                unit = "m/s²",
                icon = Icons.Outlined.Sensors,
                accentColor = ColorVibration,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EnvironmentResultPanel(
    score: Int?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignTokens.PhoneRadius),
        color = ReferenceDesignTokens.Dark,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(ReferenceDesignTokens.Yellow),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = ReferenceDesignTokens.TextPrimary)
                } else {
                    Text(
                        text = score?.toString() ?: "--",
                        color = ReferenceDesignTokens.TextPrimary,
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "환경 적합도",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                )
                Text(
                    text =
                        when {
                            isLoading -> "ML 예측 점수를 계산하는 중이에요"
                            errorMessage != null -> "예측 점수를 만들지 못했어요"
                            else -> environmentScoreLabel(score)
                        },
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = errorMessage ?: "측정값과 설문 응답을 바탕으로 예측한 ML 점수예요.",
                    color = Color.White.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("다시 측정", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onContinue,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReferenceDesignTokens.Yellow),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("집중 시작", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun EnvironmentMetricCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ReferenceDesignTokens.WhiteCard),
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    color = ReferenceDesignTokens.TextPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    maxLines = 1,
                )
                Text(
                    text = unit,
                    color = ReferenceDesignTokens.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            Text(
                text = label,
                color = ReferenceDesignTokens.TextSecondary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun EnvironmentSensorGraphCard(
    title: String,
    history: List<Float>,
    statusText: String,
    lineColor: Color,
    minValue: Float,
    maxValue: Float,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ReferenceDesignTokens.WhiteCard),
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = title,
                        color = ReferenceDesignTokens.TextPrimary,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Text(
                        text = statusText,
                        color = ReferenceDesignTokens.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (history.size >= 2) {
                MiniLineGraph(
                    dataPoints = history,
                    lineColor = lineColor,
                    minValue = minValue,
                    maxValue = maxValue,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ReferenceDesignTokens.ScreenAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "측정을 시작하면 그래프가 표시돼요",
                        color = ReferenceDesignTokens.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvironmentControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            !isRunning && !isPaused -> {
                Button(
                    onClick = onStart,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReferenceDesignTokens.Dark),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("측정 시작", fontWeight = FontWeight.ExtraBold)
                }
            }

            isRunning -> {
                OutlinedButton(
                    onClick = onPause,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = ReferenceDesignTokens.TextPrimary,
                        ),
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("일시정지", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onFinish,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReferenceDesignTokens.Yellow),
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("완료", fontWeight = FontWeight.ExtraBold)
                }
            }

            else -> {
                Button(
                    onClick = onResume,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReferenceDesignTokens.Dark),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("재개", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = onFinish,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = ReferenceDesignTokens.TextPrimary,
                        ),
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("완료", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun environmentStatusLabel(
    isRunning: Boolean,
    isPaused: Boolean,
    isCompleted: Boolean,
): String =
    when {
        isCompleted -> "측정 완료"
        isRunning -> "측정 중"
        isPaused -> "일시정지"
        else -> "대기 중"
    }

private fun sensorGraphStatusLabel(uiState: EnvironmentSessionUiState): String =
    when {
        uiState.isCompleted -> "측정이 완료되어 마지막 값으로 고정됐어요"
        uiState.isRunning -> "측정 중 실시간으로 업데이트돼요"
        uiState.isPaused -> "일시정지 중에는 그래프가 멈춰요"
        else -> "측정을 시작하면 그래프가 표시돼요"
    }

private fun environmentScoreLabel(score: Int?): String =
    when {
        score == null -> "예측 점수 대기 중"
        score >= 80 -> "집중하기 좋은 환경이에요"
        score >= 60 -> "무난한 집중 환경이에요"
        score > 0 -> "집중 전 조정이 필요해요"
        else -> "유효한 측정값이 부족해요"
    }

private fun formatRemainingTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@Preview(showBackground = true)
@Composable
private fun EnvironmentSessionPreview() {
    FocustationTheme {
        EnvironmentSessionContent(
            uiState =
                EnvironmentSessionUiState(
                    isRunning = true,
                    elapsedSeconds = 138,
                    totalSessionSeconds = 300,
                    noiseHistory = listOf(34f, 36f, 33f, 38f, 35f, 37f, 34f),
                    lightHistory = listOf(460f, 500f, 520f, 540f, 510f, 535f, 520f),
                    vibrationHistory = listOf(0.020f, 0.032f, 0.024f, 0.038f, 0.030f, 0.026f, 0.032f),
                    currentSnapshot =
                        net.focustation.myapplication.data.model.EnvironmentSnapshot(
                            noiseLevel = 35f,
                            illuminance = 520f,
                            vibration = 0.032,
                        ),
                ),
            onBack = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onFinish = {},
            onRetry = {},
            onContinue = {},
        )
    }
}

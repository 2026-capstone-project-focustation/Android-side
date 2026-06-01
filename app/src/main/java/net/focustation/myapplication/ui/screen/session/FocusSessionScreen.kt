package net.focustation.myapplication.ui.screen.session

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.FocusCard
import net.focustation.myapplication.ui.components.FocusSectionHeader
import net.focustation.myapplication.ui.components.MiniLineGraph
import net.focustation.myapplication.ui.theme.ColorLight
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.ColorVibration
import net.focustation.myapplication.ui.theme.FocusCanvas
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocusMuted
import net.focustation.myapplication.ui.theme.FocusSurface
import net.focustation.myapplication.ui.theme.FocustationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    onSessionEnd: () -> Unit,
    onBack: () -> Unit,
    viewModel: FocusSessionViewModel = viewModel(),
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
                Toast.makeText(context, "마이크 권한이 필요합니다. 소음 측정 없이 세션을 시작할 수 없습니다.", Toast.LENGTH_LONG).show()
            }
        }

    FocusSessionContent(
        uiState = uiState,
        onBack = onBack,
        onStart = {
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
        },
        onPause = viewModel::pauseSession,
        onResume = viewModel::startSession,
        onStop = {
            viewModel.stopSession()
            onSessionEnd()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusSessionContent(
    uiState: FocusSessionUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val hours = uiState.elapsedSeconds / 3600
    val minutes = (uiState.elapsedSeconds % 3600) / 60
    val seconds = uiState.elapsedSeconds % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("집중 세션", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = FocusCanvas,
                        titleContentColor = FocusInk,
                        navigationIconContentColor = FocusInk,
                    ),
            )
        },
        containerColor = FocusCanvas,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(FocusCanvas),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(FocusInk, Color(0xFF24312E)),
                            ),
                        ).padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "집중 중",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                        style =
                            MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = Color.White,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text =
                            if (uiState.isRunning) {
                                "측정 중"
                            } else if (uiState.isPaused) {
                                "일시정지"
                            } else {
                                "대기 중"
                            },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.isRunning) ColorLight else Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp, bottom = 8.dp),
            ) {
                FocusSectionHeader(
                    title = "실시간 센서",
                    subtitle = "소음, 조도, 진동 원시값",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                SensorGaugeRow(uiState = uiState, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(18.dp))
                SensorHistoryCard(
                    title = "소음 추이",
                    valueText = uiState.currentNoiseDb?.let { "%.0f dB".format(it) } ?: "-- dB",
                    history = uiState.noiseHistory,
                    lineColor = ColorNoise,
                    minValue = 20f,
                    maxValue = 90f,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                SensorHistoryCard(
                    title = "조도 추이",
                    valueText = uiState.currentLightLux?.let { "%.0f lux".format(it) } ?: "-- lux",
                    history = uiState.lightHistory,
                    lineColor = ColorLight,
                    minValue = 0f,
                    maxValue = 1000f,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                SensorHistoryCard(
                    title = "진동 추이",
                    valueText = uiState.currentVibration?.let { "%.3f".format(it) } ?: "--",
                    history = uiState.vibrationHistory,
                    lineColor = ColorVibration,
                    minValue = 0f,
                    maxValue = 0.2f,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    !uiState.isRunning && !uiState.isPaused -> {
                        Button(
                            onClick = onStart,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusInk),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("집중 시작", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    uiState.isRunning -> {
                        OutlinedButton(
                            onClick = onPause,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("일시정지")
                        }
                        Button(
                            onClick = onStop,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("종료")
                        }
                    }

                    else -> {
                        Button(
                            onClick = onResume,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusInk),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("재개")
                        }
                        OutlinedButton(
                            onClick = onStop,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("종료")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorGaugeRow(
    uiState: FocusSessionUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SensorCircleGauge(
            label = "소음",
            valueText = uiState.currentNoiseDb?.let { "%.0f".format(it) } ?: "--",
            unit = "dB",
            progress = uiState.currentNoiseDb?.toProgress(maxValue = 90f) ?: 0f,
            color = ColorNoise,
            modifier = Modifier.weight(1f),
        )
        SensorCircleGauge(
            label = "조도",
            valueText = uiState.currentLightLux?.let { "%.0f".format(it) } ?: "--",
            unit = "lux",
            progress = uiState.currentLightLux?.toProgress(maxValue = 1000f) ?: 0f,
            color = ColorLight,
            modifier = Modifier.weight(1f),
        )
        SensorCircleGauge(
            label = "진동",
            valueText = uiState.currentVibration?.let { "%.3f".format(it) } ?: "--",
            unit = "",
            progress = uiState.currentVibration?.toFloat()?.toProgress(maxValue = 0.2f) ?: 0f,
            color = ColorVibration,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SensorCircleGauge(
    label: String,
    valueText: String,
    unit: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    FocusCard(
        modifier = modifier.height(136.dp),
        containerColor = FocusSurface,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(70.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(
                        color = color.copy(alpha = 0.16f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = FocusInk,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = FocusInk)
            if (unit.isNotBlank()) {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = FocusMuted)
            }
        }
    }
}

@Composable
private fun SensorHistoryCard(
    title: String,
    valueText: String,
    history: List<Float>,
    lineColor: Color,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
) {
    FocusCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = FocusMuted)
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = FocusInk,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (history.size >= 2) {
                MiniLineGraph(
                    dataPoints = history,
                    lineColor = lineColor,
                    minValue = minValue,
                    maxValue = maxValue,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("측정값 대기 중", style = MaterialTheme.typography.bodySmall, color = FocusMuted)
                }
            }
        }
    }
}

private fun Float.toProgress(maxValue: Float): Float = (this / maxValue).coerceIn(0f, 1f)

@Preview(showBackground = true)
@Composable
private fun FocusSessionPreview() {
    FocustationTheme {
        FocusSessionContent(
            uiState =
                FocusSessionUiState(
                    isRunning = true,
                    elapsedSeconds = 1_275,
                    currentNoiseDb = 34f,
                    currentLightLux = 520f,
                    currentVibration = 0.032,
                    noiseHistory = listOf(32f, 35f, 33f, 38f, 34f, 36f),
                    lightHistory = listOf(420f, 460f, 520f, 500f, 540f, 510f),
                    vibrationHistory = listOf(0.018f, 0.026f, 0.021f, 0.034f, 0.029f, 0.032f),
                ),
            onBack = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
        )
    }
}

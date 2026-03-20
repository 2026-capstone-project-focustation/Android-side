package net.focustation.myapplication.ui.screen.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.EnvironmentSnapshotRow
import net.focustation.myapplication.ui.components.MiniLineGraph
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.ui.theme.Primary40

/**
 * Shows the environment analysis session screen and provides controls to start, pause, resume, and end a timed session.
 *
 * The composable displays remaining time, a progress indicator, real-time sensor readings (noise, illuminance, vibration),
 * a noise-level history graph when available, and an environment suitability score. Starting a session will request
 * microphone permission if needed and begin noise collection when permission is granted. The `onSessionComplete` callback
 * is invoked when the session finishes due to elapsed time or when the user ends the session.
 *
 * @param onSessionComplete Called when the session completes (time elapses or the user ends the session).
 * @param onBack Called when the user requests navigation back from the screen.
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startNoiseCollection()
        viewModel.startSession()
    }

    val remaining = (uiState.totalSessionSeconds - uiState.elapsedSeconds).coerceAtLeast(0)
    val remainingMin = remaining / 60
    val remainingSec = remaining % 60

    LaunchedEffect(uiState.elapsedSeconds) {
        if (uiState.elapsedSeconds >= uiState.totalSessionSeconds && uiState.isRunning) {
            onSessionComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("환경 분석 세션", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1B4B),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 헤더 그라디언트
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0D1B4B), Color(0xFF1A3BAA))),
                        ).padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "남은 시간",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "%02d:%02d".format(remainingMin, remainingSec),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.elapsedSeconds.toFloat() / uiState.totalSessionSeconds },
                        modifier =
                            Modifier
                                .fillMaxWidth(0.7f)
                                .height(6.dp),
                        color = ColorFocus,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 실시간 환경 지표
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "실시간 센서 측정값",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                EnvironmentSnapshotRow(
                    noise = uiState.currentSnapshot.noiseLevel,
                    illuminance = uiState.currentSnapshot.illuminance,
                    vibration = uiState.currentSnapshot.vibration,
                )
            }

            Spacer(Modifier.height(20.dp))

            // 소음 레벨 그래프
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "소음 레벨 추이 (dB)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (uiState.noiseHistory.size >= 2) {
                        MiniLineGraph(
                            dataPoints = uiState.noiseHistory,
                            lineColor = ColorNoise,
                            minValue = 20f,
                            maxValue = 80f,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "세션을 시작하면 그래프가 표시됩니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 환경 적합도 점수
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = ColorFocus.copy(alpha = 0.1f),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🎯", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "환경 적합도",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "%.0f / 100".format(uiState.environmentScore),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ColorFocus,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 컨트롤 버튼
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!uiState.isRunning && !uiState.isPaused) {
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.startNoiseCollection()
                                viewModel.startSession()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary40),
                    ) {
                        Text("▶  세션 시작", style = MaterialTheme.typography.labelLarge)
                    }
                } else if (uiState.isRunning) {
                    OutlinedButton(
                        onClick = { viewModel.pauseSession() },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("⏸  일시정지")
                    }
                    Button(
                        onClick = { onSessionComplete() },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("⏹  종료")
                    }
                } else {
                    // 일시정지 상태
                    Button(
                        onClick = { viewModel.startSession() },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary40),
                    ) {
                        Text("▶  재개")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.stopSession()
                            onSessionComplete()
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("⏹  종료")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EnvironmentSessionPreview() {
    FocustationTheme {
        EnvironmentSessionScreen(onSessionComplete = {}, onBack = {})
    }
}

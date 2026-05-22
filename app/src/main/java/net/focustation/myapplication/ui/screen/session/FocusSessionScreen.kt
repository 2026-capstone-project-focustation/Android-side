package net.focustation.myapplication.ui.screen.session

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.FocusCard
import net.focustation.myapplication.ui.components.FocusInsightCard
import net.focustation.myapplication.ui.components.FocusScoreGauge
import net.focustation.myapplication.ui.components.FocusSectionHeader
import net.focustation.myapplication.ui.components.MiniLineGraph
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.FocusCanvas
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocusMint
import net.focustation.myapplication.ui.theme.FocusMuted
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
            // 헤더 영역 — 타이머
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
                        color = if (uiState.isRunning) ColorFocus else Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 환경 적합도 게이지
            FocusSectionHeader(
                title = "환경 적합도",
                subtitle = "센서값을 합산한 현재 집중 조건",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            FocusScoreGauge(
                score = uiState.environmentFitScore,
                size = 160.dp,
            )

            Spacer(Modifier.height(20.dp))

            // 적합도 추이 그래프
            FocusCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
            ) {
                Column {
                    Text(
                        text = "환경 적합도 추이",
                        style = MaterialTheme.typography.labelLarge,
                        color = FocusMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (uiState.fitHistory.size >= 2) {
                        MiniLineGraph(
                            dataPoints = uiState.fitHistory,
                            lineColor = ColorFocus,
                            minValue = 0f,
                            maxValue = 100f,
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
                                "세션 시작 후 그래프가 표시됩니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = FocusMuted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 집중 팁 카드
            FocusInsightCard(
                title = focusTipTitle(uiState.environmentFitScore),
                message = focusTipMessage(uiState.environmentFitScore, uiState.isRunning),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                accentColor = if (uiState.environmentFitScore >= 60f) FocusMint else ColorFocus,
            )

            Spacer(Modifier.weight(1f))

            // 컨트롤 버튼
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
                            onClick = {
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
                            onClick = { viewModel.pauseSession() },
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
                            onClick = {
                                viewModel.stopSession()
                                onSessionEnd()
                            },
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
                            onClick = { viewModel.startSession() },
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
                            onClick = {
                                viewModel.stopSession()
                                onSessionEnd()
                            },
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

private fun focusTipTitle(score: Float): String =
    when {
        score <= 0f -> "세션을 시작하면 상태가 잡혀요"
        score >= 75f -> "집중 흐름이 안정적이에요"
        score >= 50f -> "조금만 정리하면 좋아져요"
        else -> "환경 조정이 먼저 필요해요"
    }

private fun focusTipMessage(
    score: Float,
    isRunning: Boolean,
): String =
    when {
        score <= 0f && !isRunning -> "마이크 권한을 허용하고 집중 시작을 누르면 소음, 조도, 진동을 함께 추적합니다."
        score >= 75f -> "지금 상태를 유지하면서 작업을 시작해도 좋아요. 세션이 끝나면 리포트에서 흐름을 확인할 수 있어요."
        score >= 50f -> "주변 소리나 조명을 한 번만 조정해보세요. 작은 변화가 점수에 바로 반영됩니다."
        else -> "자리 이동, 조명 조정, 소음 차단 중 하나를 먼저 시도한 뒤 다시 흐름을 이어가세요."
    }

@Preview(showBackground = true)
@Composable
private fun FocusSessionPreview() {
    FocustationTheme {
        FocusSessionScreen(onSessionEnd = {}, onBack = {})
    }
}

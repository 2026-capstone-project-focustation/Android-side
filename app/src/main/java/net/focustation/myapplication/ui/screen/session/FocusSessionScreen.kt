package net.focustation.myapplication.ui.screen.session

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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import net.focustation.myapplication.ui.components.FocusScoreGauge
import net.focustation.myapplication.ui.components.MiniLineGraph
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.ui.theme.Primary40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    onSessionEnd: () -> Unit,
    onBack: () -> Unit,
    viewModel: FocusSessionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
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
            // 헤더 영역 — 타이머
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0D1B4B), Color(0xFF1A3BAA)),
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
                                "● 측정 중"
                            } else if (uiState.isPaused) {
                                "⏸ 일시정지"
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
            Text(
                text = "환경 적합도",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            FocusScoreGauge(
                score = uiState.environmentFitScore,
                size = 160.dp,
            )

            Spacer(Modifier.height(20.dp))

            // 적합도 추이 그래프
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
                        text = "환경 적합도 추이",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 집중 팁 카드
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("💡", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "현재 환경은 집중에 적합합니다. 방해 요소를 최소화하고 작업에 집중해보세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                when {
                    !uiState.isRunning && !uiState.isPaused -> {
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
                            Text("▶  집중 시작", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    uiState.isRunning -> {
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
                            onClick = {
                                viewModel.stopSession()
                                onSessionEnd()
                            },
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
                    }

                    else -> {
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
                                onSessionEnd()
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
}

@Preview(showBackground = true)
@Composable
private fun FocusSessionPreview() {
    FocustationTheme {
        FocusSessionScreen(onSessionEnd = {}, onBack = {})
    }
}

package net.focustation.myapplication.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.EnvironmentSnapshotRow
import net.focustation.myapplication.ui.components.SessionSummaryCard
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.ui.theme.Primary40

@Composable
fun DashboardScreen(
    onStartSession: () -> Unit,
    onNavigateToSpaceHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 6.dp) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("홈") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("리포트") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSpaceHistory,
                    icon = { Text("🗺️", fontSize = 20.sp) },
                    label = { Text("지도") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("설정") },
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ─── 헤더 배너 ──────────────────────────────────────────────────
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF0D1B4B), Color(0xFF1A3BAA)),
                                ),
                            ).padding(horizontal = 24.dp, vertical = 28.dp),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                Text(
                                    text = "안녕하세요, ${uiState.user.name} 👋",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = "오늘도 집중해볼까요?",
                                    style =
                                        MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    color = Color.White,
                                )
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("📍", fontSize = 22.sp)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // 오늘 통계 요약
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatChip(
                                label = "평균 집중도",
                                value = "${uiState.todayAvgFocus}",
                                unit = "점",
                                modifier = Modifier.weight(1f),
                            )
                            StatChip(
                                label = "작업 시간",
                                value = "${uiState.todayWorkMinutes / 60}h ${uiState.todayWorkMinutes % 60}m",
                                unit = "",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // ─── 실시간 환경 스냅샷 ──────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    SectionTitle(title = "현재 환경")
                    Spacer(Modifier.height(10.dp))
                    EnvironmentSnapshotRow(
                        noise = uiState.environmentSnapshot.noiseLevel,
                        illuminance = uiState.environmentSnapshot.illuminance,
                        vibration = uiState.environmentSnapshot.vibration,
                    )
                }
            }

            // ─── 세션 시작 버튼 ──────────────────────────────────────────────
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Button(
                        onClick = onStartSession,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary40),
                        elevation = ButtonDefaults.buttonElevation(6.dp),
                    ) {
                        Text("🔬  환경 분석 세션 시작", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // ─── 최근 세션 목록 ──────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    SectionTitle(title = "최근 세션")
                    Spacer(Modifier.height(10.dp))
                }
            }

            items(uiState.recentSessions) { session ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    SessionSummaryCard(
                        date = session.date,
                        place = session.place,
                        focusScore = session.focusScore,
                        durationMin = session.totalMinutes,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = ColorFocus,
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    FocustationTheme {
        DashboardScreen(
            onStartSession = {},
            onNavigateToSpaceHistory = {},
            onNavigateToSettings = {},
        )
    }
}

package net.focustation.myapplication.ui.screen.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.MainBottomDestination
import net.focustation.myapplication.ui.components.MainBottomNavigationBar
import net.focustation.myapplication.ui.components.SessionSummaryCard
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.ui.theme.Primary40
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionReportScreen(
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onHistoryItemClick: (String) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToSpaceHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    isFromActiveSession: Boolean = false,
    viewModel: SessionReportViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isFromActiveSession) {
        viewModel.onScreenEntered(isFromActiveSession)
    }

    Scaffold(
        bottomBar = {
            MainBottomNavigationBar(
                selected = MainBottomDestination.REPORT,
                onTabClick = { destination ->
                    when (destination) {
                        MainBottomDestination.HOME -> onNavigateToHome()
                        MainBottomDestination.REPORT -> Unit
                        MainBottomDestination.MAP -> onNavigateToSpaceHistory()
                        MainBottomDestination.SETTINGS -> onNavigateToSettings()
                    }
                },
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("공부 리포트", fontWeight = FontWeight.Bold) },
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
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            SessionStatusCard(
                isFromActiveSession = isFromActiveSession,
                isSavingSession = uiState.isSavingSession,
                sessionSaved = uiState.sessionSaved,
                totalFocusMinutes = uiState.totalFocusMinutes,
                avgEnvironmentScore = uiState.avgEnvironmentScore,
                avgNoise = uiState.avgNoise,
                avgIlluminance = uiState.avgIlluminance,
            )

            if (isFromActiveSession) {
                Spacer(Modifier.height(12.dp))
                PlaceSaveCard(
                    placeName = uiState.placeName,
                    placeSaved = uiState.placeSaved,
                    isSavingPlace = uiState.isSavingPlace,
                    onSavePlace = viewModel::savePlace,
                    onRetry = onRetry,
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = uiState.errorMessage ?: "저장 중 오류가 발생했어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "전체 공부 내역",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))

            when {
                uiState.isLoadingHistory -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                uiState.historyErrorMessage != null -> {
                    Text(
                        text = uiState.historyErrorMessage ?: "기록을 불러오지 못했어요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                uiState.history.isEmpty() -> {
                    Text(
                        text = "저장된 공부 기록이 아직 없어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    uiState.history.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SessionSummaryCard(
                                date = formatDate(item.endedAtEpochMillis),
                                place = item.placeName,
                                focusScore = item.focusScore,
                                durationMin = item.durationMinutes,
                                onClick = { onHistoryItemClick(item.sessionId) },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { viewModel.hideHistoryItem(item.sessionId) },
                                enabled = !uiState.deletingSessionIds.contains(item.sessionId),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    if (uiState.deletingSessionIds.contains(item.sessionId)) {
                                        "삭제 중..."
                                    } else {
                                        "삭제"
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SessionStatusCard(
    isFromActiveSession: Boolean,
    isSavingSession: Boolean,
    sessionSaved: Boolean,
    totalFocusMinutes: Int,
    avgEnvironmentScore: Float,
    avgNoise: Float,
    avgIlluminance: Float,
) {
    val statusText =
        when {
            isSavingSession -> "세션 기록 저장 중..."
            sessionSaved && isFromActiveSession -> "이번 세션이 저장되었습니다."
            else -> "로그인한 계정의 누적 기록을 보여줍니다."
        }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryBadge(label = "집중 시간", value = "${totalFocusMinutes}분", modifier = Modifier.weight(1f))
                SummaryBadge(
                    label = "환경 점수",
                    value = "${avgEnvironmentScore.toInt()}점",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryBadge(label = "평균 소음", value = "${avgNoise.toInt()} dB", modifier = Modifier.weight(1f))
                SummaryBadge(label = "평균 조도", value = "${avgIlluminance.toInt()} lux", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlaceSaveCard(
    placeName: String,
    placeSaved: Boolean,
    isSavingPlace: Boolean,
    onSavePlace: () -> Unit,
    onRetry: () -> Unit,
) {
    val normalizedPlaceName = placeName.ifBlank { "장소 미지정" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("이번 세션 장소", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = normalizedPlaceName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSavePlace,
                    enabled = !isSavingPlace && !placeSaved && placeName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (placeSaved) ColorFocus else Primary40),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        when {
                            placeSaved -> "장소 저장됨"
                            isSavingPlace -> "저장 중..."
                            placeName.isBlank() -> "장소 정보 없음"
                            else -> "장소 저장"
                        },
                    )
                }
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text("재측정")
                }
            }
        }
    }
}

@Composable
private fun SummaryBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "날짜 미상"
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun SessionReportPreview() {
    FocustationTheme {
        SessionReportScreen(onBack = {}, onRetry = {}, onHistoryItemClick = {})
    }
}

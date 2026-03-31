package net.focustation.myapplication.ui.screen.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.StudySessionRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionReportDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    repository: FirestoreStudyRepository = remember { FirestoreStudyRepository() },
) {
    var isLoading by remember(sessionId) { mutableStateOf(true) }
    var record by remember(sessionId) { mutableStateOf<StudySessionRecord?>(null) }
    var errorMessage by remember(sessionId) { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        isLoading = true
        val result = repository.getStudySessionById(sessionId)
        result.fold(
            onSuccess = {
                record = it
                errorMessage = null
                isLoading = false
            },
            onFailure = {
                record = null
                errorMessage = it.message ?: "세션 상세 정보를 불러오지 못했어요."
                isLoading = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("세션 상세", fontWeight = FontWeight.Bold) },
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                errorMessage != null -> {
                    Text(text = errorMessage ?: "오류가 발생했어요.", color = MaterialTheme.colorScheme.error)
                }

                record != null -> {
                    val session = record ?: return@Column
                    DetailItemCard("장소", session.placeName)
                    DetailItemCard("세션 종료 시각", formatDate(session.endedAtEpochMillis))
                    DetailItemCard("집중 점수", "${session.focusScoreAvg.toInt()}점")
                    DetailItemCard("집중 시간", "${(session.durationSec / 60).coerceAtLeast(1)}분")
                    DetailItemCard("평균 소음", "${session.avgNoise.toInt()} dB")
                    DetailItemCard("평균 조도", "${session.avgIlluminance.toInt()} lux")
                    DetailItemCard(
                        "평균 진동",
                        String.format(Locale.getDefault(), "%.3f m/s²", session.avgVibration),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItemCard(
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
    Spacer(Modifier.height(8.dp))
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "날짜 미상"
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

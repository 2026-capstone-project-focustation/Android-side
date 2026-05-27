package net.focustation.myapplication.ui.screen.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.StudySessionRecord
import net.focustation.myapplication.ui.components.MiniLineGraph
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.ColorLight
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.ColorVibration
import net.focustation.myapplication.util.DebugLog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        DebugLog.d("[리포트상세][조회] 시작 sessionId=$sessionId")
        isLoading = true
        val result = repository.getStudySessionById(sessionId)
        result.fold(
            onSuccess = {
                DebugLog.d("[리포트상세][조회] 성공 sessionId=${it.sessionId}, 타임라인=${it.focusTimeline.size}개")
                record = it
                errorMessage = null
                isLoading = false
            },
            onFailure = {
                DebugLog.e("[리포트상세][조회] 실패 sessionId=$sessionId, ${it.message}", it)
                record = null
                errorMessage = it.message ?: "세션 상세 정보를 불러오지 못했어요."
                isLoading = false
            },
        )
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp)
                    .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DetailHeader(onBack = onBack)

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "오류가 발생했어요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                record != null -> {
                    val session = record ?: return@Column
                    SessionDetailHero(session = session)
                    FocusTimelineCard(session = session)
                    EnvironmentAnalysisGrid(session = session)
                    SessionMetaSection(session = session)
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(onBack: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ReferenceDesignTokens.PaleBlueTrack),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = ReferenceDesignTokens.TextPrimary.copy(alpha = 0.72f),
            )
        }
        Text(
            text = "세션 분석 리포트",
            color = ReferenceDesignTokens.TextPrimary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SessionDetailHero(session: StudySessionRecord) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.Dark,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = session.placeName.ifBlank { "장소 미지정" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatDate(session.endedAtEpochMillis),
                        color = Color.White.copy(alpha = 0.64f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ReferenceDesignTokens.Yellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = session.displayScore().toString(),
                        color = ReferenceDesignTokens.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetric("집중 시간", "${(session.durationSec / 60).coerceAtLeast(1)}분", Modifier.weight(1f))
                HeroMetric(
                    if (session.mlScore == null) "환경 점수" else "ML 적합도",
                    "${session.displayScore()}점",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(label, color = Color.White.copy(alpha = 0.64f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FocusTimelineCard(session: StudySessionRecord) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.WhiteCard,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "집중 점수 추이",
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )

            val timeline = session.focusTimeline
            if (timeline.size >= 2) {
                MiniLineGraph(
                    dataPoints = timeline.map { it.focusScore },
                    minValue = 0f,
                    maxValue = 100f,
                    lineColor = ReferenceDesignTokens.Blue,
                    modifier = Modifier.fillMaxWidth().height(132.dp),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = timeline.first().timeLabel,
                        color = ReferenceDesignTokens.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = timeline.last().timeLabel,
                        color = ReferenceDesignTokens.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(
                    text = "그래프로 보여줄 타임라인 데이터가 부족해요.",
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EnvironmentAnalysisGrid(session: StudySessionRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalysisMetricCard(
                icon = Icons.Outlined.GraphicEq,
                label = "평균 소음",
                value = "${session.avgNoise.toInt()} dB",
                status = noiseStatus(session.avgNoise),
                tint = ColorNoise,
                modifier = Modifier.weight(1f),
            )
            AnalysisMetricCard(
                icon = Icons.Outlined.LightMode,
                label = "평균 조도",
                value = "${session.avgIlluminance.toInt()} lux",
                status = lightStatus(session.avgIlluminance),
                tint = ColorLight,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalysisMetricCard(
                icon = Icons.Outlined.Sensors,
                label = "평균 진동",
                value = String.format(Locale.getDefault(), "%.3f", session.avgVibration),
                status = vibrationStatus(session.avgVibration),
                tint = ColorVibration,
                modifier = Modifier.weight(1f),
            )
            AnalysisMetricCard(
                icon = Icons.Outlined.Schedule,
                label = if (session.mlScore == null) "세션 길이" else "ML 적합도",
                value =
                    if (session.mlScore == null) {
                        "${(session.durationSec / 60).coerceAtLeast(1)}분"
                    } else {
                        "${session.mlScore.toInt()}점"
                    },
                status = if (session.mlScore == null) "기록됨" else "설문 기반",
                tint = ReferenceDesignTokens.Blue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnalysisMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    status: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(ReferenceDesignTokens.SmallRadius),
        color = ReferenceDesignTokens.WhiteCard,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
                }
                Text(label, color = ReferenceDesignTokens.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    value,
                    color = ReferenceDesignTokens.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(status, color = ReferenceDesignTokens.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SessionMetaSection(session: StudySessionRecord) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.WhiteCard,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "기록 정보",
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            MetaRow(icon = Icons.Outlined.Place, label = "장소", value = session.placeName.ifBlank { "장소 미지정" })
            MetaRow(icon = Icons.Outlined.Schedule, label = "종료 시각", value = formatDate(session.endedAtEpochMillis))
        }
    }
}

@Composable
private fun MetaRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ReferenceDesignTokens.BlueDark, modifier = Modifier.size(18.dp))
        Text(label, color = ReferenceDesignTokens.TextSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            value,
            color = ReferenceDesignTokens.TextPrimary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.End,
        )
    }
}

private fun noiseStatus(noise: Float): String =
    when {
        noise <= 40f -> "조용함"
        noise <= 60f -> "보통"
        else -> "소음 주의"
    }

private fun lightStatus(lux: Float): String =
    when {
        lux in 300f..700f -> "적정"
        lux < 300f -> "어두움"
        else -> "밝음"
    }

private fun vibrationStatus(vibration: Double): String =
    when {
        vibration <= 0.02 -> "안정"
        vibration <= 0.06 -> "보통"
        else -> "흔들림"
    }

private fun StudySessionRecord.displayScore(): Int = (mlScore?.toInt() ?: focusScoreAvg.toInt()).coerceIn(0, 100)

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "날짜 미상"
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

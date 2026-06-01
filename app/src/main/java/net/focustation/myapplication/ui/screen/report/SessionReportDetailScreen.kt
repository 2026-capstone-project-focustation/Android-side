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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.focustation.myapplication.data.model.SensorTimelinePoint
import net.focustation.myapplication.data.model.SensorTimelines
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.StudySessionRecord
import net.focustation.myapplication.ui.components.MiniLineGraph
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.ColorLight
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.ColorVibration
import net.focustation.myapplication.ui.theme.FocustationTheme
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
                DebugLog.d("[리포트상세][조회] 성공 sessionId=${it.sessionId}, 센서추이=${it.sensorTimelines.totalCount()}개")
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

    SessionReportDetailContent(
        isLoading = isLoading,
        record = record,
        errorMessage = errorMessage,
        onBack = onBack,
    )
}

@Composable
private fun SessionReportDetailContent(
    isLoading: Boolean,
    record: StudySessionRecord?,
    errorMessage: String?,
    onBack: () -> Unit,
) {
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
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                record != null -> {
                    SessionDetailHero(session = record)
                    SensorTimelineSection(session = record)
                    EnvironmentAnalysisGrid(session = record)
                    SessionMetaSection(session = record)
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
                        text = session.displayMlScore()?.toString() ?: "--",
                        color = ReferenceDesignTokens.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetric("집중 시간", "${(session.durationSec / 60).coerceAtLeast(1)}분", Modifier.weight(1f))
                HeroMetric(
                    "ML 적합도",
                    session.displayMlScore()?.let { "${it}점" } ?: "대기",
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
private fun SensorTimelineSection(session: StudySessionRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SensorTimelineCard(
            title = "소음 추이",
            valueText = "${session.avgNoise.toInt()} dB",
            points = session.sensorTimelines.noise,
            fallbackValue = session.avgNoise,
            minValue = 20f,
            maxValue = 90f,
            lineColor = ColorNoise,
        )
        SensorTimelineCard(
            title = "조도 추이",
            valueText = "${session.avgIlluminance.toInt()} lux",
            points = session.sensorTimelines.light,
            fallbackValue = session.avgIlluminance,
            minValue = 0f,
            maxValue = 1000f,
            lineColor = ColorLight,
        )
        SensorTimelineCard(
            title = "진동 추이",
            valueText = String.format(Locale.getDefault(), "%.3f", session.avgVibration),
            points = session.sensorTimelines.vibration,
            fallbackValue = session.avgVibration.toFloat(),
            minValue = 0f,
            maxValue = 0.2f,
            lineColor = ColorVibration,
        )
    }
}

@Composable
private fun SensorTimelineCard(
    title: String,
    valueText: String,
    points: List<SensorTimelinePoint>,
    fallbackValue: Float,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
) {
    val graphPoints =
        if (points.size >= 2) {
            points
        } else {
            listOf(
                SensorTimelinePoint("기록", fallbackValue.coerceAtLeast(0f)),
                SensorTimelinePoint("평균", fallbackValue.coerceAtLeast(0f)),
            )
        }
    val statusText = if (points.size >= 2) "${points.size}개 지점" else "저장된 추이 없음 · 평균값 표시"

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        color = ReferenceDesignTokens.TextPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = statusText,
                        color = ReferenceDesignTokens.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = valueText,
                    color = lineColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            MiniLineGraph(
                dataPoints = graphPoints.map { it.value },
                minValue = minValue,
                maxValue = maxValue,
                lineColor = lineColor,
                modifier = Modifier.fillMaxWidth().height(112.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = graphPoints.first().timeLabel,
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = graphPoints.last().timeLabel,
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
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
                label = "ML 적합도",
                value = session.displayMlScore()?.let { "${it}점" } ?: "--",
                status = if (session.validMlScore == null) "미생성" else "ML 추론",
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

// mlScore는 ML 새 계약 전까지 더미값 -1로 저장된다. 0 미만이면 아직 점수가 없는 것으로 본다.
private val StudySessionRecord.validMlScore: Double?
    get() = mlScore?.takeIf { it >= 0.0 }

private fun StudySessionRecord.displayMlScore(): Int? = validMlScore?.toInt()?.coerceIn(0, 100)

private fun SensorTimelines.totalCount(): Int = noise.size + light.size + vibration.size

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "날짜 미상"
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun SessionReportDetailPreview() {
    FocustationTheme {
        SessionReportDetailContent(
            isLoading = false,
            record = sampleStudySessionRecord(),
            errorMessage = null,
            onBack = {},
        )
    }
}

private fun sampleStudySessionRecord(): StudySessionRecord =
    StudySessionRecord(
        sessionId = "preview_session",
        endedAtEpochMillis = 1_750_000_000_000L,
        durationSec = 4_200,
        focusScoreAvg = 82f,
        avgNoise = 34f,
        avgIlluminance = 520f,
        avgVibration = 0.032,
        placeName = "중앙 도서관 3층",
        mlScore = 86.4,
        sensorTimelines =
            SensorTimelines(
                noise =
                    listOf(
                        SensorTimelinePoint("0분", 36f),
                        SensorTimelinePoint("10분", 34f),
                        SensorTimelinePoint("20분", 38f),
                        SensorTimelinePoint("30분", 32f),
                        SensorTimelinePoint("40분", 35f),
                        SensorTimelinePoint("50분", 33f),
                        SensorTimelinePoint("60분", 34f),
                    ),
                light =
                    listOf(
                        SensorTimelinePoint("0분", 480f),
                        SensorTimelinePoint("10분", 520f),
                        SensorTimelinePoint("20분", 560f),
                        SensorTimelinePoint("30분", 530f),
                        SensorTimelinePoint("40분", 510f),
                        SensorTimelinePoint("50분", 545f),
                        SensorTimelinePoint("60분", 520f),
                    ),
                vibration =
                    listOf(
                        SensorTimelinePoint("0분", 0.020f),
                        SensorTimelinePoint("10분", 0.034f),
                        SensorTimelinePoint("20분", 0.028f),
                        SensorTimelinePoint("30분", 0.041f),
                        SensorTimelinePoint("40분", 0.030f),
                        SensorTimelinePoint("50분", 0.025f),
                        SensorTimelinePoint("60분", 0.032f),
                    ),
            ),
    )

package net.focustation.myapplication.ui.screen.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.data.model.FocusDataPoint
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.ColorLight
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.ColorVibration
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.ui.theme.Primary40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionReportScreen(
    onBack: () -> Unit,
    onRetry: () -> Unit,
    viewModel: SessionReportViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("세션 리포트", fontWeight = FontWeight.Bold) },
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
                    .background(MaterialTheme.colorScheme.background),
        ) {
            // 헤더 요약 배너
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0D1B4B), Color(0xFF1A3BAA))),
                        ).padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Column {
                    Text(
                        text = "세션 완료! 🎉",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryBadge(
                            label = "총 집중 시간",
                            value = "${uiState.totalFocusMinutes}분",
                            modifier = Modifier.weight(1f),
                        )
                        SummaryBadge(
                            label = "환경 적합도",
                            value = "%.0f / 100".format(uiState.avgEnvironmentScore),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 집중도 변화 그래프
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "집중도 변화 (시간대별)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(12.dp))
                    FocusTimelineChart(
                        dataPoints = uiState.focusTimeline,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        uiState.focusTimeline.forEach { point ->
                            Text(
                                text = point.timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 환경 요소별 상세
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "환경 요소 분석",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(12.dp))
                    EnvDetailRow(
                        label = "소음",
                        value = "%.1f dB".format(uiState.avgNoise),
                        color = ColorNoise,
                        rating =
                            (
                                (
                                    80 -
                                        uiState.avgNoise
                                ) /
                                    80f
                            ).coerceIn(0f, 1f),
                    )
                    Spacer(Modifier.height(8.dp))
                    EnvDetailRow(
                        label = "조도",
                        value = "%.0f lux".format(uiState.avgIlluminance),
                        color = ColorLight,
                        rating =
                            (
                                uiState.avgIlluminance /
                                    600f
                            ).coerceIn(0f, 1f),
                    )
                    Spacer(Modifier.height(8.dp))
                    EnvDetailRow(
                        label = "진동",
                        value = "%.3f m/s²".format(uiState.avgVibration),
                        color = ColorVibration,
                        rating = (1f - (uiState.avgVibration / 0.1).toFloat()).coerceIn(0f, 1f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 장소 저장 버튼 (세션 직후에만)
            if (uiState.isFromActiveSession) {
                Button(
                    onClick = { viewModel.savePlace() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (uiState.placeSaved) ColorFocus else Primary40,
                        ),
                ) {
                    Text(
                        text = if (uiState.placeSaved) "📍 장소 저장됨" else "📍 해당 장소 저장",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // 공유 및 재측정 버튼
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { /* 공유 */ },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("📤 공유")
                }
                OutlinedButton(
                    onClick = onRetry,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("🔄 재측정")
                }
            }

            Spacer(Modifier.height(24.dp))
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ColorFocus,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * Displays a single environment metric row containing a colored indicator, the metric value, and a progress bar reflecting the metric's suitability.
 *
 * @param label The metric label shown next to the colored indicator (e.g., "Noise").
 * @param value The formatted metric value displayed on the right (e.g., "42.0 dB").
 * @param color The color used for the indicator dot and the progress bar.
 * @param rating A fraction between 0 and 1 representing the metric's suitability (0 = worst, 1 = best).
 */
@Composable
private fun EnvDetailRow(
    label: String,
    value: String,
    color: Color,
    rating: Float,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .background(color, shape = androidx.compose.foundation.shape.CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { rating },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

/**
 * Renders a horizontal timeline chart of focus scores using a filled area, a connecting line, and point markers.
 *
 * Expects each FocusDataPoint's `focusScore` to be on a 0–100 scale; if `dataPoints` contains fewer than two entries, nothing is rendered.
 *
 * @param dataPoints List of focus measurements plotted evenly across the chart width; each item's `focusScore` determines its vertical position.
 * @param modifier Modifier to apply to the chart layout and drawing area.
 */
@Composable
private fun FocusTimelineChart(
    dataPoints: List<FocusDataPoint>,
    modifier: Modifier = Modifier,
) {
    if (dataPoints.size < 2) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minVal = 0f
        val maxVal = 100f
        val stepX = w / (dataPoints.size - 1)
        val normalize = { v: Float -> (1f - (v - minVal) / (maxVal - minVal)) * h }

        // 채우기 영역
        val fillPath = Path()
        dataPoints.forEachIndexed { i, dp ->
            val x = i * stepX
            val y = normalize(dp.focusScore)
            if (i == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
        }
        fillPath.lineTo((dataPoints.size - 1) * stepX, h)
        fillPath.lineTo(0f, h)
        fillPath.close()
        drawPath(
            fillPath,
            brush =
                Brush.verticalGradient(
                    colors = listOf(ColorFocus.copy(alpha = 0.4f), Color.Transparent),
                ),
        )

        // 라인
        val linePath = Path()
        dataPoints.forEachIndexed { i, dp ->
            val x = i * stepX
            val y = normalize(dp.focusScore)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, color = ColorFocus, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

        // 점
        dataPoints.forEachIndexed { i, dp ->
            val x = i * stepX
            val y = normalize(dp.focusScore)
            drawCircle(color = ColorFocus, radius = 5.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionReportPreview() {
    FocustationTheme {
        SessionReportScreen(onBack = {}, onRetry = {})
    }
}

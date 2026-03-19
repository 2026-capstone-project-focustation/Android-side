package net.focustation.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.focustation.myapplication.ui.theme.ColorFocus
import net.focustation.myapplication.ui.theme.ColorLight
import net.focustation.myapplication.ui.theme.ColorNoise
import net.focustation.myapplication.ui.theme.ColorVibration

// ─── 환경 지표 카드 (소음, 조도, 온도) ──────────────────────────────────────

@Composable
fun EnvMetricCard(
    label: String,
    value: String,
    unit: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(indicatorColor),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── 소음 / 조도 / 온도 스냅샷 Row ──────────────────────────────────────────

@Composable
fun EnvironmentSnapshotRow(
    noise: Float,
    illuminance: Float,
    vibration: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EnvMetricCard(
            label = "소음",
            value = "%.0f".format(noise),
            unit = "dB",
            indicatorColor = ColorNoise,
            modifier = Modifier.weight(1f),
        )
        EnvMetricCard(
            label = "조도",
            value = "%.0f".format(illuminance),
            unit = "lux",
            indicatorColor = ColorLight,
            modifier = Modifier.weight(1f),
        )
        EnvMetricCard(
            label = "진동",
            value = "%.3f".format(vibration),
            unit = "m/s²",
            indicatorColor = ColorVibration,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─── 집중도 게이지 ────────────────────────────────────────────────────────────

@Composable
fun FocusScoreGauge(
    score: Float, // 0~100
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    val percentage = (score / 100f).coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 14.dp.toPx()
            drawArc(
                color = Color(0x22FFFFFF),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush =
                    Brush.sweepGradient(
                        listOf(ColorFocus.copy(alpha = 0.6f), ColorFocus),
                    ),
                startAngle = 140f,
                sweepAngle = 260f * percentage,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.0f".format(score),
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
                color = ColorFocus,
            )
            Text(
                text = "/ 100",
                fontSize = (size.value * 0.10f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── 미니 꺾은선 그래프 ──────────────────────────────────────────────────────

@Composable
fun MiniLineGraph(
    dataPoints: List<Float>,
    lineColor: Color = ColorNoise,
    modifier: Modifier = Modifier,
    minValue: Float = 20f,
    maxValue: Float = 80f,
) {
    if (dataPoints.size < 2) return
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val step = w / (dataPoints.size - 1)
        val normalize = { v: Float -> (1f - (v - minValue) / (maxValue - minValue)).coerceIn(0f, 1f) * h }
        val path =
            androidx.compose.ui.graphics
                .Path()
        dataPoints.forEachIndexed { i, v ->
            val x = i * step
            val y = normalize(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ─── 세션 요약 카드 ───────────────────────────────────────────────────────────

@Composable
fun SessionSummaryCard(
    date: String,
    place: String,
    focusScore: Int,
    durationMin: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$date · ${durationMin}분",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(ColorFocus.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$focusScore",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorFocus,
                )
            }
        }
    }
}

// ─── 별점 선택기 (1~5) ───────────────────────────────────────────────────────

@Composable
fun StarRatingSelector(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { star ->
            IconButton(
                onClick = { onRatingChange(star) },
                modifier = Modifier.size(36.dp),
            ) {
                Text(
                    text = if (star <= rating) "★" else "☆",
                    fontSize = 26.sp,
                    color = if (star <= rating) ColorLight else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── 하단 내비게이션 바 ───────────────────────────────────────────────────────

enum class BottomNavItem(
    val label: String,
    val icon: String,
    val route: String,
) {
    DASHBOARD("홈", "🏠", "dashboard"),
    REPORT("리포트", "📊", "session_report"),
    MAP("지도", "🗺️", "space_history"),
    SETTINGS("설정", "⚙️", "settings"),
}

@Composable
fun FocustationBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    Text(text = item.icon, fontSize = 20.sp)
                },
                label = {
                    Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                },
            )
        }
    }
}

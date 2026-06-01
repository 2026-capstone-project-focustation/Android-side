package net.focustation.myapplication.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.focustation.myapplication.ui.theme.FocusBlue
import net.focustation.myapplication.ui.theme.FocusCoral
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocusLavender
import net.focustation.myapplication.ui.theme.FocusLine
import net.focustation.myapplication.ui.theme.FocusMint
import net.focustation.myapplication.ui.theme.FocusMuted
import net.focustation.myapplication.ui.theme.FocusSurface
import net.focustation.myapplication.ui.theme.FocusYellow
import net.focustation.myapplication.ui.theme.FocustationTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FocusScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(ReferenceDesignTokens.Screen),
        content = content,
    )
}

@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    containerColor: Color = FocusSurface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, FocusLine.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun FocusPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = FocusBlue,
                contentColor = Color.White,
                disabledContainerColor = FocusMuted.copy(alpha = 0.25f),
                disabledContentColor = FocusMuted,
            ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FocusSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = FocusInk,
                fontWeight = FontWeight.Bold,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FocusMuted,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun FocusEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    FocusCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = FocusInk,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FocusMuted,
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = FocusInk,
                            contentColor = Color.White,
                        ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun FocusInsightCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    accentColor: Color = FocusBlue,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .background(accentColor, CircleShape)
                        .padding(top = 2.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = FocusInk,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = FocusMuted,
                )
            }
        }
    }
}

@Composable
fun FocusMetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = Color.White,
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .background(color, CircleShape),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = FocusInk,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FocusMuted,
            )
        }
    }
}

@Composable
fun AnimatedFocusOrb(
    score: Int,
    modifier: Modifier = Modifier,
    accent: Color = FocusMint,
    size: Dp = 210.dp,
) {
    val transition = rememberInfiniteTransition(label = "focus_orb")
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1800),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse",
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 5200),
                repeatMode = RepeatMode.Restart,
            ),
        label = "orbit",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(
                color = accent.copy(alpha = 0.10f * pulse),
                radius = radius * 0.92f * pulse,
                center = center,
            )
            drawCircle(
                brush =
                    Brush.radialGradient(
                        listOf(
                            accent.copy(alpha = 0.92f),
                            FocusBlue.copy(alpha = 0.88f),
                            FocusInk.copy(alpha = 0.96f),
                        ),
                        center = center,
                        radius = radius,
                    ),
                radius = radius * 0.68f,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = radius * 0.68f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )

            val colors = listOf(FocusYellow, FocusCoral, FocusLavender)
            colors.forEachIndexed { index, color ->
                val angle = Math.toRadians((orbit + index * 118f).toDouble())
                val dotRadius = radius * (0.76f + index * 0.035f)
                drawCircle(
                    color = color,
                    radius = (5 + index).dp.toPx(),
                    center =
                        Offset(
                            x = center.x + cos(angle).toFloat() * dotRadius,
                            y = center.y + sin(angle).toFloat() * dotRadius,
                        ),
                )
            }
        }
        Text(
            text = "$score",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
        )
        Text(
            text = "focus fit",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 46.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun FocusProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = FocusMint,
) {
    Canvas(modifier = modifier.height(8.dp)) {
        val stroke = 8.dp.toPx()
        drawLine(
            color = FocusLine,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = activeColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FocusDesignComponentsPreview() {
    FocustationTheme {
        FocusScreenBackground {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FocusSectionHeader(
                    title = "집중 환경",
                    subtitle = "현재 공간의 센서 상태를 요약합니다.",
                    trailing = {
                        FocusMetricPill(label = "점수", value = "82", color = FocusBlue)
                    },
                )
                FocusCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusInsightCard(
                            title = "소음 안정",
                            message = "최근 측정값이 조용한 편으로 유지되고 있어요.",
                            accentColor = FocusMint,
                        )
                        FocusProgressTrack(progress = 0.72f, modifier = Modifier.fillMaxWidth())
                        FocusPrimaryButton(text = "세션 시작", onClick = {}, modifier = Modifier.fillMaxWidth())
                    }
                }
                FocusEmptyState(
                    title = "아직 리포트가 없어요",
                    message = "첫 세션을 마치면 이곳에 분석 기록이 쌓입니다.",
                    actionLabel = "측정 시작",
                    onAction = {},
                )
                AnimatedFocusOrb(
                    score = 82,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    size = 180.dp,
                )
            }
        }
    }
}

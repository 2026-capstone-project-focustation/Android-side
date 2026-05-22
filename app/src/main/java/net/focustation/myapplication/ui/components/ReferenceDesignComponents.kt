package net.focustation.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ReferenceDesignTokens {
    val Canvas = Color(0xFFEAF4FF)
    val Contour = Color(0xFFC9DDF2)
    val Screen = Color(0xFFF4F7FC)
    val ScreenAlt = Color(0xFFF7FAFF)
    val Dark = Color(0xFF202837)
    val DarkAlt = Color(0xFF2B3545)
    val Blue = Color(0xFF126DFF)
    val BlueDark = Color(0xFF0C5FE8)
    val Yellow = Color(0xFFFFC052)
    val YellowDark = Color(0xFFE2A540)
    val WhiteCard = Color.White
    val TextPrimary = Color(0xFF141923)
    val TextSecondary = Color(0xFF7A8494)
    val TextMuted = Color(0xFFA6AFBD)
    val Border = Color(0xFFE7EDF6)
    val DarkDivider = Color(0xFF3A4556)
    val PaleBlueTrack = Color(0xFFE8EEF8)

    val PhoneRadius = 24.dp
    val LargeRadius = 20.dp
    val SmallRadius = 16.dp
    val NavRadius = 28.dp
}

@Immutable
data class CalendarDaySlot(
    val weekday: String,
    val day: String,
    val selected: Boolean = false,
)

@Immutable
data class NavItemSlot(
    val icon: ImageVector,
    val contentDescription: String,
)

@Immutable
data class ListItemSlot(
    val title: String,
    val subtitle: String,
    val thumbnailColor: Color,
    val accentColor: Color = ReferenceDesignTokens.Blue,
)

@Composable
fun ReferenceAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(ReferenceDesignTokens.Canvas),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawContourPattern()
        }
        content()
    }
}

@Composable
fun ReferencePhoneSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(ReferenceDesignTokens.PhoneRadius),
                    ambientColor = Color(0xFF91A5BC).copy(alpha = 0.16f),
                    spotColor = Color(0xFF91A5BC).copy(alpha = 0.20f),
                ).clip(RoundedCornerShape(ReferenceDesignTokens.PhoneRadius))
                .background(ReferenceDesignTokens.Screen),
        content = content,
    )
}

@Composable
fun ReferenceStatusBar(
    modifier: Modifier = Modifier,
    time: String = "09:41",
    dark: Boolean = false,
) {
    val color = if (dark) Color.White else ReferenceDesignTokens.TextPrimary
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            SignalBars(color = color)
            WifiGlyph(color = color)
            BatteryGlyph(color = color)
        }
    }
}

@Composable
fun DarkHeader(
    headerTitle: String,
    headerSubtitle: String,
    modifier: Modifier = Modifier,
    avatar: (@Composable BoxScope.() -> Unit)? = null,
    calendarDays: List<CalendarDaySlot> = emptyList(),
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (calendarDays.isEmpty()) 132.dp else 166.dp),
        shape = RoundedCornerShape(ReferenceDesignTokens.PhoneRadius),
        color = ReferenceDesignTokens.Dark,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = headerSubtitle,
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (avatar != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ReferenceDesignTokens.Yellow),
                        contentAlignment = Alignment.Center,
                        content = avatar,
                    )
                }
            }
            if (calendarDays.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                CalendarStrip(days = calendarDays)
            }
        }
    }
}

@Composable
fun CalendarStrip(
    days: List<CalendarDaySlot>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.take(7).forEach { day ->
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(62.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = if (day.selected) 0.14f else 0.06f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = day.weekday,
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (day.selected) ReferenceDesignTokens.Yellow else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.day,
                        color =
                            if (day.selected) {
                                ReferenceDesignTokens.TextPrimary
                            } else {
                                Color.White.copy(
                                    alpha = 0.72f,
                                )
                            },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(18.dp),
        color = ReferenceDesignTokens.WhiteCard,
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    color = ReferenceDesignTokens.TextPrimary,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 31.sp),
                    maxLines = 1,
                )
                Text(
                    text = label,
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ActionCardBlue(
    title: String,
    value: String,
    unit: String,
    graphPoints: List<Float>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    ActionCard(
        title = title,
        value = value,
        unit = unit,
        graphPoints = graphPoints,
        modifier = modifier,
        containerColor = ReferenceDesignTokens.Blue,
        contentColor = Color.White,
        arrowColor = ReferenceDesignTokens.BlueDark,
        onClick = onClick,
    )
}

@Composable
fun ActionCardYellow(
    title: String,
    value: String,
    unit: String,
    graphPoints: List<Float>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    ActionCard(
        title = title,
        value = value,
        unit = unit,
        graphPoints = graphPoints,
        modifier = modifier,
        containerColor = ReferenceDesignTokens.Yellow,
        contentColor = ReferenceDesignTokens.TextPrimary,
        arrowColor = ReferenceDesignTokens.YellowDark,
        onClick = onClick,
    )
}

@Composable
fun ProgressGaugeCard(
    title: String,
    subtitle: String,
    percent: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(116.dp),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.WhiteCard,
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    color = ReferenceDesignTokens.TextPrimary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = subtitle,
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            GaugeArc(percent = percent, modifier = Modifier.size(width = 116.dp, height = 72.dp))
        }
    }
}

@Composable
fun AreaChartCard(
    title: String,
    filterLabel: String,
    points: List<Float>,
    xLabels: List<String>,
    yLabels: List<String>,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 210.dp,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(cardHeight),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.WhiteCard,
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, color = ReferenceDesignTokens.TextPrimary, style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        filterLabel,
                        color = ReferenceDesignTokens.BlueDark,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = ReferenceDesignTokens.BlueDark,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AreaChart(
                    points = points,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(start = 30.dp, end = 4.dp, bottom = 18.dp, top = 4.dp),
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    yLabels.take(3).forEach {
                        Text(it, color = ReferenceDesignTokens.TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    xLabels.take(7).forEachIndexed { index, label ->
                        Text(
                            text = label,
                            color = if (index == 4) ReferenceDesignTokens.BlueDark else ReferenceDesignTokens.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMetricCardBlue(
    title: String,
    value: String,
    barValues: List<Float>,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Bedtime,
    cardHeight: Dp = 160.dp,
) {
    Surface(
        modifier =
            modifier
                .height(cardHeight)
                .referenceShadow(20.dp),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.Blue,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconPill(icon = icon, tint = Color.White, background = ReferenceDesignTokens.BlueDark)
                Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            BarMiniChart(values = barValues, modifier = Modifier.fillMaxWidth().height(68.dp))
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun MiniMetricCardYellow(
    title: String,
    value: String,
    waveValues: List<Float>,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
    cardHeight: Dp = 160.dp,
) {
    Surface(
        modifier =
            modifier
                .height(cardHeight)
                .referenceShadow(20.dp),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.Yellow,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconPill(
                    icon = icon,
                    tint = ReferenceDesignTokens.TextPrimary,
                    background = ReferenceDesignTokens.YellowDark,
                )
                Text(title, color = ReferenceDesignTokens.TextPrimary, style = MaterialTheme.typography.labelLarge)
            }
            WaveMiniChart(values = waveValues, modifier = Modifier.fillMaxWidth().height(76.dp))
            Text(value, color = ReferenceDesignTokens.TextPrimary, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun WorkoutHeader(
    title: String,
    metricALabel: String,
    metricAValue: String,
    metricBLabel: String,
    metricBValue: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPause: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxWidth().height(214.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(194.dp),
            shape = RoundedCornerShape(ReferenceDesignTokens.PhoneRadius),
            color = ReferenceDesignTokens.Dark,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                ReferenceStatusBar(dark = true)
                Spacer(Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBack,
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ReferenceDesignTokens.DarkAlt),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.78f),
                        )
                    }
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Spacer(Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricColumn(label = metricALabel, value = metricAValue, alignStart = true)
                    MetricColumn(label = metricBLabel, value = metricBValue, alignStart = false)
                }
                Spacer(Modifier.height(8.dp))
                ProgressLine(progress = progress)
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(ReferenceDesignTokens.Dark),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onPause,
                modifier =
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(ReferenceDesignTokens.Yellow),
            ) {
                Icon(Icons.Filled.Pause, contentDescription = null, tint = ReferenceDesignTokens.TextPrimary)
            }
        }
    }
}

@Composable
fun WorkoutListItem(
    item: ListItemSlot,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(78.dp)
                .referenceShadow(18.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.WhiteCard,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReferenceThumbnail(item.thumbnailColor, item.accentColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = ReferenceDesignTokens.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ReferenceDesignTokens.PaleBlueTrack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ReferenceDesignTokens.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun PrimaryCTA(
    label: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
    onClick: () -> Unit = {},
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = ReferenceDesignTokens.Blue,
                contentColor = Color.White,
            ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (showChevron) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun ReferenceBottomNavigationBar(
    selectedIndex: Int,
    items: List<NavItemSlot>,
    modifier: Modifier = Modifier,
    onItemClick: (Int) -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .width(260.dp)
                .height(62.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(ReferenceDesignTokens.NavRadius),
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.16f),
                ).clip(RoundedCornerShape(ReferenceDesignTokens.NavRadius))
                .background(ReferenceDesignTokens.Dark)
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            Box(
                modifier =
                    Modifier
                        .size(if (selected) 48.dp else 42.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { onItemClick(index) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.contentDescription,
                    tint = if (selected) ReferenceDesignTokens.Dark else Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
fun ReferenceDesignShowcaseScreen(modifier: Modifier = Modifier) {
    ReferenceAppBackground(modifier = modifier) {
        ReferencePhoneSurface(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .width(360.dp)
                    .height(780.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                ShowcaseProgressScreen()
            }
        }
    }
}

@Composable
fun ShowcaseHomeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 74.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DarkHeader(
                headerTitle = "안녕하세요",
                headerSubtitle = "Focustation은 당신에게 꼭 맞는 집중 환경을 찾고 분석해줄 당신만의 파트너예요.",
                avatar = {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = ReferenceDesignTokens.Dark)
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    value = "82",
                    label = "평균 점수",
                    icon = Icons.Filled.WaterDrop,
                    iconColor = ReferenceDesignTokens.Blue,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = "47분",
                    label = "집중 시간",
                    icon = Icons.Outlined.LocalFireDepartment,
                    iconColor = ReferenceDesignTokens.Yellow,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("실시간 환경", color = ReferenceDesignTokens.TextPrimary, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionCardBlue(
                    title = "소음",
                    value = "30",
                    unit = "dB",
                    graphPoints = listOf(0.2f, 0.48f, 0.42f, 0.76f, 0.72f),
                    modifier = Modifier.weight(1f),
                )
                ActionCardYellow(
                    title = "조도",
                    value = "40",
                    unit = "lux",
                    graphPoints = listOf(0.3f, 0.44f, 0.36f, 0.64f, 0.55f),
                    modifier = Modifier.weight(1f),
                )
            }
            Text("자주 쓰는 공간", color = ReferenceDesignTokens.TextPrimary, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    ReferenceAvatar(index)
                }
            }
        }
        ReferenceBottomNavigationBar(
            selectedIndex = 0,
            items = defaultNavSlots(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        )
    }
}

@Composable
fun ShowcaseProgressScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 74.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReferenceStatusBar(modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp))
            Text(
                "세션 리포트",
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            ProgressGaugeCard(
                title = "평균 환경 적합도 점수",
                subtitle = "최근 세션의 환경이 얼마나 적합했는지 보여드려요.",
                percent = 0.75f,
            )
            AreaChartCard(
                title = "주간 집중 흐름",
                filterLabel = "7일",
                points = listOf(0.28f, 0.54f, 0.45f, 0.58f, 0.86f, 0.43f, 0.55f),
                xLabels = listOf("S", "M", "T", "W", "T", "F", "S"),
                yLabels = listOf("200+", "100", "0"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MiniMetricCardBlue(
                    title = "최저 소음",
                    value = "28 dB",
                    barValues = listOf(0.72f, 0.92f, 0.64f, 0.82f, 0.54f, 0.76f, 0.92f),
                    modifier = Modifier.weight(1f),
                )
                MiniMetricCardYellow(
                    title = "평균 조도",
                    value = "410 lux",
                    waveValues = listOf(0.28f, 0.58f, 0.52f, 0.72f, 0.36f),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ReferenceBottomNavigationBar(
            selectedIndex = 2,
            items = defaultNavSlots(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        )
    }
}

@Composable
fun ShowcaseListScreen(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WorkoutHeader(
                title = "공간 기록",
                metricALabel = "최고 장소",
                metricAValue = "도서관",
                metricBLabel = "세션",
                metricBValue = "12회",
                progress = 0.46f,
            )
            Text("최근 측정 장소", color = ReferenceDesignTokens.TextPrimary, style = MaterialTheme.typography.labelLarge)
            listOf(
                ListItemSlot("중앙 도서관", "평균 86점 · 조용한 편", Color(0xFFEAF2FF), ReferenceDesignTokens.Blue),
                ListItemSlot("캠퍼스 카페", "평균 72점 · 오후 추천", Color(0xFFFFF2D7), ReferenceDesignTokens.YellowDark),
                ListItemSlot("스터디룸 B", "평균 81점 · 조도 안정", Color(0xFFF1E8FF), Color(0xFF8B6BE8)),
                ListItemSlot("집 책상", "평균 68점 · 소음 관리 필요", Color(0xFFEFF6F4), Color(0xFF58A68A)),
            ).forEach { item ->
                WorkoutListItem(item = item)
            }
            PrimaryCTA(label = "기록 시작하기", onClick = onFinish)
        }
    }
}

private fun Modifier.referenceShadow(radius: Dp): Modifier =
    shadow(
        elevation = radius / 2,
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        ambientColor = Color(0xFF91A5BC).copy(alpha = 0.12f),
        spotColor = Color(0xFF91A5BC).copy(alpha = 0.18f),
    )

@Composable
private fun ActionCard(
    title: String,
    value: String,
    unit: String,
    graphPoints: List<Float>,
    containerColor: Color,
    contentColor: Color,
    arrowColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier =
            modifier
                .height(164.dp)
                .referenceShadow(18.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = containerColor,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Column {
                Text(title, color = contentColor, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        color = contentColor,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 35.sp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        unit,
                        color = contentColor.copy(alpha = 0.80f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            DashedMiniLineGraph(
                points = graphPoints,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.74f)
                        .height(44.dp),
                color = if (contentColor == Color.White) Color.White else ReferenceDesignTokens.TextPrimary,
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(arrowColor.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun GaugeArc(
    percent: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val arcSize = Size(size.width * 0.82f, size.height * 1.55f)
            val topLeft = Offset(size.width * 0.09f, size.height * 0.18f)
            drawArc(
                color = ReferenceDesignTokens.PaleBlueTrack,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ReferenceDesignTokens.Blue,
                startAngle = 200f,
                sweepAngle = 140f * percent.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReferenceDesignTokens.PaleBlueTrack)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
        ) {
            Text(
                text = "${(percent.coerceIn(0f, 1f) * 100).toInt()}%",
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun AreaChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val lineColor = ReferenceDesignTokens.YellowDark
        val stroke = 2.dp.toPx()
        val chartPoints = normalizedOffsets(points, size.width, size.height)
        if (chartPoints.size < 2) return@Canvas

        repeat(3) { index ->
            val y = size.height * index / 2f
            drawLine(
                color = ReferenceDesignTokens.Border,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val fillPath =
            smoothPath(chartPoints).apply {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(
            path = fillPath,
            brush =
                Brush.verticalGradient(
                    colors = listOf(ReferenceDesignTokens.Yellow.copy(alpha = 0.28f), Color.Transparent),
                ),
        )
        drawPath(
            path = smoothPath(chartPoints),
            color = lineColor,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        chartPoints.forEach {
            drawCircle(color = ReferenceDesignTokens.Yellow, radius = 4.dp.toPx(), center = it)
            drawCircle(color = lineColor, radius = 2.dp.toPx(), center = it)
        }
    }
}

@Composable
private fun DashedMiniLineGraph(
    points: List<Float>,
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val chartPoints = normalizedOffsets(points, size.width, size.height)
        if (chartPoints.size < 2) return@Canvas
        drawPath(
            path = smoothPath(chartPoints),
            color = color.copy(alpha = 0.82f),
            style =
                Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())),
                ),
        )
        drawCircle(color = color, radius = 4.dp.toPx(), center = chartPoints.first())
        drawCircle(color = color, radius = 4.dp.toPx(), center = chartPoints.last())
    }
}

@Composable
private fun BarMiniChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = 8.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(4.dp.toPx())
        values.forEachIndexed { index, raw ->
            val height = size.height * raw.coerceIn(0.12f, 1f)
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, height),
                cornerRadius =
                    androidx.compose.ui.geometry
                        .CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}

@Composable
private fun WaveMiniChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val chartPoints = normalizedOffsets(values, size.width, size.height)
        if (chartPoints.size < 2) return@Canvas
        val fillPath =
            smoothPath(chartPoints).apply {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(fillPath, ReferenceDesignTokens.YellowDark.copy(alpha = 0.18f))
        drawPath(
            path = smoothPath(chartPoints),
            color = ReferenceDesignTokens.YellowDark,
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun IconPill(
    icon: ImageVector,
    tint: Color,
    background: Color,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(background.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    alignStart: Boolean,
) {
    Column(horizontalAlignment = if (alignStart) Alignment.Start else Alignment.End) {
        Text(label, color = Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            color = if (alignStart) ReferenceDesignTokens.Blue else ReferenceDesignTokens.Yellow,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun ProgressLine(progress: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
        drawLine(
            color = ReferenceDesignTokens.DarkDivider,
            start = Offset.Zero.copy(y = size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ReferenceDesignTokens.Blue,
            start = Offset.Zero.copy(y = size.height / 2f),
            end = Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ReferenceThumbnail(
    backgroundColor: Color,
    accentColor: Color,
) {
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(42.dp)) {
            val centerX = size.width / 2f
            drawCircle(color = Color(0xFFFFC7B2), radius = 5.dp.toPx(), center = Offset(centerX, 7.dp.toPx()))
            drawLine(
                color = accentColor,
                start = Offset(centerX, 14.dp.toPx()),
                end = Offset(centerX - 8.dp.toPx(), 28.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = accentColor,
                start = Offset(centerX, 14.dp.toPx()),
                end = Offset(centerX + 9.dp.toPx(), 26.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ReferenceDesignTokens.Dark,
                start = Offset(centerX - 4.dp.toPx(), 29.dp.toPx()),
                end = Offset(centerX - 11.dp.toPx(), 39.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ReferenceDesignTokens.Dark,
                start = Offset(centerX + 5.dp.toPx(), 27.dp.toPx()),
                end = Offset(centerX + 14.dp.toPx(), 38.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ReferenceAvatar(index: Int) {
    val colors =
        listOf(
            Brush.linearGradient(listOf(ReferenceDesignTokens.Blue, Color(0xFF7DB6FF))),
            Brush.linearGradient(listOf(ReferenceDesignTokens.Yellow, Color(0xFFFFDF85))),
            Brush.linearGradient(listOf(Color(0xFFFF5F7F), Color(0xFFFF9FAF))),
            Brush.linearGradient(listOf(Color(0xFF5D6BFF), Color(0xFFB4A6FF))),
            Brush.linearGradient(listOf(Color.White, ReferenceDesignTokens.PaleBlueTrack)),
        )
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors[index % colors.size]),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (index == 0) Icons.Filled.Person else Icons.Filled.FitnessCenter,
            contentDescription = null,
            tint = if (index == 4) ReferenceDesignTokens.Blue else ReferenceDesignTokens.Dark,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SignalBars(color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(6.dp, 8.dp, 10.dp, 12.dp).forEach {
            Box(
                modifier =
                    Modifier
                        .width(2.dp)
                        .height(it)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
            )
        }
    }
}

@Composable
private fun WifiGlyph(color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val center = Offset(size.width / 2f, size.height * 0.72f)
        repeat(2) { index ->
            drawArc(
                color = color,
                startAngle = 220f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - (5 + index * 4).dp.toPx(), center.y - (5 + index * 4).dp.toPx()),
                size = Size((10 + index * 8).dp.toPx(), (10 + index * 8).dp.toPx()),
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        drawCircle(color = color, radius = 1.6.dp.toPx(), center = center)
    }
}

@Composable
private fun BatteryGlyph(color: Color) {
    Canvas(modifier = Modifier.size(width = 24.dp, height = 12.dp)) {
        drawRoundRect(
            color = color.copy(alpha = 0.55f),
            topLeft = Offset(1.dp.toPx(), 2.dp.toPx()),
            size = Size(18.dp.toPx(), 8.dp.toPx()),
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(1.2.dp.toPx()),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
            size = Size(14.dp.toPx(), 4.dp.toPx()),
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(20.dp.toPx(), 5.dp.toPx()),
            size = Size(2.dp.toPx(), 3.dp.toPx()),
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
    }
}

private fun defaultNavSlots(): List<NavItemSlot> =
    listOf(
        NavItemSlot(Icons.Filled.Home, "NavItemA"),
        NavItemSlot(Icons.Filled.Person, "NavItemB"),
        NavItemSlot(Icons.Filled.InsertChart, "NavItemC"),
        NavItemSlot(Icons.Filled.Settings, "NavItemD"),
    )

private fun DrawScope.drawContourPattern() {
    val stroke = Stroke(width = 1.dp.toPx())
    val alpha = 0.30f

    repeat(12) { index ->
        val y = size.height * (0.02f + index * 0.085f)
        val path =
            Path().apply {
                moveTo(-size.width * 0.18f, y)
                cubicTo(
                    size.width * 0.18f,
                    y - size.height * 0.08f,
                    size.width * 0.40f,
                    y + size.height * 0.10f,
                    size.width * 0.70f,
                    y,
                )
                cubicTo(
                    size.width * 0.98f,
                    y - size.height * 0.10f,
                    size.width * 1.12f,
                    y + size.height * 0.08f,
                    size.width * 1.22f,
                    y,
                )
            }
        drawPath(path, ReferenceDesignTokens.Contour.copy(alpha = alpha), style = stroke)
    }

    repeat(7) { ring ->
        val radiusX = size.width * (0.14f + ring * 0.06f)
        val radiusY = size.height * (0.05f + ring * 0.035f)
        drawOval(
            color = ReferenceDesignTokens.Contour.copy(alpha = 0.20f),
            topLeft = Offset(size.width * 0.74f - radiusX, size.height * 0.10f - radiusY),
            size = Size(radiusX * 2f, radiusY * 2f),
            style = stroke,
        )
    }
}

private fun normalizedOffsets(
    values: List<Float>,
    width: Float,
    height: Float,
): List<Offset> {
    if (values.isEmpty()) return emptyList()
    if (values.size == 1) return listOf(Offset(width / 2f, height * (1f - values.first().coerceIn(0f, 1f))))
    val step = width / (values.size - 1)
    return values.mapIndexed { index, value ->
        Offset(x = step * index, y = height * (1f - value.coerceIn(0f, 1f)))
    }
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        val controlX = (previous.x + current.x) / 2f
        path.cubicTo(controlX, previous.y, controlX, current.y, current.x, current.y)
    }
    return path
}

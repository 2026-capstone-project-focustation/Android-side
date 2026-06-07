package net.focustation.myapplication.ui.screen.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.FocusScreenBackground
import net.focustation.myapplication.ui.components.MainBottomDestination
import net.focustation.myapplication.ui.components.MainBottomNavigationBar
import net.focustation.myapplication.ui.components.ProgressGaugeCard
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.components.StatCard
import net.focustation.myapplication.ui.theme.FocusBlue
import net.focustation.myapplication.ui.theme.FocusCanvas
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocusLine
import net.focustation.myapplication.ui.theme.FocusMuted
import net.focustation.myapplication.ui.theme.FocusSurface
import net.focustation.myapplication.ui.theme.FocusYellow
import net.focustation.myapplication.ui.theme.FocustationTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionReportScreen(
    onHistoryItemClick: (String) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToSpaceHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: SessionReportViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteItem by remember { mutableStateOf<StudyHistoryUiItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }

    LaunchedEffect(uiState.deleteFeedbackMessage) {
        val message = uiState.deleteFeedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeDeleteFeedbackMessage()
    }

    val sortedHistory =
        remember(uiState.history, uiState.sortOption) { uiState.history.sortedBy(uiState.sortOption) }
    val mlScores = remember(uiState.history) { uiState.history.mapNotNull { it.displayMlScore() } }
    val averageScore =
        if (mlScores.isNotEmpty()) {
            mlScores
                .average()
                .toFloat()
        } else {
            0f
        }
    val totalMinutes = uiState.history.sumOf { it.durationMinutes }

    // 장소순에서 같은 장소를 하나로 묶어 그룹 카드로 보여주기 위한 상태/목록
    var selectedPlace by remember { mutableStateOf<String?>(null) }
    val placeGroups =
        remember(uiState.history) {
            uiState.history
                .groupBy { it.placeName.ifBlank { "장소 미지정" } }
                .map { (place, items) -> place to items.sortedByDescending { it.endedAtEpochMillis } }
                .sortedBy { it.first }
        }
    val placeVisits =
        remember(uiState.history, selectedPlace) {
            val place = selectedPlace ?: return@remember emptyList<StudyHistoryUiItem>()
            uiState.history
                .filter { it.placeName.ifBlank { "장소 미지정" } == place }
                .sortedByDescending { it.endedAtEpochMillis }
        }

    // 정렬을 바꾸면 드릴다운 해제, 선택한 장소의 기록이 모두 사라지면 목록으로 복귀
    LaunchedEffect(uiState.sortOption) { selectedPlace = null }
    LaunchedEffect(placeVisits, selectedPlace) {
        if (selectedPlace != null && placeVisits.isEmpty()) selectedPlace = null
    }
    BackHandler(enabled = selectedPlace != null) { selectedPlace = null }

    Scaffold(
        containerColor = FocusCanvas,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
    ) { paddingValues ->
        FocusScreenBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 44.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ReportArchiveHero()
                }

                if (selectedPlace == null) {
                    item {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard(
                                value = uiState.history.size.toString(),
                                label = "저장 세션",
                                icon = Icons.Outlined.FolderOpen,
                                iconColor = FocusBlue,
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                value = totalMinutes.formatTotalTime(),
                                label = "누적 시간",
                                icon = Icons.Outlined.AccessTime,
                                iconColor = FocusYellow,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ProgressGaugeCard(
                                title = "환경적합도 점수",
                                subtitle =
                                    if (mlScores.isEmpty()) {
                                        "ML 환경적합도 점수가 생성되면 표시돼요."
                                    } else {
                                        "ML 기준 ${mlScores.size}개 세션 평균은 ${averageScore.toInt()}점이에요."
                                    },
                                percent = (averageScore / 100f).coerceIn(0f, 1f),
                            )
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ArchiveSectionHeader(
                                title = "세션 보관함",
                                onRefresh = viewModel::refreshHistory,
                            )
                            Spacer(Modifier.height(14.dp))
                            ReportSortChips(
                                selected = uiState.sortOption,
                                onSortChange = viewModel::setSortOption,
                            )
                        }
                    }

                    when {
                        uiState.isLoadingHistory -> {
                            item { ArchiveLoadingState() }
                        }

                        uiState.historyErrorMessage != null -> {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    ErrorState(
                                        message = uiState.historyErrorMessage ?: "기록을 불러오지 못했어요.",
                                        onRefresh = viewModel::refreshHistory,
                                    )
                                }
                            }
                        }

                        sortedHistory.isEmpty() -> {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    EmptyArchiveState()
                                }
                            }
                        }

                        uiState.sortOption == ReportSortOption.PLACE_NAME -> {
                            placeGroups.forEach { (place, visits) ->
                                if (visits.size == 1) {
                                    val only = visits.first()
                                    item(key = only.sessionId) {
                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            SessionReportCard(
                                                item = only,
                                                isDeleting = uiState.deletingSessionIds.contains(only.sessionId),
                                                onClick = { onHistoryItemClick(only.sessionId) },
                                                onDeleteClick = { pendingDeleteItem = only },
                                            )
                                        }
                                    }
                                } else {
                                    item(key = "group:$place") {
                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            PlaceGroupCard(
                                                placeName = place,
                                                visits = visits,
                                                onClick = { selectedPlace = place },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            items(sortedHistory, key = { it.sessionId }) { historyItem ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SessionReportCard(
                                        item = historyItem,
                                        isDeleting = uiState.deletingSessionIds.contains(historyItem.sessionId),
                                        onClick = { onHistoryItemClick(historyItem.sessionId) },
                                        onDeleteClick = { pendingDeleteItem = historyItem },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PlaceDetailHeader(
                                placeName = selectedPlace.orEmpty(),
                                visitCount = placeVisits.size,
                                onBack = { selectedPlace = null },
                            )
                        }
                    }
                    items(placeVisits, key = { it.sessionId }) { visit ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PlaceVisitCard(
                                item = visit,
                                isDeleting = uiState.deletingSessionIds.contains(visit.sessionId),
                                onClick = { onHistoryItemClick(visit.sessionId) },
                                onDeleteClick = { pendingDeleteItem = visit },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text("리포트 삭제") },
            text = { Text("이 세션 리포트를 삭제할까요? 삭제한 기록은 영구 삭제됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteItem = null
                        viewModel.hideHistoryItem(item.sessionId)
                    },
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun ReportArchiveHero() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(FocusInk)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 20.dp, end = 16.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "리포트 보관함",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "지금까지 나의 집중 여정을 한눈에 살펴봐요",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.62f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FocusYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Assessment,
                    contentDescription = null,
                    tint = FocusInk,
                )
            }
        }
    }
}

@Composable
private fun ArchiveSectionHeader(
    title: String,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FocusInk,
            fontWeight = FontWeight.Bold,
        )
        IconButton(
            onClick = onRefresh,
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ReferenceDesignTokens.PaleBlueTrack),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "세션 보관함 새로고침",
                tint = FocusInk,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ReportSortChips(
    selected: ReportSortOption,
    onSortChange: (ReportSortOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        ReportSortOption.entries.forEach { option ->
            val isSelected = selected == option
            FilterChip(
                selected = isSelected,
                onClick = { onSortChange(option) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                label = {
                    Text(
                        text = option.label(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = FocusSurface,
                        labelColor = FocusMuted,
                        selectedContainerColor = FocusInk,
                        selectedLabelColor = Color.White,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = FocusLine,
                        selectedBorderColor = FocusInk,
                    ),
            )
        }
    }
}

@Composable
private fun SessionReportCard(
    item: StudyHistoryUiItem,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !isDeleting, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = FocusSurface,
        border = BorderStroke(1.dp, FocusLine),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScoreBadge(score = item.displayScore())
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.placeName.ifBlank { "장소 미지정" },
                    color = FocusInk,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatDate(item.endedAtEpochMillis),
                    color = FocusMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    text = item.scoreSummaryLabel(),
                    color = FocusMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFECEC)),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "리포트 삭제",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ReferenceDesignTokens.PaleBlueTrack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "상세 보기",
                        tint = FocusInk,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceGroupCard(
    placeName: String,
    visits: List<StudyHistoryUiItem>,
    onClick: () -> Unit,
) {
    val scores = visits.mapNotNull { it.displayMlScore() }
    val avgScore = if (scores.isNotEmpty()) scores.average().toInt() else null
    val latestMillis = visits.maxOfOrNull { it.endedAtEpochMillis } ?: 0L
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = FocusSurface,
        border = BorderStroke(1.dp, FocusLine),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScoreBadge(score = avgScore)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = placeName.ifBlank { "장소 미지정" },
                    color = FocusInk,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "기록 ${visits.size}개",
                    color = FocusMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    text = "최근 ${formatDate(latestMillis)}",
                    color = FocusMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ReferenceDesignTokens.PaleBlueTrack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "방문 기록 보기",
                    tint = FocusInk,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaceVisitCard(
    item: StudyHistoryUiItem,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !isDeleting, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = FocusSurface,
        border = BorderStroke(1.dp, FocusLine),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScoreBadge(score = item.displayScore())
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatDate(item.endedAtEpochMillis),
                    color = FocusInk,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.scoreSummaryLabel(),
                    color = FocusMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFECEC)),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "리포트 삭제",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ReferenceDesignTokens.PaleBlueTrack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "상세 보기",
                        tint = FocusInk,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceDetailHeader(
    placeName: String,
    visitCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ReferenceDesignTokens.PaleBlueTrack)
                    .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "장소 목록으로",
                tint = FocusInk,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = placeName.ifBlank { "장소 미지정" },
                color = FocusInk,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "방문 기록 ${visitCount}개",
                color = FocusMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ScoreBadge(score: Int?) {
    Box(
        modifier =
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scoreTint(score)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = score?.toString() ?: "--",
            color = FocusInk,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ArchiveLoadingState() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = FocusBlue)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = FocusSurface,
        border = BorderStroke(1.dp, FocusLine),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Surface(
                modifier = Modifier.clickable(onClick = onRefresh),
                shape = CircleShape,
                color = ReferenceDesignTokens.PaleBlueTrack,
            ) {
                Text(
                    text = "다시 불러오기",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = FocusBlue,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun EmptyArchiveState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = FocusSurface,
        border = BorderStroke(1.dp, FocusLine),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "저장된 리포트가 아직 없어요",
                color = FocusInk,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "환경 측정과 집중 세션을 완료하면 이곳에 기록이 쌓여요.",
                color = FocusMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun Int.formatTotalTime(): String {
    val safeMinutes = coerceAtLeast(0)
    return "${safeMinutes / 60}h ${safeMinutes % 60}m"
}

private fun List<StudyHistoryUiItem>.sortedBy(option: ReportSortOption): List<StudyHistoryUiItem> =
    when (option) {
        ReportSortOption.LATEST -> sortedByDescending { it.endedAtEpochMillis }
        ReportSortOption.SCORE_HIGH ->
            sortedWith(
                compareByDescending<StudyHistoryUiItem> { it.displayMlScore() != null }
                    .thenByDescending { it.displayMlScore() ?: 0 },
            )
        ReportSortOption.DURATION_LONG -> sortedByDescending { it.durationMinutes }
        ReportSortOption.PLACE_NAME -> sortedBy { it.placeName }
    }

// mlScore는 ML 새 계약 전까지 더미값 -1로 저장된다. 0 미만이면 아직 점수가 없는 것으로 본다.
private val StudyHistoryUiItem.validMlScore: Double?
    get() = mlScore?.takeIf { it >= 0.0 }

private fun StudyHistoryUiItem.displayScore(): Int? = displayMlScore()

private fun StudyHistoryUiItem.displayMlScore(): Int? = validMlScore?.toInt()?.coerceIn(0, 100)

private fun StudyHistoryUiItem.scoreSummaryLabel(): String = displayMlScore()?.let { "ML 적합도 ${it}점" } ?: "ML 점수 미생성"

private fun ReportSortOption.label(): String =
    when (this) {
        ReportSortOption.LATEST -> "최신순"
        ReportSortOption.SCORE_HIGH -> "점수순"
        ReportSortOption.DURATION_LONG -> "긴 시간순"
        ReportSortOption.PLACE_NAME -> "장소순"
    }

private fun scoreTint(score: Int?): Color =
    when {
        score == null -> Color(0xFFF0F2F5)
        score >= 85 -> ReferenceDesignTokens.PaleBlueTrack
        score >= 70 -> Color(0xFFFFF2D7)
        else -> Color(0xFFF1E8FF)
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
        SessionReportScreen(onHistoryItemClick = {})
    }
}

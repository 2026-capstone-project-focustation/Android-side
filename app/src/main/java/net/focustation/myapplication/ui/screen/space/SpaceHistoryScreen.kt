package net.focustation.myapplication.ui.screen.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.data.model.SpaceRecord
import net.focustation.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceHistoryScreen(
    onBack: () -> Unit,
    viewModel: SpaceHistoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val sortedRecords =
        remember(uiState.spaceRecords, uiState.sortOption) {
            when (uiState.sortOption) {
                SpaceSortOption.SCORE -> uiState.spaceRecords.sortedByDescending { it.avgFocusScore }
                SpaceSortOption.PLACE -> uiState.spaceRecords.sortedBy { it.name }
                SpaceSortOption.DATE -> uiState.spaceRecords
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("공간 기반 이력", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleView() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "뷰 전환",
                            tint = Color.White,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1B4B),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
        ) {
            // 지도/리스트 토글 탭
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.isMapView,
                    onClick = { if (!uiState.isMapView) viewModel.toggleView() },
                    label = { Text("🗺️  지도 뷰") },
                )
                FilterChip(
                    selected = !uiState.isMapView,
                    onClick = { if (uiState.isMapView) viewModel.toggleView() },
                    label = { Text("📋  리스트 뷰") },
                )
            }

            if (uiState.isMapView) {
                // 지도 뷰
                MapViewPlaceholder(
                    records = sortedRecords,
                    selectedId = uiState.selectedSpaceId,
                    onPinClick = { viewModel.selectSpace(it) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )

                // 선택된 장소 팝업 카드
                uiState.selectedSpaceId?.let { id ->
                    val record = uiState.spaceRecords.find { it.id == id }
                    record?.let {
                        SpaceDetailPopup(
                            record = it,
                            onDismiss = { viewModel.selectSpace(null) },
                        )
                    }
                }
            } else {
                // 정렬 옵션
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "정렬:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SpaceSortOption.entries.forEach { opt ->
                        FilterChip(
                            selected = uiState.sortOption == opt,
                            onClick = { viewModel.setSortOption(opt) },
                            label = {
                                Text(
                                    when (opt) {
                                        SpaceSortOption.DATE -> "날짜"
                                        SpaceSortOption.PLACE -> "장소"
                                        SpaceSortOption.SCORE -> "점수"
                                    },
                                    fontSize = 12.sp,
                                )
                            },
                        )
                    }
                }

                // 리스트
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sortedRecords) { record ->
                        SpaceListCard(
                            record = record,
                            onClick = { viewModel.selectSpace(record.id) },
                        )
                    }
                }
            }
        }
    }
}

// 지도 영역 자리표시자 (실제 Maps 연동 전)
@Composable
private fun MapViewPlaceholder(
    records: List<SpaceRecord>,
    selectedId: String?,
    onPinClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color(0xFFD8E8D0)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗺️", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "지도 영역",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4A6741),
            )
            Text(
                text = "${records.size}개의 장소 핀",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A6741).copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(20.dp))
            // 핀 버튼들
            records.take(4).forEach { record ->
                Box(
                    modifier =
                        Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (record.id == selectedId) Primary40 else Color.White.copy(alpha = 0.9f),
                            ).clickable { onPinClick(record.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "📍 ${record.name}  ${record.avgFocusScore}점",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (record.id == selectedId) Color.White else Color(0xFF333333),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceDetailPopup(
    record: SpaceRecord,
    onDismiss: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(ColorFocus.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${record.avgFocusScore}점",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorFocus,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricText("소음", "%.0f dB".format(record.avgNoise), ColorNoise)
                MetricText("조도", "%.0f lux".format(record.avgIlluminance), ColorLight)
                MetricText("온도", "%.0f°C".format(record.avgTemperature), ColorTemp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "세션 ${record.sessionCount}회 · 마지막 방문: ${record.lastVisited}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricText(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpaceListCard(
    record: SpaceRecord,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 점수 원형 배지
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ColorFocus.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${record.avgFocusScore}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ColorFocus,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "세션 ${record.sessionCount}회 · 마지막 방문: ${record.lastVisited}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTag("소음 %.0fdB".format(record.avgNoise), ColorNoise)
                    SmallTag("%.0flux".format(record.avgIlluminance), ColorLight)
                    SmallTag("%.0f°C".format(record.avgTemperature), ColorTemp)
                }
            }
        }
    }
}

@Composable
private fun SmallTag(
    text: String,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Preview(showBackground = true)
@Composable
private fun SpaceHistoryPreview() {
    FocustationTheme {
        SpaceHistoryScreen(onBack = {})
    }
}

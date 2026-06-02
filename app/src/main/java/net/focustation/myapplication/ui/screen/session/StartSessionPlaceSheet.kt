package net.focustation.myapplication.ui.screen.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.focustation.myapplication.R
import net.focustation.myapplication.data.repository.FirestoreStudyRepository
import net.focustation.myapplication.data.repository.SavedPlaceRecord
import net.focustation.myapplication.data.repository.StudySessionRecord
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.FocustationTheme
import java.time.Instant
import java.time.ZoneId

data class StartSessionPlaceUiState(
    val isLoading: Boolean = true,
    val recentPlaces: List<SavedPlaceRecord> = emptyList(),
    val loadFailed: Boolean = false,
)

class StartSessionPlaceViewModel(
    private val repository: FirestoreStudyRepository = FirestoreStudyRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartSessionPlaceUiState())
    val uiState: StateFlow<StartSessionPlaceUiState> = _uiState.asStateFlow()

    init {
        loadRecentPlaces()
    }

    private fun loadRecentPlaces() {
        viewModelScope.launch {
            val result = repository.getStudySessions()
            _uiState.value =
                StartSessionPlaceUiState(
                    isLoading = false,
                    recentPlaces = result.getOrNull()?.toRecentPlaces().orEmpty(),
                    loadFailed = result.isFailure,
                )
        }
    }
}

// 리포트 보관함(세션 기록)에서 최근 방문 공간을 중복 없이 추려 카드용으로 만든다.
// 별도 savedPlaces 컬렉션 대신 세션의 placeSnapshot을 재활용한다(읽기 권한도 세션과 동일).
private fun List<StudySessionRecord>.toRecentPlaces(): List<SavedPlaceRecord> {
    val latestPerPlace =
        filter { it.latitude != null && it.longitude != null }
            .filter { it.placeName.isNotBlank() && it.placeName != "장소 미지정" }
            .groupBy { it.placeName }
            .map { (_, group) -> group.maxByOrNull { it.endedAtEpochMillis } ?: group.first() }
    return latestPerPlace
        .sortedByDescending { it.endedAtEpochMillis }
        .take(3)
        .map { session ->
            SavedPlaceRecord(
                id = session.placeName,
                name = session.placeName,
                latitude = session.latitude,
                longitude = session.longitude,
                lastUsedMillis = session.endedAtEpochMillis.takeIf { it > 0L },
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionPlaceSheet(
    onSelectRecent: (SavedPlaceRecord) -> Unit,
    onPickManually: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: StartSessionPlaceViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ReferenceDesignTokens.WhiteCard,
    ) {
        StartSessionPlaceSheetContent(
            uiState = uiState,
            onSelectRecent = onSelectRecent,
            onPickManually = onPickManually,
            onSkip = onSkip,
            modifier = Modifier.fillMaxHeight(0.72f),
        )
    }
}

@Composable
private fun StartSessionPlaceSheetContent(
    uiState: StartSessionPlaceUiState,
    onSelectRecent: (SavedPlaceRecord) -> Unit,
    onPickManually: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var selectedId by remember { mutableStateOf<String?>(null) }

        PokeyPlaceHero()

        Text(
            text = "오늘 집중할 공간은 어디인가요?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = ReferenceDesignTokens.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "최근 방문한 공간에서 바로 시작하거나,\n오늘의 새로운 집중 장소를 직접 찾아보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = ReferenceDesignTokens.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                uiState.isLoading ->
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }

                uiState.loadFailed ->
                    LoadFailedHint()

                uiState.recentPlaces.isEmpty() ->
                    EmptyRecentHint()

                else ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        uiState.recentPlaces.forEach { place ->
                            RecentPlaceCard(
                                place = place,
                                selected = place.id == selectedId,
                                onClick = { selectedId = place.id },
                            )
                        }
                    }
            }
        }

        Spacer(Modifier.height(12.dp))

        val selectedPlace = uiState.recentPlaces.firstOrNull { it.id == selectedId }
        if (selectedPlace != null) {
            Button(
                onClick = { onSelectRecent(selectedPlace) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = ReferenceDesignTokens.Yellow,
                        contentColor = ReferenceDesignTokens.TextPrimary,
                    ),
            ) {
                Text(
                    text = "이 공간에서 시작",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = onPickManually,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ReferenceDesignTokens.TextPrimary),
        ) {
            Text(
                text = "직접 선택할래요",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "위치 없이 시작",
                style = MaterialTheme.typography.labelMedium,
                color = ReferenceDesignTokens.TextSecondary,
            )
        }
    }
}

@Composable
private fun PokeyPlaceHero() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.poki_xeyes_float_lottie))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    Box(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(140.dp),
        )
    }
}

@Composable
private fun RecentPlaceCard(
    place: SavedPlaceRecord,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) ReferenceDesignTokens.Yellow.copy(alpha = 0.12f) else Color(0xFFF7F8FA),
        border =
            BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ReferenceDesignTokens.Yellow else Color(0xFFE9EBEF),
            ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ReferenceDesignTokens.Yellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = ReferenceDesignTokens.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = ReferenceDesignTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                place.lastStudiedLabel()?.let { label ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = ReferenceDesignTokens.TextSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// 최근 공간 카드의 보조 문구: "(N월 N일)에 공부했어요". 날짜가 없으면 표시하지 않는다.
private fun SavedPlaceRecord.lastStudiedLabel(): String? {
    val millis = lastUsedMillis ?: return null
    val date =
        Instant
            .ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    return "${date.monthValue}월 ${date.dayOfMonth}일에 공부했어요"
}

@Composable
private fun EmptyRecentHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ReferenceDesignTokens.PaleBlueTrack,
    ) {
        Text(
            text = "아직 저장된 공간이 없어요.\n‘직접 선택할래요’로 오늘의 공간을 찾아보세요.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = ReferenceDesignTokens.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadFailedHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ReferenceDesignTokens.PaleBlueTrack,
    ) {
        Text(
            text = "최근 공간을 불러오지 못했어요.\n‘직접 선택할래요’로 오늘의 공간을 찾아보세요.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = ReferenceDesignTokens.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartSessionPlaceSheetPreview() {
    FocustationTheme {
        Surface(color = ReferenceDesignTokens.WhiteCard) {
            StartSessionPlaceSheetContent(
                uiState =
                    StartSessionPlaceUiState(
                        isLoading = false,
                        recentPlaces =
                            listOf(
                                SavedPlaceRecord(id = "library", name = "중앙 도서관"),
                                SavedPlaceRecord(id = "cafe", name = "캠퍼스 카페"),
                                SavedPlaceRecord(id = "home", name = "집 책상"),
                            ),
                    ),
                onSelectRecent = {},
                onPickManually = {},
                onSkip = {},
                modifier = Modifier.height(620.dp),
            )
        }
    }
}

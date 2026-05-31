package net.focustation.myapplication.ui.screen.session

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import net.focustation.myapplication.ui.components.ReferenceDesignTokens

data class StartSessionPlaceUiState(
    val isLoading: Boolean = true,
    val recentPlaces: List<SavedPlaceRecord> = emptyList(),
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
            val places = repository.getSavedPlaces(RECENT_LIMIT).getOrNull().orEmpty()
            _uiState.value = StartSessionPlaceUiState(isLoading = false, recentPlaces = places)
        }
    }

    private companion object {
        private const val RECENT_LIMIT = 3L
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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

                    uiState.recentPlaces.isEmpty() ->
                        EmptyRecentHint()

                    else ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.recentPlaces.forEach { place ->
                                RecentPlaceCard(place = place, onClick = { onSelectRecent(place) })
                            }
                        }
                }
            }

            Spacer(Modifier.height(12.dp))

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
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = ReferenceDesignTokens.PaleBlueTrack,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
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
            Text(
                text = place.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = ReferenceDesignTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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

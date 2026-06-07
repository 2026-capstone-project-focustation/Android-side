package net.focustation.myapplication.ui.screen.survey

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.focustation.myapplication.survey.SurveyPreferences
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.FocustationTheme
import kotlin.math.absoluteValue

private data class SurveyPage(
    val section: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val content: @Composable (SurveyUiState, () -> Unit) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SurveyScreen(
    onComplete: () -> Unit,
    viewModel: SurveyViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pages = rememberSurveyPages(viewModel)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == pages.lastIndex

    fun goNext() {
        if (uiState.isSaving) return

        if (pagerState.currentPage == pages.lastIndex) {
            viewModel.submit()
        } else {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    fun goPrevious() {
        if (uiState.isSaving) return

        if (pagerState.currentPage > 0) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    BackHandler(enabled = currentPage > 0 && !uiState.isSaving) {
        goPrevious()
    }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            SurveyPreferences.setCompleted(context, true)
            onComplete()
        }
    }

    Scaffold(
        containerColor = ReferenceDesignTokens.Screen,
        bottomBar = {
            SurveyBottomBar(
                canGoBack = currentPage > 0,
                isLastPage = isLastPage,
                isSaving = uiState.isSaving,
                onPrevious = ::goPrevious,
                onNext = ::goNext,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ReferenceDesignTokens.Screen)
                    .padding(paddingValues),
        ) {
            SurveyHeader(
                currentPage = currentPage,
                totalPages = pages.size,
            )

            SurveyErrorMessage(errorMessage = uiState.errorMessage)

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !uiState.isSaving,
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                val page = pages[pageIndex]
                val pageOffset =
                    ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                        .absoluteValue
                        .coerceIn(0f, 1f)

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 1f - pageOffset * 0.18f
                                val scale = 0.97f + (1f - pageOffset) * 0.03f
                                scaleX = scale
                                scaleY = scale
                            },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    QuestionCard(page = page) {
                        page.content(uiState, ::goNext)
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyHeader(
    currentPage: Int,
    totalPages: Int,
) {
    val progress = (currentPage + 1).toFloat() / totalPages.toFloat()

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)),
        color = ReferenceDesignTokens.Dark,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "맞춤 집중 설문",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                    )
                    Text(
                        text = "답변은 추천과 리포트 기준을 만드는 데 사용돼요",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.64f),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ReferenceDesignTokens.Yellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${currentPage + 1}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = ReferenceDesignTokens.TextPrimary,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                    color = ReferenceDesignTokens.Blue,
                    trackColor = ReferenceDesignTokens.DarkDivider,
                )
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.58f),
                )
            }
        }
    }
}

@Composable
private fun SurveyErrorMessage(errorMessage: String?) {
    if (errorMessage.isNullOrBlank()) return

    Surface(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = errorMessage,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun rememberSurveyPages(viewModel: SurveyViewModel): List<SurveyPage> =
    remember(viewModel) {
        buildList {
            addSingleChoicePage(
                section = "기본 성향",
                title = "어떤 집중 타입에 가까운가요?",
                subtitle = "가장 비슷한 성향을 골라주세요.",
                icon = Icons.Outlined.Person,
                options = userTypeOptions,
                selected = { it.userType },
                onSelected = viewModel::setUserType,
            )
            addSingleChoicePage(
                section = "기본 성향",
                title = "평소 가장 자주 이용하는 공간은 어디인가요?",
                subtitle = "일반적인 공부나 작업 기준으로 선택해주세요.",
                icon = Icons.Outlined.Insights,
                options = placeTypeOptions,
                selected = { it.generalPlaceType },
                onSelected = viewModel::setGeneralPlaceType,
            )
            addSingleChoicePage(
                section = "기본 성향",
                title = "주로 어떤 작업을 하나요?",
                subtitle = "최근 가장 많이 하는 활동을 기준으로 골라주세요.",
                icon = Icons.Outlined.Assessment,
                options = taskTypeOptions,
                selected = { it.generalTaskType },
                onSelected = viewModel::setGeneralTaskType,
            )
            addSingleChoicePage(
                section = "기본 성향",
                title = "보통 혼자 이용하나요?",
                subtitle = "공간을 쓰는 방식에 가장 가까운 답을 선택해주세요.",
                icon = Icons.Outlined.Person,
                options = socialModeOptions,
                selected = { it.generalSocialMode },
                onSelected = viewModel::setGeneralSocialMode,
            )
            addSingleChoicePage(
                section = "기본 성향",
                title = "한 번 머무는 시간은 어느 정도인가요?",
                subtitle = "평균적인 체류 시간을 알려주세요.",
                icon = Icons.Outlined.Insights,
                options = durationOptions,
                selected = { it.generalStayDuration },
                onSelected = viewModel::setGeneralStayDuration,
            )
            addSingleChoicePage(
                section = "기본 성향",
                title = "주로 이용하는 시간대는 언제인가요?",
                subtitle = "가장 자주 이용하는 시간대를 골라주세요.",
                icon = Icons.Outlined.Insights,
                options = timeSlotOptions,
                selected = { it.generalTimeSlot },
                onSelected = viewModel::setGeneralTimeSlot,
            )
            add(
                SurveyPage(
                    section = "기본 성향",
                    title = "집중을 방해하는 요소는 무엇인가요?",
                    subtitle = "여러 개를 선택할 수 있어요.",
                    icon = Icons.Outlined.Sensors,
                ) { state, _ ->
                    MultiChoiceQuestion(
                        options = distractionOptions,
                        selectedValues = state.generalDistractions,
                        onToggle = viewModel::toggleGeneralDistraction,
                    )
                },
            )
            add(
                SurveyPage(
                    section = "기본 성향",
                    title = "공간을 고를 때 중요한 기준은 무엇인가요?",
                    subtitle = "여러 개를 선택할 수 있어요.",
                    icon = Icons.Outlined.CheckCircle,
                ) { state, _ ->
                    MultiChoiceQuestion(
                        options = priorityOptions,
                        selectedValues = state.generalPriorities,
                        onToggle = viewModel::toggleGeneralPriority,
                    )
                },
            )

            preferenceQuestions.forEach { question ->
                addLikertPage(
                    section = "선호 환경",
                    title = question.displayTitle(),
                    subtitle = "나에게 얼마나 중요한지 선택해주세요.",
                    icon = Icons.Outlined.Insights,
                    score = { state -> state.preferences[question.key] ?: 3 },
                    onScoreChange = { score -> viewModel.setPreference(question.key, score) },
                )
            }
        }
    }

private fun MutableList<SurveyPage>.addSingleChoicePage(
    section: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    options: List<SurveyOption>,
    selected: (SurveyUiState) -> String,
    onSelected: (String) -> Unit,
) {
    add(
        SurveyPage(
            section = section,
            title = title,
            subtitle = subtitle,
            icon = icon,
        ) { state, goNext ->
            SingleChoiceQuestion(
                options = options,
                selectedValue = selected(state),
                onSelected = {
                    onSelected(it)
                    goNext()
                },
            )
        },
    )
}

private fun MutableList<SurveyPage>.addLikertPage(
    section: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    score: (SurveyUiState) -> Int,
    onScoreChange: (Int) -> Unit,
) {
    add(
        SurveyPage(
            section = section,
            title = title,
            subtitle = subtitle,
            icon = icon,
        ) { state, goNext ->
            LikertChoiceQuestion(
                score = score(state),
                lowLabel = LIKERT_LOW_LABEL,
                highLabel = LIKERT_HIGH_LABEL,
                onScoreChange = {
                    onScoreChange(it)
                    goNext()
                },
            )
        },
    )
}

@Composable
private fun QuestionCard(
    page: SurveyPage,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 16.dp)
                .shadow(
                    elevation = 0.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = Color(0xFF91A5BC).copy(alpha = 0.10f),
                    spotColor = Color(0xFF91A5BC).copy(alpha = 0.16f),
                ),
        shape = RoundedCornerShape(26.dp),
        color = ReferenceDesignTokens.WhiteCard,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ReferenceDesignTokens.PaleBlueTrack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = ReferenceDesignTokens.BlueDark,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = page.section,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ReferenceDesignTokens.BlueDark,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = ReferenceDesignTokens.TextPrimary,
                )
                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReferenceDesignTokens.TextSecondary,
                )
            }

            content()
        }
    }
}

@Composable
private fun SurveyBottomBar(
    canGoBack: Boolean,
    isLastPage: Boolean,
    isSaving: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        color = ReferenceDesignTokens.WhiteCard,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = canGoBack && !isSaving,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(54.dp),
                shape = CircleShape,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = ReferenceDesignTokens.TextSecondary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "이전",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                onClick = onNext,
                enabled = !isSaving,
                modifier =
                    Modifier
                        .weight(1.4f)
                        .height(54.dp),
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = ReferenceDesignTokens.Dark,
                        contentColor = Color.White,
                    ),
            ) {
                Text(
                    text =
                        when {
                            isSaving -> "저장 중"
                            isLastPage -> "완료"
                            else -> "다음"
                        },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
                if (!isLastPage && !isSaving) {
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

private fun LikertQuestion.displayTitle(): String =
    when (key) {
        "pref_quiet" -> "조용한 공간이 중요해요"
        "pref_light" -> "밝기가 충분한 공간을 선호해요"
        "pref_low_crowd" -> "사람이 많으면 집중하기 어려워요"
        "pref_privacy" -> "시선이 덜 노출되는 자리를 선호해요"
        "pref_outlet" -> "콘센트 사용 가능 여부가 중요해요"
        "pref_distance" -> "이동 거리와 시간에 민감해요"
        "pref_thermal_air" -> "온도와 공기 상태에 민감해요"
        "pref_control" -> "자리, 조명, 환기를 조절할 수 있으면 좋아요"
        "pref_comfort" -> "의자와 책상의 편안함이 중요해요"
        "pref_deepwork" -> "혼자 깊게 집중할 수 있는 공간을 선호해요"
        else -> title
    }

private const val LIKERT_LOW_LABEL = "낮음"

private const val LIKERT_HIGH_LABEL = "높음"

@Preview(showBackground = true)
@Composable
private fun SurveyScreenPreview() {
    FocustationTheme {
        SurveyScreen(onComplete = {})
    }
}

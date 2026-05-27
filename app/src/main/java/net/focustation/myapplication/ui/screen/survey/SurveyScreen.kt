package net.focustation.myapplication.ui.screen.survey

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
            SurveyPreferences.setLatestMlScore(context, uiState.mlScore)
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

            SurveyMessage(
                errorMessage = uiState.errorMessage,
                scoreWarningMessage = uiState.scoreWarningMessage,
                mlScore = uiState.mlScore,
            )

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
private fun SurveyMessage(
    errorMessage: String?,
    scoreWarningMessage: String?,
    mlScore: Double?,
) {
    val message =
        when {
            !errorMessage.isNullOrBlank() -> errorMessage
            !scoreWarningMessage.isNullOrBlank() -> scoreWarningMessage
            mlScore != null -> "ML 적합도 점수: ${mlScore.toInt()}점"
            else -> return
        }
    val isError = !errorMessage.isNullOrBlank()

    Surface(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color =
            if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                ReferenceDesignTokens.PaleBlueTrack
            },
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    ReferenceDesignTokens.TextPrimary
                },
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
                icon = Icons.Outlined.EditLocationAlt,
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
                    key = question.key,
                    title = question.displayTitle(),
                    subtitle = "나에게 얼마나 중요한지 선택해주세요.",
                    icon = Icons.Outlined.Insights,
                    score = { state -> state.preferences[question.key] ?: 3 },
                    onScoreChange = { score -> viewModel.setPreference(question.key, score) },
                )
            }

            addSingleChoicePage(
                section = "현재 장소",
                title = "지금 떠올린 장소는 어떤 유형인가요?",
                subtitle = "가장 가까운 장소 유형을 선택해주세요.",
                icon = Icons.Outlined.EditLocationAlt,
                options = placeTypeOptions,
                selected = { it.placeType },
                onSelected = viewModel::setPlaceType,
            )
            add(
                SurveyPage(
                    section = "현재 장소",
                    title = "장소 이름을 알려주세요",
                    subtitle = "비워두면 '장소 미지정'으로 저장돼요.",
                    icon = Icons.Outlined.EditLocationAlt,
                ) { state, _ ->
                    TextQuestion(
                        value = state.placeName,
                        onValueChange = viewModel::setPlaceName,
                    )
                },
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "그 장소에서 어떤 작업을 했나요?",
                subtitle = "방금 떠올린 장소 기준으로 답해주세요.",
                icon = Icons.Outlined.Assessment,
                options = taskTypeOptions,
                selected = { it.taskType },
                onSelected = viewModel::setTaskType,
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "그 장소를 몇 명이 함께 이용했나요?",
                subtitle = "당시 함께 있던 인원을 기준으로 선택해주세요.",
                icon = Icons.Outlined.Person,
                options = groupSizeOptions,
                selected = { it.groupSize },
                onSelected = viewModel::setGroupSize,
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "그 장소에 얼마나 머물렀나요?",
                subtitle = "해당 장소에서 체류한 시간을 골라주세요.",
                icon = Icons.Outlined.Insights,
                options = durationOptions,
                selected = { it.stayDuration },
                onSelected = viewModel::setStayDuration,
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "그 장소를 이용한 시간대는 언제인가요?",
                subtitle = "이용한 시간대를 선택해주세요.",
                icon = Icons.Outlined.Insights,
                options = timeSlotOptions,
                selected = { it.timeSlot },
                onSelected = viewModel::setTimeSlot,
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "이용한 날은 언제였나요?",
                subtitle = "평일과 주말 중 더 가까운 쪽을 골라주세요.",
                icon = Icons.Outlined.Assessment,
                options = dayTypeOptions,
                selected = { it.dayType },
                onSelected = viewModel::setDayType,
            )
            add(
                SurveyPage(
                    section = "현재 장소",
                    title = "이동 시간은 어느 정도였나요?",
                    subtitle = "선택한 값은 추천 기준에 함께 반영돼요.",
                    icon = Icons.Outlined.EditLocationAlt,
                ) { state, goNext ->
                    NumericChoiceQuestion(
                        options =
                            listOf(
                                SurveyOption("5", "5분"),
                                SurveyOption("10", "10분"),
                                SurveyOption("20", "20분"),
                                SurveyOption("30", "30분"),
                                SurveyOption("40", "40분 이상"),
                            ),
                        selectedValue = state.distanceMinutes.toInt().toString(),
                        onSelected = {
                            viewModel.setDistanceMinutes(it.toFloat())
                            goNext()
                        },
                    )
                },
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "그날 날씨는 어땠나요?",
                subtitle = "공간 만족도에 영향을 줄 수 있는 맥락이에요.",
                icon = Icons.Outlined.Sensors,
                options = weatherOptions,
                selected = { it.weather },
                onSelected = viewModel::setWeather,
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "실내였나요, 실외였나요?",
                subtitle = "장소 환경을 구분하기 위한 질문이에요.",
                icon = Icons.Outlined.EditLocationAlt,
                options = indoorOutdoorOptions,
                selected = { it.indoorOutdoor },
                onSelected = viewModel::setIndoorOutdoor,
            )
            addSingleChoicePage(
                section = "현재 장소",
                title = "그 장소를 얼마나 자주 방문하나요?",
                subtitle = "익숙한 장소인지 확인하기 위한 질문이에요.",
                icon = Icons.Outlined.CheckCircle,
                options = visitFrequencyOptions,
                selected = { it.visitFrequency },
                onSelected = viewModel::setVisitFrequency,
            )

            placeRatingQuestions.forEach { question ->
                addLikertPage(
                    section = "장소 평가",
                    key = question.key,
                    title = question.displayTitle(),
                    subtitle = "방금 떠올린 장소 기준으로 평가해주세요.",
                    icon = Icons.Outlined.Sensors,
                    score = { state -> state.placeRatings[question.key] ?: 3 },
                    onScoreChange = { score -> viewModel.setPlaceRating(question.key, score) },
                )
            }

            labelQuestions.forEach { question ->
                addLikertPage(
                    section = "최근 결과",
                    key = question.key,
                    title = question.displayTitle(),
                    subtitle = "그 장소에서의 실제 결과를 알려주세요.",
                    icon = Icons.Outlined.Assessment,
                    score = { state -> state.labels[question.key] ?: 3 },
                    onScoreChange = { score -> viewModel.setLabel(question.key, score) },
                )
            }

            add(
                SurveyPage(
                    section = "최근 결과",
                    title = "종합 만족도는 어느 정도였나요?",
                    subtitle = "마지막으로 전체 경험을 점수로 선택해주세요.",
                    icon = Icons.Outlined.CheckCircle,
                ) { state, goNext ->
                    NumericChoiceQuestion(
                        options =
                            listOf(
                                SurveyOption("20", "매우 낮음"),
                                SurveyOption("40", "낮음"),
                                SurveyOption("60", "보통"),
                                SurveyOption("80", "높음"),
                                SurveyOption("100", "매우 높음"),
                            ),
                        selectedValue = state.satisfactionScore.toString(),
                        onSelected = {
                            viewModel.setSatisfactionScore(it.toInt())
                            goNext()
                        },
                    )
                },
            )
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
    key: String,
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
                lowLabel = key.likertLowLabel(),
                highLabel = key.likertHighLabel(),
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
                    elevation = 14.dp,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceQuestion(
    options: List<SurveyOption>,
    selectedValue: String,
    onSelected: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            ChoiceButton(
                text = option.displayLabel(),
                selected = selectedValue == option.value,
                onClick = { onSelected(option.value) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceQuestion(
    options: List<SurveyOption>,
    selectedValues: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedValues.contains(option.value),
                onClick = { onToggle(option.value) },
                shape = RoundedCornerShape(16.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ReferenceDesignTokens.Dark,
                        selectedLabelColor = Color.White,
                        containerColor = ReferenceDesignTokens.PaleBlueTrack,
                        labelColor = ReferenceDesignTokens.TextPrimary,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedValues.contains(option.value),
                        borderColor = ReferenceDesignTokens.Border,
                        selectedBorderColor = ReferenceDesignTokens.Dark,
                    ),
                label = {
                    Text(
                        text = option.displayLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
    }
}

@Composable
private fun LikertChoiceQuestion(
    score: Int,
    lowLabel: String,
    highLabel: String,
    onScoreChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = lowLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ReferenceDesignTokens.TextSecondary,
            )
            Text(
                text = highLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ReferenceDesignTokens.TextSecondary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..5).forEach { value ->
                Surface(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(58.dp),
                    onClick = { onScoreChange(value) },
                    shape = RoundedCornerShape(18.dp),
                    color =
                        if (score == value) {
                            ReferenceDesignTokens.Dark
                        } else {
                            ReferenceDesignTokens.PaleBlueTrack
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color =
                                if (score == value) {
                                    Color.White
                                } else {
                                    ReferenceDesignTokens.TextPrimary
                                },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumericChoiceQuestion(
    options: List<SurveyOption>,
    selectedValue: String,
    onSelected: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            ChoiceButton(
                text = option.label,
                selected = selectedValue == option.value,
                onClick = { onSelected(option.value) },
            )
        }
    }
}

@Composable
private fun TextQuestion(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("장소 이름") },
        placeholder = { Text("예: 중앙도서관 3층") },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
            ),
    )
}

@Composable
private fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color =
            if (selected) {
                ReferenceDesignTokens.Dark
            } else {
                ReferenceDesignTokens.PaleBlueTrack
            },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            style = MaterialTheme.typography.labelLarge,
            color =
                if (selected) {
                    Color.White
                } else {
                    ReferenceDesignTokens.TextPrimary
                },
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
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
        shadowElevation = 10.dp,
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
                            isSaving -> "분석 중"
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

private fun SurveyOption.displayLabel(): String =
    when (value) {
        "balanced" -> "균형형"
        "quiet_deepwork" -> "조용한 몰입형"
        "comfort_oriented" -> "편안함 중시형"
        "convenience_oriented" -> "편의성 중시형"
        "social_collaborative" -> "협업 선호형"
        "library" -> "도서관"
        "cafe" -> "카페"
        "study_room" -> "스터디룸"
        "classroom" -> "강의실"
        "computer_lab" -> "컴퓨터실"
        "coworking_space" -> "코워킹 공간"
        "dorm_lounge" -> "기숙사 라운지"
        "home" -> "집"
        "outdoor" -> "실외/야외"
        "deep_study" -> "깊은 공부"
        "coding" -> "코딩"
        "report_writing" -> "보고서 작성"
        "light_reading" -> "가벼운 독서"
        "online_class" -> "온라인 수업"
        "team_project" -> "팀 프로젝트"
        "meeting" -> "회의"
        "mostly_solo" -> "대부분 혼자"
        "mixed" -> "혼자/함께 반반"
        "mostly_group" -> "대부분 함께"
        "under_1h" -> "1시간 미만"
        "1_2h" -> "1~2시간"
        "2_4h" -> "2~4시간"
        "over_4h" -> "4시간 이상"
        "early_morning" -> "이른 아침"
        "morning" -> "오전"
        "afternoon" -> "오후"
        "evening" -> "저녁"
        "late_night" -> "늦은 밤"
        "solo" -> "혼자"
        "2" -> "2명"
        "3_4" -> "3~4명"
        "5_plus" -> "5명 이상"
        "weekday" -> "평일"
        "weekend" -> "주말"
        "clear" -> "맑음"
        "cloudy" -> "흐림"
        "rainy" -> "비"
        "hot" -> "더움"
        "cold" -> "추움"
        "indoor" -> "실내"
        "first_time" -> "처음"
        "rarely" -> "가끔"
        "sometimes" -> "종종"
        "often" -> "자주"
        "noise" -> "소음"
        "crowd" -> "혼잡함"
        "visual" -> "시각적 방해"
        "temperature" -> "온도/공기"
        "outlet" -> "콘센트"
        "distance" -> "이동 거리"
        "quiet" -> "조용함"
        "comfort" -> "편안함"
        "privacy" -> "프라이버시"
        else -> label
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
        "place_quiet" -> "소음이 집중을 방해하지 않았어요"
        "place_light" -> "밝기가 충분했어요"
        "place_low_crowd" -> "사람이 많지 않았어요"
        "place_low_visual_distraction" -> "시선이나 동선 방해가 적었어요"
        "place_control" -> "자리, 조명, 환기를 조절하기 쉬웠어요"
        "place_comfort" -> "의자와 책상이 편안했어요"
        "place_outlet" -> "콘센트를 사용하기 편했어요"
        "place_task_fit" -> "작업에 맞는 분위기였어요"
        "place_temperature_air" -> "온도와 공기가 적절했어요"
        "place_seat_availability" -> "앉을 자리를 찾기 쉬웠어요"
        "focus_score" -> "실제로 집중이 잘 됐어요"
        "productivity_score" -> "작업 효율이 높았어요"
        "revisit_score" -> "다시 선택하고 싶어요"
        "recommend_score" -> "다른 사람에게 추천하고 싶어요"
        else -> title
    }

private fun String.likertLowLabel(): String =
    when {
        startsWith("focus_") || startsWith("productivity_") || startsWith("revisit_") || startsWith("recommend_") ->
            "전혀 아님"
        else -> "낮음"
    }

private fun String.likertHighLabel(): String =
    when {
        startsWith("focus_") || startsWith("productivity_") || startsWith("revisit_") || startsWith("recommend_") ->
            "매우 그럼"
        else -> "높음"
    }

@Preview(showBackground = true)
@Composable
private fun SurveyScreenPreview() {
    FocustationTheme {
        SurveyScreen(onComplete = {})
    }
}

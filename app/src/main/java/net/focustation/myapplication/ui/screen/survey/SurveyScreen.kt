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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.focustation.myapplication.survey.SurveyPreferences
import net.focustation.myapplication.ui.theme.FocustationTheme
import kotlin.math.absoluteValue

private data class SurveyPage(
    val section: String,
    val title: String,
    val subtitle: String,
    val content: @Composable (SurveyUiState, () -> Unit) -> Unit,
)

private val SurveyPrimary = Color(0xFF2F675F)
private val SurveyPrimaryDark = Color(0xFF244E47)
private val SurveySecondary = Color(0xFF4F6D8A)
private val SurveyBackground = Color(0xFFF3F8F6)
private val SurveyBackgroundAlt = Color(0xFFEAF1FA)
private val SurveySurface = Color(0xFFFFFFFF)
private val SurveySoft = Color(0xFFE6F2EE)
private val SurveyText = Color(0xFF1F2E2B)
private val SurveyMuted = Color(0xFF61736E)
private val SurveyAccent = Color(0xFFFFC857)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
        if (pagerState.currentPage > 0) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    BackHandler(enabled = currentPage > 0) {
        goPrevious()
    }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            SurveyPreferences.setCompleted(context, true)
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("초기 집중 설문", fontWeight = FontWeight.Bold) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = SurveyPrimaryDark,
                        titleContentColor = Color.White,
                    ),
            )
        },
        bottomBar = {
            SurveyBottomBar(
                canGoBack = currentPage > 0,
                isLastPage = isLastPage,
                isSaving = uiState.isSaving,
                onPrevious = ::goPrevious,
                onSkip = ::goNext,
                onNext = ::goNext,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(SurveyBackground),
        ) {
            SurveyProgressHeader(
                currentPage = currentPage,
                totalPages = pages.size,
            )

            SurveyErrorMessage(errorMessage = uiState.errorMessage)

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
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
                                alpha = 1f - pageOffset * 0.22f
                                val scale = 0.96f + (1f - pageOffset) * 0.04f
                                scaleX = scale
                                scaleY = scale
                            },
                    contentAlignment = Alignment.Center,
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
private fun SurveyErrorMessage(errorMessage: String?) {
    if (errorMessage.isNullOrBlank()) return

    Text(
        text = errorMessage,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun rememberSurveyPages(viewModel: SurveyViewModel): List<SurveyPage> =
    remember(viewModel) {
        buildList {
            add(
                SurveyPage(
                    section = "전반적 이용 패턴",
                    title = "나에게 가까운 학습 공간 성향은?",
                    subtitle = "선택하면 바로 다음 질문으로 넘어갑니다.",
                ) { state, goNext ->
                    SingleChoiceQuestion(
                        options = userTypeOptions,
                        selectedValue = state.userType,
                        onSelected = {
                            viewModel.setUserType(it)
                            goNext()
                        },
                    )
                },
            )
            addSingleChoicePage(
                section = "전반적 이용 패턴",
                title = "가장 자주 작업/공부한 장소는?",
                subtitle = "평소 기준으로 답해주세요.",
                options = placeTypeOptions,
                selected = { it.generalPlaceType },
                onSelected = viewModel::setGeneralPlaceType,
            )
            addSingleChoicePage(
                section = "전반적 이용 패턴",
                title = "가장 자주 한 작업은?",
                subtitle = "최근 한 달 기준으로 골라주세요.",
                options = taskTypeOptions,
                selected = { it.generalTaskType },
                onSelected = viewModel::setGeneralTaskType,
            )
            addSingleChoicePage(
                section = "전반적 이용 패턴",
                title = "보통 혼자 이용하나요?",
                subtitle = "가장 가까운 이용 형태를 선택해주세요.",
                options = socialModeOptions,
                selected = { it.generalSocialMode },
                onSelected = viewModel::setGeneralSocialMode,
            )
            addSingleChoicePage(
                section = "전반적 이용 패턴",
                title = "평균 체류 시간은?",
                subtitle = "한 번 머무를 때의 평균 시간입니다.",
                options = durationOptions,
                selected = { it.generalStayDuration },
                onSelected = viewModel::setGeneralStayDuration,
            )
            addSingleChoicePage(
                section = "전반적 이용 패턴",
                title = "주로 이용하는 시간대는?",
                subtitle = "가장 자주 이용하는 시간대를 고르세요.",
                options = timeSlotOptions,
                selected = { it.generalTimeSlot },
                onSelected = viewModel::setGeneralTimeSlot,
            )
            add(
                SurveyPage(
                    section = "전반적 이용 패턴",
                    title = "전반적으로 방해되는 요인은?",
                    subtitle = "여러 개를 고를 수 있어요. 선택 후 다음을 눌러주세요.",
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
                    section = "전반적 이용 패턴",
                    title = "장소를 고를 때 중요한 요소는?",
                    subtitle = "여러 개를 고를 수 있어요. 선택 후 다음을 눌러주세요.",
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
                    section = "사용자 선호",
                    question = question,
                    score = { state -> state.preferences[question.key] ?: 3 },
                    onScoreChange = { score -> viewModel.setPreference(question.key, score) },
                )
            }

            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "대표 장소 유형은?",
                subtitle = "모델이 장소 특성을 이해하는 데 쓰입니다.",
                options = placeTypeOptions,
                selected = { it.placeType },
                onSelected = viewModel::setPlaceType,
            )
            add(
                SurveyPage(
                    section = "대표 장소와 상황",
                    title = "장소 이름은?",
                    subtitle = "건너뛰면 '장소 미지정'으로 처리됩니다.",
                ) { state, _ ->
                    TextQuestion(
                        value = state.placeName,
                        onValueChange = viewModel::setPlaceName,
                    )
                },
            )
            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "그 장소에서 한 작업은?",
                subtitle = "방금 떠올린 장소 기준으로 답해주세요.",
                options = taskTypeOptions,
                selected = { it.taskType },
                onSelected = viewModel::setTaskType,
            )
            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "그 장소 이용 인원은?",
                subtitle = "함께 있었던 사람 수 기준입니다.",
                options = groupSizeOptions,
                selected = { it.groupSize },
                onSelected = viewModel::setGroupSize,
            )
            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "그 장소 체류 시간은?",
                subtitle = "대표 장소에서 머문 시간을 골라주세요.",
                options = durationOptions,
                selected = { it.stayDuration },
                onSelected = viewModel::setStayDuration,
            )
            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "그 장소 이용 시간대는?",
                subtitle = "대표 장소를 이용한 시간대입니다.",
                options = timeSlotOptions,
                selected = { it.timeSlot },
                onSelected = viewModel::setTimeSlot,
            )
            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "이용한 날은?",
                subtitle = "평일/주말 중 가까운 쪽을 골라주세요.",
                options = dayTypeOptions,
                selected = { it.dayType },
                onSelected = viewModel::setDayType,
            )
            add(
                SurveyPage(
                    section = "대표 장소와 상황",
                    title = "이동 시간은 어느 정도였나요?",
                    subtitle = "선택하면 숫자 feature인 distance_minutes로 저장됩니다.",
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
                section = "대표 장소와 상황",
                title = "그날 날씨는?",
                subtitle = "공간 만족도에 영향을 줄 수 있는 맥락입니다.",
                options = weatherOptions,
                selected = { it.weather },
                onSelected = viewModel::setWeather,
            )
            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "실내였나요, 실외였나요?",
                subtitle = "장소 환경 구분입니다.",
                options = indoorOutdoorOptions,
                selected = { it.indoorOutdoor },
                onSelected = viewModel::setIndoorOutdoor,
            )
            addSingleChoicePage(
                section = "대표 장소와 상황",
                title = "방문 빈도는?",
                subtitle = "해당 장소에 얼마나 익숙한지 알려주세요.",
                options = visitFrequencyOptions,
                selected = { it.visitFrequency },
                onSelected = viewModel::setVisitFrequency,
            )

            placeRatingQuestions.forEach { question ->
                addLikertPage(
                    section = "특정 장소 평가",
                    question = question,
                    score = { state -> state.placeRatings[question.key] ?: 3 },
                    onScoreChange = { score -> viewModel.setPlaceRating(question.key, score) },
                )
            }

            labelQuestions.forEach { question ->
                addLikertPage(
                    section = "최근 이용 결과",
                    question = question,
                    score = { state -> state.labels[question.key] ?: 3 },
                    onScoreChange = { score -> viewModel.setLabel(question.key, score) },
                )
            }

            add(
                SurveyPage(
                    section = "최근 이용 결과",
                    title = "종합 만족도는?",
                    subtitle = "모델 학습용 satisfaction_score 후보입니다.",
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
    options: List<SurveyOption>,
    selected: (SurveyUiState) -> String,
    onSelected: (String) -> Unit,
) {
    add(
        SurveyPage(
            section = section,
            title = title,
            subtitle = subtitle,
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
    question: LikertQuestion,
    score: (SurveyUiState) -> Int,
    onScoreChange: (Int) -> Unit,
) {
    add(
        SurveyPage(
            section = section,
            title = question.title,
            subtitle = "1은 ${question.lowLabel}, 5는 ${question.highLabel}입니다.",
        ) { state, goNext ->
            LikertChoiceQuestion(
                score = score(state),
                onScoreChange = {
                    onScoreChange(it)
                    goNext()
                },
            )
        },
    )
}

@Composable
private fun SurveyProgressHeader(
    currentPage: Int,
    totalPages: Int,
) {
    val progress = (currentPage + 1).toFloat() / totalPages.toFloat()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SurveyBackgroundAlt, SurveyBackground),
                    ),
                ).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = "${currentPage + 1} / $totalPages",
            style = MaterialTheme.typography.labelLarge,
            color = SurveyMuted,
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(7.dp),
            color = SurveyPrimary,
            trackColor = Color(0xFFD9E7E3),
        )
    }
}

@Composable
private fun QuestionCard(
    page: SurveyPage,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurveySurface),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = page.section,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = SurveySecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = SurveyText,
                )
                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SurveyMuted,
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
                text = option.label,
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
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SurveySoft,
                        selectedLabelColor = SurveyPrimaryDark,
                        containerColor = Color(0xFFF7FAF9),
                        labelColor = SurveyText,
                    ),
                label = { Text(option.label) },
            )
        }
    }
}

@Composable
private fun LikertChoiceQuestion(
    score: Int,
    onScoreChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..5).forEach { value ->
            OutlinedButton(
                onClick = { onScoreChange(value) },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor =
                            if (score == value) {
                                SurveySoft
                            } else {
                                Color.Transparent
                            },
                        contentColor =
                            if (score == value) {
                                SurveyPrimaryDark
                            } else {
                                SurveyText
                            },
                    ),
            ) {
                Text(
                    text = value.toString(),
                    fontWeight = FontWeight.Bold,
                )
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
        singleLine = true,
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
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (selected) {
                        SurveyPrimary
                    } else {
                        SurveySoft
                    },
                contentColor =
                    if (selected) {
                        Color.White
                    } else {
                        SurveyText
                    },
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SurveyBottomBar(
    canGoBack: Boolean,
    isLastPage: Boolean,
    isSaving: Boolean,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        color = SurveySurface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = canGoBack && !isSaving,
                modifier =
                    Modifier
                        .weight(0.9f)
                        .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("이전")
            }
            TextButton(
                onClick = onSkip,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (isLastPage) "건너뛰고 완료" else "건너뛰기",
                    color = SurveyMuted,
                )
            }
            Button(
                onClick = onNext,
                enabled = !isSaving,
                modifier =
                    Modifier
                        .weight(1.4f)
                        .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurveyPrimary),
            ) {
                Text(
                    when {
                        isSaving -> "저장 중..."
                        isLastPage -> "완료"
                        else -> "다음"
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SurveyScreenPreview() {
    FocustationTheme {
        SurveyScreen(onComplete = {})
    }
}

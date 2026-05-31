package net.focustation.myapplication.ui.screen.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.screen.survey.LikertChoiceQuestion
import net.focustation.myapplication.ui.screen.survey.SingleChoiceQuestion
import net.focustation.myapplication.ui.screen.survey.SurveyOption
import net.focustation.myapplication.ui.screen.survey.groupSizeOptions
import net.focustation.myapplication.ui.screen.survey.indoorOutdoorOptions
import net.focustation.myapplication.ui.screen.survey.placeRatingQuestions
import net.focustation.myapplication.ui.screen.survey.placeTypeOptions
import net.focustation.myapplication.ui.screen.survey.taskTypeOptions
import net.focustation.myapplication.ui.screen.survey.visitFrequencyOptions
import net.focustation.myapplication.ui.screen.survey.weatherOptions
import net.focustation.myapplication.ui.theme.FocustationTheme

private val distanceOptions =
    listOf(
        SurveyOption("5", "5분"),
        SurveyOption("10", "10분"),
        SurveyOption("20", "20분"),
        SurveyOption("30", "30분"),
        SurveyOption("40", "40분"),
        SurveyOption("60", "1시간 이상"),
    )

@Composable
fun FeedbackSessionScreen(
    onSubmit: () -> Unit,
    viewModel: FeedbackSessionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // 종료된 세션으로 되돌아가지 않도록 뒤로가기를 막고, 명시 버튼으로만 진행한다(기존 모달과 동일 정책).
    BackHandler(enabled = true) { }

    LaunchedEffect(uiState.submitted) {
        if (uiState.submitted) onSubmit()
    }

    Scaffold(containerColor = ReferenceDesignTokens.Screen) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ReferenceDesignTokens.Screen)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "세션 피드백",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = ReferenceDesignTokens.TextPrimary,
            )
            Text(
                text = "이번 세션의 장소와 상황을 알려주세요. 답변은 추천 정확도를 높이는 데 쓰여요.",
                style = MaterialTheme.typography.bodyMedium,
                color = ReferenceDesignTokens.TextSecondary,
            )

            SectionHeader("상황")
            QuestionBlock("어떤 장소였나요?") {
                SingleChoiceQuestion(placeTypeOptions, uiState.placeType, viewModel::setPlaceType)
            }
            QuestionBlock("무슨 작업을 했나요?") {
                SingleChoiceQuestion(taskTypeOptions, uiState.taskType, viewModel::setTaskType)
            }
            QuestionBlock("몇 명이 함께 이용했나요?") {
                SingleChoiceQuestion(groupSizeOptions, uiState.groupSize, viewModel::setGroupSize)
            }
            QuestionBlock("이동 시간은 어느 정도였나요?") {
                SingleChoiceQuestion(distanceOptions, uiState.distanceMinutes.toInt().toString()) {
                    viewModel.setDistanceMinutes(it.toFloat())
                }
            }
            QuestionBlock("실내였나요, 실외였나요?") {
                SingleChoiceQuestion(indoorOutdoorOptions, uiState.indoorOutdoor, viewModel::setIndoorOutdoor)
            }
            QuestionBlock("그날 날씨는 어땠나요?") {
                SingleChoiceQuestion(weatherOptions, uiState.weather, viewModel::setWeather)
            }
            QuestionBlock("이 장소를 얼마나 자주 방문하나요?") {
                SingleChoiceQuestion(visitFrequencyOptions, uiState.visitFrequency, viewModel::setVisitFrequency)
            }

            SectionHeader("장소 평가")
            placeRatingQuestions.forEach { question ->
                QuestionBlock(question.title) {
                    LikertChoiceQuestion(
                        score = uiState.placeRatings[question.key] ?: 3,
                        lowLabel = "그렇지 않다",
                        highLabel = "그렇다",
                        onScoreChange = { viewModel.setPlaceRating(question.key, it) },
                    )
                }
            }

            uiState.saveErrorMessage?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = { viewModel.submit() },
                enabled = !uiState.isSaving,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .navigationBarsPadding(),
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
                            uiState.isSaving -> "저장 중..."
                            uiState.saveErrorMessage != null -> "다시 시도"
                            else -> "리포트 저장하기"
                        },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            // 저장 실패(POST 미생성)는 시스템 오류 → 탈출구를 노출한다.
            if (uiState.saveErrorMessage != null) {
                TextButton(
                    onClick = onSubmit,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "저장하지 않고 나가기",
                        color = ReferenceDesignTokens.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = ReferenceDesignTokens.BlueDark,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun QuestionBlock(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ReferenceDesignTokens.WhiteCard,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = ReferenceDesignTokens.TextPrimary,
            )
            content()
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun FeedbackSessionPreview() {
    FocustationTheme {
        FeedbackSessionScreen(onSubmit = {})
    }
}

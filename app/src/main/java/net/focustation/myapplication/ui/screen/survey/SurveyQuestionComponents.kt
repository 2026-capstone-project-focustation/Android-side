package net.focustation.myapplication.ui.screen.survey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.FocustationTheme

// 설문·세션 피드백이 공유하는 질문 입력 컴포저블. 디자인 토큰·동작을 한곳에서 관리한다.

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleChoiceQuestion(
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
fun MultiChoiceQuestion(
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
fun LikertChoiceQuestion(
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

@Composable
fun ChoiceButton(
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

fun SurveyOption.displayLabel(): String =
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

@Preview(showBackground = true)
@Composable
private fun SurveyQuestionComponentsPreview() {
    FocustationTheme {
        Surface(color = ReferenceDesignTokens.Screen) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SingleChoiceQuestion(
                    options = placeTypeOptions.take(5),
                    selectedValue = "library",
                    onSelected = {},
                )
                MultiChoiceQuestion(
                    options = distractionOptions,
                    selectedValues = setOf("noise", "outlet"),
                    onToggle = {},
                )
                LikertChoiceQuestion(
                    score = 4,
                    lowLabel = "낮음",
                    highLabel = "높음",
                    onScoreChange = {},
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceButton(text = "선택됨", selected = true, onClick = {})
                    ChoiceButton(text = "기본", selected = false, onClick = {})
                }
            }
        }
    }
}

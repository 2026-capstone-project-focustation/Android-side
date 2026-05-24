package net.focustation.myapplication.ui.screen.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.StarRatingSelector
import net.focustation.myapplication.ui.theme.FocusInk
import net.focustation.myapplication.ui.theme.FocusLine
import net.focustation.myapplication.ui.theme.FocusSurface
import net.focustation.myapplication.ui.theme.FocustationTheme

@Composable
fun FeedbackSessionScreen(
    onSubmit: () -> Unit,
    viewModel: FeedbackSessionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submitted) {
        if (uiState.submitted) onSubmit()
    }

    // 모달 다이얼로그 — 명시 버튼으로만 닫히도록 고정 (바깥 탭/뒤로가기 차단)
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = FocusSurface,
                ),
            border = androidx.compose.foundation.BorderStroke(1.dp, FocusLine),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = null, tint = FocusInk, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "세션 피드백",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "이번 세션은 어떠셨나요?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))

                // Q1: 전반적 집중도
                FeedbackQuestion(
                    question = "전반적인 집중도는 어땠나요?",
                    rating = uiState.subjectiveScore,
                    onRatingChange = { viewModel.updateSubjectiveScore(it) },
                )

                Spacer(Modifier.height(20.dp))

                // Q2: 환경 만족도
                FeedbackQuestion(
                    question = "작업 환경에 만족하셨나요?",
                    rating = uiState.question1,
                    onRatingChange = { viewModel.updateQuestion1(it) },
                )

                Spacer(Modifier.height(20.dp))

                // Q3: 방해 요소
                FeedbackQuestion(
                    question = "방해 요소가 적었나요?",
                    rating = uiState.question2,
                    onRatingChange = { viewModel.updateQuestion2(it) },
                )

                Spacer(Modifier.height(20.dp))

                // Q4: 다시 방문 의향
                FeedbackQuestion(
                    question = "이 장소를 다시 이용하고 싶으신가요?",
                    rating = uiState.question3,
                    onRatingChange = { viewModel.updateQuestion3(it) },
                )

                Spacer(Modifier.height(28.dp))

                // 버튼
                Button(
                    onClick = { viewModel.submit() },
                    enabled = !uiState.isSaving,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FocusInk),
                ) {
                    Text(
                        text =
                            when {
                                uiState.isSaving -> "저장 중..."
                                uiState.saveErrorMessage != null -> "다시 시도"
                                else -> "리포트 저장하기"
                            },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // 저장 실패는 시스템 오류 → 실패 상황에 한해 탈출구를 노출한다.
                uiState.saveErrorMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onSubmit,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "저장하지 않고 나가기",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackQuestion(
    question: String,
    rating: Int,
    onRatingChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StarRatingSelector(
                rating = rating,
                onRatingChange = onRatingChange,
            )
            Text(
                text =
                    when (rating) {
                        1 -> "매우 불만족"
                        2 -> "불만족"
                        3 -> "보통"
                        4 -> "만족"
                        5 -> "매우 만족"
                        else -> ""
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackSessionPreview() {
    FocustationTheme {
        FeedbackSessionScreen(onSubmit = {})
    }
}

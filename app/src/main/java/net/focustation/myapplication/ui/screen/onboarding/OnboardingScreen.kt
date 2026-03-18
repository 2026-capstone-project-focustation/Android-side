package net.focustation.myapplication.ui.screen.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.ui.theme.Primary40

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val backgroundColor: Pair<Color, Color>,
)

private val pages =
    listOf(
        OnboardingPage(
            emoji = "🎙️",
            title = "센서로 환경을 분석해요",
            description = "스마트폰의 마이크, 카메라, 온도 센서를 활용해\n소음 수준, 조도, 온도를 실시간으로 측정합니다.\n나의 작업 환경이 얼마나 적합한지 바로 확인하세요.",
            backgroundColor = Pair(Color(0xFF0D1B4B), Color(0xFF1A3BAA)),
        ),
        OnboardingPage(
            emoji = "📊",
            title = "집중도를 기록하고 분석해요",
            description = "세션별 집중도 지표와 함께\n공간 기반 기록이 자동으로 저장됩니다.\n나만의 최적 학습/작업 환경 패턴을 발견해보세요.",
            backgroundColor = Pair(Color(0xFF0D2B3B), Color(0xFF1A5F7A)),
        ),
        OnboardingPage(
            emoji = "⏱️",
            title = "세션 시작이 간단해요",
            description = "메인 화면에서 '환경 분석 세션 시작' 버튼만 누르면\n즉시 측정이 시작됩니다.\n세션 종료 후에는 상세 리포트를 확인할 수 있어요.",
            backgroundColor = Pair(Color(0xFF1B2D0D), Color(0xFF2E5C1A)),
        ),
        OnboardingPage(
            emoji = "🔒",
            title = "권한 안내 및 개인정보 보호",
            description = "마이크·카메라 권한은 환경 측정에만 사용됩니다.\n위치 정보는 장소 기록을 위해 수집되며\n언제든 설정에서 관리할 수 있습니다.",
            backgroundColor = Pair(Color(0xFF2B0D2B), Color(0xFF5A1A5A)),
        ),
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val page = pages[pageIndex]
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(page.backgroundColor.first, page.backgroundColor.second)),
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = page.emoji, fontSize = 80.sp)
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = page.title,
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )

                    // 권한 화면 전용 안심 문구
                    if (pageIndex == 3) {
                        Spacer(Modifier.height(24.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                listOf(
                                    "🎙️ 마이크 — 소음 레벨 측정 전용",
                                    "📸 카메라 — 조도 측정 전용",
                                    "📍 위치 — 장소 기록 전용",
                                    "🚫 외부 전송 없음 (설정 시 선택)",
                                ).forEach { item ->
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 하단 컨트롤
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 페이지 인디케이터
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    val color by animateColorAsState(
                        if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.3f),
                        label = "dot_color",
                    )
                    val width = if (index == pagerState.currentPage) 24.dp else 8.dp
                    Box(
                        modifier =
                            Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 다음 / 시작 버튼
            if (pagerState.currentPage < pages.size - 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onFinish) {
                        Text("건너뛰기", color = Color.White.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    ) {
                        Text("다음", color = Primary40, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                ) {
                    Text(
                        text = "시작하기",
                        color = Primary40,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    FocustationTheme { OnboardingScreen(onFinish = {}) }
}

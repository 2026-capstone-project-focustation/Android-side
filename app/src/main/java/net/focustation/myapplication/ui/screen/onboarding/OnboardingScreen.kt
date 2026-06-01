package net.focustation.myapplication.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import net.focustation.myapplication.R
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.FocustationTheme

@Composable
fun OnboardingScreen(onLoginClick: () -> Unit) {
    PokeyOnboardingScaffold(
        step = 1,
        totalSteps = 8,
        primaryLabel = "시작하기",
        onPrimary = onLoginClick,
    ) {
        PokeyLottieHero(
            rawRes = R.raw.mascot_lightpink_float_only_lottie,
            imageSize = 210,
        )

        OnboardingTitleBlock(
            title = "안녕하세요!\n저는 포키예요",
            description = "Focustation은 당신에게 꼭 맞는 집중 환경을 찾고 기록할 수 있도록 도와줄 당신만의 파트너예요.",
        )

        FeatureCard(
            features =
                listOf(
                    OnboardingFeature(Icons.Outlined.Sensors, "소음, 조도, 진동을 기반으로 공간을 분석해요"),
                    OnboardingFeature(Icons.Outlined.Insights, "집중하기 좋은 환경인지 점수로 알려줘요"),
                    OnboardingFeature(Icons.Outlined.Assessment, "기록을 모아 나에게 맞는 패턴을 보여줘요"),
                ),
        )
    }
}

@Composable
fun OnboardingSetupScreen(
    onStartSurvey: () -> Unit,
    onExploreFirst: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { OnboardingStore(context) }
    var step by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf(store.userName()) }
    val displayName = userName.trim().ifBlank { "사용자" }

    when (step) {
        0 ->
            PokeyOnboardingScaffold(
                step = 3,
                totalSteps = 8,
                primaryLabel = "이름 저장하기",
                onPrimary = {
                    store.saveUserName(displayName)
                    step = 1
                },
            ) {
                PokeyLottieHero(
                    rawRes = R.raw.poki_xeyes_float_lottie,
                    imageSize = 190,
                )
                OnboardingTitleBlock(
                    title = "로그인 완료!",
                    description = "당신은 누구인가요?\n이름을 입력해주세요.",
                )
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("이름") },
                    placeholder = { Text("예: 지수") },
                    shape = RoundedCornerShape(18.dp),
                )
            }

        1 ->
            PokeyOnboardingScaffold(
                step = 4,
                totalSteps = 8,
                primaryLabel = "좋아요",
                onPrimary = { step = 2 },
                onBack = { step = 0 },
            ) {
                PokeyLottieHero(
                    rawRes = R.raw.teamwork_palette_floating_lottie,
                    imageSize = 200,
                )
                OnboardingTitleBlock(
                    title = "$displayName 님,\n서로에 대해 알아가봐요",
                    description = "먼저 포키가 해드릴 수 있는 일을 짧게 설명할게요.",
                )
                CompactMessageCard(
                    title = "앞으로 이런 일을 도와드릴게요",
                    description = "공간 측정, 집중 세션, 리포트와 장소 기록까지 한 흐름으로 이어져요.",
                )
            }

        in 2..4 ->
            AppIntroPage(
                page = step - 2,
                displayName = displayName,
                onNext = { step += 1 },
                onBack = { step -= 1 },
            )

        else ->
            PokeyOnboardingScaffold(
                step = 8,
                totalSteps = 8,
                primaryLabel = "설문조사 진행하기",
                onPrimary = {
                    store.saveUserName(displayName)
                    store.markCompleted()
                    onStartSurvey()
                },
                secondaryLabel = "앱 먼저 둘러볼래요",
                onSecondary = {
                    store.saveUserName(displayName)
                    store.markCompleted()
                    onExploreFirst()
                },
                onBack = { step = 4 },
            ) {
                PokeyLottieHero(
                    rawRes = R.raw.celebration_floating_lottie,
                    imageSize = 190,
                )
                OnboardingTitleBlock(
                    title = "이제 본격적으로\n시작해볼까요?",
                    description = "$displayName 님을 더 잘 알기 위해서는 짧은 설문조사가 필요해요.",
                )
                FeatureCard(
                    features =
                        listOf(
                            OnboardingFeature(Icons.Outlined.Person, "선호하는 공부 장소와 집중 방식을 확인해요"),
                            OnboardingFeature(Icons.Outlined.Insights, "설문 결과를 바탕으로 더 자연스러운 추천을 준비해요"),
                        ),
                )
            }
    }
}

@Composable
private fun AppIntroPage(
    page: Int,
    displayName: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val intro = appIntroPages[page.coerceIn(0, appIntroPages.lastIndex)]
    PokeyOnboardingScaffold(
        step = 5 + page,
        totalSteps = 8,
        primaryLabel = "다음",
        onPrimary = onNext,
        onBack = onBack,
    ) {
        PokeyLottieHero(
            rawRes = intro.lottieRes,
            imageSize = 220,
        )
        OnboardingTitleBlock(
            title = intro.title.replace("{name}", displayName),
            description = intro.description,
        )
        FeatureCard(features = intro.features)
    }
}

@Composable
private fun PokeyOnboardingScaffold(
    step: Int,
    totalSteps: Int,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ReferenceDesignTokens.ScreenAlt)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        OnboardingTopBar(onBack = onBack)

        OnboardingProgress(step = step, totalSteps = totalSteps)

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onPrimary,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = ReferenceDesignTokens.Dark.copy(alpha = 0.18f),
                        spotColor = ReferenceDesignTokens.Dark.copy(alpha = 0.28f),
                    ),
            shape = CircleShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = ReferenceDesignTokens.Dark,
                    contentColor = Color.White,
                ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        ) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.size(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onSecondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = ReferenceDesignTokens.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(onBack: (() -> Unit)?) {
    if (onBack == null) return
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(40.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전 단계",
                tint = ReferenceDesignTokens.TextPrimary,
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun OnboardingProgress(
    step: Int,
    totalSteps: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(totalSteps) { index ->
                val isCurrent = index == step - 1
                val isReached = index < step
                Box(
                    modifier =
                        Modifier
                            .weight(if (isCurrent) 2.6f else 1f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isReached) {
                                    ReferenceDesignTokens.Dark
                                } else {
                                    ReferenceDesignTokens.PaleBlueTrack
                                },
                            ),
                )
            }
        }
        Text(
            text = "$step / $totalSteps 단계",
            style = MaterialTheme.typography.labelMedium,
            color = ReferenceDesignTokens.TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PokeyLottieHero(
    rawRes: Int,
    imageSize: Int,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(imageSize.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun OnboardingTitleBlock(
    title: String,
    description: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = ReferenceDesignTokens.TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            color = ReferenceDesignTokens.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun CompactMessageCard(
    title: String,
    description: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = ReferenceDesignTokens.TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ReferenceDesignTokens.TextSecondary,
            )
        }
    }
}

@Composable
private fun FeatureCard(features: List<OnboardingFeature>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column {
            features.forEachIndexed { index, feature ->
                OnboardingFeatureRow(icon = feature.icon, text = feature.text)
                if (index != features.lastIndex) OnboardingFeatureDivider()
            }
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReferenceDesignTokens.PaleBlueTrack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ReferenceDesignTokens.Dark,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ReferenceDesignTokens.TextPrimary,
        )
    }
}

@Composable
private fun OnboardingFeatureDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        thickness = 1.dp,
        color = ReferenceDesignTokens.Border,
    )
}

private data class OnboardingFeature(
    val icon: ImageVector,
    val text: String,
)

private data class AppIntro(
    val lottieRes: Int,
    val title: String,
    val description: String,
    val features: List<OnboardingFeature>,
)

private val appIntroPages =
    listOf(
        AppIntro(
            lottieRes = R.raw.analyzing_progress_floating_lottie,
            title = "{name} 님의 공간을 측정해요",
            description = "집중을 방해하는 요소는 생각보다 작게 시작돼요. 포키가 주변 환경을 센서 데이터로 확인해드릴게요.",
            features =
                listOf(
                    OnboardingFeature(Icons.Outlined.Sensors, "소음, 조도, 진동을 함께 측정"),
                    OnboardingFeature(Icons.Outlined.Insights, "현재 공간의 집중 적합도 계산"),
                ),
        ),
        AppIntro(
            lottieRes = R.raw.reading_book_lightpink_float_lottie,
            title = "집중 세션을 기록해요",
            description = "공간 분석 후 실제 집중 시간을 기록하고, 세션 중 환경 변화가 집중에 어떤 영향을 줬는지 볼 수 있어요.",
            features =
                listOf(
                    OnboardingFeature(Icons.Outlined.CheckCircle, "시작, 일시정지, 종료 흐름 제공"),
                    OnboardingFeature(Icons.Outlined.Assessment, "세션별 집중 점수와 환경 점수 저장"),
                ),
        ),
        AppIntro(
            lottieRes = R.raw.celebration_floating_lottie,
            title = "나에게 맞는 장소를 찾아요",
            description = "반복해서 쌓인 기록을 바탕으로 어떤 공간에서 더 잘 집중하는지 리포트와 장소 기록으로 확인해요.",
            features =
                listOf(
                    OnboardingFeature(Icons.Outlined.Assessment, "리포트에서 세션 기록 확인"),
                    OnboardingFeature(Icons.Outlined.Person, "설문으로 개인 선호까지 반영"),
                ),
        ),
    )

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    FocustationTheme { OnboardingScreen(onLoginClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingSetupPreview() {
    FocustationTheme {
        OnboardingSetupScreen(
            onStartSurvey = {},
            onExploreFirst = {},
        )
    }
}

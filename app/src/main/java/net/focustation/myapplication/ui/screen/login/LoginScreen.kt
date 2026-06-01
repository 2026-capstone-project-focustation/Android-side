@file:Suppress("DEPRECATION")

package net.focustation.myapplication.ui.screen.login

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import net.focustation.myapplication.BuildConfig
import net.focustation.myapplication.R
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.FocustationTheme
import com.airbnb.lottie.compose.LottieAnimation as AirbnbLottieAnimation

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val webClientId = remember(context) { context.googleWebClientId() }
    val googleSignInClient =
        remember(context, webClientId) {
            val optionsBuilder =
                GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()

            if (!webClientId.isNullOrBlank()) {
                optionsBuilder.requestIdToken(webClientId)
            }

            GoogleSignIn.getClient(context, optionsBuilder.build())
        }

    val googleSignInLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val signInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = signInTask.getResult(ApiException::class.java)
                viewModel.onGoogleIdTokenReceived(account.idToken)
            } catch (exception: ApiException) {
                if (exception.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    viewModel.onGoogleLoginCanceled()
                } else {
                    viewModel.onGoogleLoginFailed(messageForGoogleErrorCode(exception.statusCode))
                }
            }
        }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ReferenceDesignTokens.ScreenAlt),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LoginBrandHero()

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Focustation",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ReferenceDesignTokens.TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "소셜 계정으로 쉽게 계정을 생성하고, 나에게 맞는 집중 환경을 이어서 설정할 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = ReferenceDesignTokens.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    if (webClientId.isNullOrBlank()) {
                        viewModel.onGoogleLoginFailed(
                            "Google 로그인 설정이 아직 연결되지 않았어요. google-services.json 설정을 확인해주세요.",
                        )
                        return@Button
                    }

                    viewModel.onGoogleLoginStarted()
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
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
                enabled = !uiState.isLoading,
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = ReferenceDesignTokens.Dark,
                        contentColor = Color.White,
                    ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            ) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4285F4),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Google로 계속하기",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            if (BuildConfig.DEBUG && webClientId.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onLoginSuccess,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Firebase 설정 없이 UI 흐름 확인하기",
                        color = ReferenceDesignTokens.TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "계속 진행하면 이용약관 및 개인정보 처리방침에 동의하는 것으로 간주됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = ReferenceDesignTokens.TextMuted,
                textAlign = TextAlign.Center,
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = viewModel::clearError) {
                    Text(text = "닫기", color = ReferenceDesignTokens.TextSecondary)
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = ReferenceDesignTokens.Dark,
            )
        }
    }
}

@Composable
private fun LoginBrandHero() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.reading_book_lightpink_float_lottie),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        contentDescription = "포키",
        modifier = Modifier.size(200.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun LottieAnimation(
    composition: LottieComposition?,
    progress: () -> Float,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AirbnbLottieAnimation(
        composition = composition,
        progress = progress,
        modifier = modifier,
        contentScale = contentScale,
    )
}

private fun Context.googleWebClientId(): String? {
    val resourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (resourceId == 0) {
        null
    } else {
        getString(resourceId).takeIf { it.isNotBlank() }
    }
}

private fun messageForGoogleErrorCode(statusCode: Int): String =
    when (statusCode) {
        10 -> "Google 로그인 설정이 올바르지 않아요. SHA와 OAuth Client ID를 확인해주세요."
        7 -> "네트워크 연결이 불안정해요. 연결 상태를 확인하고 다시 시도해주세요."
        8 -> "일시적인 내부 오류가 발생했어요. 잠시 후 다시 시도해주세요."
        12500 -> "Google 로그인 구성이 맞지 않아요. Firebase 설정을 확인해주세요."
        else -> {
            val statusText = GoogleApiAvailability.getInstance().getErrorString(statusCode)
            "Google 로그인에 실패했어요. (code=$statusCode, $statusText)"
        }
    }

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    FocustationTheme { LoginScreen(onLoginSuccess = {}) }
}

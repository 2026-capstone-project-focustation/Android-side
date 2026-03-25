package net.focustation.myapplication.ui.screen.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.GoogleApiAvailability
import net.focustation.myapplication.R
import net.focustation.myapplication.ui.theme.FocustationTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // google-services.json 기준으로 생성되는 값을 사용해 콘솔/앱 불일치를 줄입니다.
    val webClientId = remember { context.getString(R.string.default_web_client_id) }
    val googleSignInClient =
        remember(context, webClientId) {
            val options =
                GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(webClientId)
                    .build()

            GoogleSignIn.getClient(context, options)
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
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF0D1B4B),
                                Color(0xFF1A3BAA),
                                Color(0xFF0D1B4B),
                            ),
                    ),
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
            // 로고 영역
            Text(
                text = "📍",
                fontSize = 64.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Focustation",
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "당신의 최적 집중 공간을 찾아드립니다",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(60.dp))

            // 구글 로그인 버튼
            Button(
                onClick = {
                    viewModel.onGoogleLoginStarted()
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1A1A1A),
                    ),
                elevation = ButtonDefaults.buttonElevation(4.dp),
            ) {
                Text(
                    text = "G",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Google로 계속하기",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 기타 로그인 버튼 (더미)
            OutlinedButton(
                onClick = {},
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            ) {
                Text(
                    text = "📧  이메일로 계속하기",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "계속 진행하면 이용약관 및 개인정보처리방침에 동의하는 것으로 간주됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFC7C7),
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = viewModel::clearError) {
                    Text(text = "닫기", color = Color.White)
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }
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

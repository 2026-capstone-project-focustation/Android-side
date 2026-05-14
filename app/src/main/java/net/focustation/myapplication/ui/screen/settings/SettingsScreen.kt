package net.focustation.myapplication.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.MainBottomDestination
import net.focustation.myapplication.ui.components.MainBottomNavigationBar
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.ui.theme.Primary40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToSpaceHistory: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            MainBottomNavigationBar(
                selected = MainBottomDestination.SETTINGS,
                onTabClick = { destination ->
                    when (destination) {
                        MainBottomDestination.HOME -> onNavigateToHome()
                        MainBottomDestination.REPORT -> onNavigateToReport()
                        MainBottomDestination.MAP -> onNavigateToSpaceHistory()
                        MainBottomDestination.SETTINGS -> Unit
                    }
                },
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1B4B),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // ─── 계정 정보 ───────────────────────────────────────────────────
            SettingsSection(title = "계정 정보") {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤", fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = uiState.userName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = uiState.userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("로그아웃", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ─── 센서 설정 ───────────────────────────────────────────────────
            SettingsSection(title = "센서 설정") {
                SettingsCard {
                    Column {
                        Text(
                            text = "센서 샘플링 주기",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                        Text(
                            text = "${uiState.sensorSamplingSeconds}초마다 측정",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary40,
                        )
                        Slider(
                            value = uiState.sensorSamplingSeconds.toFloat(),
                            onValueChange = { viewModel.setSamplingInterval(it.toInt()) },
                            valueRange = 1f..30f,
                            steps = 28,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "1초",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "30초",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ─── 알림 설정 ───────────────────────────────────────────────────
            SettingsSection(title = "알림 설정") {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ToggleRow(
                            label = "실시간 집중 저하 알림",
                            description = "집중도가 크게 낮아지면 알림",
                            checked = uiState.focusDropAlertEnabled,
                            onCheckedChange = { viewModel.toggleFocusDropAlert(it) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ToggleRow(
                            label = "세션 완료 알림",
                            description = "환경 분석 세션이 끝나면 알림",
                            checked = uiState.sessionCompleteAlertEnabled,
                            onCheckedChange = { viewModel.toggleSessionCompleteAlert(it) },
                        )
                    }
                }
            }

            // ─── 데이터 관리 ─────────────────────────────────────────────────
            SettingsSection(title = "데이터 관리") {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "로컬 저장 용량",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                                Text(
                                    "${uiState.localStorageMb} MB 사용 중",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = {}) {
                                Text("초기화", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ToggleRow(
                            label = "서버 익명 업로드",
                            description = "데이터를 익명화해서 연구에 기여",
                            checked = uiState.anonymousUploadEnabled,
                            onCheckedChange = { viewModel.toggleAnonymousUpload(it) },
                        )
                    }
                }
            }

            // ─── 앱 테마 ─────────────────────────────────────────────────────
            SettingsSection(title = "앱 테마") {
                SettingsCard {
                    ToggleRow(
                        label = "다크 모드",
                        description = "어두운 테마로 전환",
                        checked = uiState.isDarkTheme,
                        onCheckedChange = { viewModel.toggleDarkTheme(it) },
                    )
                }
            }

            // ─── 앱 정보 ─────────────────────────────────────────────────────
            SettingsSection(title = "앱 정보") {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow("버전", "1.0.0 (UI Preview)")
                        HorizontalDivider()
                        InfoRow("개발", "Focustation Team")
                        HorizontalDivider()
                        InfoRow("문의", "support@focustation.net")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    FocustationTheme {
        SettingsScreen(onBack = {})
    }
}

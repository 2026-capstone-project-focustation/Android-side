package net.focustation.myapplication.ui.screen.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.focustation.myapplication.ui.components.MainBottomDestination
import net.focustation.myapplication.ui.components.MainBottomNavigationBar
import net.focustation.myapplication.ui.components.ReferenceDesignTokens
import net.focustation.myapplication.ui.theme.FocustationTheme

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
        containerColor = ReferenceDesignTokens.Screen,
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
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ReferenceDesignTokens.Screen)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsHero(
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                onBack = onBack,
                onSignOut = viewModel::signOut,
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SettingsGroup(title = "환경 설정") {
                    SamplingCard(
                        seconds = uiState.sensorSamplingSeconds,
                        onSecondsChange = viewModel::setSamplingInterval,
                    )
                }

                SettingsGroup(title = "알림") {
                    SettingsItemColumn {
                        ToggleListItem(
                            icon = Icons.Filled.NotificationsActive,
                            title = "집중 저하 알림",
                            subtitle = "집중도가 낮아지면 바로 알려줘요",
                            checked = uiState.focusDropAlertEnabled,
                            onCheckedChange = viewModel::toggleFocusDropAlert,
                        )
                        ItemDivider()
                        ToggleListItem(
                            icon = Icons.Filled.NotificationsNone,
                            title = "세션 완료 알림",
                            subtitle = "환경 분석 세션이 끝나면 알려줘요",
                            checked = uiState.sessionCompleteAlertEnabled,
                            onCheckedChange = viewModel::toggleSessionCompleteAlert,
                        )
                    }
                }

                SettingsGroup(title = "데이터 & 테마") {
                    SettingsItemColumn {
                        ActionListItem(
                            icon = Icons.Filled.Storage,
                            title = "로컬 저장 용량",
                            subtitle = "${uiState.localStorageMb} MB 사용 중",
                            trailing = {
                                TextButton(
                                    onClick = viewModel::clearLocalStorage,
                                    colors =
                                        ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                ) {
                                    Text("초기화", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                        )
                        ItemDivider()
                        ToggleListItem(
                            icon = Icons.Filled.CloudUpload,
                            title = "익명 업로드",
                            subtitle = "데이터를 익명화해서 연구에 기여해요",
                            checked = uiState.anonymousUploadEnabled,
                            onCheckedChange = viewModel::toggleAnonymousUpload,
                        )
                        ItemDivider()
                        ToggleListItem(
                            icon = Icons.Filled.DarkMode,
                            title = "다크 모드",
                            subtitle = "어두운 테마로 전환해요",
                            checked = uiState.isDarkTheme,
                            onCheckedChange = viewModel::toggleDarkTheme,
                        )
                    }
                }

                SettingsGroup(title = "앱 정보") {
                    SettingsItemColumn {
                        InfoListItem(label = "버전", value = "1.0.0")
                        ItemDivider()
                        InfoListItem(label = "개발", value = "Focustation Team")
                        ItemDivider()
                        InfoListItem(label = "문의", value = "support@focustation.net")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHero(
    userName: String,
    userEmail: String,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                bottomStart = ReferenceDesignTokens.PhoneRadius,
                bottomEnd = ReferenceDesignTokens.PhoneRadius,
            ),
        color = ReferenceDesignTokens.Dark,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(46.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ReferenceDesignTokens.DarkAlt),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = Color.White.copy(alpha = 0.78f),
                    )
                }
                Text(
                    text = "설정",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ReferenceDesignTokens.Yellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = ReferenceDesignTokens.TextPrimary,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { "사용자" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = userEmail.ifBlank { "로그인 정보 없음" },
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
            ) {
                Text(
                    text = "로그아웃",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = ReferenceDesignTokens.TextPrimary,
            modifier = Modifier.padding(start = 4.dp),
        )
        content()
    }
}

@Composable
private fun SettingsItemColumn(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReferenceDesignTokens.LargeRadius),
        color = ReferenceDesignTokens.WhiteCard,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, ReferenceDesignTokens.Border),
    ) {
        Column(content = content)
    }
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        thickness = 1.dp,
        color = ReferenceDesignTokens.Border,
    )
}

@Composable
private fun ToggleListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(icon = icon, title = title, subtitle = subtitle) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = trailing,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIconTile(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ReferenceDesignTokens.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = ReferenceDesignTokens.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        trailing()
    }
}

@Composable
private fun SamplingCard(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
) {
    SettingsItemColumn {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsIconTile(icon = Icons.Filled.Sensors)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "센서 샘플링 주기",
                    color = ReferenceDesignTokens.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "몇 초마다 환경을 확인할지",
                    color = ReferenceDesignTokens.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            SamplingStepper(
                seconds = seconds,
                onSecondsChange = onSecondsChange,
            )
        }
    }
}

@Composable
private fun SamplingStepper(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
) {
    val min = 1
    val max = 30
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(ReferenceDesignTokens.PaleBlueTrack),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = Icons.Filled.Remove,
            enabled = seconds > min,
            onClick = { onSecondsChange((seconds - 1).coerceAtLeast(min)) },
        )
        Text(
            text = "${seconds}s",
            color = ReferenceDesignTokens.TextPrimary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(38.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton(
            icon = Icons.Filled.Add,
            enabled = seconds < max,
            onClick = { onSecondsChange((seconds + 1).coerceAtMost(max)) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
                if (enabled) {
                    ReferenceDesignTokens.BlueDark
                } else {
                    ReferenceDesignTokens.TextMuted
                },
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun InfoListItem(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = ReferenceDesignTokens.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            color = ReferenceDesignTokens.TextPrimary,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun SettingsIconTile(icon: ImageVector) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(ReferenceDesignTokens.PaleBlueTrack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ReferenceDesignTokens.BlueDark,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    FocustationTheme {
        SettingsScreen(onBack = {})
    }
}

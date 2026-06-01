package net.focustation.myapplication.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.focustation.myapplication.ui.theme.FocustationTheme

enum class MainBottomDestination {
    HOME,
    REPORT,
    MAP,
    SETTINGS,
}

@Composable
fun MainBottomNavigationBar(
    selected: MainBottomDestination,
    onTabClick: (MainBottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = MainBottomDestination.entries
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        ReferenceBottomNavigationBar(
            selectedIndex = destinations.indexOf(selected).coerceAtLeast(0),
            items =
                destinations.map { destination ->
                    NavItemSlot(
                        icon = destination.icon(),
                        contentDescription = destination.label(),
                    )
                },
            onItemClick = { index ->
                destinations.getOrNull(index)?.let { destination ->
                    if (selected != destination) {
                        onTabClick(destination)
                    }
                }
            },
        )
    }
}

private fun MainBottomDestination.label(): String =
    when (this) {
        MainBottomDestination.HOME -> "홈"
        MainBottomDestination.REPORT -> "리포트"
        MainBottomDestination.MAP -> "지도"
        MainBottomDestination.SETTINGS -> "설정"
    }

private fun MainBottomDestination.icon(): ImageVector =
    when (this) {
        MainBottomDestination.HOME -> Icons.Filled.Home
        MainBottomDestination.REPORT -> Icons.Filled.Insights
        MainBottomDestination.MAP -> Icons.Filled.Map
        MainBottomDestination.SETTINGS -> Icons.Filled.Settings
    }

@Preview(showBackground = true)
@Composable
private fun MainBottomNavigationBarPreview() {
    FocustationTheme {
        MainBottomNavigationBar(
            selected = MainBottomDestination.HOME,
            onTabClick = {},
        )
    }
}

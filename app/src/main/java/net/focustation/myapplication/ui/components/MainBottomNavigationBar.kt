package net.focustation.myapplication.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

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
) {
    NavigationBar {
        MainBottomDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = {
                    if (selected != destination) {
                        onTabClick(destination)
                    }
                },
                icon = { Text(destination.icon(), fontSize = 20.sp) },
                label = { Text(destination.label()) },
            )
        }
    }
}

private fun MainBottomDestination.label(): String =
    when (this) {
        MainBottomDestination.HOME -> "홈"
        MainBottomDestination.REPORT -> "리포트"
        MainBottomDestination.MAP -> "지도"
        MainBottomDestination.SETTINGS -> "설정"
    }

private fun MainBottomDestination.icon(): String =
    when (this) {
        MainBottomDestination.HOME -> "🏠"
        MainBottomDestination.REPORT -> "📊"
        MainBottomDestination.MAP -> "🗺️"
        MainBottomDestination.SETTINGS -> "⚙️"
    }

package net.focustation.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
    darkColorScheme(
        primary = Primary80,
        onPrimary = SurfaceDark,
        primaryContainer = PrimaryContainer40,
        secondary = Secondary80,
        tertiary = Tertiary80,
        background = SurfaceDark,
        surface = SurfaceDark,
        onBackground = OnSurfaceDark,
        onSurface = OnSurfaceDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Primary40,
        onPrimary = SurfaceLight,
        primaryContainer = PrimaryContainer80,
        secondary = Secondary40,
        tertiary = Tertiary40,
        background = SurfaceLight,
        surface = SurfaceLight,
        onBackground = OnSurfaceLight,
        onSurface = OnSurfaceLight,
    )

@Composable
fun FocustationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

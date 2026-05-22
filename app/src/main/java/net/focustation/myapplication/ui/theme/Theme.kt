package net.focustation.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = Primary80,
        onPrimary = SurfaceDark,
        primaryContainer = PrimaryContainer40,
        secondary = Secondary80,
        tertiary = Tertiary80,
        background = SurfaceDark,
        surface = Color(0xFF191C20),
        surfaceVariant = Color(0xFF22262D),
        onBackground = OnSurfaceDark,
        onSurface = OnSurfaceDark,
        onSurfaceVariant = Color(0xFFC9C6BD),
        outline = Color(0xFF454A52),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = ReferenceBlue,
        onPrimary = Color.White,
        primaryContainer = ReferenceScreenAlt,
        secondary = ReferenceYellow,
        tertiary = ReferenceYellowDark,
        background = ReferenceScreen,
        surface = FocusSurface,
        surfaceVariant = ReferenceScreenAlt,
        onBackground = ReferenceTextPrimary,
        onSurface = ReferenceTextPrimary,
        onSurfaceVariant = FocusMuted,
        outline = FocusLine,
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

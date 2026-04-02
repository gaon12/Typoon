package xyz.gaon.typoon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = SkyAnchorDark,
        onPrimary = Ink900,
        primaryContainer = Color(0xFF0F4D69),
        onPrimaryContainer = Color(0xFFD7F1FF),
        secondary = SunAccentDark,
        onSecondary = Ink900,
        secondaryContainer = Color(0xFF6B4216),
        onSecondaryContainer = Color(0xFFFFE3C7),
        tertiary = MintSignalDark,
        onTertiary = Ink900,
        tertiaryContainer = Color(0xFF164C42),
        onTertiaryContainer = Color(0xFFC4F4E8),
        background = Color(0xFF0D1419),
        onBackground = Color(0xFFE7EEF2),
        surface = Color(0xFF0E161B),
        onSurface = Color(0xFFE7EEF2),
        surfaceVariant = Color(0xFF1A2B35),
        onSurfaceVariant = Color(0xFFBCCAD2),
        surfaceContainer = Color(0xFF172129),
        surfaceContainerLow = Color(0xFF141D24),
        outline = Color(0xFF50606B),
        error = Color(0xFFFF8A80),
        errorContainer = Color(0xFF5F1E1A),
        onErrorContainer = Color(0xFFFFDAD5),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = SkyAnchor,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD4EFFC),
        onPrimaryContainer = Color(0xFF032C3F),
        secondary = SunAccent,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE0C3),
        onSecondaryContainer = Color(0xFF4A2800),
        tertiary = MintSignal,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD0F2E8),
        onTertiaryContainer = Color(0xFF0B3A31),
        background = Cloud100,
        onBackground = Ink900,
        surface = Color.White,
        onSurface = Ink900,
        surfaceVariant = Cloud200,
        onSurfaceVariant = Ink500,
        surfaceContainer = Color(0xFFF0F5F7),
        surfaceContainerLow = Color(0xFFFAFCFD),
        outline = Color(0xFF8BA0AE),
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )

@Composable
fun TypoonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor -> if (darkTheme) DarkColorScheme else LightColorScheme
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

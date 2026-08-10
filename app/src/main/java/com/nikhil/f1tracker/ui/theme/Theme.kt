package com.nikhil.f1tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RacingRedDark,
    onPrimary = Color.White,
    secondary = CreamText,
    onSecondary = CharcoalBackground,
    secondaryContainer = Color(0xFF4A211B),
    onSecondaryContainer = Color(0xFFFFDAD4),
    background = CharcoalBackground,
    onBackground = CreamText,
    surface = CharcoalSurface,
    onSurface = CreamText,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CreamTextMuted,
    surfaceContainerLowest = Color(0xFF0C0C0B),
    surfaceContainerLow = Color(0xFF171716),
    surfaceContainer = Color(0xFF1C1C1A),
    surfaceContainerHigh = Color(0xFF27251F),
    surfaceContainerHighest = Color(0xFF322F27),
)

private val LightColorScheme = lightColorScheme(
    primary = RacingRed,
    onPrimary = Color.White,
    secondary = Ink,
    onSecondary = CreamSurface,
    secondaryContainer = Color(0xFFF6D3CE),
    onSecondaryContainer = Color(0xFF5C120B),
    background = CreamBackground,
    onBackground = Ink,
    surface = CreamSurface,
    onSurface = Ink,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F5EC),
    surfaceContainer = Color(0xFFF3EEE0),
    surfaceContainerHigh = Color(0xFFEDE7D6),
    surfaceContainerHighest = Color(0xFFE7DFCB),
)

@Composable
fun F1TrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic (wallpaper-derived) color is available on Android 12+, but this app carries its
    // own F1-red/cream/ink branding rather than adapting to the user's wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

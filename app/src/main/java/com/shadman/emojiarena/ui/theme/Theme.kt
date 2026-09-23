package com.shadman.emojiarena.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed dark scheme — no dynamicColor param, no darkTheme param. The app
// always renders this, regardless of system theme or Android version.
private val AppColorScheme = darkColorScheme(
    primary = AccentCoral,
    onPrimary = Color.White,
    secondary = AccentCoral,
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = OutlineDark
)

@Composable
fun EmojiArenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}

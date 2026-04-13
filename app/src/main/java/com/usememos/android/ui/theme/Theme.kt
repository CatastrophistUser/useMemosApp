package com.usememos.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    primaryContainer = AccentSoft,
    secondary = Slate,
    secondaryContainer = SurfaceRaised,
    surface = Paper,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    surfaceContainer = SurfaceSoft,
    surfaceContainerHigh = SurfaceRaised,
    outlineVariant = Line,
    errorContainer = ErrorSoft,
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    primaryContainer = AccentSoft,
    secondary = Slate,
    secondaryContainer = SurfaceRaised,
    surface = Paper,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    surfaceContainer = SurfaceSoft,
    surfaceContainerHigh = SurfaceRaised,
    outlineVariant = Line,
    errorContainer = ErrorSoft,
)

@Composable
fun UseMemosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

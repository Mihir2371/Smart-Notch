package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ObsidianColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = IslandBlack,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondary = AccentEmerald,
    onSecondary = IslandBlack,
    secondaryContainer = SurfaceInteractive,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentViolet,
    onTertiary = IslandBlack,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SurfaceCarbon,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderHairline,
    error = AccentCoral,
    onError = IslandBlack,
)

@Composable
fun AuraIslandTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = AuraTypography,
        content = content,
    )
}

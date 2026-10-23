package com.sandesh.thecount.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    background = TheCountColors.DarkBackground,
    surface = TheCountColors.DarkSurface,
    surfaceContainerLowest = TheCountColors.DarkSurfaceContainerLowest,
    surfaceContainerLow = TheCountColors.DarkSurfaceContainerLow,
    surfaceContainer = TheCountColors.DarkSurfaceContainer,
    surfaceContainerHigh = TheCountColors.DarkSurfaceContainerHigh,
    surfaceContainerHighest = TheCountColors.DarkSurfaceContainerHighest,
    onSurface = TheCountColors.DarkOnSurface,
    onSurfaceVariant = TheCountColors.DarkOnSurfaceVariant,
    outline = TheCountColors.DarkOutline,
    outlineVariant = TheCountColors.DarkOutlineVariant,
    primary = TheCountColors.DarkPrimary,
    onPrimary = TheCountColors.DarkOnPrimary,
    primaryContainer = TheCountColors.DarkPrimaryContainer,
    onPrimaryContainer = TheCountColors.DarkOnPrimaryContainer,
    secondary = TheCountColors.DarkSecondary,
    onSecondary = TheCountColors.DarkOnSecondary,
    secondaryContainer = TheCountColors.DarkSecondaryContainer,
    onSecondaryContainer = TheCountColors.DarkOnSecondaryContainer,
    tertiary = TheCountColors.DarkTertiary,
    onTertiary = TheCountColors.DarkOnTertiary,
    tertiaryContainer = TheCountColors.DarkTertiaryContainer,
    onTertiaryContainer = TheCountColors.DarkOnTertiaryContainer,
    error = TheCountColors.DarkError,
    onError = TheCountColors.DarkOnError,
    errorContainer = TheCountColors.DarkErrorContainer,
    onErrorContainer = TheCountColors.DarkOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    background = TheCountColors.LightBackground,
    surface = TheCountColors.LightSurface,
    surfaceContainerLowest = TheCountColors.LightSurfaceContainerLowest,
    surfaceContainerLow = TheCountColors.LightSurfaceContainerLow,
    surfaceContainer = TheCountColors.LightSurfaceContainer,
    surfaceContainerHigh = TheCountColors.LightSurfaceContainerHigh,
    surfaceContainerHighest = TheCountColors.LightSurfaceContainerHighest,
    onSurface = TheCountColors.LightOnSurface,
    onSurfaceVariant = TheCountColors.LightOnSurfaceVariant,
    outline = TheCountColors.LightOutline,
    outlineVariant = TheCountColors.LightOutlineVariant,
    primary = TheCountColors.LightPrimary,
    onPrimary = TheCountColors.LightOnPrimary,
    primaryContainer = TheCountColors.LightPrimaryContainer,
    onPrimaryContainer = TheCountColors.LightOnPrimaryContainer,
    secondary = TheCountColors.LightSecondary,
    onSecondary = TheCountColors.LightOnSecondary,
    secondaryContainer = TheCountColors.LightSecondaryContainer,
    onSecondaryContainer = TheCountColors.LightOnSecondaryContainer,
    tertiary = TheCountColors.LightTertiary,
    onTertiary = TheCountColors.LightOnTertiary,
    tertiaryContainer = TheCountColors.LightTertiaryContainer,
    onTertiaryContainer = TheCountColors.LightOnTertiaryContainer,
    error = TheCountColors.LightError,
    onError = TheCountColors.LightOnError,
    errorContainer = TheCountColors.LightErrorContainer,
    onErrorContainer = TheCountColors.LightOnErrorContainer,
)

@Composable
fun TheCountTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

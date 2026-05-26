package com.mistyislet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Danger,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Obsidian,
    primaryContainer = Color(0xFF204D45),
    onPrimaryContainer = Mist,
    secondary = Secondary,
    secondaryContainer = Color(0xFF4C3513),
    onSecondaryContainer = Mist,
    background = Obsidian,
    onBackground = Mist,
    surface = Graphite,
    surfaceVariant = Color(0xFF24251F),
    onSurface = Mist,
    onSurfaceVariant = Smoke,
    outline = Color(0xFF4B4B43),
    error = Danger,
)

@Composable
fun MistyisletTheme(
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

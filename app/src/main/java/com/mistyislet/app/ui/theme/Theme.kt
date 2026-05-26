package com.mistyislet.app.ui.theme

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
    primary = Mist,
    onPrimary = Obsidian,
    primaryContainer = FogRaised,
    onPrimaryContainer = Mist,
    secondary = Teal,
    secondaryContainer = Color(0xFF263331),
    onSecondaryContainer = Mist,
    background = Obsidian,
    onBackground = Mist,
    surface = Graphite,
    surfaceVariant = Fog,
    onSurface = Mist,
    onSurfaceVariant = Smoke,
    outline = Hairline,
    error = Danger,
)

@Composable
fun MistyisletTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

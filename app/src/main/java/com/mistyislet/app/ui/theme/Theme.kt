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
    primaryContainer = Color(0xFF252721),
    onPrimaryContainer = Mist,
    secondary = Teal,
    secondaryContainer = Color(0xFF284844),
    onSecondaryContainer = Mist,
    background = Color(0xFF0D0D0C),
    onBackground = Mist,
    surface = Graphite,
    surfaceVariant = Color(0xFF20221D),
    onSurface = Mist,
    onSurfaceVariant = Smoke,
    outline = Color(0xFF53564D),
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

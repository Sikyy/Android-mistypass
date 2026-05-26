package com.mistyislet.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.mistyislet.app.R
import com.mistyislet.app.ui.theme.FogRaised
import com.mistyislet.app.ui.theme.Graphite
import com.mistyislet.app.ui.theme.Hairline
import com.mistyislet.app.ui.theme.Obsidian

@Composable
fun MistyScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Obsidian),
    ) {
        MistyNoiseOverlay(alpha = 0.045f)
        content()
    }
}

@Composable
fun MistyListCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val clickableModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Card(
        modifier = clickableModifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Hairline),
        colors = CardDefaults.cardColors(containerColor = FogRaised),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() },
    )
}

@Composable
fun MistyNoiseOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 0.05f,
) {
    val noise = ImageBitmap.imageResource(R.drawable.login_noise)
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                listOf(
                    Obsidian,
                    Graphite,
                    Color(0xFF0A0C0C),
                ),
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.055f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.28f),
                ),
            ),
        )
        drawRect(
            brush = ShaderBrush(
                ImageShader(
                    image = noise,
                    tileModeX = TileMode.Repeated,
                    tileModeY = TileMode.Repeated,
                ),
            ),
            alpha = alpha,
        )
    }
}

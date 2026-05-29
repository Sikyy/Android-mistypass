package com.mistyislet.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom door glyph approximating iOS's `door.left.hand` SF Symbol — a tall, narrow,
 * minimal door with a left-side handle, instead of Material's wider framed door + knob.
 * SF Symbols are Apple-proprietary and can't be embedded, so this is original line-art.
 * Color is [Color.Black]; the consuming [androidx.compose.material3.Icon] re-tints it.
 */
val MistyDoorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MistyDoor",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Door panel: tall narrow rounded-top rectangle.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(8.6f, 21f)
            lineTo(8.6f, 5.6f)
            quadTo(8.6f, 3.6f, 10.6f, 3.6f)
            lineTo(13.4f, 3.6f)
            quadTo(15.4f, 3.6f, 15.4f, 5.6f)
            lineTo(15.4f, 21f)
        }
        // Baseline the door sits on.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(6.4f, 21f)
            lineTo(17.6f, 21f)
        }
        // Handle (left-of-center -> "left.hand").
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(10.5f, 11.6f)
            lineTo(10.5f, 13.4f)
        }
    }.build()
}

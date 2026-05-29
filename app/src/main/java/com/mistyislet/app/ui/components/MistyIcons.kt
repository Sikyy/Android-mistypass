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

/**
 * Custom Face ID glyph approximating iOS's `faceid` SF Symbol — a rounded bracket frame with
 * eyes, nose and a smile — instead of Material's smiley `Face` icon.
 */
val MistyFaceIdIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MistyFaceId",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        val s = SolidColor(Color.Black)
        // Corner brackets.
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4f, 8f); lineTo(4f, 5.5f); quadTo(4f, 4f, 5.5f, 4f); lineTo(8f, 4f)
        }
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(16f, 4f); lineTo(18.5f, 4f); quadTo(20f, 4f, 20f, 5.5f); lineTo(20f, 8f)
        }
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4f, 16f); lineTo(4f, 18.5f); quadTo(4f, 20f, 5.5f, 20f); lineTo(8f, 20f)
        }
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(16f, 20f); lineTo(18.5f, 20f); quadTo(20f, 20f, 20f, 18.5f); lineTo(20f, 16f)
        }
        // Eyes.
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) { moveTo(9f, 9.3f); lineTo(9f, 11.2f) }
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) { moveTo(15f, 9.3f); lineTo(15f, 11.2f) }
        // Nose.
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 9.6f); lineTo(12f, 13f); lineTo(13f, 13f)
        }
        // Smile.
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
            moveTo(9.3f, 15.3f); quadTo(12f, 17.4f, 14.7f, 15.3f)
        }
    }.build()
}

/**
 * Custom key glyph approximating iOS's vertical `key.fill` SF Symbol — a round bow at top,
 * a downward shaft with two teeth — instead of Material's diagonal key.
 */
val MistyKeyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MistyKey",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        val s = SolidColor(Color.Black)
        // Bow (ring) at top.
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(8.4f, 7.5f)
            arcToRelative(3.6f, 3.6f, 0f, false, true, 7.2f, 0f)
            arcToRelative(3.6f, 3.6f, 0f, false, true, -7.2f, 0f)
            close()
        }
        // Shaft.
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 11.1f)
            lineTo(12f, 20f)
        }
        // Teeth (right side).
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 16.2f); lineTo(14.8f, 16.2f)
        }
        path(stroke = s, strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 19f); lineTo(14f, 19f)
        }
    }.build()
}

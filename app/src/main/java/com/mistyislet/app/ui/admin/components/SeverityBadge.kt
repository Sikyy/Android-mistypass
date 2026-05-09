package com.mistyislet.app.ui.admin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun severityColor(severity: String): Color = when (severity.lowercase()) {
    "critical" -> Color(0xFFD93025)
    "high" -> Color(0xFFFF9800)
    "medium" -> Color(0xFFD98B06)
    "low" -> Color(0xFF4285F4)
    else -> Color(0xFF9E9E9E)
}

fun statusColor(status: String): Color = when (status.lowercase()) {
    "open", "triggered", "active" -> Color(0xFFD93025)
    "acknowledged" -> Color(0xFF4285F4)
    "investigating" -> Color(0xFFFF9800)
    "resolved", "completed", "checked_out" -> Color(0xFF35A853)
    "false_positive", "cancelled" -> Color(0xFF9E9E9E)
    "checked_in", "confirmed" -> Color(0xFF4285F4)
    else -> Color(0xFF9E9E9E)
}

@Composable
fun SeverityDot(severity: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(severityColor(severity)),
    )
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Text(
        text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

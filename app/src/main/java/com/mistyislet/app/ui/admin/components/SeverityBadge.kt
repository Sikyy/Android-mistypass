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
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosRed
import com.mistyislet.app.ui.theme.IosYellow

fun severityColor(severity: String): Color = when (severity.lowercase()) {
    "critical" -> IosRed
    "high" -> IosOrange
    "medium" -> IosYellow
    "low" -> IosBlue
    else -> IosGray
}

fun statusColor(status: String): Color = when (status.lowercase()) {
    "open", "triggered" -> IosRed
    "expected", "pending" -> IosOrange
    "active", "available", "enabled" -> IosGreen
    "acknowledged" -> IosBlue
    "investigating" -> IosOrange
    "resolved", "completed", "checked_out" -> IosGreen
    "false_positive", "cancelled", "disabled", "full" -> IosGray
    "checked_in", "confirmed" -> IosBlue
    else -> IosGray
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
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

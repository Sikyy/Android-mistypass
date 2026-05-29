package com.mistyislet.app.ui.admin

import java.time.Duration
import java.time.Instant

internal fun adminCompactRelativeTime(isoTime: String?): String {
    val raw = isoTime?.takeIf { it.isNotBlank() } ?: return ""
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "Just now"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}

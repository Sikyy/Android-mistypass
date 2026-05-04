package com.mistyislet.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.AccessLog
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.logs.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    items(uiState.logs, key = { it.id }) { log ->
                        AccessLogCard(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessLogCard(log: AccessLog) {
    val isSuccess = log.result == "allow" || log.result == "success" || log.result == "app_unlock" || log.displayType == "access_granted"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSuccess) Success else Danger,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatDisplayTime(log.displayTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            log.credentialType?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDisplayTime(isoTime: String): String {
    if (isoTime.isBlank()) return ""
    return try {
        val instant = java.time.Instant.parse(isoTime)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        val now = java.time.ZonedDateTime.now()
        val minutes = java.time.Duration.between(instant, now.toInstant()).toMinutes()
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 1440 -> {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                "Today ${local.format(fmt)}"
            }
            minutes < 2880 -> {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                "Yesterday ${local.format(fmt)}"
            }
            else -> {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
                local.format(fmt)
            }
        }
    } catch (e: Exception) {
        isoTime.take(16).replace("T", " ")
    }
}

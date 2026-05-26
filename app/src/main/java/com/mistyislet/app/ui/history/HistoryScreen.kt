package com.mistyislet.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.AccessLog
import com.mistyislet.app.ui.components.MistyListCard
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.Success
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedLog by remember { mutableStateOf<AccessLog?>(null) }

    val groupedLogs = remember(uiState.logs) {
        uiState.logs.groupBy { log ->
            try {
                Instant.parse(log.displayTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (_: Exception) {
                LocalDate.now()
            }
        }.toSortedMap(compareByDescending { it })
    }

    val flatItems = remember(groupedLogs) {
        buildList {
            groupedLogs.forEach { (date, logs) ->
                add(HistoryItem.Header(date))
                logs.forEach { add(HistoryItem.Event(it)) }
            }
        }
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= flatItems.size - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && flatItems.isNotEmpty()) {
            viewModel.loadMore()
        }
    }

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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(flatItems, key = { it.key }) { item ->
                        when (item) {
                            is HistoryItem.Header -> DateSectionHeader(item.date)
                            is HistoryItem.Event -> AccessLogCard(
                                log = item.log,
                                onClick = { selectedLog = item.log },
                            )
                        }
                    }
                    if (uiState.isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    selectedLog?.let { log ->
        EventDetailSheet(log = log, viewModel = viewModel, onDismiss = { selectedLog = null })
    }
}

private sealed class HistoryItem {
    abstract val key: String
    data class Header(val date: LocalDate) : HistoryItem() {
        override val key = "header_$date"
    }
    data class Event(val log: AccessLog) : HistoryItem() {
        override val key = log.id
    }
}

@Composable
private fun DateSectionHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> stringResource(R.string.history_today)
        today.minusDays(1) -> stringResource(R.string.history_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun AccessLogCard(log: AccessLog, onClick: () -> Unit) {
    val isSuccess = isGranted(log)

    MistyListCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
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
                log.reason?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatTimeOnly(log.displayTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            log.displayMethod?.let { method ->
                MethodBadge(method)
            }
        }
    }
}

@Composable
private fun MethodBadge(method: String) {
    Text(
        text = method.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(50),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailSheet(log: AccessLog, viewModel: HistoryViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isSuccess = isGranted(log)
    val media by viewModel.eventMedia.collectAsStateWithLifecycle()
    val isLoadingMedia by viewModel.isLoadingMedia.collectAsStateWithLifecycle()

    LaunchedEffect(log.id) {
        viewModel.loadEventMedia(log.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.history_event_detail),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.history_door)) },
                supportingContent = { Text(log.displayName) },
                leadingContent = {
                    Icon(Icons.Default.DoorFront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.history_time)) },
                supportingContent = { Text(formatFullTime(log.displayTime)) },
                leadingContent = {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.history_result)) },
                supportingContent = {
                    Text(
                        text = if (isSuccess) stringResource(R.string.history_granted) else stringResource(R.string.history_denied),
                        color = if (isSuccess) Success else Danger,
                    )
                },
                leadingContent = {
                    Icon(
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isSuccess) Success else Danger,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            log.displayMethod?.let { method ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.history_method)) },
                    supportingContent = { Text(method.uppercase()) },
                    leadingContent = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            log.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.history_reason)) },
                    supportingContent = { Text(reason) },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // Camera snapshots section
            if (isLoadingMedia) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                )
            } else if (media.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.history_camera_snapshots),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                media.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = item.snapshotUrl,
                            contentDescription = item.cameraName,
                            modifier = Modifier
                                .size(80.dp, 60.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp),
                                ),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.cameraName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = item.datetime.replace("T", " ").take(16),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isGranted(log: AccessLog): Boolean =
    log.result == "allow" || log.result == "success" ||
        log.result == "app_unlock" || log.displayType == "access_granted"

private fun formatTimeOnly(isoTime: String): String {
    if (isoTime.isBlank()) return ""
    return try {
        val instant = Instant.parse(isoTime)
        val local = instant.atZone(ZoneId.systemDefault())
        val now = ZonedDateTime.now()
        val minutes = Duration.between(instant, now.toInstant()).toMinutes()
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            else -> local.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    } catch (_: Exception) {
        isoTime.take(16).replace("T", " ")
    }
}

private fun formatFullTime(isoTime: String): String {
    if (isoTime.isBlank()) return ""
    return try {
        val instant = Instant.parse(isoTime)
        val local = instant.atZone(ZoneId.systemDefault())
        local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    } catch (_: Exception) {
        isoTime
    }
}

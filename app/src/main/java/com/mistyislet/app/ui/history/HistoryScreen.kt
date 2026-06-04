package com.mistyislet.app.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.AccessLog
import com.mistyislet.app.domain.model.EventMedia
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyLargeTitle
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPage
import com.mistyislet.app.ui.components.MistyPagePadding
import com.mistyislet.app.ui.components.MistySectionTitle
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosRed
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
    onBack: (() -> Unit)? = null,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedLog by remember { mutableStateOf<AccessLog?>(null) }

    selectedLog?.let { log ->
        BackHandler { selectedLog = null }
        EventDetailPage(
            log = log,
            viewModel = viewModel,
            onBack = { selectedLog = null },
        )
        return
    }

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

    val sections = remember(groupedLogs) { groupedLogs.entries.toList() }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= sections.size - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && sections.isNotEmpty()) {
            viewModel.loadMore()
        }
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        HistoryContent(
            modifier = modifier,
            isLoading = uiState.isLoading,
            isLoadingMore = uiState.isLoadingMore,
            sections = sections,
            listState = listState,
            onRefresh = viewModel::refresh,
            onLogClick = { selectedLog = it },
        )
    }

    if (onBack != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            HistoryLargeHeader(
                title = stringResource(R.string.history_title),
                onBack = onBack,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                content(Modifier.fillMaxSize())
            }
        }
    } else {
        MistyPage {
            MistyLargeTitle(text = stringResource(R.string.history_title))
            content(Modifier.fillMaxSize())
        }
    }
}

/** Stateless History (large-title + grouped log list) for the DEBUG parity harness. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreenContent(logs: List<AccessLog>) {
    val groupedLogs = remember(logs) {
        logs.groupBy { log ->
            try {
                Instant.parse(log.displayTime).atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (_: Exception) {
                LocalDate.now()
            }
        }.toSortedMap(compareByDescending { it })
    }
    val sections = remember(groupedLogs) { groupedLogs.entries.toList() }
    val listState = rememberLazyListState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HistoryLargeHeader(title = stringResource(R.string.history_title), onBack = {})
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            HistoryContent(
                modifier = Modifier.fillMaxSize(),
                isLoading = false,
                isLoadingMore = false,
                sections = sections,
                listState = listState,
                onRefresh = {},
                onLogClick = {},
            )
        }
    }
}

@Composable
private fun HistoryLargeHeader(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(14.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    modifier: Modifier,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    sections: List<Map.Entry<LocalDate, List<AccessLog>>>,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onLogClick: (AccessLog) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (sections.isEmpty() && !isLoading) {
            MistyEmptyState(
                icon = Icons.Default.History,
                title = stringResource(R.string.history_empty),
                description = stringResource(R.string.history_empty_description),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = MistyPagePadding,
            ) {
                sections.forEach { (date, logs) ->
                    item(key = "section_$date") {
                        HistoryDateSection(
                            date = date,
                            logs = logs,
                            onLogClick = onLogClick,
                        )
                    }
                }
                if (isLoadingMore) {
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

@Composable
private fun HistoryDateSection(
    date: LocalDate,
    logs: List<AccessLog>,
    onLogClick: (AccessLog) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateSectionHeader(
            date = date,
            modifier = Modifier.padding(start = 14.dp),
        )
        MistyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                logs.forEachIndexed { index, log ->
                    AccessLogRow(
                        log = log,
                        onClick = { onLogClick(log) },
                    )
                    if (index < logs.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSectionHeader(date: LocalDate, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> stringResource(R.string.history_today)
        today.minusDays(1) -> stringResource(R.string.history_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

    MistySectionTitle(text = label, modifier = modifier)
}

@Composable
private fun AccessLogRow(log: AccessLog, onClick: () -> Unit) {
    val isSuccess = isGranted(log)

    Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isSuccess) IosGreen else IosRed,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTimeOnly(log.displayTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            log.displayMethod?.let { method ->
                MethodBadge(method)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
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
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun EventDetailPage(log: AccessLog, viewModel: HistoryViewModel, onBack: () -> Unit) {
    val media by viewModel.eventMedia.collectAsStateWithLifecycle()
    val isLoadingMedia by viewModel.isLoadingMedia.collectAsStateWithLifecycle()

    LaunchedEffect(log.id) {
        viewModel.loadEventMedia(log.id)
    }

    EventDetailContent(
        log = log,
        media = media,
        isLoadingMedia = isLoadingMedia,
        onBack = onBack,
    )
}

/** Stateless Event Detail (info section + camera snapshots) for the DEBUG parity harness. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventDetailContent(
    log: AccessLog,
    media: List<EventMedia>,
    isLoadingMedia: Boolean,
    onBack: () -> Unit,
) {
    val isSuccess = isGranted(log)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.history_event_detail),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 24.dp,
                end = 16.dp,
                bottom = MistyBottomNavInset,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MistyGroupedSection {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        HistoryDetailRow(stringResource(R.string.history_door), log.displayName)
                        IosFormDivider()
                        HistoryDetailRow(stringResource(R.string.history_time), formatFullTime(log.displayTime))
                        IosFormDivider()
                        HistoryDetailRow(
                            label = stringResource(R.string.history_result),
                            value = if (isSuccess) stringResource(R.string.history_granted) else stringResource(R.string.history_denied),
                            valueColor = if (isSuccess) IosGreen else IosRed,
                        )
                        log.displayMethod?.let { method ->
                            IosFormDivider()
                            HistoryDetailRow(stringResource(R.string.history_method), method.uppercase())
                        }
                        log.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                            IosFormDivider()
                            HistoryDetailRow("", reason)
                        }
                    }
                }
            }

            item {
                MistyGroupedSection(title = stringResource(R.string.history_camera_snapshots)) {
                    when {
                        isLoadingMedia -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                        media.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.history_no_media),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                        else -> {
                            media.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
                                if (index < media.lastIndex) {
                                    IosFormDivider(startInset = 112.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** iOS inset-grouped form separator: hairline, faint, leading-inset (full to the right edge). */
@Composable
private fun IosFormDivider(startInset: Dp = 0.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startInset),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
    )
}

@Composable
private fun HistoryDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (value.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (label.isBlank()) FontWeight.Normal else FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
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

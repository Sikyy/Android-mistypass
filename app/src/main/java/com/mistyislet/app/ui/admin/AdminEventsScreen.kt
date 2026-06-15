package com.mistyislet.app.ui.admin

import android.text.format.DateUtils
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AdminEvent
import com.mistyislet.app.domain.model.RelatedAdminEvent
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyLabeledContentRow
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistySectionTitle
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AdminEventsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminEvent>>(emptyList())
    val items: StateFlow<List<AdminEvent>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var placeId: String? = null

    init {
        viewModelScope.launch {
            placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            loadData()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        val state = demoFallback.resolveList(adminRepository.getEvents(pid)) { AdminDemoData.events }
        _items.value = state.items
        _error.value = state.error
        _isLoading.value = false
    }
}

data class AdminEventDetailUiState(
    val event: AdminEvent? = null,
    val relatedEvents: List<RelatedAdminEvent> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val relatedError: String? = null,
)

@HiltViewModel
class AdminEventDetailViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminEventDetailUiState())
    val state: StateFlow<AdminEventDetailUiState> = _state
    private var loadedEventId: String? = null

    fun load(eventId: String) {
        if (eventId.isBlank()) {
            _state.value = AdminEventDetailUiState(isLoading = false, error = "Event id is missing")
            return
        }
        if (loadedEventId == eventId && !_state.value.isLoading) return
        loadedEventId = eventId

        viewModelScope.launch {
            val placeId = selectedPlaceRepository.scope.first().placeId
            if (placeId.isNullOrBlank()) {
                _state.value = AdminEventDetailUiState(isLoading = false, error = "No place selected")
                return@launch
            }

            _state.value = AdminEventDetailUiState(isLoading = true)
            when (val eventResult = adminRepository.getEvent(placeId, eventId)) {
                is ApiResult.Success -> {
                    when (val relatedResult = adminRepository.getRelatedEvents(placeId, eventId)) {
                        is ApiResult.Success -> _state.value = AdminEventDetailUiState(
                            event = eventResult.data,
                            relatedEvents = relatedResult.data.ifEmpty {
                                demoFallback.demoOrNull { AdminDemoData.relatedEvents(eventId) }.orEmpty()
                            },
                            isLoading = false,
                        )
                        is ApiResult.Error -> _state.value = AdminEventDetailUiState(
                            event = eventResult.data,
                            relatedEvents = demoFallback.demoOrNull { AdminDemoData.relatedEvents(eventId) }.orEmpty(),
                            isLoading = false,
                            relatedError = null,
                        )
                        is ApiResult.Exception -> _state.value = AdminEventDetailUiState(
                            event = eventResult.data,
                            relatedEvents = demoFallback.demoOrNull { AdminDemoData.relatedEvents(eventId) }.orEmpty(),
                            isLoading = false,
                            relatedError = null,
                        )
                    }
                }
                is ApiResult.Error -> _state.value = demoFallback.demoOrNull { AdminDemoData.event(eventId) }?.let { demo ->
                    AdminEventDetailUiState(
                        event = demo,
                        relatedEvents = AdminDemoData.relatedEvents(eventId),
                        isLoading = false,
                    )
                } ?: AdminEventDetailUiState(isLoading = false, error = eventResult.message)
                is ApiResult.Exception -> _state.value = demoFallback.demoOrNull { AdminDemoData.event(eventId) }?.let { demo ->
                    AdminEventDetailUiState(
                        event = demo,
                        relatedEvents = AdminDemoData.relatedEvents(eventId),
                        isLoading = false,
                    )
                } ?: AdminEventDetailUiState(isLoading = false, error = eventResult.throwable.localizedMessage)
            }
        }
    }
}

private fun resultIcon(result: String) = when (result.lowercase()) {
    "granted", "success" -> Icons.Default.CheckCircle
    "denied", "failed" -> Icons.Default.Cancel
    else -> Icons.Default.Circle
}

private fun resultColor(result: String): Color = when (result.lowercase()) {
    "granted", "success" -> IosGreen
    "denied", "failed" -> IosRed
    else -> IosGray
}

private fun resultTitle(result: String): String = result
    .split("_")
    .joinToString("_") { part -> part.replaceFirstChar { it.uppercase() } }

private fun eventDisplayTime(displayTime: String, timestamp: String): String {
    if (displayTime.isNotBlank()) return displayTime
    val epochMillis = runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrNull()
        ?: return timestamp
    return DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

@Composable
fun AdminEventsScreen(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: AdminEventsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_events),
        items = items.map { event ->
            val icon = resultIcon(event.result)
            val color = resultColor(event.result)
            val subtitle = listOf(event.objectName, eventDisplayTime(event.displayTime, event.timestamp))
                .filter { it.isNotBlank() }
                .joinToString("\n")
            AdminListItem(
                id = event.id,
                title = "${event.actor} · ${event.action}".ifBlank { event.objectName },
                subtitle = subtitle.ifBlank { null },
                leadingIcon = icon,
                leadingIconColor = color,
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        emptyIcon = Icons.Default.Info,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        onItemClick = { onEventClick(it.id) },
        errorMessage = error,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: AdminEventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) {
        viewModel.load(eventId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = state.event?.objectName?.takeIf { it.isNotBlank() } ?: "Event detail",
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                else -> LazyColumn(
                    contentPadding = MistyGroupedListPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.event?.let { event ->
                        item { EventDetailCard(event) }
                    }
                    item {
                        RelatedEventsSection(
                            relatedEvents = state.relatedEvents,
                            relatedError = state.relatedError,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailCard(event: AdminEvent) {
    val detailRows = listOf(
        "Actor" to event.actor,
        "Action" to event.action,
        "Object" to event.objectName,
        "Object Type" to event.eventType.orEmpty(),
        "Object ID" to event.objectId,
        "Door ID" to event.doorId.orEmpty(),
        "Area ID" to event.areaId.orEmpty(),
        "Gateway ID" to event.gatewayId.orEmpty(),
        "Detail" to event.detail.orEmpty(),
    ).filter { (_, value) -> value.isNotBlank() }

    MistyGroupedSection(title = "Event") {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = resultIcon(event.result),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = resultColor(event.result),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = resultTitle(event.result),
                    style = MaterialTheme.typography.bodyMedium,
                    color = resultColor(event.result),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = eventDisplayTime(event.displayTime, event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            detailRows.forEachIndexed { index, row ->
                DetailRow(row.first, row.second)
                if (index < detailRows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun RelatedEventsSection(
    relatedEvents: List<RelatedAdminEvent>,
    relatedError: String?,
) {
    if (relatedEvents.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MistySectionTitle(text = "Related Events", modifier = Modifier.padding(start = 14.dp))
            MistyEmptyState(
                icon = Icons.Default.Info,
                title = "No Related Events",
                description = relatedError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
        }
        return
    }

    MistyGroupedSection(title = "Related Events") {
        relatedEvents.forEachIndexed { index, event ->
            RelatedEventRow(event)
            if (index < relatedEvents.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun RelatedEventRow(event: RelatedAdminEvent) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = resultIcon(event.result),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = resultColor(event.result),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.actor} · ${event.action}".trim(' ', '·').ifBlank { "Event" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = event.objectName.ifBlank { event.objectId },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOf(
                        eventDisplayTime(event.displayTime, event.timestamp),
                        event.relation.replace("_", " ").replaceFirstChar { it.uppercase() },
                    )
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    MistyLabeledContentRow(label = label, value = value)
}

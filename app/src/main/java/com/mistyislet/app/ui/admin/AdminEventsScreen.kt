package com.mistyislet.app.ui.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminEventsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
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
        when (val result = adminRepository.getEvents(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
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
                            relatedEvents = relatedResult.data,
                            isLoading = false,
                        )
                        is ApiResult.Error -> _state.value = AdminEventDetailUiState(
                            event = eventResult.data,
                            isLoading = false,
                            relatedError = relatedResult.message,
                        )
                        is ApiResult.Exception -> _state.value = AdminEventDetailUiState(
                            event = eventResult.data,
                            isLoading = false,
                            relatedError = relatedResult.throwable.localizedMessage,
                        )
                    }
                }
                is ApiResult.Error -> _state.value = AdminEventDetailUiState(
                    isLoading = false,
                    error = eventResult.message,
                )
                is ApiResult.Exception -> _state.value = AdminEventDetailUiState(
                    isLoading = false,
                    error = eventResult.throwable.localizedMessage,
                )
            }
        }
    }
}

private fun resultIcon(color: String) = when (color.lowercase()) {
    "green" -> Icons.Default.CheckCircle
    "red" -> Icons.Default.Error
    "orange" -> Icons.Default.Warning
    else -> Icons.Default.Info
}

private fun resultColor(color: String) = when (color.lowercase()) {
    "green" -> Color(0xFF35A853)
    "red" -> Color(0xFFD93025)
    "orange" -> Color(0xFFFF9800)
    "yellow" -> Color(0xFFD98B06)
    "blue" -> Color(0xFF4285F4)
    else -> Color(0xFF9E9E9E)
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
            val icon = resultIcon(event.resultColor)
            val color = resultColor(event.resultColor)
            AdminListItem(
                id = event.id,
                title = "${event.actor} · ${event.action}".ifBlank { event.objectName },
                subtitle = event.objectName.ifBlank { null },
                trailing = event.displayTime.ifBlank { null },
                leadingIcon = icon,
                leadingIconColor = color,
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
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
        topBar = {
            TopAppBar(
                title = { Text("Event detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.event?.let { event ->
                        item { EventDetailCard(event) }
                    }
                    item {
                        Text(
                            text = "Related events",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (state.relatedEvents.isEmpty()) {
                        item {
                            Text(
                                text = state.relatedError ?: stringResource(R.string.dashboard_no_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.relatedError == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    } else {
                        items(state.relatedEvents, key = { it.id }) { event ->
                            RelatedEventRow(event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailCard(event: AdminEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.action.ifBlank { "Event" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow("Actor", event.actor)
            DetailRow("Result", event.result)
            DetailRow("Object", event.objectName.ifBlank { event.objectId })
            DetailRow("Object type", event.eventType.orEmpty())
            DetailRow("Gateway", event.gatewayId.orEmpty())
            DetailRow("Area", event.areaId.orEmpty())
            DetailRow("Time", event.timestamp)
            if (!event.detail.isNullOrBlank()) {
                DetailRow("Detail", event.detail)
            }
            DetailRow("Event ID", event.id)
        }
    }
}

@Composable
private fun RelatedEventRow(event: RelatedAdminEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = event.action.ifBlank { "Event" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = event.relation,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${event.actor} · ${event.objectName}".trim(' ', '·'),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = event.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.58f),
        )
    }
}

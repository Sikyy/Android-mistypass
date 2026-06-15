package com.mistyislet.app.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AdminIncident
import com.mistyislet.app.domain.model.IncidentEvent
import com.mistyislet.app.domain.model.IncidentOccurrence
import com.mistyislet.app.ui.admin.components.severityColor
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyLabeledContentRow
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistySectionTitle
import com.mistyislet.app.ui.theme.IosOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminIncidentsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminIncident>>(emptyList())
    val items: StateFlow<List<AdminIncident>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
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
        _items.value = demoFallback.resolveList(adminRepository.getIncidents(pid)) { AdminDemoData.incidents }.items
        _isLoading.value = false
    }
}

data class AdminIncidentDetailUiState(
    val incident: AdminIncident? = null,
    val occurrences: List<IncidentOccurrence> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val occurrencesError: String? = null,
)

@HiltViewModel
class AdminIncidentDetailViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminIncidentDetailUiState())
    val state: StateFlow<AdminIncidentDetailUiState> = _state
    private var loadedIncidentId: String? = null

    fun load(incidentId: String) {
        if (incidentId.isBlank()) {
            _state.value = AdminIncidentDetailUiState(isLoading = false, error = "Incident id is missing")
            return
        }
        if (loadedIncidentId == incidentId && !_state.value.isLoading) return
        loadedIncidentId = incidentId

        viewModelScope.launch {
            val placeId = selectedPlaceRepository.scope.first().placeId
            if (placeId.isNullOrBlank()) {
                _state.value = AdminIncidentDetailUiState(isLoading = false, error = "No place selected")
                return@launch
            }

            _state.value = AdminIncidentDetailUiState(isLoading = true)
            when (val incidentResult = adminRepository.getIncident(placeId, incidentId)) {
                is ApiResult.Success -> {
                    when (val occurrencesResult = adminRepository.getIncidentOccurrences(placeId, incidentId)) {
                        is ApiResult.Success -> _state.value = AdminIncidentDetailUiState(
                            incident = incidentResult.data,
                            occurrences = occurrencesResult.data.ifEmpty {
                                demoFallback.demoOrNull { AdminDemoData.incidentOccurrences }.orEmpty()
                            },
                            isLoading = false,
                        )
                        is ApiResult.Error -> _state.value = AdminIncidentDetailUiState(
                            incident = incidentResult.data,
                            occurrences = demoFallback.demoOrNull { AdminDemoData.incidentOccurrences }.orEmpty(),
                            isLoading = false,
                            occurrencesError = null,
                        )
                        is ApiResult.Exception -> _state.value = AdminIncidentDetailUiState(
                            incident = incidentResult.data,
                            occurrences = demoFallback.demoOrNull { AdminDemoData.incidentOccurrences }.orEmpty(),
                            isLoading = false,
                            occurrencesError = null,
                        )
                    }
                }
                is ApiResult.Error -> _state.value = demoFallback.demoOrNull { AdminDemoData.incident(incidentId) }?.let { demo ->
                    AdminIncidentDetailUiState(
                        incident = demo,
                        occurrences = AdminDemoData.incidentOccurrences,
                        isLoading = false,
                    )
                } ?: AdminIncidentDetailUiState(isLoading = false, error = incidentResult.message)
                is ApiResult.Exception -> _state.value = demoFallback.demoOrNull { AdminDemoData.incident(incidentId) }?.let { demo ->
                    AdminIncidentDetailUiState(
                        incident = demo,
                        occurrences = AdminDemoData.incidentOccurrences,
                        isLoading = false,
                    )
                } ?: AdminIncidentDetailUiState(isLoading = false, error = incidentResult.throwable.localizedMessage)
            }
        }
    }
}

@Composable
fun AdminIncidentsScreen(
    onBack: () -> Unit,
    onIncidentClick: (String) -> Unit,
    viewModel: AdminIncidentsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_incidents),
        items = items.map { incident ->
            AdminListItem(
                id = incident.id,
                title = incident.title.replace("_", " ").replaceFirstChar { it.uppercase() },
                subtitle = listOfNotNull(
                    incident.description?.takeIf { it.isNotBlank() },
                    incident.state.takeIf { it.isNotBlank() }?.replace("_", " ")?.replaceFirstChar { it.uppercase() },
                ).joinToString("\n").ifBlank { null },
                trailing = incident.severity.uppercase(),
                trailingColor = severityColor(incident.severity),
                trailingChip = true,
                leadingDotColor = severityColor(incident.severity),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        emptyIcon = Icons.Default.Warning,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        onItemClick = { onIncidentClick(it.id) },
    )
}

@Composable
fun AdminIncidentDetailScreen(
    incidentId: String,
    onBack: () -> Unit,
    viewModel: AdminIncidentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(incidentId) {
        viewModel.load(incidentId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = state.incident?.title?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Incident detail",
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
                    state.incident?.let { incident ->
                        item { IncidentDetailCard(incident) }
                        if (incident.events.isNotEmpty()) {
                            item { IncidentEventsSection(incident.events) }
                        }
                    }
                    item {
                        OccurrencesSection(
                            occurrences = state.occurrences,
                            occurrencesError = state.occurrencesError,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentDetailCard(incident: AdminIncident) {
    val detailRows = listOf(
        "Type" to incident.title.replace("_", " ").replaceFirstChar { it.uppercase() },
        "State" to incident.state.replace("_", " ").replaceFirstChar { it.uppercase() },
        "Subject Type" to incident.subjectType.replace("_", " ").replaceFirstChar { it.uppercase() },
        "Subject ID" to incident.subjectId,
        "Count" to incident.count.takeIf { it > 0 }?.toString().orEmpty(),
        "Created" to incident.createdAt.orEmpty(),
    ).filter { (_, value) -> value.isNotBlank() }

    MistyGroupedSection(title = "Incident") {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = incident.severity.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(severityColor(incident.severity).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = severityColor(incident.severity),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = incident.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (!incident.description.isNullOrBlank()) {
                Text(
                    text = incident.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            detailRows.forEachIndexed { index, row ->
                IncidentDetailRow(row.first, row.second)
                if (index < detailRows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun OccurrencesSection(
    occurrences: List<IncidentOccurrence>,
    occurrencesError: String?,
) {
    if (occurrences.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MistySectionTitle(text = "Occurrences", modifier = Modifier.padding(start = 14.dp))
            MistyEmptyState(
                icon = Icons.Default.Warning,
                title = "No Occurrences",
                description = occurrencesError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
        }
        return
    }

    MistyGroupedSection(title = "Occurrences") {
        occurrences.forEachIndexed { index, occurrence ->
            OccurrenceRow(occurrence)
            if (index < occurrences.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                )
            }
        }
    }
}

@Composable
private fun IncidentEventsSection(events: List<IncidentEvent>) {
    MistyGroupedSection(title = "Events") {
        events.forEachIndexed { index, event ->
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = event.actor.ifBlank { "Event" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = event.eventId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                )
            }
            if (index < events.lastIndex) {
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                )
            }
        }
    }
}

@Composable
private fun OccurrenceRow(occurrence: IncidentOccurrence) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val denied = occurrence.result.equals("denied", ignoreCase = true)
        Icon(
            imageVector = if (denied) Icons.Default.Error else Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (denied) MaterialTheme.colorScheme.error else IosOrange,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = occurrence.actor ?: occurrence.gatewayId ?: occurrence.eventId,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = occurrence.doorId ?: occurrence.detail ?: occurrence.gatewayId ?: occurrence.eventId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = occurrence.occurredAt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun IncidentDetailRow(label: String, value: String) {
    MistyLabeledContentRow(label = label, value = value)
}

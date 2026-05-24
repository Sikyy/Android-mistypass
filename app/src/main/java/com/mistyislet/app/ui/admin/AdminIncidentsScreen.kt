package com.mistyislet.app.ui.admin

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mistyislet.app.domain.model.IncidentOccurrence
import com.mistyislet.app.ui.admin.components.StatusBadge
import com.mistyislet.app.ui.admin.components.severityColor
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
        when (val result = adminRepository.getIncidents(pid)) {
            is ApiResult.Success -> _items.value = result.data
            else -> {}
        }
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
                            occurrences = occurrencesResult.data,
                            isLoading = false,
                        )
                        is ApiResult.Error -> _state.value = AdminIncidentDetailUiState(
                            incident = incidentResult.data,
                            isLoading = false,
                            occurrencesError = occurrencesResult.message,
                        )
                        is ApiResult.Exception -> _state.value = AdminIncidentDetailUiState(
                            incident = incidentResult.data,
                            isLoading = false,
                            occurrencesError = occurrencesResult.throwable.localizedMessage,
                        )
                    }
                }
                is ApiResult.Error -> _state.value = AdminIncidentDetailUiState(
                    isLoading = false,
                    error = incidentResult.message,
                )
                is ApiResult.Exception -> _state.value = AdminIncidentDetailUiState(
                    isLoading = false,
                    error = incidentResult.throwable.localizedMessage,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIncidentsScreen(
    onBack: () -> Unit,
    onIncidentClick: (String) -> Unit,
    viewModel: AdminIncidentsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_incidents)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && items.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items, key = { it.id }) { incident ->
                            IncidentRow(
                                incident = incident,
                                onClick = { onIncidentClick(incident.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("Incident detail") },
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
                    state.incident?.let { incident ->
                        item { IncidentDetailCard(incident) }
                    }
                    item {
                        Text(
                            text = "Occurrences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (state.occurrences.isEmpty()) {
                        item {
                            Text(
                                text = state.occurrencesError ?: stringResource(R.string.dashboard_no_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.occurrencesError == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    } else {
                        items(state.occurrences, key = { it.eventId }) { occurrence ->
                            OccurrenceRow(occurrence)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentRow(
    incident: AdminIncident,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(severityColor(incident.severity)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = incident.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(incident.status)
            }
            if (!incident.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = incident.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (incident.createdAt != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = incident.createdAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IncidentDetailCard(incident: AdminIncident) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(severityColor(incident.severity)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = incident.title.ifBlank { "Incident" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(incident.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            IncidentDetailRow("Severity", incident.severity)
            IncidentDetailRow("State", incident.state)
            IncidentDetailRow("Subject", listOf(incident.subjectType, incident.subjectId).filter { it.isNotBlank() }.joinToString(" · "))
            IncidentDetailRow("Count", incident.count.takeIf { it > 0 }?.toString().orEmpty())
            IncidentDetailRow("Created", incident.createdAt.orEmpty())
            if (!incident.description.isNullOrBlank()) {
                IncidentDetailRow("Description", incident.description)
            }
            IncidentDetailRow("Incident ID", incident.id)
        }
    }
}

@Composable
private fun OccurrenceRow(occurrence: IncidentOccurrence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = occurrence.eventId.ifBlank { "Occurrence" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            IncidentDetailRow("Actor", occurrence.actor.orEmpty())
            IncidentDetailRow("Door", occurrence.doorId.orEmpty())
            IncidentDetailRow("Gateway", occurrence.gatewayId.orEmpty())
            IncidentDetailRow("Result", occurrence.result)
            IncidentDetailRow("Time", occurrence.occurredAt)
            if (!occurrence.detail.isNullOrBlank()) {
                IncidentDetailRow("Detail", occurrence.detail)
            }
        }
    }
}

@Composable
private fun IncidentDetailRow(label: String, value: String) {
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

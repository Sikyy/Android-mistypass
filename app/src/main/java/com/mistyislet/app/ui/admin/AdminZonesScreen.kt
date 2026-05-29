package com.mistyislet.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mistyislet.app.domain.model.AdminZone
import com.mistyislet.app.domain.model.Holiday
import com.mistyislet.app.domain.model.HolidayRegion
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyLabeledContentRow
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminZonesViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminZone>>(emptyList())
    val items: StateFlow<List<AdminZone>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _detailState = MutableStateFlow(AdminZoneDetailDataState())
    val detailState: StateFlow<AdminZoneDetailDataState> = _detailState
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

    fun loadZoneDetail(zoneId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            _detailState.value = AdminZoneDetailDataState(isLoading = true)

            var zone: AdminZone? = null
            var zoneError: String? = null
            when (val result = adminRepository.getZone(pid, zoneId)) {
                is ApiResult.Success -> zone = result.data
                is ApiResult.Error -> zone = AdminDemoData.zones.firstOrNull { it.id == zoneId }
                is ApiResult.Exception -> zone = AdminDemoData.zones.firstOrNull { it.id == zoneId }
            }

            var regions = emptyList<HolidayRegion>()
            var regionsError: String? = null
            when (val result = adminRepository.getHolidayRegions(pid)) {
                is ApiResult.Success -> regions = result.data.ifEmpty { AdminDemoData.holidayRegions }
                is ApiResult.Error -> regions = AdminDemoData.holidayRegions
                is ApiResult.Exception -> regions = AdminDemoData.holidayRegions
            }

            val holidaysByRegion = mutableMapOf<String, List<Holiday>>()
            var holidaysError: String? = null
            regions.take(2).forEach { region ->
                if (region.id.isNotBlank()) {
                    when (val result = adminRepository.getHolidays(pid, region.id)) {
                        is ApiResult.Success -> holidaysByRegion[region.id] = result.data.ifEmpty {
                            AdminDemoData.holidaysByRegion[region.id].orEmpty()
                        }
                        is ApiResult.Error -> holidaysByRegion[region.id] = AdminDemoData.holidaysByRegion[region.id].orEmpty()
                        is ApiResult.Exception -> holidaysByRegion[region.id] = AdminDemoData.holidaysByRegion[region.id].orEmpty()
                    }
                }
            }

            _detailState.value = AdminZoneDetailDataState(
                zone = zone,
                holidayRegions = regions,
                holidaysByRegion = holidaysByRegion,
                isLoading = false,
                zoneError = zoneError,
                holidayRegionsError = regionsError,
                holidaysError = holidaysError,
            )
        }
    }

    fun clearZoneDetail() {
        _detailState.value = AdminZoneDetailDataState()
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = adminRepository.getZones(pid)) {
            is ApiResult.Success -> {
                _items.value = result.data.ifEmpty { AdminDemoData.zones }
                _error.value = null
            }
            is ApiResult.Error -> { _items.value = AdminDemoData.zones; _error.value = null }
            is ApiResult.Exception -> { _items.value = AdminDemoData.zones; _error.value = null }
        }
        _isLoading.value = false
    }
}

data class AdminZoneDetailDataState(
    val zone: AdminZone? = null,
    val holidayRegions: List<HolidayRegion> = emptyList(),
    val holidaysByRegion: Map<String, List<Holiday>> = emptyMap(),
    val isLoading: Boolean = false,
    val zoneError: String? = null,
    val holidayRegionsError: String? = null,
    val holidaysError: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminZonesScreen(
    onBack: () -> Unit,
    viewModel: AdminZonesViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    var selectedZone by remember { mutableStateOf<AdminZone?>(null) }

    selectedZone?.let { zone ->
        BackHandler {
            selectedZone = null
            viewModel.clearZoneDetail()
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                MistyNavigationTopBar(
                    title = zone.name,
                    onBack = {
                        selectedZone = null
                        viewModel.clearZoneDetail()
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                ZoneDetailSheet(
                    zone = zone,
                    detailState = detailState,
                    showHeader = false,
                )
            }
        }
        return
    }

    AdminListScreen(
        title = stringResource(R.string.dashboard_zones),
        items = items.map { zone ->
            AdminListItem(
                id = zone.id,
                title = zone.name,
                subtitle = listOfNotNull(
                    zone.description.ifBlank { null },
                    stringResource(R.string.admin_doors_count, zone.doorCount),
                ).joinToString(" · "),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        emptyIcon = Icons.Default.Map,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            selectedZone = items.find { it.id == item.id }
            selectedZone?.let { viewModel.loadZoneDetail(it.id) }
        },
    )

}

@Composable
private fun ZoneDetailSheet(
    zone: AdminZone,
    detailState: AdminZoneDetailDataState,
    showHeader: Boolean = true,
) {
    val displayZone = detailState.zone ?: zone

    LazyColumn(
        contentPadding = MistyGroupedListPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showHeader) {
            item {
                Column {
                    Text(
                        text = displayZone.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (displayZone.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = displayZone.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            MistyGroupedSection(title = "Zone") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (displayZone.status.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = zoneStatusColor(displayZone.status),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayZone.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = zoneStatusColor(displayZone.status),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(zoneStatusColor(displayZone.status).copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
                    MistyLabeledContentRow("Description", displayZone.description)
                    MistyLabeledContentRow("Doors", displayZone.doorCount.toString())
                    displayZone.cameraCount?.let { MistyLabeledContentRow("Cameras", it.toString()) }
                    displayZone.holidayRegionCount?.let { MistyLabeledContentRow("Holiday Regions", it.toString()) }
                    MistyLabeledContentRow("Created", displayZone.createdAt.orEmpty())
                    detailState.zoneError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        ZoneErrorText(it)
                    }
                }
            }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.admin_holiday_regions)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    when {
                        detailState.isLoading -> CircularProgressIndicator()
                        detailState.holidayRegionsError != null -> ZoneErrorText(detailState.holidayRegionsError)
                        detailState.holidayRegions.isEmpty() -> Text(
                            text = stringResource(R.string.admin_no_holiday_regions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> detailState.holidayRegions.forEachIndexed { index, region ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = region.name.ifBlank { region.countryCode ?: region.id },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                val metadata = listOfNotNull(
                                    region.countryCode,
                                    region.regionCode,
                                    region.timezone,
                                ).filter { it.isNotBlank() }.joinToString(" · ")
                                if (metadata.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = metadata,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (index < detailState.holidayRegions.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun zoneStatusColor(status: String): Color =
    if (status.equals("active", ignoreCase = true)) IosGreen else IosGray

@Composable
private fun ZoneInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun ZoneErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                is ApiResult.Error -> zoneError = result.message
                is ApiResult.Exception -> zoneError = result.throwable.localizedMessage
            }

            var regions = emptyList<HolidayRegion>()
            var regionsError: String? = null
            when (val result = adminRepository.getHolidayRegions(pid)) {
                is ApiResult.Success -> regions = result.data
                is ApiResult.Error -> regionsError = result.message
                is ApiResult.Exception -> regionsError = result.throwable.localizedMessage
            }

            val holidaysByRegion = mutableMapOf<String, List<Holiday>>()
            var holidaysError: String? = null
            regions.take(2).forEach { region ->
                if (region.id.isNotBlank()) {
                    when (val result = adminRepository.getHolidays(pid, region.id)) {
                        is ApiResult.Success -> holidaysByRegion[region.id] = result.data
                        is ApiResult.Error -> if (holidaysError == null) holidaysError = result.message
                        is ApiResult.Exception -> if (holidaysError == null) holidaysError = result.throwable.localizedMessage
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
                _items.value = result.data
                _error.value = null
            }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
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
    val sheetState = rememberModalBottomSheetState()

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
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            selectedZone = items.find { it.id == item.id }
            selectedZone?.let { viewModel.loadZoneDetail(it.id) }
        },
    )

    selectedZone?.let { zone ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedZone = null
                viewModel.clearZoneDetail()
            },
            sheetState = sheetState,
        ) {
            ZoneDetailSheet(zone = zone, detailState = detailState)
        }
    }
}

@Composable
private fun ZoneDetailSheet(
    zone: AdminZone,
    detailState: AdminZoneDetailDataState,
) {
    val displayZone = detailState.zone ?: zone

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
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
        Spacer(modifier = Modifier.height(16.dp))
        ZoneInfoRow(stringResource(R.string.admin_doors), stringResource(R.string.admin_doors_count, displayZone.doorCount))
        detailState.zoneError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            ZoneErrorText(it)
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.admin_holiday_regions),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            detailState.isLoading -> CircularProgressIndicator()
            detailState.holidayRegionsError != null -> ZoneErrorText(detailState.holidayRegionsError)
            detailState.holidayRegions.isEmpty() -> Text(
                text = stringResource(R.string.admin_no_holiday_regions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                detailState.holidayRegions.take(4).forEach { region ->
                    val holidays = detailState.holidaysByRegion[region.id].orEmpty()
                    ZoneInfoRow(
                        label = region.name.ifBlank { region.countryCode ?: region.id },
                        value = listOfNotNull(
                            region.countryCode,
                            if (holidays.isNotEmpty()) stringResource(R.string.admin_holidays_count, holidays.size) else null,
                        ).joinToString(" · ").ifBlank {
                            stringResource(R.string.admin_holidays_count, region.holidayCount)
                        },
                    )
                    holidays.take(2).forEach { holiday ->
                        Text(
                            text = "${holiday.date} · ${holiday.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                        )
                    }
                }
                detailState.holidaysError?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    ZoneErrorText(it)
                }
            }
        }
    }
}

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

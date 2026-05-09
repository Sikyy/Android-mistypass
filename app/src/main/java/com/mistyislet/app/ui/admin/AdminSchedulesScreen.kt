package com.mistyislet.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mistyislet.app.domain.model.AdminSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun scheduleTypeColor(type: String?) = when (type?.lowercase()) {
    "unlock" -> Color(0xFF35A853)
    "access_denial" -> Color(0xFFD93025)
    "first_to_arrive" -> Color(0xFFFF9800)
    "holiday" -> Color(0xFF9C27B0)
    else -> Color(0xFF4285F4)
}

private val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val weekends = listOf("Sat", "Sun")

@HiltViewModel
class AdminSchedulesViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminSchedule>>(emptyList())
    val items: StateFlow<List<AdminSchedule>> = _items
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

    fun createSchedule(schedule: AdminSchedule) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.createSchedule(pid, schedule)
            loadData()
        }
    }

    fun updateSchedule(schedule: AdminSchedule) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.updateSchedule(pid, schedule.id, schedule)
            loadData()
        }
    }

    fun deleteSchedule(scheduleId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.deleteSchedule(pid, scheduleId)
            loadData()
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = adminRepository.getSchedules(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSchedulesScreen(
    onBack: () -> Unit,
    viewModel: AdminSchedulesViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showFormSheet by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<AdminSchedule?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    AdminListScreen(
        title = stringResource(R.string.dashboard_schedules),
        items = items.map { schedule ->
            val timeRange = listOfNotNull(schedule.startTime, schedule.endTime).joinToString(" – ")
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val daysStr = if (schedule.daysOfWeek.isNotEmpty()) schedule.daysOfWeek.mapNotNull { dayNames.getOrNull(it) }.joinToString(", ") else null
            AdminListItem(
                id = schedule.id,
                title = schedule.name,
                subtitle = listOfNotNull(daysStr, timeRange.ifBlank { null }).joinToString(" · "),
                trailing = schedule.type?.replace("_", " ")?.replaceFirstChar { it.uppercase() },
                trailingColor = scheduleTypeColor(schedule.type),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            editingSchedule = items.find { it.id == item.id }
            showFormSheet = true
        },
        actions = {
            IconButton(onClick = { editingSchedule = null; showFormSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    )

    if (showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFormSheet = false; editingSchedule = null },
            sheetState = sheetState,
        ) {
            ScheduleFormSheet(
                existing = editingSchedule,
                onSave = { schedule ->
                    if (editingSchedule != null) {
                        viewModel.updateSchedule(schedule)
                    } else {
                        viewModel.createSchedule(schedule)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showFormSheet = false
                        editingSchedule = null
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showFormSheet = false
                        editingSchedule = null
                    }
                },
                onDelete = editingSchedule?.let { s ->
                    {
                        viewModel.deleteSchedule(s.id)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showFormSheet = false
                            editingSchedule = null
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleFormSheet(
    existing: AdminSchedule? = null,
    onSave: (AdminSchedule) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var selectedType by remember { mutableStateOf(existing?.type ?: "unlock") }
    var startTime by remember { mutableStateOf(existing?.startTime ?: "") }
    var endTime by remember { mutableStateOf(existing?.endTime ?: "") }
    val dayIndexToName = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val selectedDays = remember { mutableStateListOf<String>().also { it.addAll(existing?.daysOfWeek?.mapNotNull { d -> dayIndexToName.getOrNull(d) } ?: emptyList()) } }
    var showTypeMenu by remember { mutableStateOf(false) }

    val types = listOf("unlock", "access_denial", "first_to_arrive", "holiday")
    val typeLabels = mapOf(
        "unlock" to stringResource(R.string.admin_schedule_unlock),
        "access_denial" to stringResource(R.string.admin_schedule_denial),
        "first_to_arrive" to stringResource(R.string.admin_schedule_first),
        "holiday" to stringResource(R.string.admin_schedule_holiday),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = if (existing != null) stringResource(R.string.admin_edit) else stringResource(R.string.admin_create),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.admin_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.admin_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(stringResource(R.string.admin_type), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showTypeMenu = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(scheduleTypeColor(selectedType)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(typeLabels[selectedType] ?: selectedType)
            }
            DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(typeLabels[type] ?: type) },
                        onClick = { selectedType = type; showTypeMenu = false },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text(stringResource(R.string.admin_start_time)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("HH:MM") },
            )
            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                label = { Text(stringResource(R.string.admin_end_time)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("HH:MM") },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            allDays.forEach { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = {
                        if (day in selectedDays) selectedDays.remove(day) else selectedDays.add(day)
                    },
                    label = { Text(day) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { selectedDays.clear(); selectedDays.addAll(weekdays) }, label = { Text(stringResource(R.string.admin_weekdays)) })
            AssistChip(onClick = { selectedDays.clear(); selectedDays.addAll(weekends) }, label = { Text(stringResource(R.string.admin_weekends)) })
            AssistChip(onClick = { selectedDays.clear(); selectedDays.addAll(allDays) }, label = { Text(stringResource(R.string.admin_every_day)) })
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.admin_delete), color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
            }
            Button(
                onClick = {
                    onSave(
                        AdminSchedule(
                            id = existing?.id ?: "",
                            name = name,
                            description = description.ifBlank { null },
                            type = selectedType,
                            daysOfWeek = selectedDays.mapNotNull { dayIndexToName.indexOf(it).takeIf { i -> i >= 0 } },
                            startTime = startTime.ifBlank { null },
                            endTime = endTime.ifBlank { null },
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank(),
            ) {
                Text(if (existing != null) stringResource(R.string.admin_save) else stringResource(R.string.admin_create))
            }
        }
    }
}

package com.mistyislet.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AdminSchedule
import com.mistyislet.app.ui.components.MistyFormSheet
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyListRowHeight
import com.mistyislet.app.ui.components.MistyPickerSheet
import com.mistyislet.app.ui.components.MistyReadonlyField
import com.mistyislet.app.ui.components.MistyTopBarIconButton
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosPurple
import com.mistyislet.app.ui.theme.IosRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun scheduleTypeColor(type: String?) = when (type?.lowercase()) {
    "unlock" -> IosGreen
    "access_denial" -> IosRed
    "first_to_arrive" -> IosOrange
    "holiday" -> IosPurple
    else -> IosBlue
}

private val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val weekends = listOf("Sat", "Sun")

@HiltViewModel
class AdminSchedulesViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
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
        val state = demoFallback.resolveList(adminRepository.getSchedules(pid)) { AdminDemoData.schedules }
        _items.value = state.items
        _error.value = state.error
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
                leadingIcon = Icons.Default.CalendarMonth,
                leadingIconColor = scheduleTypeColor(schedule.type),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        emptyIcon = Icons.Default.CalendarMonth,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            editingSchedule = items.find { it.id == item.id }
            showFormSheet = true
        },
        actions = {
            MistyTopBarIconButton(
                icon = Icons.Default.Add,
                onClick = { editingSchedule = null; showFormSheet = true },
            )
        },
    )

    if (showFormSheet) {
        ScheduleFormSheet(
            existing = editingSchedule,
            onSave = { schedule ->
                if (editingSchedule != null) {
                    viewModel.updateSchedule(schedule)
                } else {
                    viewModel.createSchedule(schedule)
                }
                showFormSheet = false
                editingSchedule = null
            },
            onCancel = {
                showFormSheet = false
                editingSchedule = null
            },
            onDelete = editingSchedule?.let { s ->
                {
                    viewModel.deleteSchedule(s.id)
                    showFormSheet = false
                    editingSchedule = null
                }
            },
        )
    }
}

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
    var showTypePicker by remember { mutableStateOf(false) }

    val types = listOf("unlock", "access_denial", "first_to_arrive", "holiday")
    val typeLabels = mapOf(
        "unlock" to stringResource(R.string.admin_schedule_unlock),
        "access_denial" to stringResource(R.string.admin_schedule_denial),
        "first_to_arrive" to stringResource(R.string.admin_schedule_first),
        "holiday" to stringResource(R.string.admin_schedule_holiday),
    )

    MistyFormSheet(
        title = if (existing != null) stringResource(R.string.admin_edit) else stringResource(R.string.admin_create),
        cancelLabel = stringResource(R.string.cancel),
        confirmLabel = stringResource(R.string.admin_save),
        onCancel = onCancel,
        onConfirm = {
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
        confirmEnabled = name.isNotBlank(),
    ) {
        item {
            MistyGroupedSection(title = stringResource(R.string.schedule_details)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyFormTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(R.string.admin_name),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MistyFormTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = stringResource(R.string.admin_description),
                        singleLine = false,
                        minLines = 2,
                    )
                    if (existing == null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MistyReadonlyField(
                            value = typeLabels[selectedType] ?: selectedType,
                            label = stringResource(R.string.admin_type),
                            onClick = { showTypePicker = true },
                        )
                    }
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.schedule_time_range)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ScheduleTimeRow(
                        label = stringResource(R.string.admin_start_time),
                        value = startTime,
                        onValueChange = { startTime = it },
                        placeholder = "08:00",
                    )
                    HorizontalDivider()
                    ScheduleTimeRow(
                        label = stringResource(R.string.admin_end_time),
                        value = endTime,
                        onValueChange = { endTime = it },
                        placeholder = "18:00",
                    )
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.schedule_days)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allDays.forEach { day ->
                            val selected = day in selectedDays
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    )
                                    .clickable {
                                        if (selected) selectedDays.remove(day) else selectedDays.add(day)
                                    }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SchedulePresetButton(
                            text = stringResource(R.string.admin_weekdays),
                            onClick = { selectedDays.clear(); selectedDays.addAll(weekdays) },
                        )
                        SchedulePresetButton(
                            text = stringResource(R.string.admin_weekends),
                            onClick = { selectedDays.clear(); selectedDays.addAll(weekends) },
                        )
                        SchedulePresetButton(
                            text = stringResource(R.string.admin_every_day),
                            onClick = { selectedDays.clear(); selectedDays.addAll(allDays) },
                        )
                    }
                }
            }
        }
        if (onDelete != null) {
            item {
                MistyGroupedSection {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(stringResource(R.string.admin_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showTypePicker) {
        MistyPickerSheet(
            title = stringResource(R.string.admin_type),
            cancelLabel = stringResource(R.string.cancel),
            items = types,
            itemLabel = { type -> typeLabels[type] ?: type },
            isSelected = { type -> type == selectedType },
            onSelect = { type ->
                selectedType = type
                showTypePicker = false
            },
            onDismiss = { showTypePicker = false },
        )
    }
}

@Composable
private fun ScheduleTimeRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MistyListRowHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(top = 13.dp, bottom = 13.dp, end = 16.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .padding(top = 13.dp, bottom = 13.dp)
                .weight(0.45f),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun SchedulePresetButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

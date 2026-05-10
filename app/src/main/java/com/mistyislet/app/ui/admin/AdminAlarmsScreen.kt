package com.mistyislet.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.mistyislet.app.core.network.AlarmStreamManager
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.domain.model.Alarm
import com.mistyislet.app.domain.model.AlarmCalendarEntry
import com.mistyislet.app.domain.model.AlarmSchedule
import com.mistyislet.app.ui.admin.components.AdminTabPicker
import com.mistyislet.app.ui.admin.components.KpiItem
import com.mistyislet.app.ui.admin.components.SeverityDot
import com.mistyislet.app.ui.admin.components.StatusBadge
import com.mistyislet.app.ui.admin.components.StatusSummaryRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAlarmsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val alarmStreamManager: AlarmStreamManager,
) : ViewModel() {
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms
    private val _schedules = MutableStateFlow<List<AlarmSchedule>>(emptyList())
    val schedules: StateFlow<List<AlarmSchedule>> = _schedules
    private val _calendar = MutableStateFlow<List<AlarmCalendarEntry>>(emptyList())
    val calendar: StateFlow<List<AlarmCalendarEntry>> = _calendar
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            loadData()
            startStreaming()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData()
            _isRefreshing.value = false
        }
    }

    fun updateStatus(alarmId: String, status: String) {
        viewModelScope.launch {
            adminRepository.updateAlarmStatus(alarmId, status)
            loadData()
        }
    }

    fun startStreaming() {
        if (streamJob?.isActive == true) return
        streamJob = viewModelScope.launch {
            _isStreaming.value = true
            alarmStreamManager.alarmEvents().collect { alarm ->
                val current = _alarms.value.toMutableList()
                val index = current.indexOfFirst { it.id == alarm.id }
                if (index >= 0) {
                    current[index] = alarm
                } else {
                    current.add(0, alarm)
                }
                _alarms.value = current
            }
            _isStreaming.value = false
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _isStreaming.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }

    private suspend fun loadData() {
        when (val result = adminRepository.getAlarms()) {
            is ApiResult.Success -> { _alarms.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        when (val result = adminRepository.getAlarmSchedules()) {
            is ApiResult.Success -> _schedules.value = result.data
            else -> {}
        }
        when (val result = adminRepository.getAlarmCalendar()) {
            is ApiResult.Success -> _calendar.value = result.data
            else -> {}
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAlarmsScreen(
    onBack: () -> Unit,
    viewModel: AdminAlarmsViewModel = hiltViewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        viewModel.startStreaming()
        onDispose { viewModel.stopStreaming() }
    }

    val tabs = listOf(
        stringResource(R.string.alarm_open),
        stringResource(R.string.alarm_all),
        stringResource(R.string.alarm_schedules),
        stringResource(R.string.alarm_calendar),
    )

    val openAlarms = alarms.filter { it.isOpen }
    val critical = alarms.count { it.severity.lowercase() == "critical" }
    val high = alarms.count { it.severity.lowercase() == "high" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_alarms)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (isStreaming) {
                        Row(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF35A853).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF35A853)),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF35A853),
                            )
                        }
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
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            StatusSummaryRow(
                                items = listOf(
                                    KpiItem(openAlarms.size.toString(), stringResource(R.string.alarm_open), Color(0xFFD93025)),
                                    KpiItem(critical.toString(), stringResource(R.string.alarm_critical), Color(0xFF9C27B0)),
                                    KpiItem(high.toString(), stringResource(R.string.alarm_high), Color(0xFFFF9800)),
                                    KpiItem(alarms.size.toString(), stringResource(R.string.admin_total), Color(0xFF4285F4)),
                                ),
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            AdminTabPicker(tabs = tabs, selectedIndex = selectedTab, onTabSelected = { selectedTab = it })
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        when (selectedTab) {
                            0 -> {
                                if (openAlarms.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text(stringResource(R.string.alarm_all_clear), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    items(openAlarms, key = { it.id }) { alarm ->
                                        AlarmRow(alarm = alarm, onAction = { status -> viewModel.updateStatus(alarm.id, status) })
                                    }
                                }
                            }
                            1 -> {
                                items(alarms, key = { it.id }) { alarm ->
                                    AlarmRow(alarm = alarm, onAction = if (alarm.isOpen) { { status -> viewModel.updateStatus(alarm.id, status) } } else null)
                                }
                            }
                            2 -> {
                                items(schedules, key = { it.id }) { schedule ->
                                    AlarmScheduleRow(schedule)
                                }
                            }
                            3 -> {
                                if (calendar.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text(stringResource(R.string.dashboard_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    items(calendar, key = { it.id }) { entry ->
                                        AlarmCalendarRow(entry)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onAction: ((String) -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityDot(alarm.severity)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(alarm.type.ifBlank { alarm.id }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (alarm.location.isNotBlank()) {
                        Text(alarm.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(alarm.status)
            }
            if (!alarm.triggeredAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(alarm.triggeredAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAction("acknowledged") }) {
                        Text(stringResource(R.string.alarm_acknowledge), style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { onAction("resolved") }) {
                        Text(stringResource(R.string.alarm_resolve), style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { onAction("false_positive") }) {
                        Text(stringResource(R.string.alarm_false_positive), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmScheduleRow(schedule: AlarmSchedule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(schedule.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                StatusBadge(if (schedule.enabled) "active" else "disabled")
            }
            val timeRange = listOfNotNull(schedule.startTime, schedule.endTime).joinToString(" – ")
            if (timeRange.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(timeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (schedule.daysOfWeek.isNotEmpty()) {
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                Text(schedule.daysOfWeek.mapNotNull { dayNames.getOrNull(it) }.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AlarmCalendarRow(entry: AlarmCalendarEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${entry.alarmCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (entry.alarmCount > 0) Color(0xFFD93025) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.alarms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                entry.alarms.take(3).forEach { alarm ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SeverityDot(alarm.severity)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alarm.type.ifBlank { alarm.id },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        StatusBadge(alarm.status)
                    }
                }
                if (entry.alarms.size > 3) {
                    Text(
                        text = "+${entry.alarms.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

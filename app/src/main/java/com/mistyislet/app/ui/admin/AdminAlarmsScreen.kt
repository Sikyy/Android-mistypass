package com.mistyislet.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
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
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosPurple
import com.mistyislet.app.ui.theme.IosRed
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
            is ApiResult.Success -> { _alarms.value = result.data.ifEmpty { AdminDemoData.alarms }; _error.value = null }
            is ApiResult.Error -> { _alarms.value = AdminDemoData.alarms; _error.value = null }
            is ApiResult.Exception -> { _alarms.value = AdminDemoData.alarms; _error.value = null }
        }
        when (val result = adminRepository.getAlarmSchedules()) {
            is ApiResult.Success -> _schedules.value = result.data.ifEmpty { AdminDemoData.alarmSchedules }
            else -> _schedules.value = AdminDemoData.alarmSchedules
        }
        when (val result = adminRepository.getAlarmCalendar()) {
            is ApiResult.Success -> _calendar.value = result.data.ifEmpty { AdminDemoData.alarmCalendar }
            else -> _calendar.value = AdminDemoData.alarmCalendar
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

    DisposableEffect(Unit) {
        viewModel.startStreaming()
        onDispose { viewModel.stopStreaming() }
    }

    AdminAlarmsContent(
        alarms = alarms,
        schedules = schedules,
        calendar = calendar,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        isStreaming = isStreaming,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onUpdateStatus = viewModel::updateStatus,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminAlarmsContent(
    alarms: List<Alarm>,
    schedules: List<AlarmSchedule>,
    calendar: List<AlarmCalendarEntry>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isStreaming: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateStatus: (String, String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.dashboard_alarms),
                onBack = onBack,
                actions = {
                    if (isStreaming) {
                        Row(
                            modifier = Modifier
                                .padding(end = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(IosGreen),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = IosGreen,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        contentPadding = MistyGroupedListPadding,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                        item {
                            StatusSummaryRow(
                                items = listOf(
                                    KpiItem(openAlarms.size.toString(), stringResource(R.string.alarm_open), IosRed),
                                    KpiItem(critical.toString(), stringResource(R.string.alarm_critical), IosPurple),
                                    KpiItem(high.toString(), stringResource(R.string.alarm_high), IosOrange),
                                    KpiItem(alarms.size.toString(), stringResource(R.string.admin_total), MaterialTheme.colorScheme.onSurface),
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
                                        MistyEmptyState(
                                            icon = Icons.Default.VerifiedUser,
                                            title = stringResource(R.string.alarm_no_open),
                                            description = stringResource(R.string.alarm_all_clear),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp),
                                        )
                                    }
                                } else {
                                    items(openAlarms, key = { it.id }) { alarm ->
                                        AlarmRow(alarm = alarm, onAction = { status -> onUpdateStatus(alarm.id, status) })
                                    }
                                }
                            }
                            1 -> {
                                items(alarms, key = { it.id }) { alarm ->
                                    AlarmRow(alarm = alarm, onAction = if (alarm.isOpen) { { status -> onUpdateStatus(alarm.id, status) } } else null)
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
                                    // iOS groups the weekly calendar into day-of-week sections (0=Mo..6=Su).
                                    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                                    (0..6).forEach { day ->
                                        val dayEntries = calendar.filter { it.dayOfWeek == day }
                                        if (dayEntries.isNotEmpty()) {
                                            item(key = "cal-day-$day") {
                                                MistyGroupedSection(title = dayLabels.getOrNull(day) ?: "") {
                                                    dayEntries.forEachIndexed { idx, entry ->
                                                        AlarmCalendarRow(entry)
                                                        if (idx < dayEntries.lastIndex) {
                                                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
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
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onAction: ((String) -> Unit)?) {
    MistyCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityDot(alarm.severity)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(alarm.type.ifBlank { alarm.id }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(alarm.status)
            }
            val timeAgo = adminCompactRelativeTime(alarm.triggeredAt)
            if (alarm.location.isNotBlank() || timeAgo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (alarm.location.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = alarm.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (timeAgo.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MistyPillActionButton(
                        text = stringResource(R.string.alarm_acknowledge),
                        onClick = { onAction("acknowledged") },
                        icon = Icons.Outlined.FrontHand,
                        tint = IosBlue,
                    )
                    MistyPillActionButton(
                        text = stringResource(R.string.alarm_resolve),
                        onClick = { onAction("resolved") },
                        icon = Icons.Outlined.CheckCircle,
                        tint = IosGreen,
                    )
                    MistyPillActionButton(
                        text = stringResource(R.string.alarm_false_positive),
                        onClick = { onAction("false_positive") },
                        // iOS false-positive is a text-only, no-tint .bordered button → global teal accent.
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmScheduleRow(schedule: AlarmSchedule) {
    MistyCard {
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
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // iOS AlarmsView.dayLabel maps 0=Mo … 6=Su.
                    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    schedule.daysOfWeek.forEach { day ->
                        dayLabels.getOrNull(day)?.let { label ->
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            if (schedule.alarmTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule.alarmTypes.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AlarmCalendarRow(entry: AlarmCalendarEntry) {
    // Mirrors iOS calendarEntryRow: red rounded left bar + name + time range + alarm types.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(IosRed.copy(alpha = 0.7f)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${entry.startTime} – ${entry.endTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.alarmTypes.isNotEmpty()) {
                Text(
                    text = entry.alarmTypes.joinToString(", ") { type ->
                        type.replace("_", " ").replaceFirstChar { it.uppercase() }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

package com.mistyislet.app.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.UserPresenceRecord
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyInlineSectionLabel
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPickerSheet
import com.mistyislet.app.ui.components.MistySearchField
import com.mistyislet.app.ui.components.MistySegmentedControl
import com.mistyislet.app.ui.components.MistyTopBarIconButton
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminUserPresenceViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _items = MutableStateFlow<List<UserPresenceRecord>>(emptyList())
    val items: StateFlow<List<UserPresenceRecord>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _days = MutableStateFlow(30)
    val days: StateFlow<Int> = _days
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

    fun setDays(d: Int) {
        _days.value = d
        viewModelScope.launch { loadData() }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        val state = demoFallback.resolveList(adminRepository.getUserPresence(pid, _days.value)) { AdminDemoData.userPresenceRecords }
        _items.value = state.items
        _error.value = state.error
        _isLoading.value = false
    }
}

private enum class PresenceSort { DAYS_DESC, UNLOCKS_DESC, NAME_ASC, DAYS_ASC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserPresenceScreen(
    onBack: () -> Unit,
    viewModel: AdminUserPresenceViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val presenceDayOptions = listOf(7, 14, 30)
    var selectedPeriod by rememberSaveable { mutableIntStateOf(presenceDayOptions.indexOf(days).coerceAtLeast(0)) }
    var sortMode by rememberSaveable { mutableIntStateOf(0) }
    var showSortPicker by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val sortLabels = listOf(
        stringResource(R.string.admin_sort_days_desc),
        stringResource(R.string.admin_sort_unlocks_desc),
        stringResource(R.string.admin_sort_name_asc),
        stringResource(R.string.admin_sort_days_asc),
    )

    val filtered = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.presenceDisplayName().contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    val sorted = remember(filtered, sortMode) {
        when (PresenceSort.entries[sortMode]) {
            PresenceSort.DAYS_DESC -> filtered.sortedByDescending { it.daysPresent }
            PresenceSort.UNLOCKS_DESC -> filtered.sortedByDescending { it.totalUnlocks }
            PresenceSort.NAME_ASC -> filtered.sortedBy { it.presenceDisplayName().lowercase() }
            PresenceSort.DAYS_ASC -> filtered.sortedBy { it.daysPresent }
        }
    }

    val totalUsers = items.size
    val avgDays = if (items.isNotEmpty()) items.sumOf { it.daysPresent } / items.size else 0
    val totalUnlocks = items.sumOf { it.totalUnlocks }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.dashboard_user_presence),
                onBack = onBack,
                actions = {
                    MistyTopBarIconButton(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        onClick = { showSortPicker = true },
                    )
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
                } else if (error != null && items.isEmpty()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                } else {
                    LazyColumn(
                        contentPadding = MistyGroupedListPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Period picker
                        item {
                            MistySegmentedControl(
                                labels = presenceDayOptions.map { stringResource(R.string.analytics_last_n_days, it) },
                                selectedIndex = selectedPeriod,
                                onSelected = { index ->
                                    selectedPeriod = index
                                    viewModel.setDays(presenceDayOptions[index])
                                },
                            )
                        }

                        item {
                            MistySearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = stringResource(R.string.admin_search_users),
                            )
                        }

                        // KPI
                        item {
                            PresenceKpiGrid(
                                items = listOf(
                                    PresenceKpiItem(totalUsers.toString(), stringResource(R.string.analytics_unique_users), IosBlue),
                                    PresenceKpiItem(avgDays.toString(), stringResource(R.string.admin_avg_days), IosGreen),
                                    PresenceKpiItem(totalUnlocks.toString(), stringResource(R.string.admin_total_unlocks), IosOrange),
                                ),
                            )
                        }

                        // Weekday activity chart
                        item {
                            PresenceSectionCard(title = stringResource(R.string.presence_weekday_activity)) {
                                WeekdayActivityChart(items)
                            }
                        }

                        // Per-user heatmap
                        val heatmapUsers = sorted.take(15)
                        item {
                            PresenceSectionCard(title = stringResource(R.string.presence_user_heatmap)) {
                                if (heatmapUsers.any { !it.weekdayBreakdown.isNullOrEmpty() }) {
                                    UserHeatmapGrid(heatmapUsers)
                                } else {
                                    Text(
                                        text = stringResource(R.string.dashboard_no_data),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // User list
                        item {
                            PresenceSectionCard(title = stringResource(R.string.presence_user_details)) {
                                if (sorted.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.dashboard_no_data),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    PresenceTable(sorted, days = presenceDayOptions[selectedPeriod])
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    if (showSortPicker) {
        MistyPickerSheet(
            title = stringResource(R.string.doors_sort),
            cancelLabel = stringResource(R.string.cancel),
            items = sortLabels.indices.toList(),
            itemLabel = { index -> sortLabels[index] },
            isSelected = { index -> index == sortMode },
            onSelect = { index ->
                sortMode = index
                showSortPicker = false
            },
            onDismiss = { showSortPicker = false },
        )
    }
}

private data class PresenceKpiItem(
    val value: String,
    val label: String,
    val color: Color,
)

@Composable
private fun PresenceKpiGrid(items: List<PresenceKpiItem>) {
    MistyCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(item.color.copy(alpha = 0.06f))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 27.sp),
                        fontWeight = FontWeight.Bold,
                        color = item.color,
                        maxLines = 1,
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PresenceSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    MistyCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MistyInlineSectionLabel(text = title)
            content()
        }
    }
}

@Composable
private fun localizedWeekdayLabels(): List<String> = listOf(
    stringResource(R.string.analytics_day_mon),
    stringResource(R.string.analytics_day_tue),
    stringResource(R.string.analytics_day_wed),
    stringResource(R.string.analytics_day_thu),
    stringResource(R.string.analytics_day_fri),
    stringResource(R.string.analytics_day_sat),
    stringResource(R.string.analytics_day_sun),
)

@Composable
private fun PresenceTable(records: List<UserPresenceRecord>, days: Int) {
    Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.analytics_user),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.presence_days_col),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(42.dp),
                )
                Text(
                    text = stringResource(R.string.presence_unlocks_col),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(62.dp),
                )
                Text(
                    text = stringResource(R.string.presence_first_seen),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(54.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            records.forEachIndexed { index, record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.presenceDisplayName(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val secondary = record.presenceSecondaryText()
                        if (secondary.isNotBlank()) {
                            Text(
                                text = secondary,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = "${record.daysPresent}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = attendanceColor(record.daysPresent, days),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(42.dp),
                    )
                    Text(
                        text = "${record.totalUnlocks}",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(62.dp),
                    )
                    Text(
                        text = shortDateOnly(record.firstUnlock),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(54.dp),
                    )
                }
                if (index < records.lastIndex) {
                    HorizontalDivider()
                }
            }
    }
}

private fun attendanceColor(daysPresent: Int, selectedDays: Int): Color {
    val ratio = daysPresent.toFloat() / selectedDays.coerceAtLeast(1)
    return when {
        ratio >= 0.8f -> IosGreen
        ratio >= 0.5f -> IosOrange
        else -> IosRed
    }
}

private fun UserPresenceRecord.presenceDisplayName(): String = when {
    userName.isNotBlank() -> userName
    email.isNotBlank() -> email
    else -> id
}

private fun UserPresenceRecord.presenceSecondaryText(): String = when {
    userName.isNotBlank() && email.isNotBlank() -> email
    userName.isNotBlank() -> id
    email.isNotBlank() -> id.takeIf { it != email }.orEmpty()
    else -> id
}

private fun shortDateOnly(isoDate: String?): String {
    val date = isoDate?.takeIf { it.length >= 10 } ?: return "-"
    return date.take(10).takeLast(5)
}

@Composable
private fun PresenceUserRow(record: UserPresenceRecord) {
    MistyCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IosBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = record.userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = IosBlue,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = record.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${record.daysPresent}d",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        record.daysPresent >= 20 -> IosGreen
                        record.daysPresent >= 10 -> IosOrange
                        else -> IosRed
                    },
                )
                Text(
                    text = "${record.totalUnlocks} unlocks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeekdayActivityChart(records: List<UserPresenceRecord>) {
    val dayLabels = localizedWeekdayLabels()
    val totals = remember(records) {
        val result = IntArray(7)
        records.forEach { record ->
            record.weekdayBreakdown?.let { bd ->
                if (bd.size == 7) {
                    for (i in 0..6) result[i] += bd[i]
                }
            }
        }
        result.toList()
    }

    val maxVal = (totals.maxOrNull() ?: 1).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val weekend = IosOrange
    val textColor = MaterialTheme.colorScheme.onSurface

    if (totals.sum() == 0) {
        Text(
            text = stringResource(R.string.dashboard_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val barCount = 7
                val spacing = 12.dp.toPx()
                val barWidth = (size.width - spacing * (barCount - 1)) / barCount
                val chartHeight = size.height * 0.75f

                totals.forEachIndexed { i, value ->
                    val barHeight = (value.toFloat() / maxVal) * chartHeight
                    val x = i * (barWidth + spacing)
                    val color = if (i >= 5) weekend else primary
                    drawRoundRect(
                        color = color.copy(alpha = 0.7f),
                        topLeft = Offset(x, size.height - barHeight - 20.dp.toPx()),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                    val paint = android.graphics.Paint().apply {
                        this.color = textColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "$value",
                        x + barWidth / 2,
                        size.height - barHeight - 24.dp.toPx(),
                        paint,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                text = stringResource(R.string.presence_weekday_note),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UserHeatmapGrid(users: List<UserPresenceRecord>) {
    val dayLabels = localizedWeekdayLabels()
    val heatmapColor = IosGreen
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maxVal = remember(users) {
        users.maxOfOrNull { record ->
            record.weekdayBreakdown?.maxOrNull() ?: 0
        }?.coerceAtLeast(1) ?: 1
    }

    Column {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                // User names column
                Column(modifier = Modifier.width(70.dp).padding(top = 20.dp)) {
                    users.forEach { user ->
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = user.userName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = surfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                // Grid
                Column {
                    // Day labels
                    Row {
                        dayLabels.forEach { label ->
                            Box(
                                modifier = Modifier.size(width = 28.dp, height = 18.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = surfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }

                    // Cells
                    users.forEach { user ->
                        Row {
                            (0..6).forEach { day ->
                                val value = user.weekdayBreakdown?.getOrNull(day) ?: 0
                                val intensity = if (maxVal > 0) value.toFloat() / maxVal else 0f
                                Box(
                                    modifier = Modifier
                                        .size(width = 28.dp, height = 18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(heatmapColor.copy(alpha = 0.08f + intensity * 0.82f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (value > 0) {
                                        Text(
                                            text = "$value",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = if (intensity > 0.5f) Color.White else Color.Black,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            // Legend
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.analytics_less),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = surfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(heatmapColor.copy(alpha = 0.08f + level * 0.82f)),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = stringResource(R.string.analytics_more),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = surfaceVariant,
                )
            }
    }
}

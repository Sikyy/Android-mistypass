package com.mistyislet.app.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.UserPresenceRecord
import com.mistyislet.app.ui.admin.components.KpiItem
import com.mistyislet.app.ui.admin.components.StatusSummaryRow
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
        when (val result = adminRepository.getUserPresence(pid, _days.value)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

private enum class PresenceSort { DAYS_DESC, UNLOCKS_DESC, NAME_ASC, DAYS_ASC }
private val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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
    var showSortMenu by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filtered = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.userName.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    val sorted = remember(filtered, sortMode) {
        when (PresenceSort.entries[sortMode]) {
            PresenceSort.DAYS_DESC -> filtered.sortedByDescending { it.daysPresent }
            PresenceSort.UNLOCKS_DESC -> filtered.sortedByDescending { it.totalUnlocks }
            PresenceSort.NAME_ASC -> filtered.sortedBy { it.userName.lowercase() }
            PresenceSort.DAYS_ASC -> filtered.sortedBy { it.daysPresent }
        }
    }

    val totalUsers = items.size
    val avgDays = if (items.isNotEmpty()) items.sumOf { it.daysPresent } / items.size else 0
    val totalUnlocks = items.sumOf { it.totalUnlocks }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_user_presence)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_sort_days_desc)) },
                            onClick = { sortMode = 0; showSortMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_sort_unlocks_desc)) },
                            onClick = { sortMode = 1; showSortMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_sort_name_asc)) },
                            onClick = { sortMode = 2; showSortMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_sort_days_asc)) },
                            onClick = { sortMode = 3; showSortMenu = false },
                        )
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
                } else if (error != null && items.isEmpty()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Period picker
                        item {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                presenceDayOptions.forEachIndexed { index, d ->
                                    SegmentedButton(
                                        selected = selectedPeriod == index,
                                        onClick = {
                                            selectedPeriod = index
                                            viewModel.setDays(d)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index, presenceDayOptions.size),
                                    ) {
                                        Text("${d}d")
                                    }
                                }
                            }
                        }

                        // KPI
                        item {
                            StatusSummaryRow(
                                items = listOf(
                                    KpiItem(totalUsers.toString(), stringResource(R.string.analytics_unique_users), Color(0xFF4285F4)),
                                    KpiItem(avgDays.toString(), stringResource(R.string.admin_avg_days), Color(0xFF35A853)),
                                    KpiItem(totalUnlocks.toString(), stringResource(R.string.admin_total_unlocks), Color(0xFFFF9800)),
                                ),
                            )
                        }

                        // Search
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.admin_search_presence)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Weekday activity chart
                        if (items.any { !it.weekdayBreakdown.isNullOrEmpty() }) {
                            item {
                                Text(
                                    text = stringResource(R.string.presence_weekday_activity),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                WeekdayActivityChart(items)
                            }
                        }

                        // Per-user heatmap
                        val usersWithBreakdown = sorted.filter { !it.weekdayBreakdown.isNullOrEmpty() }.take(15)
                        if (usersWithBreakdown.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.presence_user_heatmap),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                UserHeatmapGrid(usersWithBreakdown)
                            }
                        }

                        // User list
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.presence_users_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (sorted.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.dashboard_no_data),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(sorted, key = { it.id }) { record ->
                                PresenceUserRow(record)
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresenceUserRow(record: UserPresenceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
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
                    .background(Color(0xFF4285F4).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = record.userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF4285F4),
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
                        record.daysPresent >= 20 -> Color(0xFF35A853)
                        record.daysPresent >= 10 -> Color(0xFFFF9800)
                        else -> Color(0xFFD93025)
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
    val weekend = Color(0xFFFF9800)
    val textColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        this.color = textColor.hashCode()
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
                weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun UserHeatmapGrid(users: List<UserPresenceRecord>) {
    val heatmapColor = Color(0xFF35A853)
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maxVal = remember(users) {
        users.maxOfOrNull { record ->
            record.weekdayBreakdown?.maxOrNull() ?: 0
        }?.coerceAtLeast(1) ?: 1
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        weekdayLabels.forEach { label ->
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
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.mistyislet.app.domain.model.AnalyticsSummary
import com.mistyislet.app.domain.model.DailyTrendPoint
import com.mistyislet.app.domain.model.FailedAttemptEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _summary = MutableStateFlow<AnalyticsSummary?>(null)
    val summary: StateFlow<AnalyticsSummary?> = _summary
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _days = MutableStateFlow(30)
    val days: StateFlow<Int> = _days
    private val _failedAttempts = MutableStateFlow<List<FailedAttemptEvent>>(emptyList())
    val failedAttempts: StateFlow<List<FailedAttemptEvent>> = _failedAttempts
    private var placeId: String? = null

    init {
        viewModelScope.launch {
            placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            loadData()
        }
    }

    fun setDays(d: Int) {
        _days.value = d
        viewModelScope.launch { loadData() }
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
        when (val result = adminRepository.getAnalyticsSummary(pid, _days.value)) {
            is ApiResult.Success -> { _summary.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        when (val result = adminRepository.getFailedAttempts(pid, _days.value)) {
            is ApiResult.Success -> _failedAttempts.value = result.data
            else -> {}
        }
        _isLoading.value = false
    }
}

private val dayOptions = listOf(7, 14, 30)
private val methodColors = mapOf(
    "mobile" to Color(0xFF4285F4),
    "ble" to Color(0xFF00BCD4),
    "card" to Color(0xFFFF9800),
    "pin" to Color(0xFF9C27B0),
    "qr" to Color(0xFF35A853),
    "visitor" to Color(0xFFE91E63),
    "remote" to Color(0xFF5C6BC0),
)
private val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AdminAnalyticsViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val failedAttempts by viewModel.failedAttempts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    var selectedPeriod by rememberSaveable { mutableIntStateOf(dayOptions.indexOf(days).coerceAtLeast(0)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_analytics)) },
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
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (summary == null) {
                    Text(
                        text = error ?: stringResource(R.string.dashboard_no_data),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    val data = summary!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Period picker
                        item {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                dayOptions.forEachIndexed { index, d ->
                                    SegmentedButton(
                                        selected = selectedPeriod == index,
                                        onClick = {
                                            selectedPeriod = index
                                            viewModel.setDays(d)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index, dayOptions.size),
                                    ) {
                                        Text("${d}d")
                                    }
                                }
                            }
                        }

                        // KPI cards
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                KpiCard(
                                    value = "${data.totalUnlocks}",
                                    label = stringResource(R.string.analytics_total_unlocks),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                KpiCard(
                                    value = "${data.uniqueUsers}",
                                    label = stringResource(R.string.analytics_unique_users),
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                KpiCard(
                                    value = "${data.failedAttempts}",
                                    label = stringResource(R.string.analytics_failed_attempts),
                                    color = Color(0xFFD93025),
                                    modifier = Modifier.weight(1f),
                                )
                                KpiCard(
                                    value = String.format("%.1f", data.avgDailyUnlocks),
                                    label = stringResource(R.string.analytics_daily_avg),
                                    color = Color(0xFF9C27B0),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // Daily trend bar chart
                        if (data.dailyTrend.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_daily_trend))
                            }
                            item { DailyTrendChart(data.dailyTrend) }
                        }

                        // Unlock heatmap (7 days × 24 hours)
                        if (!data.heatmap.isNullOrEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_heatmap))
                            }
                            item { UnlockHeatmap(data.heatmap!!) }
                        }

                        // Donut chart for unlock methods
                        if (data.unlocksByMethod.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_unlock_methods))
                            }
                            item { UnlockMethodsDonut(data.unlocksByMethod, data.totalUnlocks) }
                        }

                        // Weekly users bar chart
                        if (!data.weeklyUsers.isNullOrEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_weekly_users))
                            }
                            item { WeeklyUsersChart(data.weeklyUsers!!) }
                        }

                        // Top doors
                        if (data.topDoors.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_top_doors))
                            }
                            val maxCount = data.topDoors.maxOf { it.count }
                            data.topDoors.take(5).forEachIndexed { index, door ->
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = door.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                Text(
                                                    text = "${door.count}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val fraction = if (maxCount > 0) door.count.toFloat() / maxCount else 0f
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction)
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(MaterialTheme.colorScheme.primary),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Failed attempts table
                        if (failedAttempts.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_failed_attempts))
                            }
                            item { FailedAttemptsTable(failedAttempts.take(10)) }
                        }

                        // Daily trend table
                        if (data.dailyTrend.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_trend_table))
                            }
                            item { DailyTrendTable(data.dailyTrend) }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun KpiCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyTrendChart(points: List<DailyTrendPoint>) {
    val primary = MaterialTheme.colorScheme.primary
    val maxVal = (points.maxOfOrNull { it.unlocks } ?: 1).coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(16.dp),
        ) {
            val barCount = points.size
            if (barCount == 0) return@Canvas
            val spacing = 4.dp.toPx()
            val barWidth = ((size.width - spacing * (barCount - 1)) / barCount).coerceAtLeast(2f)

            points.forEachIndexed { i, point ->
                val barHeight = (point.unlocks.toFloat() / maxVal) * size.height * 0.85f
                val x = i * (barWidth + spacing)
                drawRoundRect(
                    color = primary.copy(alpha = 0.7f),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labels = if (points.size <= 7) points else listOf(points.first(), points[points.size / 2], points.last())
            labels.forEach { p ->
                Text(
                    text = p.date.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UnlockHeatmap(cells: List<com.mistyislet.app.domain.model.HeatmapCell>) {
    val primary = MaterialTheme.colorScheme.primary
    val maxVal = (cells.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                // Day labels column
                Column(modifier = Modifier.padding(end = 4.dp, top = 18.dp)) {
                    weekdayLabels.forEach { label ->
                        Box(
                            modifier = Modifier.size(width = 28.dp, height = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = surfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                // Heatmap grid
                Column {
                    // Hour labels
                    Row {
                        (0..23).forEach { h ->
                            Box(
                                modifier = Modifier.size(width = 14.dp, height = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (h % 3 == 0) {
                                    Text(
                                        text = "$h",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                        color = surfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    // Grid cells
                    (0..6).forEach { day ->
                        Row {
                            (0..23).forEach { hour ->
                                val value = cells.find { it.dayOfWeek == day && it.hour == hour }?.value ?: 0
                                val intensity = if (maxVal > 0) value.toFloat() / maxVal else 0f
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(primary.copy(alpha = 0.08f + intensity * 0.82f)),
                                )
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
                            .background(primary.copy(alpha = 0.08f + level * 0.82f)),
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

@Composable
private fun UnlockMethodsDonut(
    methods: List<com.mistyislet.app.domain.model.MethodCount>,
    total: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 32.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    var startAngle = -90f
                    methods.forEach { method ->
                        val sweep = if (total > 0) (method.count.toFloat() / total) * 360f else 0f
                        val color = methodColors[method.method.lowercase()] ?: Color(0xFF9E9E9E)
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.analytics_total_unlocks),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            methods.forEach { method ->
                val color = methodColors[method.method.lowercase()] ?: Color(0xFF9E9E9E)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = method.method.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${method.count}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (total > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(method.count * 100 / total)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyUsersChart(points: List<com.mistyislet.app.domain.model.WeeklyUserPoint>) {
    val primary = Color(0xFF4285F4)
    val maxVal = (points.maxOfOrNull { it.uniqueUsers } ?: 1).coerceAtLeast(1)
    val textColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(16.dp),
        ) {
            val barCount = points.size
            if (barCount == 0) return@Canvas
            val spacing = 8.dp.toPx()
            val barWidth = ((size.width - spacing * (barCount - 1)) / barCount).coerceAtLeast(4f)

            points.forEachIndexed { i, point ->
                val barHeight = (point.uniqueUsers.toFloat() / maxVal) * size.height * 0.8f
                val x = i * (barWidth + spacing)
                drawRoundRect(
                    color = primary.copy(alpha = 0.7f),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                )
                if (barCount <= 8) {
                    val paint = android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${point.uniqueUsers}",
                        x + barWidth / 2,
                        size.height - barHeight - 4.dp.toPx(),
                        paint,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labels = if (points.size <= 6) points else listOf(points.first(), points.last())
            labels.forEach { p ->
                Text(
                    text = p.weekStart.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DailyTrendTable(points: List<DailyTrendPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.analytics_date),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.2f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.analytics_total_unlocks),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.analytics_unique_users),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.analytics_failed_short),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.7f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            points.take(7).forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                ) {
                    Text(
                        text = point.date.takeLast(5),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${point.unlocks}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${point.uniqueUsers}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${point.failed}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.7f),
                        color = if (point.failed > 0) Color(0xFFD93025) else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun FailedAttemptsTable(events: List<FailedAttemptEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.analytics_user),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.analytics_door),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.analytics_reason),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.8f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            events.forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                ) {
                    Text(
                        text = event.userName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = event.doorName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = event.reason,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.8f),
                        color = Color(0xFFD93025),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

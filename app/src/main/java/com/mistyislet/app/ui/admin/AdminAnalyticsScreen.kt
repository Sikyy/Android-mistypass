package com.mistyislet.app.ui.admin

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.People
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.mistyislet.app.domain.model.TopDoor
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyInlineSectionLabel
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistySegmentedControl
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosCyan
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosIndigo
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosPink
import com.mistyislet.app.ui.theme.IosPurple
import com.mistyislet.app.ui.theme.IosRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.FormatStyle
import java.util.Locale
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
    private val _placeName = MutableStateFlow("")
    val placeName: StateFlow<String> = _placeName
    private var placeId: String? = null

    init {
        viewModelScope.launch {
            val scope = selectedPlaceRepository.scope.first()
            placeId = scope.placeId ?: return@launch
            _placeName.value = scope.placeName.orEmpty()
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
            is ApiResult.Success -> {
                _summary.value = if (result.data.isEmptyAnalytics()) AdminDemoData.analyticsSummary(_days.value) else result.data
                _error.value = null
            }
            is ApiResult.Error -> { _summary.value = AdminDemoData.analyticsSummary(_days.value); _error.value = null }
            is ApiResult.Exception -> { _summary.value = AdminDemoData.analyticsSummary(_days.value); _error.value = null }
        }
        when (val result = adminRepository.getFailedAttempts(pid, _days.value)) {
            is ApiResult.Success -> _failedAttempts.value = result.data.ifEmpty { AdminDemoData.failedAttempts() }
            else -> _failedAttempts.value = AdminDemoData.failedAttempts()
        }
        _isLoading.value = false
    }
}

private fun AnalyticsSummary.isEmptyAnalytics(): Boolean =
    totalUnlocks == 0 &&
        uniqueUsers == 0 &&
        failedAttempts == 0 &&
        dailyTrend.isEmpty() &&
        unlocksByMethod.isEmpty() &&
        topDoors.isEmpty() &&
        heatmap.isNullOrEmpty() &&
        weeklyUsers.isNullOrEmpty()

private val dayOptions = listOf(7, 14, 30)
private val methodColors = mapOf(
    "mobile" to IosBlue,
    "ble" to IosCyan,
    "card" to IosOrange,
    "pin" to IosPurple,
    "qr" to IosGreen,
    "visitor" to IosPink,
    "remote" to IosIndigo,
)

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
    val placeName by viewModel.placeName.collectAsStateWithLifecycle()
    var selectedPeriod by rememberSaveable { mutableIntStateOf(dayOptions.indexOf(days).coerceAtLeast(0)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.dashboard_analytics),
                onBack = onBack,
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
                    val dailyTrend = remember(data.dailyTrend) { data.dailyTrend.sortedBy { it.date } }
                    val weeklyUsers = remember(data.weeklyUsers) { data.weeklyUsers?.sortedBy { it.weekStart } }
                    LazyColumn(
                        contentPadding = MistyGroupedListPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            ReportHeader(
                                placeName = placeName,
                                days = dayOptions[selectedPeriod],
                            )
                        }

                        // Period picker
                        item {
                            MistySegmentedControl(
                                labels = dayOptions.map { stringResource(R.string.analytics_last_n_days, it) },
                                selectedIndex = selectedPeriod,
                                onSelected = { index ->
                                    selectedPeriod = index
                                    viewModel.setDays(dayOptions[index])
                                },
                            )
                        }

                        // KPI cards
                        item { AnalyticsKpiGrid(data) }

                        // Daily trend bar chart
                        if (dailyTrend.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_daily_trend))
                            }
                            item { DailyTrendChart(dailyTrend) }
                            item { DailyTrendTable(dailyTrend) }
                        }

                        // Unlock heatmap (7 days × 24 hours)
                        if (!data.heatmap.isNullOrEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_heatmap))
                            }
                            item { UnlockHeatmap(data.heatmap!!) }
                        }

                        // Weekly users bar chart
                        if (!weeklyUsers.isNullOrEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_weekly_users))
                            }
                            item { WeeklyUsersChart(weeklyUsers) }
                        }

                        // Donut chart for unlock methods
                        if (data.unlocksByMethod.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_unlock_methods))
                            }
                            item { UnlockMethodsDonut(data.unlocksByMethod) }
                        }

                        // Top doors
                        if (data.topDoors.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.analytics_top_doors))
                            }
                            item { TopDoorsSection(data.topDoors.take(5)) }
                        }

                        // Failed attempts table
                        item {
                            SectionTitle(stringResource(R.string.analytics_recent_failed))
                        }
                        item { FailedAttemptsTable(failedAttempts.take(10)) }

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
    MistyInlineSectionLabel(text = text)
}

@Composable
private fun ReportHeader(placeName: String, days: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = placeName.ifBlank { stringResource(R.string.admin_scope_place) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = analyticsDateRange(days),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun analyticsDateRange(days: Int): String {
    val to = java.time.LocalDate.now()
    val from = to.minusDays(days.toLong())
    val formatter = java.time.format.DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
    return "${from.format(formatter)} - ${to.format(formatter)}"
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

private fun analyticsRelativeTime(timestamp: String): String {
    val epochMillis = runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrNull()
        ?: return timestamp.take(16)
    return DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

@Composable
private fun AnalyticsKpiGrid(data: AnalyticsSummary) {
    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiTile(
                    value = "${data.totalUnlocks}",
                    label = stringResource(R.string.analytics_total_unlocks),
                    icon = Icons.Outlined.LockOpen,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    value = "${data.uniqueUsers}",
                    label = stringResource(R.string.analytics_unique_users),
                    icon = Icons.Outlined.People,
                    color = IosBlue,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiTile(
                    value = "${data.failedAttempts}",
                    label = stringResource(R.string.analytics_failed_attempts),
                    icon = Icons.Outlined.GppMaybe,
                    color = IosRed,
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    value = String.format("%.1f", data.avgDailyUnlocks),
                    label = stringResource(R.string.analytics_daily_avg),
                    icon = Icons.AutoMirrored.Outlined.ShowChart,
                    color = IosPurple,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KpiTile(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KpiCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    MistyCard(
        modifier = modifier,
        containerColor = color.copy(alpha = 0.08f),
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

    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
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
    val dayLabels = localizedWeekdayLabels()
    val maxVal = (cells.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                // Day labels column
                Column(modifier = Modifier.padding(end = 4.dp, top = 18.dp)) {
                    dayLabels.forEach { label ->
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
) {
    val total = methods.sumOf { it.count }.coerceAtLeast(0)
    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 24.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    var startAngle = -90f
                    methods.forEach { method ->
                        val sweep = if (total > 0) (method.count.toFloat() / total) * 360f else 0f
                        val color = methodColors[method.method.lowercase()] ?: IosGray
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.analytics_method_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                methods.forEach { method ->
                    val color = methodColors[method.method.lowercase()] ?: IosGray
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method.method.replace("_", " ").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${method.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
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
    val primary = IosBlue
    val maxVal = (points.maxOfOrNull { it.uniqueUsers } ?: 1).coerceAtLeast(1)
    val textColor = MaterialTheme.colorScheme.onSurface

    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
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
                        color = textColor.toArgb()
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
private fun TopDoorsSection(doors: List<TopDoor>) {
    val maxCount = doors.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "",
                    modifier = Modifier.width(20.dp),
                )
                Text(
                    text = stringResource(R.string.analytics_door),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.presence_unlocks_col),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(72.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            doors.forEachIndexed { index, door ->
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(20.dp),
                        )
                        Text(
                            text = door.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${door.count}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(72.dp),
                        )
                    }
                    val fraction = door.count.toFloat() / maxCount
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
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
                if (index < doors.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyTrendTable(points: List<DailyTrendPoint>) {
    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    text = stringResource(R.string.analytics_users_short),
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
            points.takeLast(7).forEach { point ->
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
                        color = if (point.failed > 0) IosRed else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun FailedAttemptsTable(events: List<FailedAttemptEvent>) {
    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (events.isEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_no_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.history_time),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.15f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.analytics_user),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.9f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.analytics_door),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.95f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 6.dp, bottom = 3.dp))
                events.forEachIndexed { index, event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) {
                        Text(
                            text = analyticsRelativeTime(event.timestamp).ifBlank { "-" },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.15f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = event.userName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = event.doorName,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.95f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (index < events.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

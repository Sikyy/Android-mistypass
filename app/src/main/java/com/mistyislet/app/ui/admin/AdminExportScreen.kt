package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.mistyislet.app.domain.model.ReportExportRequest
import com.mistyislet.app.domain.model.ReportExportResponse
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyDatePickerDialog
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPickerSheet
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.components.MistySegmentedControl
import com.mistyislet.app.ui.theme.IosGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

private data class ReportType(val key: String, val labelResId: Int, val descriptionResId: Int)

private val reportTypes = listOf(
    ReportType("weekly_analytics", R.string.export_weekly_analytics, R.string.export_desc_weekly),
    ReportType("events", R.string.export_events, R.string.export_desc_events),
    ReportType("unlock_stats", R.string.export_unlock_stats, R.string.export_desc_unlock_stats),
    ReportType("user_presence", R.string.export_user_presence, R.string.export_desc_user_presence),
    ReportType("incidents", R.string.export_incidents, R.string.export_desc_incidents),
    ReportType("hardware_summary", R.string.export_hardware, R.string.export_desc_hardware),
)

private data class DatePreset(val labelResId: Int, val days: Int)

@HiltViewModel
class AdminExportViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting
    private val _result = MutableStateFlow<ReportExportResponse?>(null)
    val result: StateFlow<ReportExportResponse?> = _result
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun export(type: String, format: String, from: String, to: String) {
        viewModelScope.launch {
            val placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            _isExporting.value = true
            _error.value = null
            _result.value = null
            val request = ReportExportRequest(
                type = type,
                from = from,
                to = to,
                format = format,
            )
            when (val result = adminRepository.exportReport(placeId, request)) {
                is ApiResult.Success -> _result.value = result.data
                is ApiResult.Error -> _error.value = result.message
                is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
            }
            _isExporting.value = false
        }
    }
}

@Composable
private fun ExportSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MistyCard {
            Column(content = content)
        }
    }
}

@Composable
private fun ExportValueRow(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Default.UnfoldMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExportDateRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExportResultCard(res: ReportExportResponse) {
    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = IosGreen.copy(alpha = 0.40f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = IosGreen,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    stringResource(R.string.analytics_export_ready),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = IosGreen,
                )
                Text(
                    res.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                res.expiresAt?.let {
                    Text(
                        stringResource(R.string.admin_expires, it.take(16)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportErrorCard(err: String) {
    MistyCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.40f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
            Text(
                err,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExportScreen(
    onBack: () -> Unit,
    viewModel: AdminExportViewModel = hiltViewModel(),
) {
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedType by remember { mutableIntStateOf(0) }
    var selectedFormat by remember { mutableIntStateOf(1) }
    var showTypePicker by remember { mutableStateOf(false) }
    val formats = listOf("CSV", "PDF")
    val reportLabels = reportTypes.map { stringResource(it.labelResId) }
    val reportDescriptions = reportTypes.map { stringResource(it.descriptionResId) }
    val presets = listOf(
        DatePreset(R.string.export_preset_7d, 7),
        DatePreset(R.string.export_preset_14d, 14),
        DatePreset(R.string.export_preset_30d, 30),
        DatePreset(R.string.export_preset_90d, 90),
    )

    val today = java.time.LocalDate.now()
    var fromDate by remember { mutableStateOf(today.minusDays(30)) }
    var toDate by remember { mutableStateOf(today) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val dateFormatter = remember {
        java.time.format.DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.dashboard_export_events),
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = MistyGroupedListPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ExportSection(title = stringResource(R.string.export_report_type)) {
                        ExportValueRow(
                            title = reportLabels[selectedType],
                            subtitle = reportDescriptions[selectedType],
                            leadingIcon = Icons.AutoMirrored.Outlined.ShowChart,
                            onClick = { showTypePicker = true },
                        )
                    }
                }

                item {
                    ExportSection(title = stringResource(R.string.export_date_range)) {
                        ExportDateRow(
                            label = stringResource(R.string.export_from),
                            value = fromDate.format(dateFormatter),
                            onClick = { showFromPicker = true },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ExportDateRow(
                            label = stringResource(R.string.export_to),
                            value = toDate.format(dateFormatter),
                            onClick = { showToPicker = true },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets.size) { index ->
                            val preset = presets[index]
                            val isSelected = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate).toInt() == preset.days
                            Text(
                                text = stringResource(preset.labelResId),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    )
                                    .border(
                                        width = 0.7.dp,
                                        color = if (isSelected) {
                                            Color.Transparent
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                    )
                                    .clickable {
                                        toDate = today
                                        fromDate = today.minusDays(preset.days.toLong())
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                item {
                    ExportSection(title = stringResource(R.string.analytics_format)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            MistySegmentedControl(
                                labels = formats,
                                selectedIndex = selectedFormat,
                                onSelected = { selectedFormat = it },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (formats[selectedFormat] == "PDF") {
                                    stringResource(R.string.export_pdf_note)
                                } else {
                                    stringResource(R.string.export_csv_note)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    MistyPillActionButton(
                        text = stringResource(R.string.analytics_export),
                        onClick = { viewModel.export(reportTypes[selectedType].key, formats[selectedFormat].lowercase(), fromDate.toString(), toDate.toString()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isExporting,
                        isLoading = isExporting,
                        fillContent = true,
                        tint = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                result?.let { res ->
                    item { ExportResultCard(res) }
                }

                error?.let { err ->
                    item { ExportErrorCard(err) }
                }
            }

            if (showFromPicker) {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = fromDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                )
                MistyDatePickerDialog(
                    state = state,
                    confirmLabel = stringResource(R.string.ok),
                    dismissLabel = stringResource(R.string.cancel),
                    onDismissRequest = { showFromPicker = false },
                    onConfirm = {
                        state.selectedDateMillis?.let { millis ->
                            fromDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                            if (fromDate.isAfter(toDate)) toDate = fromDate
                        }
                        showFromPicker = false
                    },
                )
            }
            if (showToPicker) {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = toDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                )
                MistyDatePickerDialog(
                    state = state,
                    confirmLabel = stringResource(R.string.ok),
                    dismissLabel = stringResource(R.string.cancel),
                    onDismissRequest = { showToPicker = false },
                    onConfirm = {
                        state.selectedDateMillis?.let { millis ->
                            toDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                            if (toDate.isBefore(fromDate)) fromDate = toDate
                        }
                        showToPicker = false
                    },
                )
            }
        }
    }

    if (showTypePicker) {
        MistyPickerSheet(
            title = stringResource(R.string.export_report_type),
            cancelLabel = stringResource(R.string.cancel),
            items = reportTypes.indices.toList(),
            itemLabel = { index -> reportLabels[index] },
            itemDetail = { index -> reportDescriptions[index] },
            isSelected = { index -> index == selectedType },
            onSelect = { index ->
                selectedType = index
                showTypePicker = false
            },
            onDismiss = { showTypePicker = false },
        )
    }
}

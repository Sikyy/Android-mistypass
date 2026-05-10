package com.mistyislet.app.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.domain.model.Booking
import com.mistyislet.app.domain.model.BookingSpace
import com.mistyislet.app.domain.model.BookingSpaceStatus
import com.mistyislet.app.domain.model.CreateBookingRequest
import com.mistyislet.app.ui.admin.components.AdminTabPicker
import com.mistyislet.app.ui.admin.components.StatusBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminBookingsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings
    private val _spaces = MutableStateFlow<List<BookingSpace>>(emptyList())
    val spaces: StateFlow<List<BookingSpace>> = _spaces
    private val _spaceStatuses = MutableStateFlow<Map<String, BookingSpaceStatus>>(emptyMap())
    val spaceStatuses: StateFlow<Map<String, BookingSpaceStatus>> = _spaceStatuses
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch { loadData() }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData()
            _isRefreshing.value = false
        }
    }

    fun createBooking(spaceId: String, title: String?, startTime: String, endTime: String) {
        viewModelScope.launch {
            adminRepository.createBooking(CreateBookingRequest(spaceId, title, startTime, endTime))
            loadData()
        }
    }

    fun updateBookingStatus(bookingId: String, action: String) {
        viewModelScope.launch {
            adminRepository.updateBookingStatus(bookingId, action)
            loadData()
        }
    }

    private suspend fun loadData() {
        when (val result = adminRepository.getBookings()) {
            is ApiResult.Success -> { _bookings.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        when (val result = adminRepository.getBookingSpaces()) {
            is ApiResult.Success -> {
                _spaces.value = result.data
                // Fetch statuses for all spaces concurrently
                val statuses = mutableMapOf<String, BookingSpaceStatus>()
                result.data.forEach { space ->
                    when (val statusResult = adminRepository.getBookableSpaceStatus(space.id)) {
                        is ApiResult.Success -> statuses[space.id] = statusResult.data
                        else -> {}
                    }
                }
                _spaceStatuses.value = statuses
            }
            else -> {}
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBookingsScreen(
    onBack: () -> Unit,
    viewModel: AdminBookingsViewModel = hiltViewModel(),
) {
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val spaces by viewModel.spaces.collectAsStateWithLifecycle()
    val spaceStatuses by viewModel.spaceStatuses.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.booking_spaces),
        stringResource(R.string.booking_active),
        stringResource(R.string.booking_past),
    )

    val active = bookings.filter { it.status.lowercase() in listOf("confirmed", "checked_in") }
    val past = bookings.filter { it.status.lowercase() !in listOf("confirmed", "checked_in") }
    var showCreateSheet by remember { mutableStateOf(false) }
    val createSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_bookings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.booking_create))
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
                            AdminTabPicker(tabs = tabs, selectedIndex = selectedTab, onTabSelected = { selectedTab = it })
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        when (selectedTab) {
                            0 -> {
                                items(spaces, key = { it.id }) { space ->
                                    SpaceRow(space, spaceStatuses[space.id])
                                }
                            }
                            1 -> {
                                if (active.isEmpty()) {
                                    item { EmptyBox() }
                                } else {
                                    items(active, key = { it.id }) { booking ->
                                        BookingRow(booking = booking, onAction = { action -> viewModel.updateBookingStatus(booking.id, action) })
                                    }
                                }
                            }
                            2 -> {
                                if (past.isEmpty()) {
                                    item { EmptyBox() }
                                } else {
                                    items(past, key = { it.id }) { booking ->
                                        BookingRow(booking = booking, onAction = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = createSheetState,
        ) {
            CreateBookingSheet(
                spaces = spaces,
                onBook = { spaceId, title, start, end ->
                    viewModel.createBooking(spaceId, title, start, end)
                    scope.launch { createSheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
                onCancel = {
                    scope.launch { createSheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBookingSheet(
    spaces: List<BookingSpace>,
    onBook: (spaceId: String, title: String?, startTime: String, endTime: String) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedSpaceId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var endTime by remember { mutableStateOf(LocalTime.now().plusHours(2).withMinute(0)) }
    var spaceDropdownExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    val availableSpaces = spaces.filter { it.enabled }
    val selectedSpace = availableSpaces.find { it.id == selectedSpaceId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.booking_create),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = spaceDropdownExpanded,
            onExpandedChange = { spaceDropdownExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedSpace?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.booking_select_space)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = spaceDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = spaceDropdownExpanded,
                onDismissRequest = { spaceDropdownExpanded = false },
            ) {
                availableSpaces.forEach { space ->
                    DropdownMenuItem(
                        text = { Text("${space.name} (${space.currentOccupancy}/${space.capacity})") },
                        onClick = {
                            selectedSpaceId = space.id
                            spaceDropdownExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.booking_title_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { showStartDatePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("${stringResource(R.string.booking_start)}: ${startDate} ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            }
            OutlinedButton(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("${stringResource(R.string.booking_end)}: ${endDate} ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    val start = startDate.atTime(startTime).format(formatter)
                    val end = endDate.atTime(endTime).format(formatter)
                    onBook(selectedSpaceId, title.ifBlank { null }, start, end)
                },
                modifier = Modifier.weight(1f),
                enabled = selectedSpaceId.isNotEmpty(),
            ) {
                Text(stringResource(R.string.booking_book))
            }
        }
    }

    if (showStartDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showEndDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        endDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = dateState) }
    }
}

@Composable
private fun EmptyBox() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.dashboard_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpaceRow(space: BookingSpace, status: BookingSpaceStatus? = null) {
    val statusDotColor = when (status?.status?.lowercase()) {
        "available" -> Color(0xFF4CAF50)
        "occupied" -> Color(0xFFF44336)
        "upcoming" -> Color(0xFFFFC107)
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (statusDotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(space.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${space.type.replaceFirstChar { it.uppercase() }} · ${space.currentOccupancy}/${space.capacity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(if (space.enabled) "available" else "full")
            }
        }
    }
}

@Composable
private fun BookingRow(booking: Booking, onAction: ((String) -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(booking.title ?: booking.spaceId, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(booking.bookedBy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${booking.startTime.take(16).replace("T", " ")} – ${booking.endTime.takeLast(8).take(5)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(booking.status)
            }
            if (onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (booking.status.lowercase() == "confirmed") {
                        Button(onClick = { onAction("check_in") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35A853))) {
                            Text(stringResource(R.string.booking_check_in))
                        }
                    }
                    if (booking.status.lowercase() == "checked_in") {
                        Button(onClick = { onAction("check_out") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))) {
                            Text(stringResource(R.string.booking_check_out))
                        }
                    }
                    if (booking.status.lowercase() == "confirmed") {
                        OutlinedButton(onClick = { onAction("cancel") }) {
                            Text(stringResource(R.string.booking_cancel), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

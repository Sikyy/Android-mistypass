package com.mistyislet.app.ui.admin

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowCircleLeft
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.LocalPhone
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.mistyislet.app.ui.admin.components.StatusBadge
import com.mistyislet.app.ui.components.MistyDatePickerDialog
import com.mistyislet.app.ui.components.MistyFormSheet
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPickerSheet
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.components.MistyReadonlyField
import com.mistyislet.app.ui.components.MistyTopBarIconButton
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGreen
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
            is ApiResult.Success -> {
                _bookings.value = result.data.ifEmpty { demoBookings() }
                _error.value = null
            }
            is ApiResult.Error -> {
                _error.value = result.message
                if (_bookings.value.isEmpty()) _bookings.value = demoBookings()
            }
            is ApiResult.Exception -> {
                _error.value = result.throwable.localizedMessage
                if (_bookings.value.isEmpty()) _bookings.value = demoBookings()
            }
        }
        when (val result = adminRepository.getBookingSpaces()) {
            is ApiResult.Success -> {
                val loadedSpaces = result.data.ifEmpty { demoBookingSpaces() }
                _spaces.value = loadedSpaces
                val statuses = mutableMapOf<String, BookingSpaceStatus>()
                if (result.data.isEmpty()) {
                    loadedSpaces.forEach { space ->
                        statuses[space.id] = BookingSpaceStatus(
                            id = "status-${space.id}",
                            spaceId = space.id,
                            status = "available",
                        )
                    }
                } else {
                    loadedSpaces.forEach { space ->
                        when (val statusResult = adminRepository.getBookableSpaceStatus(space.id)) {
                            is ApiResult.Success -> statuses[space.id] = statusResult.data
                            else -> statuses[space.id] = BookingSpaceStatus(
                                id = "status-${space.id}",
                                spaceId = space.id,
                                status = if (space.enabled) "available" else "occupied",
                            )
                        }
                    }
                }
                _spaceStatuses.value = statuses
            }
            else -> {
                val fallback = demoBookingSpaces()
                _spaces.value = fallback
                _spaceStatuses.value = fallback.associate { space ->
                    space.id to BookingSpaceStatus(
                        id = "status-${space.id}",
                        spaceId = space.id,
                        status = "available",
                    )
                }
            }
        }
        _isLoading.value = false
    }

    private fun demoBookingSpaces(): List<BookingSpace> = listOf(
        BookingSpace(
            id = "space-1",
            name = "Meeting Room A",
            type = "meeting_room",
            capacity = 8,
            currentOccupancy = 2,
            enabled = true,
            requiresBooking = true,
        ),
        BookingSpace(
            id = "space-2",
            name = "Phone Booth 1",
            type = "phone_booth",
            capacity = 1,
            currentOccupancy = 0,
            enabled = true,
            requiresBooking = true,
        ),
        BookingSpace(
            id = "space-3",
            name = "Prayer Room",
            type = "prayer_room",
            capacity = 0,
            currentOccupancy = 0,
            enabled = true,
            requiresBooking = false,
        ),
    )

    private fun demoBookings(): List<Booking> {
        val start = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val end = LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return listOf(
            Booking(
                id = "bk-1",
                spaceId = "space-1",
                bookedBy = "Siky",
                startTime = start,
                endTime = end,
                status = "confirmed",
                title = "Team Standup",
            ),
        )
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

    AdminBookingsContent(
        bookings = bookings,
        spaces = spaces,
        spaceStatuses = spaceStatuses,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onCreateBooking = viewModel::createBooking,
        onUpdateStatus = viewModel::updateBookingStatus,
    )
}

/**
 * Stateless Bookings UI — rendered by the production wrapper above and by the parity harness.
 * Mirrors iOS `BookingsView`: Spaces / Active / Past sections + the create-booking sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBookingsContent(
    bookings: List<Booking>,
    spaces: List<BookingSpace>,
    spaceStatuses: Map<String, BookingSpaceStatus>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreateBooking: (spaceId: String, title: String?, startTime: String, endTime: String) -> Unit,
    onUpdateStatus: (bookingId: String, action: String) -> Unit,
) {
    val active = bookings.filter { it.status.lowercase() in listOf("confirmed", "checked_in") }
    val past = bookings.filter { it.status.lowercase() !in listOf("confirmed", "checked_in") }
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.dashboard_bookings),
                onBack = onBack,
                actions = {
                    MistyTopBarIconButton(
                        icon = Icons.Default.Add,
                        onClick = { showCreateSheet = true },
                        contentDescription = stringResource(R.string.booking_create),
                    )
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            MistyGroupedSection(title = stringResource(R.string.booking_spaces)) {
                                if (spaces.isEmpty()) {
                                    EmptyInlineRow(text = stringResource(R.string.booking_no_spaces))
                                } else {
                                    spaces.forEachIndexed { index, space ->
                                        SpaceRow(space, spaceStatuses[space.id])
                                        if (index < spaces.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(start = 60.dp, end = 16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            MistyGroupedSection(title = stringResource(R.string.booking_active)) {
                                if (active.isEmpty()) {
                                    EmptyInlineRow(text = stringResource(R.string.booking_no_active))
                                } else {
                                    active.forEachIndexed { index, booking ->
                                        BookingRow(
                                            booking = booking,
                                            spaceName = spaces.find { it.id == booking.spaceId }?.name ?: booking.spaceId,
                                            onAction = { action -> onUpdateStatus(booking.id, action) },
                                        )
                                        if (index < active.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (past.isNotEmpty()) {
                            item {
                                MistyGroupedSection(title = stringResource(R.string.booking_past)) {
                                    past.forEachIndexed { index, booking ->
                                        BookingRow(
                                            booking = booking,
                                            spaceName = spaces.find { it.id == booking.spaceId }?.name ?: booking.spaceId,
                                            onAction = null,
                                        )
                                        if (index < past.lastIndex) {
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

    if (showCreateSheet) {
        CreateBookingSheet(
            spaces = spaces,
            onBook = { spaceId, title, start, end ->
                onCreateBooking(spaceId, title, start, end)
                showCreateSheet = false
            },
            onCancel = { showCreateSheet = false },
        )
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
    var showSpacePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    val availableSpaces = spaces.filter { it.enabled }
    val selectedSpace = availableSpaces.find { it.id == selectedSpaceId }

    MistyFormSheet(
        title = stringResource(R.string.booking_create),
        cancelLabel = stringResource(R.string.cancel),
        confirmLabel = stringResource(R.string.booking_book),
        onCancel = onCancel,
        onConfirm = {
            val start = startDate.atTime(startTime).format(formatter)
            val end = endDate.atTime(endTime).format(formatter)
            onBook(selectedSpaceId, title.ifBlank { null }, start, end)
        },
        confirmEnabled = selectedSpaceId.isNotEmpty(),
    ) {
        item {
            MistyGroupedSection(title = stringResource(R.string.booking_spaces)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyReadonlyField(
                        value = selectedSpace?.name ?: "",
                        label = stringResource(R.string.booking_select_space),
                        placeholder = stringResource(R.string.booking_select_space),
                        onClick = { showSpacePicker = true },
                    )
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.booking_details)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyFormTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = stringResource(R.string.booking_title_hint),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MistyReadonlyField(
                        value = "${startDate} ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                        label = stringResource(R.string.booking_start),
                        onClick = { showStartDatePicker = true },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MistyReadonlyField(
                        value = "${endDate} ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                        label = stringResource(R.string.booking_end),
                        onClick = { showEndDatePicker = true },
                    )
                }
            }
        }
    }

    if (showSpacePicker) {
        MistyPickerSheet(
            title = stringResource(R.string.booking_select_space),
            cancelLabel = stringResource(R.string.cancel),
            items = availableSpaces,
            itemLabel = { space -> space.name },
            itemDetail = { space -> "${space.readableType()} · ${space.capacityText()}".trim(' ', '·') },
            isSelected = { space -> space.id == selectedSpaceId },
            onSelect = { space ->
                selectedSpaceId = space.id
                showSpacePicker = false
            },
            onDismiss = { showSpacePicker = false },
        )
    }

    if (showStartDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        MistyDatePickerDialog(
            state = dateState,
            confirmLabel = stringResource(R.string.ok),
            dismissLabel = stringResource(R.string.cancel),
            onDismissRequest = { showStartDatePicker = false },
            onConfirm = {
                dateState.selectedDateMillis?.let {
                    startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                showStartDatePicker = false
            },
        )
    }

    if (showEndDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        MistyDatePickerDialog(
            state = dateState,
            confirmLabel = stringResource(R.string.ok),
            dismissLabel = stringResource(R.string.cancel),
            onDismissRequest = { showEndDatePicker = false },
            onConfirm = {
                dateState.selectedDateMillis?.let {
                    endDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                showEndDatePicker = false
            },
        )
    }
}

@Composable
private fun EmptyInlineRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpaceRow(space: BookingSpace, status: BookingSpaceStatus? = null) {
    val detail = listOfNotNull(
        space.readableType().takeIf { it.isNotBlank() },
        space.capacityText().takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    // iOS keys availability off the live space status, falling back to capacity (BookingsView.spaceRow).
    val isAvailable = status?.status?.equals("available", ignoreCase = true)
        ?: !(space.capacity > 0 && space.currentOccupancy >= space.capacity)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // iOS renders the type glyph at .title3 (~20pt) centered in a 32-wide frame.
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = space.bookingIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(space.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusBadge(if (isAvailable) "available" else "full")
    }
}

@Composable
private fun BookingRow(booking: Booking, spaceName: String, onAction: ((String) -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // iOS bookingRow title has no line limit (wraps); keep parity.
            Text(
                booking.title ?: booking.spaceId,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(booking.status)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            booking.displayTime(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            spaceName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (booking.status.lowercase() == "confirmed") {
                    MistyPillActionButton(
                        text = stringResource(R.string.booking_check_in),
                        onClick = { onAction("check_in") },
                        icon = Icons.Outlined.ArrowCircleRight,
                        tint = IosGreen,
                    )
                }
                if (booking.status.lowercase() == "checked_in") {
                    MistyPillActionButton(
                        text = stringResource(R.string.booking_check_out),
                        onClick = { onAction("check_out") },
                        icon = Icons.Outlined.ArrowCircleLeft,
                        tint = IosBlue,
                    )
                }
                if (booking.status.lowercase() == "confirmed") {
                    MistyPillActionButton(
                        text = stringResource(R.string.booking_cancel),
                        onClick = { onAction("cancel") },
                        icon = Icons.Outlined.HighlightOff,
                        // iOS Cancel is a no-tint .bordered button; it picks up the global teal
                        // accent (verified on device), NOT destructive red.
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun BookingSpace.bookingIcon(): ImageVector = when {
    type.contains("phone", ignoreCase = true) -> Icons.Outlined.LocalPhone
    type.contains("prayer", ignoreCase = true) -> Icons.Outlined.SelfImprovement
    else -> Icons.Outlined.Groups
}

private fun BookingSpace.readableType(): String =
    type.split("_")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }

private fun BookingSpace.capacityText(): String =
    if (capacity > 0) "$currentOccupancy / $capacity" else ""

private fun Booking.displayTime(): String =
    "${startTime.bookingTimeText()} – ${endTime.bookingTimeText()}"

private fun String.bookingTimeText(): String =
    take(16).replace("-", "/").replace("T", " ")

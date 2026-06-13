package com.mistyislet.app.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.ArrowCircleLeft
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import com.mistyislet.app.ui.components.MistyAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.CreateGuestRequest
import com.mistyislet.app.domain.model.GuestVisit
import com.mistyislet.app.ui.admin.components.AdminTabPicker
import com.mistyislet.app.ui.admin.components.StatusBadge
import com.mistyislet.app.ui.components.MistyDatePickerDialog
import com.mistyislet.app.ui.components.MistyFormSheet
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.components.MistyMultiSelectPickerSheet
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPickerSheet
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.components.MistyReadonlyField
import com.mistyislet.app.ui.components.MistySegmentedControl
import com.mistyislet.app.ui.components.MistyTopBarIconButton
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGuestManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _guests = MutableStateFlow<List<GuestVisit>>(emptyList())
    val guests: StateFlow<List<GuestVisit>> = _guests
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _availableDoors = MutableStateFlow<List<AccessibleDoor>>(emptyList())
    val availableDoors: StateFlow<List<AccessibleDoor>> = _availableDoors
    private val _createGuestState = MutableStateFlow<CreateGuestState>(CreateGuestState.Idle)
    val createGuestState: StateFlow<CreateGuestState> = _createGuestState
    private var placeId: String? = null

    init {
        viewModelScope.launch {
            placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            loadData()
            loadDoors()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData()
            _isRefreshing.value = false
        }
    }

    fun createGuest(request: CreateGuestRequest) {
        val pid = placeId ?: return
        viewModelScope.launch {
            _createGuestState.value = CreateGuestState.Submitting
            val state = createGuestStateOf(adminRepository.createGuest(pid, request))
            _createGuestState.value = state
            if (state is CreateGuestState.Success) loadData()
        }
    }

    fun consumeCreateGuestState() {
        _createGuestState.value = CreateGuestState.Idle
    }

    fun updateStatus(guestId: String, action: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.updateGuestStatus(pid, guestId, action)
            loadData()
        }
    }

    fun deleteGuest(guestId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.deleteGuest(pid, guestId)
            loadData()
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        val state = demoFallback.resolveList(adminRepository.getGuests(pid)) { AdminDemoData.guestVisits }
        _guests.value = state.items
        _error.value = state.error
        _isLoading.value = false
    }

    private suspend fun loadDoors() {
        val pid = placeId ?: return
        // Doors are an optional pick-list for the create form: demo-gated like every list
        // here, but a load failure must not block guest creation, so the error is dropped.
        _availableDoors.value = demoFallback
            .resolveList(placeRepository.listPlaceDoors(pid)) { AdminDemoData.accessibleDoors }
            .items
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGuestManagementScreen(
    onBack: () -> Unit,
    viewModel: AdminGuestManagementViewModel = hiltViewModel(),
) {
    val guests by viewModel.guests.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val availableDoors by viewModel.availableDoors.collectAsStateWithLifecycle()
    val createGuestState by viewModel.createGuestState.collectAsStateWithLifecycle()

    AdminGuestManagementContent(
        guests = guests,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        availableDoors = availableDoors,
        createGuestState = createGuestState,
        onConsumeCreateGuestState = viewModel::consumeCreateGuestState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onCreateGuest = viewModel::createGuest,
        onUpdateStatus = viewModel::updateStatus,
        onDeleteGuest = viewModel::deleteGuest,
    )
}

/**
 * Stateless Guest Management UI — rendered by the wrapper above and the parity harness.
 * Mirrors iOS `AdminGuestManagementView`: KPI summary + segmented tabs + guest rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGuestManagementContent(
    guests: List<GuestVisit>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    availableDoors: List<AccessibleDoor> = emptyList(),
    createGuestState: CreateGuestState = CreateGuestState.Idle,
    onConsumeCreateGuestState: () -> Unit = {},
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreateGuest: (CreateGuestRequest) -> Unit,
    onUpdateStatus: (guestId: String, action: String) -> Unit,
    onDeleteGuest: (guestId: String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var guestToDelete by remember { mutableStateOf<GuestVisit?>(null) }

    LaunchedEffect(createGuestState) {
        if (createGuestState is CreateGuestState.Success) {
            showCreateSheet = false
            onConsumeCreateGuestState()
        }
    }

    val tabs = listOf(
        stringResource(R.string.guest_expected),
        stringResource(R.string.guest_on_site),
        stringResource(R.string.guest_completed),
    )

    val expected = guests.filter { it.status.lowercase() == "expected" }
    val onSite = guests.filter { it.status.lowercase() == "checked_in" }
    val completed = guests.filter { it.status.lowercase() in listOf("checked_out", "cancelled") }
    val filtered = when (selectedTab) { 0 -> expected; 1 -> onSite; else -> completed }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.dashboard_guest_management),
                onBack = onBack,
                actions = {
                    MistyTopBarIconButton(
                        icon = Icons.Default.Add,
                        onClick = { showCreateSheet = true },
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
                } else if (guests.isEmpty()) {
                    MistyEmptyState(
                        icon = Icons.Default.PersonAdd,
                        title = stringResource(R.string.guest_empty),
                        description = stringResource(R.string.guest_empty_description),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        contentPadding = MistyGroupedListPadding,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            GuestSummaryRow(
                                expected = expected.size,
                                onSite = onSite.size,
                                total = guests.size,
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            AdminTabPicker(tabs = tabs, selectedIndex = selectedTab, onTabSelected = { selectedTab = it })
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        item {
                            MistyGroupedSection {
                                if (filtered.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                        Text(stringResource(R.string.guest_none_in_tab), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    filtered.forEachIndexed { index, guest ->
                                        GuestRow(
                                            guest = guest,
                                            onCheckIn = { onUpdateStatus(guest.id, "check_in") },
                                            onCheckOut = { onUpdateStatus(guest.id, "check_out") },
                                            onDelete = { guestToDelete = guest },
                                        )
                                        if (index < filtered.lastIndex) {
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
        CreateGuestSheet(
            availableDoors = availableDoors,
            errorMessage = (createGuestState as? CreateGuestState.Error)?.message,
            isSubmitting = createGuestState is CreateGuestState.Submitting,
            onSave = onCreateGuest,
            onCancel = {
                onConsumeCreateGuestState()
                showCreateSheet = false
            },
        )
    }

    guestToDelete?.let { guest ->
        MistyAlertDialog(
            onDismissRequest = { guestToDelete = null },
            title = { Text(stringResource(R.string.admin_delete)) },
            text = { Text(stringResource(R.string.admin_confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGuest(guest.id)
                    guestToDelete = null
                }) { Text(stringResource(R.string.admin_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { guestToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun GuestSummaryRow(
    expected: Int,
    onSite: Int,
    total: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        GuestKpiChip(expected.toString(), stringResource(R.string.guest_expected), IosOrange)
        GuestKpiChip(onSite.toString(), stringResource(R.string.guest_on_site), IosGreen)
        GuestKpiChip(total.toString(), stringResource(R.string.admin_total), MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RowScope.GuestKpiChip(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 27.sp),
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GuestRow(
    guest: GuestVisit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guest.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                guest.company?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(guest.status)
        }

        // iOS puts host + expected time on one line, and the ID-doc on its OWN line below.
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            guest.hostName?.takeIf { it.isNotBlank() }?.let { host ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(host, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            guest.expectedAt?.takeIf { it.isNotBlank() }?.let { at ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(at.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        guest.idDocumentType?.takeIf { it.isNotBlank() }?.let { idType ->
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${idType.uppercase()}: ${guest.idDocumentNumber ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val canCheckIn = guest.status.lowercase() == "expected"
        val canCheckOut = guest.status.lowercase() == "checked_in"
        val canCancel = guest.status.lowercase() in listOf("expected", "checked_in")
        if (canCheckIn || canCheckOut || canCancel) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (canCheckIn) {
                MistyPillActionButton(
                    text = stringResource(R.string.guest_check_in),
                    onClick = onCheckIn,
                    icon = Icons.Outlined.ArrowCircleRight,
                    tint = IosGreen,
                )
                }
                if (canCheckOut) {
                MistyPillActionButton(
                    text = stringResource(R.string.guest_check_out),
                    onClick = onCheckOut,
                    icon = Icons.Outlined.ArrowCircleLeft,
                    tint = IosBlue,
                )
                }
                if (canCancel) {
                MistyPillActionButton(
                    text = stringResource(R.string.guest_cancel),
                    onClick = onDelete,
                    icon = Icons.Outlined.HighlightOff,
                    // iOS Cancel is a no-tint .bordered button → global teal accent (not red).
                    tint = MaterialTheme.colorScheme.primary,
                )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGuestSheet(
    availableDoors: List<AccessibleDoor>,
    errorMessage: String?,
    isSubmitting: Boolean,
    onSave: (CreateGuestRequest) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var hostName by remember { mutableStateOf("") }
    var hostEmail by remember { mutableStateOf("") }
    var hostPhone by remember { mutableStateOf("") }
    var notifyHost by remember { mutableStateOf(true) }
    var idDocType by remember { mutableStateOf("") }
    var idDocNumber by remember { mutableStateOf("") }
    var hasExpectedTime by remember { mutableStateOf(false) }
    var expectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTtl by remember { mutableIntStateOf(24) }
    var showIdTypePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDoorIds by remember { mutableStateOf(setOf<String>()) }
    var showDoorPicker by remember { mutableStateOf(false) }

    val idTypes = listOf(
        "" to stringResource(R.string.guest_id_none),
        "ktp" to stringResource(R.string.guest_id_ktp),
        "sim" to stringResource(R.string.guest_id_sim),
        "passport" to stringResource(R.string.guest_id_passport),
        "other" to stringResource(R.string.guest_id_other),
    )
    val ttlOptions = listOf(4, 8, 24, 48, 72)
    val isValid = name.isNotBlank() && phone.isNotBlank() && hostName.isNotBlank()

    MistyFormSheet(
        title = stringResource(R.string.guest_create),
        cancelLabel = stringResource(R.string.cancel),
        confirmLabel = stringResource(R.string.guest_register),
        onCancel = onCancel,
        onConfirm = {
            val expectedIso = if (hasExpectedTime && expectedDate != null) {
                java.time.Instant.ofEpochMilli(expectedDate!!).toString()
            } else null
            onSave(
                CreateGuestRequest(
                    name = name,
                    email = email.ifBlank { null },
                    phone = phone.ifBlank { null },
                    company = company.ifBlank { null },
                    purpose = purpose.ifBlank { null },
                    hostName = hostName.ifBlank { null },
                    hostEmail = hostEmail.ifBlank { null },
                    hostPhone = hostPhone.ifBlank { null },
                    idDocumentType = idDocType.ifBlank { null },
                    idDocumentNumber = idDocNumber.ifBlank { null },
                    expectedAt = expectedIso,
                    notifyHost = notifyHost,
                    doorIds = selectedDoorIds.sorted(),
                    accessTtlHours = selectedTtl,
                ),
            )
        },
        confirmEnabled = isValid && !isSubmitting,
        errorMessage = errorMessage,
    ) {
        item {
            MistyGroupedSection(title = stringResource(R.string.guest_section_visitor)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyFormTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.visitor_name))
                    Spacer(modifier = Modifier.height(8.dp))
                    MistyFormTextField(value = phone, onValueChange = { phone = it }, label = stringResource(R.string.visitor_phone))
                    Spacer(modifier = Modifier.height(8.dp))
                    MistyFormTextField(value = email, onValueChange = { email = it }, label = stringResource(R.string.guest_email_optional))
                    Spacer(modifier = Modifier.height(8.dp))
                    MistyFormTextField(value = company, onValueChange = { company = it }, label = stringResource(R.string.guest_company_optional))
                    Spacer(modifier = Modifier.height(8.dp))
                    MistyFormTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = stringResource(R.string.guest_purpose_optional),
                        singleLine = false,
                        minLines = 2,
                    )
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.guest_section_host)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyFormTextField(value = hostName, onValueChange = { hostName = it }, label = stringResource(R.string.visitor_host))
                    Spacer(modifier = Modifier.height(8.dp))
                    MistyFormTextField(value = hostEmail, onValueChange = { hostEmail = it }, label = stringResource(R.string.guest_host_email_optional))
                    Spacer(modifier = Modifier.height(8.dp))
                    MistyFormTextField(value = hostPhone, onValueChange = { hostPhone = it }, label = stringResource(R.string.guest_host_phone_optional))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.guest_notify_host),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = notifyHost, onCheckedChange = { notifyHost = it })
                    }
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.guest_section_id)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyReadonlyField(
                        value = idTypes.find { it.first == idDocType }?.second ?: stringResource(R.string.guest_id_none),
                        label = stringResource(R.string.guest_id_type),
                        onClick = { showIdTypePicker = true },
                    )
                    if (idDocType.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MistyFormTextField(
                            value = idDocNumber,
                            onValueChange = { idDocNumber = it },
                            label = stringResource(R.string.guest_id_number),
                        )
                    }
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.guest_section_schedule)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.guest_set_expected_time),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = hasExpectedTime, onCheckedChange = { hasExpectedTime = it })
                    }
                    if (hasExpectedTime) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val display = expectedDate?.let {
                            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        }.orEmpty()
                        MistyReadonlyField(
                            value = display,
                            label = stringResource(R.string.guest_pick_date),
                            placeholder = stringResource(R.string.guest_pick_date),
                            onClick = { showDatePicker = true },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.guest_access_duration), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    MistySegmentedControl(
                        labels = ttlOptions.map { stringResource(R.string.visitors_hours, it) },
                        selectedIndex = ttlOptions.indexOf(selectedTtl).coerceAtLeast(0),
                        onSelected = { selectedTtl = ttlOptions[it] },
                    )
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.guest_section_doors)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyReadonlyField(
                        value = if (selectedDoorIds.isEmpty()) {
                            stringResource(R.string.guest_doors_default)
                        } else {
                            stringResource(R.string.guest_doors_selected, selectedDoorIds.size)
                        },
                        label = stringResource(R.string.guest_select_doors),
                        onClick = { showDoorPicker = true },
                    )
                }
            }
        }
    }

    if (showIdTypePicker) {
        MistyPickerSheet(
            title = stringResource(R.string.guest_id_type),
            cancelLabel = stringResource(R.string.cancel),
            items = idTypes,
            itemLabel = { option -> option.second },
            isSelected = { option -> option.first == idDocType },
            onSelect = { option ->
                idDocType = option.first
                showIdTypePicker = false
            },
            onDismiss = { showIdTypePicker = false },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expectedDate ?: System.currentTimeMillis())
        MistyDatePickerDialog(
            state = datePickerState,
            confirmLabel = stringResource(R.string.save),
            dismissLabel = stringResource(R.string.cancel),
            onDismissRequest = { showDatePicker = false },
            onConfirm = {
                expectedDate = datePickerState.selectedDateMillis
                showDatePicker = false
            },
        )
    }

    if (showDoorPicker) {
        MistyMultiSelectPickerSheet(
            title = stringResource(R.string.guest_select_doors),
            doneLabel = stringResource(R.string.common_done),
            items = availableDoors,
            itemLabel = { it.name },
            isSelected = { it.id in selectedDoorIds },
            onToggle = { door ->
                selectedDoorIds = if (door.id in selectedDoorIds) selectedDoorIds - door.id else selectedDoorIds + door.id
            },
            onDismiss = { showDoorPicker = false },
            searchPlaceholder = stringResource(R.string.search_doors),
            itemDetail = { it.groupName },
        )
    }
}

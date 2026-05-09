package com.mistyislet.app.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.CreateGuestRequest
import com.mistyislet.app.domain.model.GuestVisit
import com.mistyislet.app.ui.admin.components.AdminTabPicker
import com.mistyislet.app.ui.admin.components.KpiItem
import com.mistyislet.app.ui.admin.components.StatusBadge
import com.mistyislet.app.ui.admin.components.StatusSummaryRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGuestManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _guests = MutableStateFlow<List<GuestVisit>>(emptyList())
    val guests: StateFlow<List<GuestVisit>> = _guests
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
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

    fun createGuest(request: CreateGuestRequest) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.createGuest(pid, request)
            loadData()
        }
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
        when (val result = adminRepository.getGuests(pid)) {
            is ApiResult.Success -> { _guests.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var guestToDelete by remember { mutableStateOf<GuestVisit?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_guest_management)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
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
                                    KpiItem(expected.size.toString(), stringResource(R.string.guest_expected), Color(0xFFFF9800)),
                                    KpiItem(onSite.size.toString(), stringResource(R.string.guest_on_site), Color(0xFF35A853)),
                                    KpiItem(guests.size.toString(), stringResource(R.string.admin_total), Color(0xFF4285F4)),
                                ),
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            AdminTabPicker(tabs = tabs, selectedIndex = selectedTab, onTabSelected = { selectedTab = it })
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.dashboard_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { guest ->
                                GuestRow(
                                    guest = guest,
                                    onCheckIn = { viewModel.updateStatus(guest.id, "check_in") },
                                    onCheckOut = { viewModel.updateStatus(guest.id, "check_out") },
                                    onDelete = { guestToDelete = guest },
                                )
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
            sheetState = sheetState,
        ) {
            CreateGuestSheet(
                onSave = { request ->
                    viewModel.createGuest(request)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
            )
        }
    }

    guestToDelete?.let { guest ->
        AlertDialog(
            onDismissRequest = { guestToDelete = null },
            title = { Text(stringResource(R.string.admin_delete)) },
            text = { Text(stringResource(R.string.admin_confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGuest(guest.id)
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
private fun GuestRow(
    guest: GuestVisit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(guest.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOfNotNull(guest.company, guest.hostName?.let { "Host: $it" }).joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(guest.status)
            }

            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                guest.expectedAt?.takeIf { it.isNotBlank() }?.let { at ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(at.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                guest.idDocumentType?.takeIf { it.isNotBlank() }?.let { idType ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "${idType.uppercase()}: ${guest.idDocumentNumber ?: "—"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (guest.status.lowercase() == "expected") {
                    Button(
                        onClick = onCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35A853)),
                    ) { Text(stringResource(R.string.guest_check_in)) }
                }
                if (guest.status.lowercase() == "checked_in") {
                    Button(
                        onClick = onCheckOut,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    ) { Text(stringResource(R.string.guest_check_out)) }
                }
                if (guest.status.lowercase() in listOf("expected", "checked_in")) {
                    OutlinedButton(onClick = onDelete) {
                        Text(stringResource(R.string.guest_cancel), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGuestSheet(
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
    var showIdTypeMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val idTypes = listOf(
        "" to stringResource(R.string.guest_id_none),
        "ktp" to stringResource(R.string.guest_id_ktp),
        "sim" to stringResource(R.string.guest_id_sim),
        "passport" to stringResource(R.string.guest_id_passport),
        "other" to stringResource(R.string.guest_id_other),
    )
    val ttlOptions = listOf(4, 8, 24, 48, 72)
    val isValid = name.isNotBlank() && phone.isNotBlank() && hostName.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(stringResource(R.string.guest_create), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        // Visitor info
        SectionLabel(stringResource(R.string.guest_section_visitor))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.visitor_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.visitor_phone)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.guest_email_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text(stringResource(R.string.guest_company_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = purpose, onValueChange = { purpose = it }, label = { Text(stringResource(R.string.guest_purpose_optional)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(modifier = Modifier.height(16.dp))

        // Host info
        SectionLabel(stringResource(R.string.guest_section_host))
        OutlinedTextField(value = hostName, onValueChange = { hostName = it }, label = { Text(stringResource(R.string.visitor_host)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = hostEmail, onValueChange = { hostEmail = it }, label = { Text(stringResource(R.string.guest_host_email_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = hostPhone, onValueChange = { hostPhone = it }, label = { Text(stringResource(R.string.guest_host_phone_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.guest_notify_host), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = notifyHost, onCheckedChange = { notifyHost = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ID Verification
        SectionLabel(stringResource(R.string.guest_section_id))
        Box {
            OutlinedTextField(
                value = idTypes.find { it.first == idDocType }?.second ?: stringResource(R.string.guest_id_none),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.guest_id_type)) },
                modifier = Modifier.fillMaxWidth().clickable { showIdTypeMenu = true },
                singleLine = true,
            )
            DropdownMenu(expanded = showIdTypeMenu, onDismissRequest = { showIdTypeMenu = false }) {
                idTypes.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { idDocType = value; showIdTypeMenu = false },
                    )
                }
            }
        }
        if (idDocType.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = idDocNumber, onValueChange = { idDocNumber = it }, label = { Text(stringResource(R.string.guest_id_number)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Schedule
        SectionLabel(stringResource(R.string.guest_section_schedule))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.guest_set_expected_time), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = hasExpectedTime, onCheckedChange = { hasExpectedTime = it })
        }
        if (hasExpectedTime) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val display = expectedDate?.let {
                    java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                } ?: stringResource(R.string.guest_pick_date)
                Text(display)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.guest_access_duration), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ttlOptions.forEachIndexed { index, hours ->
                SegmentedButton(
                    selected = selectedTtl == hours,
                    onClick = { selectedTtl = hours },
                    shape = SegmentedButtonDefaults.itemShape(index, ttlOptions.size),
                ) { Text("${hours}h") }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = {
                    val expectedIso = if (hasExpectedTime && expectedDate != null) {
                        java.time.Instant.ofEpochMilli(expectedDate!!).toString()
                    } else null
                    onSave(CreateGuestRequest(
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
                        accessTtlHours = selectedTtl,
                    ))
                },
                modifier = Modifier.weight(1f),
                enabled = isValid,
            ) { Text(stringResource(R.string.admin_create)) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expectedDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

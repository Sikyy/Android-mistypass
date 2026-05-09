package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mistyislet.app.domain.model.AdminGroup
import com.mistyislet.app.domain.model.CreateGroupRequest
import com.mistyislet.app.domain.model.GroupDoor
import com.mistyislet.app.domain.model.GroupMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGroupsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminGroup>>(emptyList())
    val items: StateFlow<List<AdminGroup>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var placeId: String? = null

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<GroupMember>> = _groupMembers
    private val _groupDoors = MutableStateFlow<List<GroupDoor>>(emptyList())
    val groupDoors: StateFlow<List<GroupDoor>> = _groupDoors
    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading

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

    fun createGroup(name: String, description: String?) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.createGroup(pid, CreateGroupRequest(name, description))
            loadData()
        }
    }

    fun deleteGroup(groupId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.deleteGroup(pid, groupId)
            loadData()
        }
    }

    fun loadGroupDetail(groupId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            _detailLoading.value = true
            _groupMembers.value = emptyList()
            _groupDoors.value = emptyList()
            when (val r = adminRepository.getGroupMembers(pid, groupId)) {
                is ApiResult.Success -> _groupMembers.value = r.data
                else -> {}
            }
            when (val r = adminRepository.getGroupDoors(pid, groupId)) {
                is ApiResult.Success -> _groupDoors.value = r.data
                else -> {}
            }
            _detailLoading.value = false
        }
    }

    fun addGroupMember(groupId: String, email: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.addGroupMember(pid, groupId, email)
            loadGroupDetail(groupId)
            loadData()
        }
    }

    fun addGroupDoor(groupId: String, doorId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.addGroupDoor(pid, groupId, doorId)
            loadGroupDetail(groupId)
            loadData()
        }
    }

    fun removeGroupMember(groupId: String, memberId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.removeGroupMember(pid, groupId, memberId)
            loadGroupDetail(groupId)
            loadData()
        }
    }

    fun removeGroupDoor(groupId: String, doorId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.removeGroupDoor(pid, groupId, doorId)
            loadGroupDetail(groupId)
            loadData()
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = adminRepository.getGroups(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGroupsScreen(
    onBack: () -> Unit,
    viewModel: AdminGroupsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<AdminGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<AdminGroup?>(null) }
    val createSheetState = rememberModalBottomSheetState()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    AdminListScreen(
        title = stringResource(R.string.dashboard_groups),
        items = items.map { group ->
            AdminListItem(
                id = group.id,
                title = group.name,
                subtitle = group.description ?: stringResource(R.string.admin_members_count, group.memberCount) +
                    " · " + stringResource(R.string.admin_doors_count, group.doorCount),
                trailing = "${group.memberCount}",
                leadingInitial = group.name.take(1).uppercase(),
                leadingInitialColor = Color(0xFF26A69A),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        searchPlaceholder = stringResource(R.string.admin_search),
        onItemClick = { item ->
            selectedGroup = items.find { it.id == item.id }
        },
        actions = {
            IconButton(onClick = { showCreateSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    )

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = createSheetState,
        ) {
            CreateNameDescSheet(
                title = stringResource(R.string.dashboard_groups),
                onSave = { name, desc ->
                    viewModel.createGroup(name, desc)
                    scope.launch { createSheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
                onCancel = {
                    scope.launch { createSheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
            )
        }
    }

    selectedGroup?.let { group ->
        ModalBottomSheet(
            onDismissRequest = { selectedGroup = null },
            sheetState = detailSheetState,
        ) {
            GroupDetailSheet(
                group = group,
                viewModel = viewModel,
                onDelete = {
                    groupToDelete = group
                    scope.launch { detailSheetState.hide() }.invokeOnCompletion { selectedGroup = null }
                },
            )
        }
    }

    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text(stringResource(R.string.admin_delete)) },
            text = { Text(stringResource(R.string.admin_confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(group.id)
                    groupToDelete = null
                }) { Text(stringResource(R.string.admin_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailSheet(
    group: AdminGroup,
    viewModel: AdminGroupsViewModel,
    onDelete: () -> Unit,
) {
    val members by viewModel.groupMembers.collectAsStateWithLifecycle()
    val doors by viewModel.groupDoors.collectAsStateWithLifecycle()
    val detailLoading by viewModel.detailLoading.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMember by remember { mutableStateOf(false) }
    var addMemberEmail by remember { mutableStateOf("") }
    var showAddDoor by remember { mutableStateOf(false) }
    val tabs = listOf(
        stringResource(R.string.admin_members),
        stringResource(R.string.admin_doors),
    )

    LaunchedEffect(group.id) {
        viewModel.loadGroupDetail(group.id)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                group.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (selectedTab == 0) {
                    addMemberEmail = ""
                    showAddMember = true
                } else {
                    showAddDoor = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (selectedTab == 0) stringResource(R.string.admin_add_member)
                else stringResource(R.string.admin_add_door),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (detailLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
        } else {
            when (selectedTab) {
                0 -> MembersList(
                    members = members,
                    onRemove = { viewModel.removeGroupMember(group.id, it.id) },
                )
                1 -> DoorsList(
                    doors = doors,
                    onRemove = { viewModel.removeGroupDoor(group.id, it.id) },
                )
            }
        }
    }

    if (showAddMember) {
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text(stringResource(R.string.admin_add_member)) },
            text = {
                OutlinedTextField(
                    value = addMemberEmail,
                    onValueChange = { addMemberEmail = it },
                    placeholder = { Text(stringResource(R.string.admin_enter_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addGroupMember(group.id, addMemberEmail.trim())
                        showAddMember = false
                    },
                    enabled = addMemberEmail.isNotBlank(),
                ) { Text(stringResource(R.string.admin_add_member)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showAddDoor) {
        AlertDialog(
            onDismissRequest = { showAddDoor = false },
            title = { Text(stringResource(R.string.admin_add_door)) },
            text = {
                Text(stringResource(R.string.admin_select_door))
            },
            confirmButton = {
                TextButton(onClick = { showAddDoor = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun MembersList(
    members: List<GroupMember>,
    onRemove: (GroupMember) -> Unit,
) {
    if (members.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_no_members),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    } else {
        LazyColumn {
            items(members, key = { it.id }) { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF26A69A).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF26A69A),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = member.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = member.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(member) }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoorsList(
    doors: List<GroupDoor>,
    onRemove: (GroupDoor) -> Unit,
) {
    if (doors.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_no_doors),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    } else {
        LazyColumn {
            items(doors, key = { it.id }) { door ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.DoorFront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = door.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = door.status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (door.status.lowercase() == "online") Color(0xFF35A853) else Color(0xFFD93025),
                        )
                    }
                    IconButton(onClick = { onRemove(door) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CreateNameDescSheet(
    title: String,
    onSave: (String, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.admin_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.admin_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = { onSave(name, description.ifBlank { null }) },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.admin_create))
            }
        }
    }
}

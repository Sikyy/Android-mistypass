package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.mistyislet.app.domain.model.AdminTeam
import com.mistyislet.app.domain.model.CreateTeamRequest
import com.mistyislet.app.domain.model.TeamAccessRight
import com.mistyislet.app.domain.model.TeamMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminTeamsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminTeam>>(emptyList())
    val items: StateFlow<List<AdminTeam>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var placeId: String? = null

    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val teamMembers: StateFlow<List<TeamMember>> = _teamMembers
    private val _teamAccessRights = MutableStateFlow<List<TeamAccessRight>>(emptyList())
    val teamAccessRights: StateFlow<List<TeamAccessRight>> = _teamAccessRights
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

    fun createTeam(name: String, description: String?) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.createTeam(pid, CreateTeamRequest(name, description))
            loadData()
        }
    }

    fun deleteTeam(teamId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.deleteTeam(pid, teamId)
            loadData()
        }
    }

    fun loadTeamDetail(teamId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            _detailLoading.value = true
            _teamMembers.value = emptyList()
            _teamAccessRights.value = emptyList()
            when (val r = adminRepository.getTeamMembers(pid, teamId)) {
                is ApiResult.Success -> _teamMembers.value = r.data
                else -> {}
            }
            when (val r = adminRepository.getTeamAccessRights(pid, teamId)) {
                is ApiResult.Success -> _teamAccessRights.value = r.data
                else -> {}
            }
            _detailLoading.value = false
        }
    }

    fun addTeamMember(teamId: String, email: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.addTeamMember(pid, teamId, email)
            loadTeamDetail(teamId)
            loadData()
        }
    }

    fun addTeamAccessRight(teamId: String, doorId: String, scheduleId: String? = null) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.addTeamAccessRight(pid, teamId, doorId, scheduleId)
            loadTeamDetail(teamId)
        }
    }

    fun removeTeamMember(teamId: String, memberId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.removeTeamMember(pid, teamId, memberId)
            loadTeamDetail(teamId)
            loadData()
        }
    }

    fun removeTeamAccessRight(teamId: String, rightId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.removeTeamAccessRight(pid, teamId, rightId)
            loadTeamDetail(teamId)
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = adminRepository.getTeams(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeamsScreen(
    onBack: () -> Unit,
    viewModel: AdminTeamsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf<AdminTeam?>(null) }
    var teamToDelete by remember { mutableStateOf<AdminTeam?>(null) }
    val createSheetState = rememberModalBottomSheetState()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    AdminListScreen(
        title = stringResource(R.string.dashboard_teams),
        items = items.map { team ->
            AdminListItem(
                id = team.id,
                title = team.name,
                subtitle = team.description ?: stringResource(R.string.admin_members_count, team.memberCount),
                trailing = "${team.memberCount}",
                leadingInitial = team.name.take(1).uppercase(),
                leadingInitialColor = Color(0xFF5C6BC0),
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
            selectedTeam = items.find { it.id == item.id }
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
                title = stringResource(R.string.dashboard_teams),
                onSave = { name, desc ->
                    viewModel.createTeam(name, desc)
                    scope.launch { createSheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
                onCancel = {
                    scope.launch { createSheetState.hide() }.invokeOnCompletion { showCreateSheet = false }
                },
            )
        }
    }

    selectedTeam?.let { team ->
        ModalBottomSheet(
            onDismissRequest = { selectedTeam = null },
            sheetState = detailSheetState,
        ) {
            TeamDetailSheet(
                team = team,
                viewModel = viewModel,
                onDelete = {
                    teamToDelete = team
                    scope.launch { detailSheetState.hide() }.invokeOnCompletion { selectedTeam = null }
                },
            )
        }
    }

    teamToDelete?.let { team ->
        AlertDialog(
            onDismissRequest = { teamToDelete = null },
            title = { Text(stringResource(R.string.admin_delete)) },
            text = { Text(stringResource(R.string.admin_confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTeam(team.id)
                    teamToDelete = null
                }) { Text(stringResource(R.string.admin_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { teamToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamDetailSheet(
    team: AdminTeam,
    viewModel: AdminTeamsViewModel,
    onDelete: () -> Unit,
) {
    val members by viewModel.teamMembers.collectAsStateWithLifecycle()
    val accessRights by viewModel.teamAccessRights.collectAsStateWithLifecycle()
    val detailLoading by viewModel.detailLoading.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMember by remember { mutableStateOf(false) }
    var addMemberEmail by remember { mutableStateOf("") }
    val tabs = listOf(
        stringResource(R.string.admin_members),
        stringResource(R.string.admin_access_rights),
    )

    LaunchedEffect(team.id) {
        viewModel.loadTeamDetail(team.id)
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
                    text = team.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                team.description?.let {
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

        if (selectedTab == 0) {
            Button(
                onClick = { addMemberEmail = ""; showAddMember = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.admin_add_member))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (detailLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
        } else {
            when (selectedTab) {
                0 -> TeamMembersList(
                    members = members,
                    onRemove = { viewModel.removeTeamMember(team.id, it.id) },
                )
                1 -> AccessRightsList(
                    rights = accessRights,
                    onRemove = { viewModel.removeTeamAccessRight(team.id, it.id) },
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
                        viewModel.addTeamMember(team.id, addMemberEmail.trim())
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
}

@Composable
private fun TeamMembersList(
    members: List<TeamMember>,
    onRemove: (TeamMember) -> Unit,
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
                        color = Color(0xFF5C6BC0).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5C6BC0),
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
private fun AccessRightsList(
    rights: List<TeamAccessRight>,
    onRemove: (TeamAccessRight) -> Unit,
) {
    if (rights.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_no_access_rights),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    } else {
        LazyColumn {
            items(rights, key = { it.id }) { right ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = right.doorName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = listOfNotNull(
                                right.accessType.replace("_", " ").replaceFirstChar { it.uppercase() },
                                right.scheduleName,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(right) }) {
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

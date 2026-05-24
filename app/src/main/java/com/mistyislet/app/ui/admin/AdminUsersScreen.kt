package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mistyislet.app.domain.model.AccessRight
import com.mistyislet.app.domain.model.AdminUser
import com.mistyislet.app.domain.model.UserLogin
import com.mistyislet.app.ui.admin.components.StatusBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminUser>>(emptyList())
    val items: StateFlow<List<AdminUser>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _detailState = MutableStateFlow(AdminUserDetailDataState())
    val detailState: StateFlow<AdminUserDetailDataState> = _detailState
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

    fun forceSignOut(userId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.forceSignOutUser(pid, userId)
            loadData()
        }
    }

    fun removeUser(userId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.removeUser(pid, userId)
            loadData()
        }
    }

    fun updateRole(userId: String, role: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.updateUserRole(pid, userId, role)
            loadData()
        }
    }

    fun inviteUser(email: String, role: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.inviteUser(pid, email, role)
            loadData()
        }
    }

    fun loadUserDetail(userId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            _detailState.value = AdminUserDetailDataState(isLoading = true)

            var user: AdminUser? = null
            var userError: String? = null
            when (val result = adminRepository.getUser(pid, userId)) {
                is ApiResult.Success -> user = result.data
                is ApiResult.Error -> userError = result.message
                is ApiResult.Exception -> userError = result.throwable.localizedMessage
            }

            var logins = emptyList<UserLogin>()
            var loginsError: String? = null
            when (val result = adminRepository.getUserLogins(pid, userId)) {
                is ApiResult.Success -> logins = result.data
                is ApiResult.Error -> loginsError = result.message
                is ApiResult.Exception -> loginsError = result.throwable.localizedMessage
            }

            var accessRights = emptyList<AccessRight>()
            var accessRightsError: String? = null
            when (val result = adminRepository.getUserAccessRights(pid, userId)) {
                is ApiResult.Success -> accessRights = result.data
                is ApiResult.Error -> accessRightsError = result.message
                is ApiResult.Exception -> accessRightsError = result.throwable.localizedMessage
            }

            _detailState.value = AdminUserDetailDataState(
                user = user,
                logins = logins,
                accessRights = accessRights,
                isLoading = false,
                userError = userError,
                loginsError = loginsError,
                accessRightsError = accessRightsError,
            )
        }
    }

    fun clearUserDetail() {
        _detailState.value = AdminUserDetailDataState()
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = adminRepository.getUsers(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

private val availableRoles = listOf(
    "door_access", "group_manager",
    "place_door_access", "place_access_manager", "place_administrator",
    "observer", "user_manager", "organization_access_manager", "organization_administrator",
)

data class AdminUserDetailDataState(
    val user: AdminUser? = null,
    val logins: List<UserLogin> = emptyList(),
    val accessRights: List<AccessRight> = emptyList(),
    val isLoading: Boolean = false,
    val userError: String? = null,
    val loginsError: String? = null,
    val accessRightsError: String? = null,
)

private fun roleColor(role: String): Color = when {
    role.contains("admin", ignoreCase = true) || role.contains("owner", ignoreCase = true) -> Color(0xFFD93025)
    role.contains("manager", ignoreCase = true) -> Color(0xFFFF9800)
    role.contains("observer", ignoreCase = true) -> Color(0xFF4285F4)
    else -> Color(0xFF35A853)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    viewModel: AdminUsersViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }
    var showInviteSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val inviteSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    AdminListScreen(
        title = stringResource(R.string.dashboard_users),
        items = items.map { user ->
            AdminListItem(
                id = user.id,
                title = user.name.ifBlank { user.email },
                subtitle = if (user.name.isNotBlank()) user.email else null,
                trailing = user.role.replace("_", " ").replaceFirstChar { it.uppercase() },
                trailingColor = roleColor(user.role),
                leadingInitial = (user.name.ifBlank { user.email }).take(1).uppercase(),
                leadingInitialColor = roleColor(user.role),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        searchPlaceholder = stringResource(R.string.admin_search_users),
        onItemClick = { item ->
            selectedUser = items.find { it.id == item.id }
            selectedUser?.let { viewModel.loadUserDetail(it.id) }
        },
        actions = {
            IconButton(onClick = { showInviteSheet = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
            }
        },
    )

    selectedUser?.let { user ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedUser = null
                viewModel.clearUserDetail()
            },
            sheetState = sheetState,
        ) {
            UserDetailSheet(
                user = user,
                detailState = detailState,
                onSignOut = { showSignOutDialog = true },
                onRemove = { showRemoveDialog = true },
                onRoleChange = { newRole ->
                    viewModel.updateRole(user.id, newRole)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        selectedUser = null
                        viewModel.clearUserDetail()
                    }
                },
            )
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.admin_force_sign_out)) },
            text = { Text(stringResource(R.string.admin_confirm_sign_out)) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    selectedUser?.let { viewModel.forceSignOut(it.id) }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        selectedUser = null
                        viewModel.clearUserDetail()
                    }
                }) { Text(stringResource(R.string.admin_force_sign_out), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.admin_remove_user)) },
            text = { Text(stringResource(R.string.admin_confirm_remove)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    selectedUser?.let { viewModel.removeUser(it.id) }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        selectedUser = null
                        viewModel.clearUserDetail()
                    }
                }) { Text(stringResource(R.string.admin_remove_user), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showInviteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInviteSheet = false },
            sheetState = inviteSheetState,
        ) {
            InviteUserSheet(
                onInvite = { email, role ->
                    viewModel.inviteUser(email, role)
                    scope.launch { inviteSheetState.hide() }.invokeOnCompletion { showInviteSheet = false }
                },
                onCancel = {
                    scope.launch { inviteSheetState.hide() }.invokeOnCompletion { showInviteSheet = false }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserDetailSheet(
    user: AdminUser,
    detailState: AdminUserDetailDataState,
    onSignOut: () -> Unit,
    onRemove: () -> Unit,
    onRoleChange: (String) -> Unit = {},
) {
    val displayUser = detailState.user ?: user
    var selectedRole by remember(displayUser.id, displayUser.role) { mutableStateOf(displayUser.role) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = roleColor(displayUser.role).copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp),
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (displayUser.name.ifBlank { displayUser.email }).take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = roleColor(displayUser.role),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = displayUser.name.ifBlank { displayUser.email },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (displayUser.name.isNotBlank()) {
                    Text(
                        text = displayUser.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (displayUser.status.isNotBlank()) {
            DetailRow(stringResource(R.string.admin_status), null) {
                StatusBadge(displayUser.status)
            }
        }
        displayUser.lastActivity?.let {
            DetailRow(stringResource(R.string.admin_last_activity), it.take(10))
        }
        displayUser.createdAt?.let {
            DetailRow(stringResource(R.string.admin_joined), it.take(10))
        }
        detailState.userError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            DetailErrorText(it)
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.admin_login_sessions),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            detailState.isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
            detailState.loginsError != null -> DetailErrorText(detailState.loginsError)
            detailState.logins.isEmpty() -> Text(
                text = stringResource(R.string.settings_no_logins),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> detailState.logins.take(3).forEach { login ->
                DetailRow(
                    label = login.deviceName.ifBlank { login.platform },
                    value = listOfNotNull(
                        login.platform.ifBlank { null },
                        login.lastActive.takeIf { it.isNotBlank() }?.take(10),
                    ).joinToString(" · ").ifBlank { null },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.admin_access_rights),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            detailState.isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
            detailState.accessRightsError != null -> DetailErrorText(detailState.accessRightsError)
            detailState.accessRights.isEmpty() -> Text(
                text = stringResource(R.string.admin_no_access_rights),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> detailState.accessRights.take(4).forEach { right ->
                DetailRow(
                    label = right.doorName.ifBlank { right.id },
                    value = listOfNotNull(
                        right.teamName.ifBlank { null },
                        right.scheduleName,
                    ).joinToString(" · ").ifBlank { null },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.admin_change_role),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            availableRoles.forEach { r ->
                androidx.compose.material3.FilterChip(
                    selected = selectedRole == r,
                    onClick = { selectedRole = r },
                    label = { Text(r.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        if (selectedRole != displayUser.role) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onRoleChange(selectedRole) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.admin_apply_role))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800)),
            ) {
                Text(stringResource(R.string.admin_force_sign_out))
            }
            Button(
                onClick = onRemove,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.admin_remove_user))
            }
        }
    }
}

@Composable
private fun DetailErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun DetailRow(label: String, value: String?, badge: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (badge != null) {
            badge()
        } else if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteUserSheet(
    onInvite: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("door_access") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.admin_invite_user),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.admin_email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(stringResource(R.string.admin_role), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            availableRoles.forEach { r ->
                androidx.compose.material3.FilterChip(
                    selected = role == r,
                    onClick = { role = r },
                    label = { Text(r.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = { onInvite(email, role) },
                modifier = Modifier.weight(1f),
                enabled = email.contains("@"),
            ) {
                Text(stringResource(R.string.admin_invite))
            }
        }
    }
}

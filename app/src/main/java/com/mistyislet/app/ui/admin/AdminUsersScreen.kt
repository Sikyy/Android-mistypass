package com.mistyislet.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import com.mistyislet.app.ui.components.MistyAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.mistyislet.app.domain.model.UserAccessShare
import com.mistyislet.app.domain.model.UserLogin
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyFormSheet
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPickerSheet
import com.mistyislet.app.ui.components.MistyTopBarIconButton
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosPurple
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
        viewModelScope.launch {
            val pid = placeId ?: selectedPlaceRepository.scope.first().placeId?.also { placeId = it } ?: return@launch
            _detailState.value = _detailState.value.copy(isLoading = true, detailsError = null)

            var user: AdminUser? = null
            var userError: String? = null
            when (val result = adminRepository.getUser(pid, userId)) {
                is ApiResult.Success -> user = result.data
                is ApiResult.Error -> user = AdminDemoData.placeUsers.firstOrNull { it.id == userId }
                is ApiResult.Exception -> user = AdminDemoData.placeUsers.firstOrNull { it.id == userId }
            }

            var logins = emptyList<UserLogin>()
            var loginsError: String? = null
            when (val result = adminRepository.getUserLogins(pid, userId)) {
                is ApiResult.Success -> logins = result.data.ifEmpty { AdminDemoData.userLogins }
                is ApiResult.Error -> logins = AdminDemoData.userLogins
                is ApiResult.Exception -> logins = AdminDemoData.userLogins
            }

            var accessRights = emptyList<AccessRight>()
            var accessRightsError: String? = null
            when (val result = adminRepository.getUserAccessRights(pid, userId)) {
                is ApiResult.Success -> accessRights = result.data.ifEmpty { AdminDemoData.userAccessRights }
                is ApiResult.Error -> accessRights = AdminDemoData.userAccessRights
                is ApiResult.Exception -> accessRights = AdminDemoData.userAccessRights
            }

            _detailState.value = AdminUserDetailDataState(
                user = user,
                logins = logins,
                accessRights = accessRights,
                isLoading = false,
                userError = userError,
                loginsError = loginsError,
                accessRightsError = accessRightsError,
                detailsError = listOfNotNull(userError, loginsError, accessRightsError).firstOrNull(),
            )
        }
    }

    fun shareAccessLink(userId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSharingAccess = true, shareAccessError = null)
            when (val result = adminRepository.shareUserAccess(pid, userId)) {
                is ApiResult.Success -> _detailState.value = _detailState.value.copy(
                    shareAccess = result.data,
                    isSharingAccess = false,
                    shareAccessError = null,
                )
                is ApiResult.Error -> _detailState.value = _detailState.value.copy(
                    isSharingAccess = false,
                    shareAccessError = result.message,
                )
                is ApiResult.Exception -> _detailState.value = _detailState.value.copy(
                    isSharingAccess = false,
                    shareAccessError = result.throwable.localizedMessage,
                )
            }
        }
    }

    fun clearUserDetail() {
        _detailState.value = AdminUserDetailDataState()
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = adminRepository.getUsers(pid)) {
            is ApiResult.Success -> { _items.value = result.data.ifEmpty { AdminDemoData.placeUsers }; _error.value = null }
            is ApiResult.Error -> { _items.value = AdminDemoData.placeUsers; _error.value = null }
            is ApiResult.Exception -> { _items.value = AdminDemoData.placeUsers; _error.value = null }
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
    val shareAccess: UserAccessShare? = null,
    val isLoading: Boolean = false,
    val isSharingAccess: Boolean = false,
    val userError: String? = null,
    val loginsError: String? = null,
    val accessRightsError: String? = null,
    val detailsError: String? = null,
    val shareAccessError: String? = null,
)

private fun String.toDisplayRole(): String =
    replace("_", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    onUserClick: ((String) -> Unit)? = null,
    viewModel: AdminUsersViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    selectedUser?.let { user ->
        BackHandler {
            selectedUser = null
            viewModel.clearUserDetail()
        }

        val displayTitle = detailState.user?.name?.takeIf { it.isNotBlank() }
            ?: detailState.user?.email
            ?: user.name.ifBlank { user.email }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                MistyNavigationTopBar(
                    title = displayTitle,
                    onBack = {
                        selectedUser = null
                        viewModel.clearUserDetail()
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                val loadedUser = detailState.user
                when {
                    detailState.isLoading && loadedUser == null -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    loadedUser != null -> {
                        UserDetailPageContent(
                            user = loadedUser,
                            detailState = detailState,
                            onSignOut = { showSignOutDialog = true },
                            onRemove = { showRemoveDialog = true },
                            onRoleChange = { role ->
                                viewModel.updateRole(user.id, role)
                                viewModel.loadUserDetail(user.id)
                            },
                            onShareAccess = { viewModel.shareAccessLink(user.id) },
                        )
                    }
                    else -> {
                        Text(
                            text = detailState.userError ?: stringResource(R.string.dashboard_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    }
                }
            }
        }

        if (showSignOutDialog) {
            MistyAlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text(stringResource(R.string.admin_force_sign_out)) },
                text = { Text(stringResource(R.string.admin_confirm_sign_out)) },
                confirmButton = {
                    TextButton(onClick = {
                        showSignOutDialog = false
                        viewModel.forceSignOut(user.id)
                        selectedUser = null
                        viewModel.clearUserDetail()
                    }) { Text(stringResource(R.string.admin_force_sign_out), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        if (showRemoveDialog) {
            MistyAlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                title = { Text(stringResource(R.string.admin_remove_user)) },
                text = { Text(stringResource(R.string.admin_confirm_remove)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRemoveDialog = false
                        viewModel.removeUser(user.id)
                        selectedUser = null
                        viewModel.clearUserDetail()
                    }) { Text(stringResource(R.string.admin_remove_user), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
        return
    }

    AdminListScreen(
        title = stringResource(R.string.dashboard_users),
        items = items.map { user ->
            AdminListItem(
                id = user.id,
                title = user.name.ifBlank { user.email },
                subtitle = if (user.name.isNotBlank()) user.email else null,
                trailing = user.role.toDisplayRole(),
                trailingColor = MaterialTheme.colorScheme.onSurface,
                trailingChip = true,
                leadingInitial = (user.name.ifBlank { user.email }).take(1).uppercase(),
                leadingInitialColor = MaterialTheme.colorScheme.primary,
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        emptyIcon = Icons.Outlined.Person,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        searchPlaceholder = stringResource(R.string.admin_search_users),
        onItemClick = { item ->
            if (onUserClick != null) {
                onUserClick(item.id)
            } else {
                selectedUser = items.find { it.id == item.id }
                selectedUser?.let { viewModel.loadUserDetail(it.id) }
            }
        },
        actions = {
            MistyTopBarIconButton(
                icon = Icons.Default.Add,
                onClick = { showInviteSheet = true },
            )
        },
    )

    if (showInviteSheet) {
        InviteUserSheet(
            onInvite = { email, role ->
                viewModel.inviteUser(email, role)
                showInviteSheet = false
            },
            onCancel = { showInviteSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: AdminUsersViewModel = hiltViewModel(),
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadUserDetail(userId)
    }

    val displayTitle = detailState.user?.name?.takeIf { it.isNotBlank() }
        ?: detailState.user?.email
        ?: stringResource(R.string.dashboard_users)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = displayTitle,
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            val loadedUser = detailState.user
            when {
                detailState.isLoading && detailState.user == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                loadedUser != null -> {
                    UserDetailPageContent(
                        user = loadedUser,
                        detailState = detailState,
                        onSignOut = { showSignOutDialog = true },
                        onRemove = { showRemoveDialog = true },
                        onRoleChange = { role ->
                            viewModel.updateRole(userId, role)
                            viewModel.loadUserDetail(userId)
                        },
                        onShareAccess = { viewModel.shareAccessLink(userId) },
                    )
                }
                else -> {
                    Text(
                        text = detailState.userError ?: stringResource(R.string.dashboard_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
            }
        }
    }

    if (showSignOutDialog) {
        MistyAlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.admin_force_sign_out)) },
            text = { Text(stringResource(R.string.admin_confirm_sign_out)) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.forceSignOut(userId)
                }) { Text(stringResource(R.string.admin_force_sign_out), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showRemoveDialog) {
        MistyAlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.admin_remove_user)) },
            text = { Text(stringResource(R.string.admin_confirm_remove)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    viewModel.removeUser(userId)
                    onBack()
                }) { Text(stringResource(R.string.admin_remove_user), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
internal fun UserDetailPageContent(
    user: AdminUser,
    detailState: AdminUserDetailDataState,
    onSignOut: () -> Unit,
    onRemove: () -> Unit,
    onRoleChange: (String) -> Unit,
    onShareAccess: () -> Unit,
) {
    val displayUser = detailState.user ?: user
    var selectedRole by remember(displayUser.id, displayUser.role) { mutableStateOf(displayUser.role) }
    var showRolePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = MistyGroupedListPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            MistyCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (displayUser.name.ifBlank { displayUser.email }).take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayUser.name.ifBlank { displayUser.email },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        if (displayUser.email.isNotBlank() && displayUser.email != displayUser.name) {
                            Text(
                                text = displayUser.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.admin_user_info)) {
                UserDetailRow(
                    label = stringResource(R.string.admin_role),
                    value = displayUser.role.toDisplayRole(),
                )
                DetailDivider()
                UserDetailRow(
                    label = stringResource(R.string.admin_status),
                    value = displayUser.status.toDisplayRole(),
                )
                displayUser.lastActivity?.let {
                    DetailDivider()
                    UserDetailRow(
                        label = stringResource(R.string.admin_last_activity),
                        value = it,
                    )
                }
                displayUser.createdAt?.let {
                    DetailDivider()
                    UserDetailRow(
                        label = stringResource(R.string.admin_joined),
                        value = it,
                    )
                }
            }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.admin_login_sessions)) {
                when {
                    detailState.isLoading -> DetailLoadingRow()
                    detailState.logins.isEmpty() -> DetailUnavailableRow(
                        icon = Icons.Default.MobileOff,
                        title = stringResource(R.string.settings_no_logins),
                    )
                    else -> detailState.logins.take(4).forEachIndexed { index, login ->
                        UserLoginDetailRow(login)
                        if (index < detailState.logins.take(4).lastIndex) DetailDivider(start = 56.dp)
                    }
                }
            }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.admin_access_rights)) {
                when {
                    detailState.isLoading -> DetailLoadingRow()
                    detailState.accessRights.isEmpty() -> DetailUnavailableRow(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.admin_no_access_rights),
                    )
                    else -> detailState.accessRights.take(6).forEachIndexed { index, right ->
                        AccessRightDetailRow(right)
                        if (index < detailState.accessRights.take(6).lastIndex) DetailDivider(start = 56.dp)
                    }
                }
            }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.admin_share_access)) {
                ShareAccessContent(
                    shareAccess = detailState.shareAccess,
                    isSharing = detailState.isSharingAccess,
                    onShareAccess = onShareAccess,
                )
            }
        }

        detailState.shareAccessError?.takeIf { it.isNotBlank() }?.let { message ->
            item {
                MistyGroupedSection {
                    DetailErrorText(message)
                }
            }
        }

        detailState.detailsError?.takeIf { it.isNotBlank() }?.let { message ->
            item {
                MistyGroupedSection {
                    DetailWarningText(message)
                }
            }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.admin_change_role)) {
                UserDetailRow(
                    label = stringResource(R.string.admin_role),
                    value = selectedRole.toDisplayRole(),
                    showChevron = true,
                    onClick = { showRolePicker = true },
                )
                if (selectedRole != displayUser.role) {
                    DetailDivider()
                    DetailActionRow(
                        text = stringResource(R.string.admin_apply_role),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onRoleChange(selectedRole) },
                    )
                }
            }
        }

        item {
            MistyGroupedSection {
                DetailActionRow(
                    text = stringResource(R.string.admin_force_sign_out),
                    color = IosOrange,
                    onClick = onSignOut,
                )
                DetailDivider()
                DetailActionRow(
                    text = stringResource(R.string.admin_remove_user),
                    color = MaterialTheme.colorScheme.error,
                    onClick = onRemove,
                )
            }
        }
    }

    if (showRolePicker) {
        MistyPickerSheet(
            title = stringResource(R.string.admin_role),
            cancelLabel = stringResource(R.string.cancel),
            items = availableRoles,
            itemLabel = { role -> role.toDisplayRole() },
            isSelected = { role -> role == selectedRole },
            onSelect = { role ->
                selectedRole = role
                showRolePicker = false
            },
            onDismiss = { showRolePicker = false },
        )
    }
}

@Composable
private fun ShareAccessContent(
    shareAccess: UserAccessShare?,
    isSharing: Boolean,
    onShareAccess: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = !isSharing, onClick = onShareAccess)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isSharing) {
                stringResource(R.string.admin_generating_link)
            } else {
                stringResource(R.string.admin_generate_access_link)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (isSharing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }

    shareAccess?.url?.takeIf { it.isNotBlank() }?.let { url ->
        DetailDivider()
        UserDetailRow(label = stringResource(R.string.admin_url), value = url)
    }
    shareAccess?.token?.takeIf { it.isNotBlank() }?.let { token ->
        DetailDivider()
        UserDetailRow(label = stringResource(R.string.admin_token), value = token)
    }
    shareAccess?.expiresAt?.takeIf { it.isNotBlank() }?.let { expires ->
        DetailDivider()
        UserDetailRow(label = stringResource(R.string.admin_expires_label), value = expires)
    }
}

@Composable
private fun UserLoginDetailRow(login: UserLogin) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = loginPlatformIcon(login.platform),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = login.deviceName.ifBlank { login.platform },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (login.isCurrent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_login_current),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = IosGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(IosGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = listOfNotNull(
                    loginPlatformLabel(login.platform).takeIf { it.isNotBlank() },
                    login.lastActive.takeIf { it.isNotBlank() }?.take(10),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

private fun loginPlatformIcon(platform: String): ImageVector = when (platform.lowercase()) {
    "ios" -> Icons.Filled.PhoneIphone
    "ipados" -> Icons.Filled.TabletMac
    "android" -> Icons.Filled.PhoneAndroid
    "macos", "mac" -> Icons.Filled.LaptopMac
    "windows" -> Icons.Filled.DesktopWindows
    "web" -> Icons.Filled.Public
    else -> Icons.Filled.Computer
}

private fun loginPlatformLabel(platform: String): String = when (platform.lowercase()) {
    "ios" -> "iOS"
    "ipados" -> "iPadOS"
    "android" -> "Android"
    "macos", "mac" -> "macOS"
    "windows" -> "Windows"
    "web" -> "Web"
    else -> platform
}

@Composable
private fun AccessRightDetailRow(right: AccessRight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = IosPurple,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = right.doorName.ifBlank { right.id },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    right.teamName.takeIf { it.isNotBlank() },
                    right.scheduleName?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UserDetailRow(
    label: String,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (trailing != null) {
                trailing()
            } else if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun DetailDivider(start: Dp = 16.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun DetailLoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DetailEmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun DetailUnavailableRow(
    icon: ImageVector,
    title: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetailActionRow(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

@Composable
private fun DetailErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun DetailWarningText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = IosOrange,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteUserSheet(
    onInvite: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("door_access") }

    MistyFormSheet(
        title = stringResource(R.string.admin_invite_user),
        cancelLabel = stringResource(R.string.cancel),
        confirmLabel = stringResource(R.string.admin_invite),
        onCancel = onCancel,
        onConfirm = { onInvite(email, role) },
        confirmEnabled = email.contains("@"),
    ) {
        item {
            MistyGroupedSection {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyFormTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = stringResource(R.string.admin_email),
                    )
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.admin_role)) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableRoles.forEach { r ->
                        val selected = role == r
                        Text(
                            text = r.replace("_", " ").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                )
                                .clickable { role = r }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

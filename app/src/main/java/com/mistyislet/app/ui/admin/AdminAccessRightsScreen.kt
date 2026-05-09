package com.mistyislet.app.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AdminUser
import com.mistyislet.app.ui.admin.components.AdminTabPicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val scopeTabs = listOf("All", "Org", "Place", "Group")

private fun roleScopeIndex(role: String): Int = when {
    role.contains("organization", ignoreCase = true) -> 1
    role.contains("place", ignoreCase = true) -> 2
    role.contains("group", ignoreCase = true) -> 3
    else -> 0
}

private fun roleColor(role: String): Color = when {
    role.contains("admin", ignoreCase = true) || role.contains("owner", ignoreCase = true) -> Color(0xFFD93025)
    role.contains("manager", ignoreCase = true) -> Color(0xFFFF9800)
    role.contains("observer", ignoreCase = true) -> Color(0xFF4285F4)
    else -> Color(0xFF35A853)
}

@HiltViewModel
class AdminAccessRightsViewModel @Inject constructor(
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

@Composable
fun AdminAccessRightsScreen(
    onBack: () -> Unit,
    viewModel: AdminAccessRightsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val localizedTabs = listOf(
        stringResource(R.string.admin_scope_all),
        stringResource(R.string.admin_scope_org),
        stringResource(R.string.admin_scope_place),
        stringResource(R.string.admin_scope_group),
    )

    val filtered = if (selectedTab == 0) items else items.filter { roleScopeIndex(it.role) == selectedTab }

    AdminListScreen(
        title = stringResource(R.string.dashboard_access_rights),
        items = filtered.map { user ->
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
        headerContent = {
            AdminTabPicker(
                tabs = localizedTabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    )
}

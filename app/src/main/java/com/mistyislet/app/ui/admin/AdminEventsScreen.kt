package com.mistyislet.app.ui.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mistyislet.app.domain.model.AdminEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminEventsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminEvent>>(emptyList())
    val items: StateFlow<List<AdminEvent>> = _items
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
        when (val result = adminRepository.getEvents(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

private fun resultIcon(color: String) = when (color.lowercase()) {
    "green" -> Icons.Default.CheckCircle
    "red" -> Icons.Default.Error
    "orange" -> Icons.Default.Warning
    else -> Icons.Default.Info
}

private fun resultColor(color: String) = when (color.lowercase()) {
    "green" -> Color(0xFF35A853)
    "red" -> Color(0xFFD93025)
    "orange" -> Color(0xFFFF9800)
    "yellow" -> Color(0xFFD98B06)
    "blue" -> Color(0xFF4285F4)
    else -> Color(0xFF9E9E9E)
}

@Composable
fun AdminEventsScreen(
    onBack: () -> Unit,
    viewModel: AdminEventsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_events),
        items = items.map { event ->
            val icon = resultIcon(event.resultColor)
            val color = resultColor(event.resultColor)
            AdminListItem(
                id = event.id,
                title = "${event.actor} · ${event.action}".ifBlank { event.objectName },
                subtitle = event.objectName.ifBlank { null },
                trailing = event.displayTime.ifBlank { null },
                leadingIcon = icon,
                leadingIconColor = color,
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
    )
}

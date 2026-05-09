package com.mistyislet.app.ui.admin

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
import com.mistyislet.app.domain.model.Camera
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminCamerasViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<Camera>>(emptyList())
    val items: StateFlow<List<Camera>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            when (val result = adminRepository.getCameras()) {
                is ApiResult.Success -> _items.value = result.data
                else -> {}
            }
            _isLoading.value = false
        }
    }
}

@Composable
fun AdminCamerasScreen(
    onBack: () -> Unit,
    viewModel: AdminCamerasViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_cameras),
        items = items.map { camera ->
            AdminListItem(
                id = camera.id,
                title = camera.name,
                subtitle = listOfNotNull(camera.vendor, camera.doorName).joinToString(" · ").ifBlank { null },
                trailing = camera.status.replaceFirstChar { it.uppercase() },
                trailingColor = when (camera.status.lowercase()) {
                    "online" -> Color(0xFF35A853)
                    "offline" -> Color(0xFFD93025)
                    "error" -> Color(0xFFFF9800)
                    else -> null
                },
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
    )
}

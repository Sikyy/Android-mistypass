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
import com.mistyislet.app.domain.model.Alarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAlarmsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<Alarm>>(emptyList())
    val items: StateFlow<List<Alarm>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            when (val result = adminRepository.getAlarms()) {
                is ApiResult.Success -> _items.value = result.data
                else -> {}
            }
            _isLoading.value = false
        }
    }
}

@Composable
fun AdminAlarmsScreen(
    onBack: () -> Unit,
    viewModel: AdminAlarmsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_alarms),
        items = items.map { alarm ->
            AdminListItem(
                id = alarm.id,
                title = alarm.name,
                subtitle = alarm.triggeredAt?.take(16)?.replace("T", " "),
                trailing = alarm.status.replaceFirstChar { it.uppercase() },
                trailingColor = when (alarm.status.lowercase()) {
                    "triggered", "active" -> Color(0xFFD93025)
                    "resolved" -> Color(0xFF35A853)
                    else -> null
                },
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
    )
}

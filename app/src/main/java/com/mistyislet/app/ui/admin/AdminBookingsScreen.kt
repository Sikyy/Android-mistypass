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
import com.mistyislet.app.domain.model.Booking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminBookingsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<Booking>>(emptyList())
    val items: StateFlow<List<Booking>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            when (val result = adminRepository.getBookings()) {
                is ApiResult.Success -> _items.value = result.data
                else -> {}
            }
            _isLoading.value = false
        }
    }
}

@Composable
fun AdminBookingsScreen(
    onBack: () -> Unit,
    viewModel: AdminBookingsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_bookings),
        items = items.map { booking ->
            AdminListItem(
                id = booking.id,
                title = booking.spaceName,
                subtitle = "${booking.bookedBy} · ${booking.startTime.take(16).replace("T", " ")}",
                trailing = booking.status.replaceFirstChar { it.uppercase() },
                trailingColor = when (booking.status.lowercase()) {
                    "confirmed" -> Color(0xFF35A853)
                    "cancelled" -> Color(0xFFD93025)
                    "checked_in" -> Color(0xFF4285F4)
                    else -> null
                },
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
    )
}

package com.mistyislet.app.ui.doors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.data.repository.SelectedScope
import com.mistyislet.app.ui.places.MyOrgsScreen
import com.mistyislet.app.ui.places.MyPlacesScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class DoorsRootViewModel @Inject constructor(
    selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    val scope: StateFlow<SelectedScope> = selectedPlaceRepository.scope.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SelectedScope(),
    )
}

@Composable
fun DoorsRootScreen(viewModel: DoorsRootViewModel = hiltViewModel()) {
    val scope by viewModel.scope.collectAsState()
    when {
        scope.placeId != null -> DoorsScreen()
        scope.orgId != null -> MyPlacesScreen()
        else -> MyOrgsScreen()
    }
}

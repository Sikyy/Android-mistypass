package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontFamily
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
import com.mistyislet.app.domain.model.AdminCard
import com.mistyislet.app.ui.admin.components.StatusBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardUserGroup(
    val id: String,
    val userName: String,
    val userEmail: String?,
    val cards: List<AdminCard>,
)

@HiltViewModel
class AdminCardsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminCard>>(emptyList())
    val items: StateFlow<List<AdminCard>> = _items
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

    fun unbindCard(cardId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.unbindCard(pid, cardId)
            loadData()
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = adminRepository.getCards(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCardsScreen(
    onBack: () -> Unit,
    viewModel: AdminCardsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val groups = remember(items) {
        items.groupBy { it.assignedTo ?: it.uid }.map { (key, cards) ->
            CardUserGroup(
                id = key,
                userName = cards.first().assignedTo ?: cards.first().uid,
                userEmail = cards.first().assignedEmail,
                cards = cards,
            )
        }.sortedBy { it.userName }
    }

    var selectedGroup by remember { mutableStateOf<CardUserGroup?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var cardToUnbind by remember { mutableStateOf<AdminCard?>(null) }

    AdminListScreen(
        title = stringResource(R.string.dashboard_cards),
        items = groups.map { group ->
            AdminListItem(
                id = group.id,
                title = group.userName,
                subtitle = group.userEmail,
                trailing = "${group.cards.size}",
                leadingInitial = group.userName.take(1).uppercase(),
                leadingInitialColor = Color(0xFFFF9800),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        searchPlaceholder = stringResource(R.string.admin_search_cards),
        onItemClick = { item ->
            selectedGroup = groups.find { it.id == item.id }
        },
    )

    selectedGroup?.let { group ->
        ModalBottomSheet(
            onDismissRequest = { selectedGroup = null },
            sheetState = sheetState,
        ) {
            CardGroupDetailSheet(
                group = group,
                onUnbind = { card -> cardToUnbind = card },
            )
        }
    }

    cardToUnbind?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToUnbind = null },
            title = { Text(stringResource(R.string.admin_unbind)) },
            text = { Text(stringResource(R.string.admin_confirm_unbind)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unbindCard(card.uid)
                    cardToUnbind = null
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedGroup = null }
                }) { Text(stringResource(R.string.admin_unbind), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { cardToUnbind = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CardGroupDetailSheet(
    group: CardUserGroup,
    onUnbind: (AdminCard) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = group.userName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        group.userEmail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(group.cards, key = { it.id }) { card ->
                CardDetailRow(card = card, onUnbind = { onUnbind(card) })
            }
        }
    }
}

@Composable
private fun CardDetailRow(card: AdminCard, onUnbind: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.uid,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                )
                StatusBadge(card.status)
            }
            card.cardType.ifBlank { null }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it.replaceFirstChar { c -> c.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    card.issuedAt?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.admin_issued, it.take(10)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    card.expiresAt?.let {
                        Text(
                            text = stringResource(R.string.admin_expires, it.take(10)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (card.status.lowercase() == "active") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUnbind,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.admin_unbind))
                    }
                }
            }
        }
    }
}

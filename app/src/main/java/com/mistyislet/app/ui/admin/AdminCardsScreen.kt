package com.mistyislet.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import com.mistyislet.app.ui.components.MistyAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AdminCard
import com.mistyislet.app.ui.admin.components.StatusBadge
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyLabeledContentRow
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.theme.IosOrange
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
    private val demoFallback: AdminDemoFallback,
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
        val state = demoFallback.resolveList(adminRepository.getCards(pid)) { AdminDemoData.cards }
        _items.value = state.items
        _error.value = state.error
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
    var cardToUnbind by remember { mutableStateOf<AdminCard?>(null) }

    selectedGroup?.let { group ->
        BackHandler { selectedGroup = null }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                MistyNavigationTopBar(
                    title = group.userName,
                    onBack = { selectedGroup = null },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CardGroupDetailSheet(
                    group = group,
                    onUnbind = { card -> cardToUnbind = card },
                    showHeader = false,
                )
            }
        }

        cardToUnbind?.let { card ->
            MistyAlertDialog(
                onDismissRequest = { cardToUnbind = null },
                title = { Text(stringResource(R.string.admin_unbind)) },
                text = { Text(stringResource(R.string.admin_confirm_unbind)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.unbindCard(card.uid)
                        cardToUnbind = null
                        selectedGroup = null
                    }) { Text(stringResource(R.string.admin_unbind), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { cardToUnbind = null }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
        return
    }

    AdminListScreen(
        title = stringResource(R.string.dashboard_cards),
        items = groups.map { group ->
            AdminListItem(
                id = group.id,
                title = group.userName,
                subtitle = listOfNotNull(
                    group.userEmail,
                    stringResource(R.string.admin_cards_count, group.cards.size),
                ).joinToString("\n"),
                leadingInitial = group.userName.take(1).uppercase(),
                leadingInitialColor = IosOrange,
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.admin_no_cards),
        emptyIcon = Icons.Default.CreditCard,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        searchPlaceholder = stringResource(R.string.admin_search_cards),
        onItemClick = { item ->
            selectedGroup = groups.find { it.id == item.id }
        },
    )

}

@Composable
private fun CardGroupDetailSheet(
    group: CardUserGroup,
    onUnbind: (AdminCard) -> Unit,
    showHeader: Boolean = true,
) {
    LazyColumn(
        contentPadding = MistyGroupedListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showHeader) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                }
            }
        }

        items(group.cards, key = { it.id }) { card ->
            CardDetailRow(card = card, onUnbind = { onUnbind(card) })
        }
    }
}

@Composable
private fun CardDetailRow(card: AdminCard, onUnbind: () -> Unit) {
    val cardTitle = card.cardType
        .takeIf { it.isNotBlank() }
        ?.replaceFirstChar { c -> c.uppercase() }
        ?: stringResource(R.string.credential_physical_card)

    MistyCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = IosOrange,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = card.uid,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(card.status)
            }
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
            )
            card.cardNumber?.takeIf { it.isNotBlank() }?.let {
                MistyLabeledContentRow(stringResource(R.string.admin_card_number), it)
            }
            card.issuedAt?.let {
                MistyLabeledContentRow(stringResource(R.string.admin_issued_label), it.take(10))
            }
            card.expiresAt?.let {
                MistyLabeledContentRow(stringResource(R.string.admin_expires_label), it.take(10))
            }
            if (card.status.lowercase() == "active") {
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                TextButton(
                    onClick = onUnbind,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.admin_unbind),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

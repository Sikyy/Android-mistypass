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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AdminDigitalCredential
import com.mistyislet.app.ui.admin.components.StatusBadge
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyLabeledContentRow
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosCyan
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosPurple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CredentialUserGroup(
    val id: String,
    val userName: String,
    val userEmail: String?,
    val credentials: List<AdminDigitalCredential>,
)

private fun platformIcon(platform: String): ImageVector = when (platform.lowercase()) {
    "ios", "apple" -> Icons.Default.PhoneIphone
    "android", "google" -> Icons.Default.Wallet
    "qr", "qrcode" -> Icons.Default.QrCode
    else -> Icons.Default.Key
}

private fun platformColor(platform: String): Color = when (platform.lowercase()) {
    "ios", "apple" -> IosBlue
    "android", "google" -> IosGreen
    "qr", "qrcode" -> IosPurple
    else -> IosCyan
}

private fun platformLabel(platform: String): String = when (platform.lowercase()) {
    "ios", "apple" -> "Apple Wallet"
    "android", "google" -> "Google Wallet"
    "qr", "qrcode" -> "QR Code"
    else -> platform.replaceFirstChar { it.uppercase() }
}

@HiltViewModel
class AdminDigitalCredentialsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminDigitalCredential>>(emptyList())
    val items: StateFlow<List<AdminDigitalCredential>> = _items
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

    fun revokeCredential(credentialId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.revokeCredential(pid, credentialId)
            loadData()
        }
    }

    fun suspendCredential(credentialId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.suspendCredential(pid, credentialId)
            loadData()
        }
    }

    fun activateCredential(credentialId: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.activateCredential(pid, credentialId)
            loadData()
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        val state = demoFallback.resolveList(adminRepository.getCredentials(pid)) { AdminDemoData.digitalCredentials }
        _items.value = state.items
        _error.value = state.error
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDigitalCredentialsScreen(
    onBack: () -> Unit,
    viewModel: AdminDigitalCredentialsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val groups = remember(items) {
        items.groupBy { it.userEmail ?: it.userName ?: it.id }.map { (key, creds) ->
            CredentialUserGroup(
                id = key,
                userName = creds.first().userName ?: key,
                userEmail = creds.first().userEmail,
                credentials = creds,
            )
        }.sortedBy { it.userName }
    }

    var selectedGroup by remember { mutableStateOf<CredentialUserGroup?>(null) }
    var credToRevoke by remember { mutableStateOf<AdminDigitalCredential?>(null) }

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
                CredentialGroupDetailSheet(
                    group = group,
                    onRevoke = { cred -> credToRevoke = cred },
                    showHeader = false,
                )
            }
        }

        credToRevoke?.let { cred ->
            MistyAlertDialog(
                onDismissRequest = { credToRevoke = null },
                title = { Text(stringResource(R.string.admin_revoke)) },
                text = { Text(stringResource(R.string.admin_confirm_revoke)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.revokeCredential(cred.id)
                        credToRevoke = null
                        selectedGroup = null
                    }) { Text(stringResource(R.string.admin_revoke), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { credToRevoke = null }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        return
    }

    AdminListScreen(
        title = stringResource(R.string.dashboard_digital_credentials),
        items = groups.map { group ->
            val platformSummary = group.credentials
                .joinToString(" · ") { credential -> platformLabel(credential.platform) }
            AdminListItem(
                id = group.id,
                title = group.userName,
                subtitle = listOfNotNull(
                    group.userEmail?.takeIf { it != group.userName },
                    platformSummary.takeIf { it.isNotBlank() },
                ).joinToString("\n").ifBlank { null },
                leadingInitial = group.userName.take(1).uppercase(),
                leadingInitialColor = IosCyan,
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.admin_no_digital_credentials),
        emptyIcon = Icons.Default.Key,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        searchPlaceholder = stringResource(R.string.admin_search_credentials),
        onItemClick = { item ->
            selectedGroup = groups.find { it.id == item.id }
        },
    )

}

@Composable
private fun CredentialGroupDetailSheet(
    group: CredentialUserGroup,
    onRevoke: (AdminDigitalCredential) -> Unit,
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

        items(group.credentials, key = { it.id }) { cred ->
            CredentialDetailRow(
                credential = cred,
                onRevoke = { onRevoke(cred) },
            )
        }
    }
}

@Composable
private fun CredentialDetailRow(
    credential: AdminDigitalCredential,
    onRevoke: () -> Unit,
) {
    val isRevoked = credential.status.equals("revoked", ignoreCase = true)
    val isActive = credential.status.equals("active", ignoreCase = true)

    MistyCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = platformIcon(credential.platform),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = platformColor(credential.platform),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = credential.deviceName.ifBlank { credential.platform },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(credential.status)
            }
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
            )
            credential.deviceModel?.takeIf { it.isNotBlank() }?.let {
                MistyLabeledContentRow(stringResource(R.string.dashboard_my_device), it)
            }
            MistyLabeledContentRow(
                label = stringResource(R.string.admin_usage_label),
                value = credential.usageCount.toString(),
            )
            credential.issuedAt?.let {
                MistyLabeledContentRow(stringResource(R.string.admin_issued_label), it.take(10))
            }
            credential.expiresAt?.let {
                MistyLabeledContentRow(stringResource(R.string.admin_expires_label), it.take(10))
            }
            if (isActive && !isRevoked) {
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                TextButton(
                    onClick = onRevoke,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.admin_revoke),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

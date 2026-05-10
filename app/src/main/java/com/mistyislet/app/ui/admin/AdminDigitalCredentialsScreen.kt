package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import com.mistyislet.app.domain.model.AdminDigitalCredential
import com.mistyislet.app.ui.admin.components.StatusBadge
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
    "ios", "apple" -> Icons.Default.PhoneAndroid
    "android", "google" -> Icons.Default.Wallet
    "qr", "qrcode" -> Icons.Default.QrCode
    else -> Icons.Default.Key
}

private fun platformColor(platform: String): Color = when (platform.lowercase()) {
    "ios", "apple" -> Color(0xFF4285F4)
    "android", "google" -> Color(0xFF35A853)
    "qr", "qrcode" -> Color(0xFF9C27B0)
    else -> Color(0xFF00ACC1)
}

@HiltViewModel
class AdminDigitalCredentialsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
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
        when (val result = adminRepository.getCredentials(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
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
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var credToRevoke by remember { mutableStateOf<AdminDigitalCredential?>(null) }
    var credToSuspend by remember { mutableStateOf<AdminDigitalCredential?>(null) }

    AdminListScreen(
        title = stringResource(R.string.dashboard_digital_credentials),
        items = groups.map { group ->
            AdminListItem(
                id = group.id,
                title = group.userName,
                subtitle = group.userEmail,
                trailing = "${group.credentials.size}",
                leadingInitial = group.userName.take(1).uppercase(),
                leadingInitialColor = Color(0xFF00ACC1),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        searchPlaceholder = stringResource(R.string.admin_search_credentials),
        onItemClick = { item ->
            selectedGroup = groups.find { it.id == item.id }
        },
    )

    selectedGroup?.let { group ->
        ModalBottomSheet(
            onDismissRequest = { selectedGroup = null },
            sheetState = sheetState,
        ) {
            CredentialGroupDetailSheet(
                group = group,
                onRevoke = { cred -> credToRevoke = cred },
                onSuspend = { cred -> credToSuspend = cred },
                onActivate = { cred ->
                    viewModel.activateCredential(cred.id)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedGroup = null }
                },
            )
        }
    }

    credToRevoke?.let { cred ->
        AlertDialog(
            onDismissRequest = { credToRevoke = null },
            title = { Text(stringResource(R.string.admin_revoke)) },
            text = { Text(stringResource(R.string.admin_confirm_revoke)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.revokeCredential(cred.id)
                    credToRevoke = null
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedGroup = null }
                }) { Text(stringResource(R.string.admin_revoke), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { credToRevoke = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    credToSuspend?.let { cred ->
        AlertDialog(
            onDismissRequest = { credToSuspend = null },
            title = { Text(stringResource(R.string.admin_suspend)) },
            text = { Text(stringResource(R.string.admin_confirm_suspend)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.suspendCredential(cred.id)
                    credToSuspend = null
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedGroup = null }
                }) { Text(stringResource(R.string.admin_suspend), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { credToSuspend = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CredentialGroupDetailSheet(
    group: CredentialUserGroup,
    onRevoke: (AdminDigitalCredential) -> Unit,
    onSuspend: (AdminDigitalCredential) -> Unit,
    onActivate: (AdminDigitalCredential) -> Unit,
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
            items(group.credentials, key = { it.id }) { cred ->
                CredentialDetailRow(
                    credential = cred,
                    onRevoke = { onRevoke(cred) },
                    onSuspend = { onSuspend(cred) },
                    onActivate = { onActivate(cred) },
                )
            }
        }
    }
}

@Composable
private fun CredentialDetailRow(
    credential: AdminDigitalCredential,
    onRevoke: () -> Unit,
    onSuspend: () -> Unit,
    onActivate: () -> Unit,
) {
    val isSuspended = credential.status.equals("suspended", ignoreCase = true)
    val isRevoked = credential.status.equals("revoked", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
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
            credential.deviceModel?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                if (credential.usageCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.admin_usage_count, credential.usageCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                credential.issuedAt?.let {
                    Text(
                        text = stringResource(R.string.admin_issued, it.take(10)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!isRevoked) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSuspended) {
                        OutlinedButton(onClick = onActivate) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.admin_activate))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        OutlinedButton(onClick = onSuspend) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.admin_suspend))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = onRevoke,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.admin_revoke))
                    }
                }
            }
        }
    }
}

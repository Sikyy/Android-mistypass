package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.OrgSettings
import com.mistyislet.app.domain.model.OrgSettingsUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminOrgSettingsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _settings = MutableStateFlow<OrgSettings?>(null)
    val settings: StateFlow<OrgSettings?> = _settings
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving
    private var orgId: String? = null

    init {
        viewModelScope.launch {
            orgId = selectedPlaceRepository.scope.first().orgId ?: return@launch
            loadData()
        }
    }

    fun save(request: OrgSettingsUpdateRequest) {
        val oid = orgId ?: return
        viewModelScope.launch {
            _isSaving.value = true
            when (val result = adminRepository.updateOrgSettings(oid, request)) {
                is ApiResult.Success -> _settings.value = result.data
                else -> {}
            }
            _isSaving.value = false
        }
    }

    private suspend fun loadData() {
        val oid = orgId ?: return
        when (val result = adminRepository.getOrgSettings(oid)) {
            is ApiResult.Success -> _settings.value = result.data
            else -> {}
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrgSettingsScreen(
    onBack: () -> Unit,
    viewModel: AdminOrgSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var name by remember(settings) { mutableStateOf(settings?.name ?: "") }
    var domain by remember(settings) { mutableStateOf(settings?.domain ?: "") }
    var sendEmails by remember(settings) { mutableStateOf(settings?.sendEmails ?: false) }
    var pushNotifications by remember(settings) { mutableStateOf(settings?.pushNotifications ?: false) }
    var weeklyReports by remember(settings) { mutableStateOf(settings?.weeklyReports ?: false) }
    var whatsappEnabled by remember(settings) { mutableStateOf(settings?.whatsappEnabled ?: false) }
    var enforceMfa by remember(settings) { mutableStateOf(settings?.enforceMfa ?: false) }
    var webauthn by remember(settings) { mutableStateOf(settings?.webauthnEnabled ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_org_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (settings != null) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { SectionTitle(stringResource(R.string.org_general)) }
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.settings_name_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = domain,
                            onValueChange = { domain = it },
                            label = { Text(stringResource(R.string.org_domain)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    settings?.timezone?.let { tz ->
                        item { ReadOnlyRow(stringResource(R.string.dashboard_timezone), tz) }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)); SectionTitle(stringResource(R.string.org_email_section)) }
                    item { ToggleRow(stringResource(R.string.org_send_emails), sendEmails) { sendEmails = it } }
                    item { ToggleRow(stringResource(R.string.org_push_notifications), pushNotifications) { pushNotifications = it } }
                    item { ToggleRow(stringResource(R.string.org_email_reports), weeklyReports) { weeklyReports = it } }

                    item { Spacer(modifier = Modifier.height(8.dp)); SectionTitle(stringResource(R.string.org_whatsapp_section)) }
                    item { ToggleRow(stringResource(R.string.org_whatsapp_enabled), whatsappEnabled) { whatsappEnabled = it } }

                    item { Spacer(modifier = Modifier.height(8.dp)); SectionTitle(stringResource(R.string.org_security_section)) }
                    item { ToggleRow(stringResource(R.string.org_enforce_mfa), enforceMfa) { enforceMfa = it } }
                    item { ToggleRow(stringResource(R.string.org_webauthn), webauthn) { webauthn = it } }
                    settings?.sessionTimeoutMinutes?.let {
                        item { ReadOnlyRow("Session Timeout", "${it}m") }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.save(OrgSettingsUpdateRequest(
                                    name = name.ifBlank { null },
                                    domain = domain.ifBlank { null },
                                    sendEmails = sendEmails,
                                    pushNotifications = pushNotifications,
                                    weeklyReports = weeklyReports,
                                    whatsappEnabled = whatsappEnabled,
                                    enforceMfa = enforceMfa,
                                    webauthnEnabled = webauthn,
                                ))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving,
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.dashboard_no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

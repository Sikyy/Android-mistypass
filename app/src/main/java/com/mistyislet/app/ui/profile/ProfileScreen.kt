package com.mistyislet.app.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.ui.theme.Danger

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToCredentials: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLogout() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // User info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.user?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = uiState.user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Credentials & Settings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_credentials)) },
                leadingContent = {
                    Icon(Icons.Default.Key, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onNavigateToCredentials),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_biometric)) },
                supportingContent = { Text(stringResource(R.string.profile_biometric_desc)) },
                leadingContent = {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = uiState.biometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometric(it) },
                        enabled = uiState.biometricAvailable,
                    )
                },
            )
            HorizontalDivider()
            // Language picker
            var langExpanded by remember { mutableStateOf(false) }
            val languages = listOf("English" to "en", "中文" to "zh", "Indonesia" to "in")
            Box {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.profile_language)) },
                    leadingContent = {
                        Icon(Icons.Default.Language, contentDescription = null)
                    },
                    modifier = Modifier.clickable { langExpanded = true },
                )
                DropdownMenu(
                    expanded = langExpanded,
                    onDismissRequest = { langExpanded = false },
                ) {
                    languages.forEach { (label, code) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                langExpanded = false
                                viewModel.setLanguage(code)
                            },
                        )
                    }
                }
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_notifications)) },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) },
                    )
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = viewModel::logout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Danger,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_logout),
                color = Danger,
            )
        }
    }
}

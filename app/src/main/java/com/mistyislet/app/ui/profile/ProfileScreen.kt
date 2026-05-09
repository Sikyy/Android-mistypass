package com.mistyislet.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.mistyislet.app.R
import kotlinx.coroutines.launch
import com.mistyislet.app.domain.model.UserLogin
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.Success

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLogout() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = Color.Transparent,
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(
                    text = stringResource(R.string.profile_tab_main),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(
                    text = stringResource(R.string.profile_tab_logins),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text(
                    text = stringResource(R.string.profile_tab_help),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }

        when (selectedTab) {
            0 -> MainSettingsTab(uiState, viewModel)
            1 -> LoginsTab(uiState, viewModel)
            2 -> HelpTab(viewModel)
        }
    }
}

@Composable
private fun MainSettingsTab(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    var showChangePassword by remember { mutableStateOf(false) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Profile header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
        ) {
            if (uiState.user != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                    ) {
                        if (uiState.user.avatar != null) {
                            AsyncImage(
                                model = uiState.user.avatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = userInitials(uiState.user.name),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = uiState.user.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = uiState.user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        // Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_password)) },
                    leadingContent = { Icon(Icons.Default.Key, contentDescription = null) },
                    modifier = Modifier.clickable { showChangePassword = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = {
                        Text(
                            if (uiState.biometricAvailable) uiState.biometricTypeName
                            else stringResource(R.string.settings_biometric),
                        )
                    },
                    supportingContent = {
                        Text(
                            if (uiState.biometricAvailable)
                                stringResource(R.string.settings_biometric_subtitle)
                            else
                                stringResource(R.string.settings_biometric_unavailable),
                        )
                    },
                    leadingContent = {
                        Icon(
                            when {
                                uiState.biometricTypeName.contains("Face") -> Icons.Default.Face
                                else -> Icons.Default.Fingerprint
                            },
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        scope.launch {
                                            val ok = viewModel.biometricHelper.authenticate(
                                                activity,
                                                title = context.getString(R.string.biometric_prompt_title),
                                                subtitle = context.getString(R.string.biometric_prompt_subtitle),
                                            )
                                            if (ok) viewModel.toggleBiometric(true)
                                        }
                                    }
                                } else {
                                    viewModel.toggleBiometric(false)
                                }
                            },
                            enabled = uiState.biometricAvailable,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                var langExpanded by remember { mutableStateOf(false) }
                val languages = listOf("English" to "en", "中文" to "zh", "Indonesia" to "in")
                val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
                    .toLanguageTags().takeIf { it.isNotBlank() }
                val currentLabel = languages.find { it.second == currentLocale }?.first
                    ?: languages.find { it.second == currentLocale?.take(2) }?.first
                    ?: "English"
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.profile_language)) },
                        supportingContent = { Text(currentLabel) },
                        leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                        modifier = Modifier.clickable { langExpanded = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    DropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false },
                    ) {
                        languages.forEach { (label, code) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, modifier = Modifier.weight(1f))
                                        if (code == currentLocale || code == currentLocale?.take(2) || (currentLocale.isNullOrBlank() && code == "en")) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Success)
                                        }
                                    }
                                },
                                onClick = {
                                    langExpanded = false
                                    viewModel.setLanguage(code)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Geofence settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.geofence_toggle)) },
                    supportingContent = { Text(stringResource(R.string.geofence_description)) },
                    leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.geofenceEnabled,
                            onCheckedChange = { viewModel.toggleGeofence(it) },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        // Sign out
        TextButton(
            onClick = viewModel::logout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Danger)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_sign_out), color = Danger)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showChangePassword) {
        ChangePasswordSheet(
            viewModel = viewModel,
            uiState = uiState,
            onDismiss = {
                showChangePassword = false
                viewModel.clearPasswordState()
            },
        )
    }
}

@Composable
private fun ChangePasswordSheet(
    viewModel: ProfileViewModel,
    uiState: ProfileUiState,
    onDismiss: () -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
    val isValid = currentPassword.isNotEmpty() && passwordsMatch

    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_password),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.profile_current_password)) },
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrent = !showCurrent }) {
                            Icon(
                                if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.profile_new_password)) },
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(
                                if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.profile_confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.profile_passwords_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                    )
                }

                uiState.passwordChangeError?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error, style = MaterialTheme.typography.bodySmall, color = Danger)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.changePassword(currentPassword, newPassword) },
                        enabled = isValid,
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginsTab(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    LaunchedEffect(Unit) { viewModel.fetchLogins() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (uiState.isLoadingLogins) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.logins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_no_logins),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column {
                    uiState.logins.forEachIndexed { index, login ->
                        LoginSessionRow(login = login, onLogout = { viewModel.remoteLogout(login) })
                        if (index < uiState.logins.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginSessionRow(login: UserLogin, onLogout: () -> Unit) {
    val platformIcon = when {
        login.platform.contains("ios", ignoreCase = true) -> Icons.Default.PhoneAndroid
        login.platform.contains("android", ignoreCase = true) -> Icons.Default.PhoneAndroid
        login.platform.contains("web", ignoreCase = true) -> Icons.Default.Laptop
        else -> Icons.Default.PhoneAndroid
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = platformIcon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = login.deviceName,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (login.isCurrent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_login_current),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Success,
                        modifier = Modifier
                            .background(Success.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = login.platform,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_login_last_active, login.lastActive),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        if (!login.isCurrent) {
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.settings_login_logout),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun HelpTab(viewModel: ProfileViewModel) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about)) },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.clickable { showAbout = !showAbout },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                if (showAbout) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        AboutRow(label = stringResource(R.string.profile_version), value = viewModel.appVersion)
                        AboutRow(label = stringResource(R.string.profile_build), value = viewModel.buildNumber)
                        AboutRow(label = stringResource(R.string.profile_device), value = viewModel.deviceModel)
                        AboutRow(label = "Android", value = viewModel.androidVersion)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_help)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_acknowledgments)) },
                    leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun userInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        else -> parts[0].take(2).uppercase()
    }
}

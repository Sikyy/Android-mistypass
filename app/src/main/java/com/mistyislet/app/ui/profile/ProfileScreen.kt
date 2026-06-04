package com.mistyislet.app.ui.profile

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.mistyislet.app.R
import kotlinx.coroutines.launch
import com.mistyislet.app.domain.model.UserLogin
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyFaceIdIcon
import com.mistyislet.app.ui.components.MistyKeyIcon
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyLargeTitle
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.components.MistySegmentedControl
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.Success
import java.util.Locale

private const val PROFILE_PAGE_CHANGE_PASSWORD = "change_password"
private const val PROFILE_PAGE_LANGUAGE = "language"
private const val PROFILE_PAGE_GEOFENCE = "geofence"
private const val PROFILE_PAGE_ABOUT = "about"
private const val PROFILE_PAGE_HELP = "help"
private const val PROFILE_PAGE_ACKNOWLEDGMENTS = "acknowledgments"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToTCPTest: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var subpage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLogout() }
    }

    subpage?.let { page ->
        BackHandler { subpage = null }
        when (page) {
            PROFILE_PAGE_CHANGE_PASSWORD -> ChangePasswordContent(
                uiState = uiState,
                onBack = {
                    subpage = null
                    viewModel.clearPasswordState()
                },
                onSubmit = viewModel::changePassword,
            )
            PROFILE_PAGE_LANGUAGE -> LanguageSettingsContent(
                onBack = { subpage = null },
                onSelect = viewModel::setLanguage,
            )
            PROFILE_PAGE_GEOFENCE -> GeofenceSettingsContent(onBack = { subpage = null })
            PROFILE_PAGE_ABOUT -> AboutPage(
                viewModel = viewModel,
                onBack = { subpage = null },
                onNavigateToTCPTest = onNavigateToTCPTest,
            )
            PROFILE_PAGE_HELP -> SimpleTextPage(
                title = stringResource(R.string.settings_help),
                body = stringResource(R.string.profile_help_placeholder),
                onBack = { subpage = null },
            )
            PROFILE_PAGE_ACKNOWLEDGMENTS -> SimpleTextPage(
                title = stringResource(R.string.settings_acknowledgments),
                body = stringResource(R.string.profile_acknowledgments_placeholder),
                onBack = { subpage = null },
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            MistyLargeTitle(text = stringResource(R.string.profile_title))

            Spacer(modifier = Modifier.height(10.dp))
            MistySegmentedControl(
                labels = listOf(
                    stringResource(R.string.profile_tab_main),
                    stringResource(R.string.profile_tab_logins),
                    stringResource(R.string.profile_tab_help),
                ),
                selectedIndex = selectedTab,
                onSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        when (selectedTab) {
            0 -> MainSettingsTab(
                uiState = uiState,
                viewModel = viewModel,
                onNavigateSubpage = { subpage = it },
            )
            1 -> LoginsTab(uiState, viewModel)
            2 -> HelpTab(
                viewModel = viewModel,
                onNavigateToTCPTest = onNavigateToTCPTest,
                onNavigateSubpage = { subpage = it },
            )
        }
    }
}

@Composable
private fun MainSettingsTab(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onNavigateSubpage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.uploadAvatar(it) } }

    ProfileMainContent(
        uiState = uiState,
        onPickAvatar = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onChangePassword = { onNavigateSubpage(PROFILE_PAGE_CHANGE_PASSWORD) },
        onLanguage = { onNavigateSubpage(PROFILE_PAGE_LANGUAGE) },
        onGeofence = { onNavigateSubpage(PROFILE_PAGE_GEOFENCE) },
        onLogout = viewModel::logout,
        onToggleBiometric = { enabled ->
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
    )
}

/**
 * Stateless main Profile tab content — driven by [uiState] + callbacks so it renders in the
 * DEBUG parity harness with mock data. Exact production UI.
 */
@Composable
internal fun ProfileMainContent(
    uiState: ProfileUiState,
    onPickAvatar: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onToggleBiometric: (Boolean) -> Unit = {},
    onLanguage: () -> Unit = {},
    onGeofence: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MistyCard(modifier = Modifier.fillMaxWidth()) {
            if (uiState.user != null) {
                Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPickAvatar),
                    ) {
                        if (uiState.user.avatar != null) {
                            AsyncImage(
                                model = uiState.user.avatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.user.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = uiState.user.email,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val organizationLine = listOfNotNull(
                            uiState.user.organizationName?.takeIf { it.isNotBlank() },
                            (uiState.user.roleDisplayLabel ?: uiState.user.role)?.toRoleLabel()?.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                        if (organizationLine.isNotBlank()) {
                            Text(
                                text = organizationLine,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.profile_settings_section),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp),
            )
            MistyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileRow(
                    icon = MistyKeyIcon,
                    title = stringResource(R.string.settings_password),
                    onClick = onChangePassword,
                )
                ProfileDivider()

                ProfileRow(
                    icon = if (uiState.biometricTypeName.contains("Face")) MistyFaceIdIcon else Icons.Outlined.Fingerprint,
                    title = if (uiState.biometricAvailable) uiState.biometricTypeName else stringResource(R.string.settings_biometric),
                    showChevron = false,
                    trailing = {
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            enabled = uiState.biometricAvailable,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    },
                )
                ProfileDivider()

                val languages = listOf("中文" to "zh", "English" to "en", "Bahasa Indonesia" to "in")
                val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
                    .toLanguageTags().takeIf { it.isNotBlank() }
                val fallbackLanguage = Locale.getDefault().language
                val currentLabel = languages.find { it.second == currentLocale }?.first
                    ?: languages.find { it.second == currentLocale?.take(2) }?.first
                    ?: languages.find { it.second == fallbackLanguage }?.first
                    ?: "English"
                ProfileRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.profile_language),
                    value = currentLabel,
                    onClick = onLanguage,
                )
                ProfileDivider()
                ProfileRow(
                    icon = Icons.Outlined.NearMe,
                    title = stringResource(R.string.profile_auto_unlock_zone),
                    onClick = onGeofence,
                )
            }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        MistyCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLogout,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.settings_sign_out),
                    color = Danger,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(MistyBottomNavInset))
    }
}

/** Large title + segmented header + main settings — used by the DEBUG parity harness. */
@Composable
internal fun ProfileMainView(uiState: ProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            MistyLargeTitle(text = stringResource(R.string.profile_title))
            Spacer(modifier = Modifier.height(10.dp))
            MistySegmentedControl(
                labels = listOf(
                    stringResource(R.string.profile_tab_main),
                    stringResource(R.string.profile_tab_logins),
                    stringResource(R.string.profile_tab_help),
                ),
                selectedIndex = 0,
                onSelected = {},
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        ProfileMainContent(uiState = uiState)
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
internal fun ChangePasswordContent(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
    val isValid = currentPassword.isNotEmpty() && passwordsMatch

    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.profile_change_password),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = MistyGroupedListPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MistyGroupedSection {
                    SecureFormRow(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        placeholder = stringResource(R.string.profile_current_password),
                        isVisible = showCurrent,
                        onToggleVisible = { showCurrent = !showCurrent },
                    )
                }
            }

            item {
                MistyGroupedSection {
                    SecureFormRow(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = stringResource(R.string.profile_new_password),
                        isVisible = showNew,
                        onToggleVisible = { showNew = !showNew },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    SecureFormRow(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = stringResource(R.string.profile_confirm_password),
                    )
                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Text(
                            text = stringResource(R.string.profile_passwords_mismatch),
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            uiState.passwordChangeError?.let { error ->
                item {
                    MistyGroupedSection {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Danger,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            item {
                MistyGroupedSection {
                    // iOS renders the form action as plain tinted text, not a filled pill.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .then(
                                if (isValid) {
                                    Modifier.clickable { onSubmit(currentPassword, newPassword) }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_update_password),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isValid) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LanguageSettingsContent(
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val languages = listOf("中文" to "zh", "English" to "en", "Bahasa Indonesia" to "in")
    val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        .toLanguageTags().takeIf { it.isNotBlank() }
    val fallbackLanguage = Locale.getDefault().language

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.profile_language),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = MistyGroupedListPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MistyGroupedSection {
                    languages.forEachIndexed { index, (label, code) ->
                        val selected = code == currentLocale ||
                            code == currentLocale?.take(2) ||
                            (currentLocale.isNullOrBlank() && code == fallbackLanguage)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { onSelect(code) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (index < languages.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GeofenceSettingsContent(onBack: () -> Unit) {
    var enabled by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.profile_auto_unlock_zone),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = MistyGroupedListPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MistyGroupedSection {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.geofence_toggle),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.geofence_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item {
                MistyGroupedSection {
                    AboutRow(
                        label = stringResource(R.string.geofence_location_permission),
                        value = stringResource(R.string.geofence_perm_not_set),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    AboutRow(
                        label = stringResource(R.string.geofence_monitored_doors),
                        value = "0",
                    )
                }
            }
            item {
                MistyGroupedSection(title = stringResource(R.string.geofence_how_it_works)) {
                    GeofenceInstructionRow(
                        icon = Icons.Outlined.LocationOn,
                        text = stringResource(R.string.geofence_step_1),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    GeofenceInstructionRow(
                        icon = Icons.Outlined.Notifications,
                        text = stringResource(R.string.geofence_step_2),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    GeofenceInstructionRow(
                        icon = Icons.Outlined.LockOpen,
                        text = stringResource(R.string.geofence_step_3),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    GeofenceInstructionRow(
                        icon = Icons.Outlined.FrontHand,
                        text = stringResource(R.string.geofence_step_4),
                    )
                }
            }
        }
    }
}

@Composable
private fun GeofenceInstructionRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutPage(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onNavigateToTCPTest: (() -> Unit)?,
) {
    var versionTapCount by remember { mutableIntStateOf(0) }
    var showDevOptions by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.settings_about),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = MistyGroupedListPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MistyGroupedSection {
                    AboutRow(
                        label = stringResource(R.string.profile_version),
                        value = viewModel.appVersion,
                        onClick = {
                            versionTapCount++
                            if (versionTapCount >= 7) {
                                showDevOptions = true
                                versionTapCount = 0
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    AboutRow(label = stringResource(R.string.profile_build), value = viewModel.buildNumber)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    AboutRow(label = stringResource(R.string.profile_device), value = viewModel.deviceModel)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    AboutRow(label = "Android", value = viewModel.androidVersion)
                }
            }

            if (showDevOptions && com.mistyislet.app.BuildConfig.DEBUG && onNavigateToTCPTest != null) {
                item {
                    MistyGroupedSection(title = "Developer Options") {
                        ListItem(
                            headlineContent = { Text("TCP Auth Test") },
                            supportingContent = { Text("Test BLE auth via Gateway TCP simulator") },
                            leadingContent = { Icon(Icons.Default.PhonelinkSetup, contentDescription = null) },
                            modifier = Modifier.clickable { onNavigateToTCPTest() },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }

            item {
                MistyGroupedSection {
                    Text(
                        text = stringResource(R.string.profile_app_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleTextPage(
    title: String,
    body: String,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = title,
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = MistyGroupedListPadding,
        ) {
            item {
                MistyGroupedSection {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecureFormRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isVisible: Boolean = false,
    onToggleVisible: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (onToggleVisible != null) {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
        MistyCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false) {},
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_password),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))

                SecureFormRow(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    placeholder = stringResource(R.string.profile_current_password),
                    isVisible = showCurrent,
                    onToggleVisible = { showCurrent = !showCurrent },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

                SecureFormRow(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = stringResource(R.string.profile_new_password),
                    isVisible = showNew,
                    onToggleVisible = { showNew = !showNew },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

                SecureFormRow(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = stringResource(R.string.profile_confirm_password),
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
                    MistyPillActionButton(
                        text = stringResource(R.string.save),
                        onClick = { viewModel.changePassword(currentPassword, newPassword) },
                        enabled = isValid,
                        tint = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
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
            MistyCard(modifier = Modifier.fillMaxWidth()) {
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
            MistyPillActionButton(
                text = stringResource(R.string.settings_login_logout),
                onClick = onLogout,
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                tint = Danger,
            )
        }
    }
}

@Composable
private fun HelpTab(
    viewModel: ProfileViewModel,
    onNavigateToTCPTest: (() -> Unit)? = null,
    onNavigateSubpage: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        MistyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about)) },
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    },
                    modifier = Modifier.clickable { onNavigateSubpage(PROFILE_PAGE_ABOUT) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_help)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    },
                    modifier = Modifier.clickable { onNavigateSubpage(PROFILE_PAGE_HELP) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_acknowledgments)) },
                    leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    },
                    modifier = Modifier.clickable { onNavigateSubpage(PROFILE_PAGE_ACKNOWLEDGMENTS) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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

private fun String.toRoleLabel(): String =
    when (lowercase().replace('_', ' ').trim()) {
        "building", "building_admin" -> "Building Admin"
        "tenant", "tenant admin" -> "Organization Admin"
        "super admin" -> "Super Admin"
        "building admin" -> "Building Admin"
        "organization admin" -> "Organization Admin"
        else -> null
    } ?: replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }

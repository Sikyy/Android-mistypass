package com.mistyislet.app.ui.login

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    magicLinkToken: String? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loginSuccess.collect { onLoginSuccess() }
    }

    // Auto-verify if a magic link token was passed via deep link
    LaunchedEffect(magicLinkToken) {
        if (magicLinkToken != null) {
            viewModel.verifyMagicLink(magicLinkToken)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState.authStep) {
            AuthStep.EmailInput -> EmailInputStep(uiState, viewModel)
            AuthStep.OrgLookupLoading -> OrgLookupLoadingStep()
            AuthStep.PasswordInput -> PasswordInputStep(uiState, viewModel)
            AuthStep.MagicLinkSent -> MagicLinkSentStep(uiState, viewModel)
            AuthStep.SSORedirect -> SSORedirectStep(uiState, viewModel)
        }
    }
}

@Composable
private fun EmailInputStep(state: LoginUiState, vm: LoginViewModel) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = { Text(stringResource(R.string.login_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { focusManager.clearFocus(); vm.submitEmail() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { focusManager.clearFocus(); vm.submitEmail() },
            enabled = state.email.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.continue_button)) }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OrgLookupLoadingStep() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.looking_up_org), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PasswordInputStep(state: LoginUiState, vm: LoginViewModel) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf(state.email) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = vm::goBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(state.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.orgAuthConfig?.orgName?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = { Text(stringResource(R.string.login_password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); vm.login() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { focusManager.clearFocus(); vm.login() },
            enabled = state.password.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.login_button))
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { vm.requestMagicLink() }) {
            Text(stringResource(R.string.send_magic_link))
        }

        TextButton(onClick = { showForgotPassword = true }) {
            Text(stringResource(R.string.login_forgot_password))
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false; vm.clearForgotPasswordState() },
            title = { Text(stringResource(R.string.login_reset_password)) },
            text = {
                Column {
                    if (state.forgotPasswordSent) {
                        Text(stringResource(R.string.login_reset_sent))
                    } else {
                        OutlinedTextField(value = forgotEmail, onValueChange = { forgotEmail = it }, label = { Text(stringResource(R.string.login_email)) }, modifier = Modifier.fillMaxWidth())
                        state.forgotPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                if (!state.forgotPasswordSent) TextButton(onClick = { vm.restorePassword(forgotEmail) }) { Text(stringResource(R.string.login_send_reset)) }
            },
            dismissButton = { TextButton(onClick = { showForgotPassword = false; vm.clearForgotPasswordState() }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun MagicLinkSentStep(state: LoginUiState, vm: LoginViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.magic_link_sent_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.magic_link_sent_body, state.email), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            OutlinedButton(onClick = { vm.requestMagicLink() }) { Text(stringResource(R.string.resend)) }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = vm::goBack) { Text(stringResource(R.string.back_to_login)) }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SSORedirectStep(state: LoginUiState, vm: LoginViewModel) {
    val context = LocalContext.current
    val ssoUrl = state.orgAuthConfig?.ssoUrl

    LaunchedEffect(ssoUrl) {
        if (ssoUrl != null) {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, ssoUrl.toUri())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.sso_redirecting, state.orgAuthConfig?.orgName ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = vm::goBack) { Text(stringResource(R.string.cancel)) }
    }
}

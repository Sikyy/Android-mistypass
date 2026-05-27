package com.mistyislet.app.ui.login

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
            AuthStep.MfaInput -> MfaInputStep(uiState, viewModel)
            AuthStep.MagicLinkSent -> MagicLinkSentStep(uiState, viewModel)
            AuthStep.SSORedirect -> SSORedirectStep(uiState, viewModel)
        }
    }
}

@Composable
private fun EmailInputStep(state: LoginUiState, vm: LoginViewModel) {
    val focusManager = LocalFocusManager.current
    val canContinue = state.email.isNotBlank() && !state.isLoading

    AuthStepLayout(
        title = stringResource(R.string.login_email_title),
        body = stringResource(R.string.login_email_body),
        content = {
            AuthTextField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                label = stringResource(R.string.login_email),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        focusManager.clearFocus()
                        vm.submitEmail()
                    },
                ),
            )
            ErrorText(state.errorMessage)
        },
        footer = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    vm.requestMagicLink()
                },
                enabled = canContinue,
                colors = authTextButtonColors(),
            ) {
                Text(stringResource(R.string.send_magic_link))
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    vm.submitEmail()
                },
                enabled = canContinue,
                shape = RoundedCornerShape(999.dp),
                colors = authPrimaryButtonColors(),
            ) {
                Text(stringResource(R.string.continue_button))
            }
        },
    )
}

@Composable
private fun OrgLookupLoadingStep() {
    AuthStepLayout(
        title = stringResource(R.string.looking_up_org),
        body = stringResource(R.string.login_lookup_body),
        content = {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        },
        footer = {},
    )
}

@Composable
private fun PasswordInputStep(state: LoginUiState, vm: LoginViewModel) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf(state.email) }
    val isValid = state.email.isNotBlank() && state.password.isNotBlank() && !state.isLoading

    AuthStepLayout(
        showBack = true,
        onBack = vm::goBack,
        title = stringResource(R.string.login_signin_title),
        body = stringResource(R.string.login_signin_body),
        content = {
            state.orgAuthConfig?.orgName?.let { orgName ->
                OrgHeader(orgName)
                Spacer(Modifier.height(24.dp))
            }

            AuthTextField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                label = stringResource(R.string.login_email),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                label = { Text(stringResource(R.string.login_password)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        vm.login()
                    },
                ),
                shape = RoundedCornerShape(12.dp),
                colors = authTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            ErrorText(state.errorMessage)
        },
        footer = {
            TextButton(onClick = { showForgotPassword = true }, colors = authPlainTextButtonColors()) {
                Text(stringResource(R.string.login_forgot_password))
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    vm.login()
                },
                enabled = isValid,
                shape = RoundedCornerShape(999.dp),
                colors = authPrimaryButtonColors(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(stringResource(R.string.login_button))
                }
            }
        },
    )

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = {
                showForgotPassword = false
                vm.clearForgotPasswordState()
            },
            title = { Text(stringResource(R.string.login_reset_password)) },
            text = {
                Column {
                    if (state.forgotPasswordSent) {
                        Text(stringResource(R.string.login_reset_sent))
                    } else {
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text(stringResource(R.string.login_email)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        state.forgotPasswordError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                if (!state.forgotPasswordSent) {
                    TextButton(onClick = { vm.restorePassword(forgotEmail) }) {
                        Text(stringResource(R.string.login_send_reset))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPassword = false
                        vm.clearForgotPasswordState()
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MfaInputStep(state: LoginUiState, vm: LoginViewModel) {
    val focusManager = LocalFocusManager.current

    AuthStepLayout(
        showBack = true,
        onBack = vm::goBack,
        title = stringResource(R.string.login_mfa_title),
        body = stringResource(R.string.login_mfa_body),
        content = {
            AuthTextField(
                value = state.mfaCode,
                onValueChange = vm::onMfaCodeChange,
                label = stringResource(R.string.login_mfa_code),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        vm.login()
                    },
                ),
            )

            ErrorText(state.errorMessage)
        },
        footer = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    vm.login()
                },
                enabled = state.mfaCode.isNotBlank() && !state.isLoading,
                shape = RoundedCornerShape(999.dp),
                colors = authPrimaryButtonColors(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(stringResource(R.string.login_mfa_verify))
                }
            }
        },
    )
}

@Composable
private fun MagicLinkSentStep(state: LoginUiState, vm: LoginViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.magic_link_sent_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.magic_link_sent_body, state.email),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { vm.requestMagicLink() },
            enabled = !state.isLoading,
            shape = RoundedCornerShape(999.dp),
            colors = authOutlinedButtonColors(),
            border = authOutlinedButtonBorder(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.resend))
        }

        TextButton(onClick = vm::goBack, colors = authTextButtonColors(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_login))
        }

        ErrorText(state.errorMessage, centered = true)
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

    AuthStepLayout(
        showBack = true,
        onBack = vm::goBack,
        title = stringResource(R.string.sso_redirecting, state.orgAuthConfig?.orgName ?: ""),
        body = stringResource(R.string.login_lookup_body),
        content = {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        },
        footer = {},
    )
}

@Composable
private fun AuthStepLayout(
    title: String,
    body: String,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            AppLogo(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))
        content()

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            content = footer,
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(12.dp),
        colors = authTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AppLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .width(58.dp)
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}

@Composable
private fun OrgHeader(orgName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = orgName.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = orgName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ErrorText(errorMessage: String?, centered: Boolean = false) {
    errorMessage?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun authPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = Color.White,
    disabledContainerColor = Color(0xFFE5E5EA),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun authTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
)

@Composable
private fun authPlainTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
)

@Composable
private fun authOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
)

@Composable
private fun authOutlinedButtonBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

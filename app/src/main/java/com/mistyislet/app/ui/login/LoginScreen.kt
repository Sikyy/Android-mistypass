package com.mistyislet.app.ui.login

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
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
import com.mistyislet.app.ui.theme.Graphite
import com.mistyislet.app.ui.theme.Mist
import com.mistyislet.app.ui.theme.Obsidian
import com.mistyislet.app.ui.theme.Smoke

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

    Surface(modifier = Modifier.fillMaxSize(), color = Obsidian) {
        AuthBackground {
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
                color = Mist,
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
        title = stringResource(R.string.login_password_title),
        body = stringResource(R.string.login_password_body),
        content = {
            state.orgAuthConfig?.orgName?.let { OrgHeader(it) }

            AuthTextField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                label = stringResource(R.string.login_email),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(16.dp))

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
            TextButton(onClick = { showForgotPassword = true }, colors = authTextButtonColors()) {
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
                        color = Obsidian,
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
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        state.forgotPasswordError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!state.forgotPasswordSent) {
                    TextButton(onClick = { vm.restorePassword(forgotEmail) }, colors = authTextButtonColors()) {
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
                    colors = authTextButtonColors(),
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
                        color = Obsidian,
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Mist,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.magic_link_sent_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = Mist,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.magic_link_sent_body, state.email),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Smoke,
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
                color = Mist,
            )
        },
        footer = {},
    )
}

@Composable
private fun AuthBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian),
    ) {
        Image(
            painter = painterResource(R.drawable.login_hero_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.14f),
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.54f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                )
        )
        NoiseOverlay(alpha = 0.075f)
        content()
    }
}

@Composable
private fun NoiseOverlay(alpha: Float) {
    val noise = ImageBitmap.imageResource(R.drawable.login_noise)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated)),
            alpha = alpha,
        )
    }
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showBack) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Mist,
                    )
                }
            }
        } else {
            BrandLogo(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 8.dp, bottom = 92.dp),
            )
        }

        if (!showBack) {
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal,
            color = Mist,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = Mist.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(42.dp))
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
private fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.mistyislet_mark_no_r_white),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(78.dp)
            .height(24.dp),
    )
}

@Composable
private fun OrgHeader(orgName: String) {
    Row(
        modifier = Modifier.padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Mist.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = orgName.firstOrNull()?.uppercase() ?: "?",
                color = Mist,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = orgName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Mist,
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
    containerColor = Mist,
    contentColor = Obsidian,
    disabledContainerColor = Mist.copy(alpha = 0.26f),
    disabledContentColor = Obsidian.copy(alpha = 0.52f),
)

@Composable
private fun authTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = Mist,
    disabledContentColor = Mist.copy(alpha = 0.36f),
)

@Composable
private fun authOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = Mist,
    disabledContentColor = Mist.copy(alpha = 0.36f),
)

@Composable
private fun authOutlinedButtonBorder() = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Mist,
    unfocusedTextColor = Mist,
    disabledTextColor = Mist.copy(alpha = 0.38f),
    focusedContainerColor = Color.Black.copy(alpha = 0.30f),
    unfocusedContainerColor = Color.Black.copy(alpha = 0.24f),
    disabledContainerColor = Graphite.copy(alpha = 0.24f),
    cursorColor = Mist,
    focusedBorderColor = Mist.copy(alpha = 0.54f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
    disabledBorderColor = Color.White.copy(alpha = 0.12f),
    focusedLabelColor = Mist,
    unfocusedLabelColor = Mist.copy(alpha = 0.62f),
    disabledLabelColor = Mist.copy(alpha = 0.36f),
    focusedTrailingIconColor = Mist,
    unfocusedTrailingIconColor = Mist.copy(alpha = 0.62f),
)

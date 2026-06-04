package com.mistyislet.app.ui.credentials

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Nfc
import com.mistyislet.app.ui.components.MistyAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.core.nfc.NFCReader
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.theme.IosGreen

enum class BindCardStep {
    WAITING,
    DETECTED,
    BINDING,
    SUCCESS,
    ERROR,
    NO_NFC,
}

@Composable
fun BindCardScreen(
    onBindSuccess: () -> Unit = {},
    viewModel: BindCardViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val nfcReader = remember { NFCReader() }
    val bindState by viewModel.uiState.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(BindCardStep.WAITING) }
    var cardUid by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // 检查 NFC 可用性
    LaunchedEffect(Unit) {
        if (activity == null) return@LaunchedEffect
        if (!nfcReader.isAvailable(activity)) {
            step = BindCardStep.NO_NFC
        } else if (!nfcReader.isEnabled()) {
            errorMsg = ""
            step = BindCardStep.ERROR
        }
    }

    // NFC 生命周期管理
    DisposableEffect(activity, step) {
        if (step == BindCardStep.WAITING) {
            activity?.let { nfcReader.enableReaderMode(it) }
        }
        onDispose {
            activity?.let { nfcReader.disableReaderMode(it) }
        }
    }

    // 监听 NFC 标签事件
    LaunchedEffect(Unit) {
        nfcReader.tagEvents.collect { event ->
            if (step == BindCardStep.WAITING) {
                viewModel.reset()
                cardUid = event.uid
                step = BindCardStep.DETECTED
            }
        }
    }

    LaunchedEffect(bindState.boundCard, bindState.errorMessage) {
        bindState.boundCard?.let {
            step = BindCardStep.SUCCESS
            showSuccessDialog = true
            if (it.cardUid != null) {
                cardUid = it.cardUid
            }
        }
        bindState.errorMessage?.let {
            errorMsg = it
            step = BindCardStep.ERROR
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = stringResource(R.string.nfc_title),
                onBack = onBindSuccess,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = MistyGroupedListPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                MistyGroupedSection {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.nfc_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.nfc_description),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                MistyGroupedSection(title = stringResource(R.string.nfc_scan_step)) {
                    when (step) {
                        BindCardStep.NO_NFC -> NFCMessageRow(
                            iconError = true,
                            title = stringResource(R.string.nfc_no_support),
                            body = null,
                        )
                        BindCardStep.WAITING -> NFCWaitingRow(pulseAlpha = pulseAlpha)
                        BindCardStep.DETECTED, BindCardStep.BINDING, BindCardStep.SUCCESS -> NFCDetectedRow(cardUid = cardUid)
                        BindCardStep.ERROR -> NFCMessageRow(
                            iconError = true,
                            title = errorMsg.ifBlank { stringResource(R.string.nfc_disabled) },
                            body = null,
                        )
                    }
                }
            }

            if (step == BindCardStep.DETECTED || step == BindCardStep.BINDING) {
                item {
                    MistyGroupedSection {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            MistyPillActionButton(
                                text = stringResource(R.string.nfc_bind_card),
                                onClick = {
                                    step = BindCardStep.BINDING
                                    viewModel.bindCard(cardUid)
                                },
                                enabled = step != BindCardStep.BINDING,
                                isLoading = step == BindCardStep.BINDING,
                                fillContent = true,
                                tint = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.reset()
                                    step = BindCardStep.WAITING
                                    cardUid = ""
                                },
                                enabled = step != BindCardStep.BINDING,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.nfc_try_another))
                            }
                        }
                    }
                }
            }

            if (step == BindCardStep.ERROR) {
                item {
                    MistyGroupedSection {
                        TextButton(
                            onClick = {
                                viewModel.reset()
                                step = BindCardStep.WAITING
                                errorMsg = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.nfc_try_again))
                        }
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        MistyAlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(R.string.nfc_card_bound)) },
            text = { Text(stringResource(R.string.nfc_bind_success)) },
            confirmButton = {
                TextButton(onClick = onBindSuccess) {
                    Text(stringResource(R.string.common_done))
                }
            },
        )
    }
}

@Composable
private fun NFCWaitingRow(pulseAlpha: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Nfc,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .alpha(pulseAlpha),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = stringResource(R.string.nfc_hold_near),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.nfc_make_sure_enabled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NFCDetectedRow(cardUid: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = IosGreen,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = stringResource(R.string.nfc_card_detected),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "UID: $cardUid",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NFCMessageRow(iconError: Boolean, title: String, body: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (iconError) Icons.Default.ErrorOutline else Icons.Default.Nfc,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = if (iconError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (iconError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (!body.isNullOrBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

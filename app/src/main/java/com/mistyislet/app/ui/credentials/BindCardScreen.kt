package com.mistyislet.app.ui.credentials

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.core.nfc.NFCReader
import com.mistyislet.app.ui.theme.Success

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

    // 检查 NFC 可用性
    LaunchedEffect(Unit) {
        if (activity == null) return@LaunchedEffect
        if (!nfcReader.isAvailable(activity)) {
            step = BindCardStep.NO_NFC
        } else if (!nfcReader.isEnabled()) {
            errorMsg = "NFC is disabled. Please enable it in Settings."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            BindCardStep.NO_NFC -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This device does not support NFC",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBindSuccess) { Text("Go Back") }
            }

            BindCardStep.WAITING -> {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .alpha(pulseAlpha),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Hold your card against the back of your phone",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Make sure NFC is enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BindCardStep.DETECTED -> {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Card Detected",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "UID: $cardUid",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        step = BindCardStep.BINDING
                        viewModel.bindCard(cardUid)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Bind This Card")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.reset(); step = BindCardStep.WAITING; cardUid = "" }) {
                    Text("Try Another Card")
                }
            }

            BindCardStep.BINDING -> {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Binding card to your account...")
            }

            BindCardStep.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Success,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Card Bound Successfully",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "UID: $cardUid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBindSuccess) { Text("Done") }
            }

            BindCardStep.ERROR -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.reset(); step = BindCardStep.WAITING; errorMsg = "" }) {
                    Text("Try Again")
                }
            }
        }
    }
}

package com.mistyislet.app.ui.credentials

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosRed
import com.mistyislet.app.ui.theme.IosYellow
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun QRPassScreen(
    onBack: () -> Unit,
    viewModel: CredentialsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDoor = uiState.qrDoors.firstOrNull { it.id == uiState.selectedQrDoorId }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f
        if (window != null) {
            window.attributes = window.attributes.apply { screenBrightness = 1.0f }
        }
        onDispose {
            val currentWindow = (context as? Activity)?.window
            if (currentWindow != null) {
                currentWindow.attributes = currentWindow.attributes.apply {
                    screenBrightness = originalBrightness
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        MistyNavigationTopBar(
            title = stringResource(R.string.qrcode_title),
            onBack = onBack,
        )

        if (uiState.qrDoors.isNotEmpty()) {
            QrDoorSelector(
                doors = uiState.qrDoors,
                selectedDoorId = uiState.selectedQrDoorId,
                onSelect = viewModel::selectQrDoor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isQrLoading && uiState.dynamicQrContent == null -> QRPassLoading()
                uiState.qrDoors.isEmpty() -> QRPassEmpty(
                    title = stringResource(R.string.pass_no_doors),
                    description = stringResource(R.string.pass_no_doors_desc),
                )
                selectedDoor == null -> QRPassEmpty(
                    title = stringResource(R.string.pass_select_door),
                    description = null,
                )
                uiState.dynamicQrContent != null -> QRPassContent(
                    token = uiState.dynamicQrContent.orEmpty(),
                    expiresAt = uiState.qrExpiresAt,
                    onRefresh = viewModel::manualRefreshQr,
                )
                else -> QRPassError(
                    message = uiState.qrErrorMessage ?: stringResource(R.string.qrcode_error),
                    onRetry = viewModel::manualRefreshQr,
                )
            }
        }
    }
}

@Composable
private fun QRPassLoading() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.qrcode_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QRPassContent(
    token: String,
    expiresAt: Instant?,
    onRefresh: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val bitmap = remember(token) { generateQRCode(token, 280) }
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR access code",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (expiresAt != null) {
            Spacer(modifier = Modifier.height(20.dp))
            QRPassExpiryTimer(expiresAt = expiresAt)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.pass_present_to_scanner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        MistyPillActionButton(
            text = stringResource(R.string.pass_refresh_qr),
            icon = Icons.Default.Refresh,
            onClick = onRefresh,
            tint = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun QRPassExpiryTimer(expiresAt: Instant) {
    var remaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(expiresAt) {
        while (true) {
            remaining = ((expiresAt.toEpochMilli() - System.currentTimeMillis()) / 1000)
                .toInt()
                .coerceAtLeast(0)
            delay(1000)
        }
    }

    val color = when {
        remaining > 20 -> IosGreen
        remaining > 10 -> IosYellow
        else -> IosRed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = if (remaining > 0) {
                "${stringResource(R.string.pass_refreshes_in)} ${remaining}s"
            } else {
                stringResource(R.string.pass_refreshing)
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = color,
        )
    }
}

@Composable
private fun QRPassError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QRPassIcon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.qrcode_error),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        MistyPillActionButton(
            text = stringResource(R.string.nfc_try_again),
            onClick = onRetry,
            tint = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun QRPassEmpty(
    title: String,
    description: String?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QRPassIcon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QRPassIcon() {
    Icon(
        imageVector = Icons.Default.QrCode2,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

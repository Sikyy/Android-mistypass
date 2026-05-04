package com.mistyislet.app.ui.visitors

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.VisitorPass
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorsScreen(
    viewModel: VisitorsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var qrDialogPassId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateSheet) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_visitor_pass))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                text = stringResource(R.string.visitors_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::loadPasses,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uiState.passes.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.visitors_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    val activePasses = uiState.passes.filter { !isExpired(it) }
                    val expiredPasses = uiState.passes.filter { isExpired(it) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (activePasses.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.visitors_active).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            items(activePasses, key = { it.id }) { pass ->
                                VisitorPassCard(pass, isActive = true, onShowQR = { qrDialogPassId = it })
                            }
                        }
                        if (expiredPasses.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.visitors_expired).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                                )
                            }
                            items(expiredPasses, key = { it.id }) { pass ->
                                VisitorPassCard(pass, isActive = false)
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showCreateSheet) {
        CreateVisitorSheet(
            isCreating = uiState.isCreating,
            onDismiss = viewModel::hideCreateSheet,
            onCreate = { visitor, method, hours ->
                viewModel.createPass(visitor, method, hours)
            },
        )
    }

    // QR Code full-screen dialog
    qrDialogPassId?.let { passId ->
        val passLink = "https://app.mistyislet.com/access-link/$passId"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { qrDialogPassId = null },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp),
                ) {
                    val bitmap = remember(passLink) { generateVisitorQR(passLink, 512) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Visitor QR",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap anywhere to close",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun VisitorPassCard(pass: VisitorPass, isActive: Boolean, onShowQR: (String) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pass.visitor,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    pass.host?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isActive) {
                    val remaining = getRemainingTime(pass)
                    if (remaining != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = remaining,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.visitor_expired),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isActive) {
                val context = LocalContext.current
                val passLink = "https://app.mistyislet.com/access-link/${pass.id}"

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    IconButton(onClick = { onShowQR(pass.id) }) {
                        Icon(Icons.Default.QrCode, contentDescription = "QR", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Visitor Pass", passLink))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Your visitor pass: $passLink")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Visitor Pass"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun isExpired(pass: VisitorPass): Boolean {
    val expiresAt = pass.expiresAt ?: return false
    return try {
        Instant.parse(expiresAt).isBefore(Instant.now())
    } catch (e: Exception) {
        false
    }
}

private fun generateVisitorQR(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

private fun getRemainingTime(pass: VisitorPass): String? {
    val expiresAt = pass.expiresAt ?: return null
    return try {
        val expiry = Instant.parse(expiresAt)
        val now = Instant.now()
        if (expiry.isBefore(now)) return null
        val duration = Duration.between(now, expiry)
        val hours = duration.toHours()
        when {
            hours >= 24 -> "${hours / 24}d ${hours % 24}h"
            hours >= 1 -> "${hours}h"
            else -> "${duration.toMinutes()}m"
        }
    } catch (e: Exception) {
        null
    }
}

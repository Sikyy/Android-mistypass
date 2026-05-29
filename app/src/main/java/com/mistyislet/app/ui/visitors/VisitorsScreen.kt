package com.mistyislet.app.ui.visitors

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mistyislet.app.domain.model.VisitorGroupMember
import com.mistyislet.app.domain.model.VisitorPass
import com.mistyislet.app.ui.components.MistyBottomSheet
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyPagePadding
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosRed
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorsScreen(
    viewModel: VisitorsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var qrDialogPass by remember { mutableStateOf<VisitorPass?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.visitors_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::showCreateSheet) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_visitor_pass),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::loadPasses,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uiState.passes.isEmpty() && !uiState.isLoading) {
                    MistyEmptyState(
                        icon = Icons.Default.PersonAdd,
                        title = stringResource(R.string.visitors_empty),
                    )
                } else {
                    val activePasses = uiState.passes.filter { !isExpired(it) }
                    val expiredPasses = uiState.passes.filter { isExpired(it) }
                    val activeMembers = uiState.groupMembers.filter { it.isActive && !isMemberExpired(it) }
                    val expiredMembers = uiState.groupMembers.filter { !it.isActive || isMemberExpired(it) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = MistyPagePadding,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        uiState.visitorGroup?.let { group ->
                            item(key = "group_header") {
                                MistyGroupedSection(title = stringResource(R.string.visitors_temp_group)) {
                                    VisitorGroupHeader(
                                        group = group,
                                        activeCount = activeMembers.size,
                                    )
                                    if (activeMembers.isNotEmpty() || expiredMembers.isNotEmpty()) {
                                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                                    }
                                    val allMembers = activeMembers.map { it to true } + expiredMembers.map { it to false }
                                    allMembers.forEachIndexed { index, (member, active) ->
                                        GroupMemberRow(member = member, isActive = active)
                                        if (index < allMembers.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                                        }
                                    }
                                    if (expiredMembers.isNotEmpty()) {
                                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            TextButton(
                                                onClick = viewModel::cleanupExpired,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = IosRed,
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    stringResource(R.string.visitors_cleanup_expired),
                                                    color = IosRed,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (activePasses.isNotEmpty()) {
                            item {
                                VisitorPassSection(
                                    title = stringResource(R.string.visitors_active),
                                    passes = activePasses,
                                    isActive = true,
                                    onShowQR = { qrDialogPass = it },
                                )
                            }
                        }
                        if (expiredPasses.isNotEmpty()) {
                            item {
                                VisitorPassSection(
                                    title = stringResource(R.string.visitors_expired),
                                    passes = expiredPasses,
                                    isActive = false,
                                )
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

    qrDialogPass?.let { pass ->
        VisitorQRDialog(
            pass = pass,
            onDismiss = { qrDialogPass = null },
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color, compact: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp,
            ),
    )
}

@Composable
private fun VisitorPassSection(
    title: String,
    passes: List<VisitorPass>,
    isActive: Boolean,
    onShowQR: (VisitorPass) -> Unit = {},
) {
    MistyGroupedSection(title = title) {
        passes.forEachIndexed { index, pass ->
            VisitorPassRow(
                pass = pass,
                isActive = isActive,
                onShowQR = onShowQR,
            )
            if (index < passes.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun VisitorPassRow(pass: VisitorPass, isActive: Boolean, onShowQR: (VisitorPass) -> Unit = {}) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pass.visitor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            StatusBadge(
                text = if (isActive) stringResource(R.string.visitors_active) else stringResource(R.string.visitor_expired),
                color = if (isActive) IosGreen else IosRed,
            )
        }

        pass.host?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isActive) {
            getRemainingTime(pass)?.let { remaining ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.visitors_remaining, remaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = IosOrange,
                )
            }

            val passLink = "https://app.mistyislet.com/access-link/${pass.id}"
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MistyPillActionButton(
                    text = stringResource(R.string.visitor_copy_link),
                    icon = Icons.Default.ContentCopy,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Visitor Pass", passLink))
                        Toast.makeText(context, context.getString(R.string.visitor_link_copied), Toast.LENGTH_SHORT).show()
                    },
                )
                MistyPillActionButton(
                    text = stringResource(R.string.visitor_share_pass),
                    icon = Icons.Default.Share,
                    tint = IosBlue,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.visitor_share_subject))
                            putExtra(Intent.EXTRA_TEXT, passLink)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.visitor_share_title)))
                    },
                )
                MistyPillActionButton(
                    text = "QR",
                    icon = Icons.Default.QrCode,
                    tint = IosGreen,
                    onClick = { onShowQR(pass) },
                )
            }
        }
    }
}

@Composable
private fun VisitorQRDialog(pass: VisitorPass, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val passLink = "https://app.mistyislet.com/access-link/${pass.id}"
    val remaining = getRemainingTime(pass)

    MistyBottomSheet(
        title = stringResource(R.string.pass_title),
        doneLabel = stringResource(R.string.common_done),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pass.visitor,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(290.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(20.dp),
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

            if (remaining != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.visitors_remaining, remaining),
                    style = MaterialTheme.typography.bodyLarge,
                    color = IosOrange,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MistyPillActionButton(
                    text = stringResource(R.string.visitor_share_pass),
                    icon = Icons.Default.Share,
                    tint = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.visitor_share_subject))
                            putExtra(Intent.EXTRA_TEXT, passLink)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.visitor_share_title)))
                    },
                )
                MistyPillActionButton(
                    text = stringResource(R.string.visitor_copy_link),
                    icon = Icons.Default.ContentCopy,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Visitor Pass", passLink))
                        Toast.makeText(context, context.getString(R.string.visitor_link_copied), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun VisitorGroupHeader(
    group: com.mistyislet.app.domain.model.VisitorGroup,
    activeCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        if (activeCount > 0) {
            Text(
                text = "$activeCount",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = IosBlue,
                modifier = Modifier
                    .background(IosBlue.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun GroupMemberRow(member: VisitorGroupMember, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.visitorName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isActive) {
                member.expiresAt?.let { exp ->
                    getMemberRemainingTime(exp)?.let { remaining ->
                        Text(
                            text = remaining,
                            style = MaterialTheme.typography.bodySmall,
                            color = IosOrange,
                        )
                    }
                }
            }
        }
        StatusBadge(
            text = if (isActive) stringResource(R.string.visitors_active) else stringResource(R.string.visitor_expired),
            color = if (isActive) IosGreen else IosRed,
            compact = true,
        )
    }
}

private fun getMemberRemainingTime(expiresAt: String): String? {
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
    } catch (_: Exception) {
        null
    }
}

private fun isExpired(pass: VisitorPass): Boolean {
    val expiresAt = pass.expiresAt ?: return false
    return try {
        Instant.parse(expiresAt).isBefore(Instant.now())
    } catch (_: Exception) {
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
    } catch (_: Exception) {
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
    } catch (_: Exception) {
        null
    }
}

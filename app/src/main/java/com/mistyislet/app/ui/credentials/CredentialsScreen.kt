package com.mistyislet.app.ui.credentials

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyDoorIcon
import com.mistyislet.app.ui.components.MistyLargeTitle
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosRed
import com.mistyislet.app.ui.theme.IosYellow
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val AccessPassBg = Color(0xFF1A1F36)
private val PinPassBg = Color(0xFF0F2027)
private val DeviceCredentialBg = Color(0xFF2C3E50)
private val CardFg = Color.White
private val CardLabel = Color.White.copy(alpha = 0.6f)

enum class PassType { ACCESS_PASS, PIN_PASS, DEVICE_CREDENTIAL }

@Composable
fun CredentialsScreen(
    onNavigateToBindCard: () -> Unit = {},
    onNavigateToQrPass: () -> Unit = {},
    viewModel: CredentialsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CredentialsScreenContent(uiState = uiState, onRefreshQr = viewModel::manualRefreshQr)
}

/**
 * Stateless Pass/credentials content — driven entirely by [uiState] so it can render in the
 * DEBUG parity harness with mock data (no Hilt). This is the exact production UI.
 */
@Composable
internal fun CredentialsScreenContent(
    uiState: CredentialsUiState,
    onRefreshQr: () -> Unit = {},
) {
    var expandedPassId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    DisposableEffect(expandedPassId) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            window.attributes = window.attributes.apply {
                screenBrightness = if (expandedPassId == "access_pass") 1.0f else -1f
            }
        }
        onDispose {
            val win = (context as? android.app.Activity)?.window
            if (win != null) {
                win.attributes = win.attributes.apply {
                    screenBrightness = -1f
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState()),
    ) {
        MistyLargeTitle(text = stringResource(R.string.pass_title))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Access Pass card
            PassCard(
                passId = "access_pass",
                passType = PassType.ACCESS_PASS,
                organizationName = uiState.organizationName,
                placeName = uiState.placeName,
                isExpanded = expandedPassId == "access_pass",
                onTap = {
                    expandedPassId = if (expandedPassId == "access_pass") null else "access_pass"
                },
                qrToken = uiState.dynamicQrContent,
                qrExpiresAt = uiState.qrExpiresAt,
                qrErrorMessage = uiState.qrErrorMessage,
                isQrLoading = uiState.isQrLoading,
                onRefreshQr = onRefreshQr,
            )

            // PIN Pass card
            PassCard(
                passId = "pin_pass",
                passType = PassType.PIN_PASS,
                organizationName = uiState.organizationName,
                placeName = uiState.placeName,
                isExpanded = expandedPassId == "pin_pass",
                onTap = {
                    expandedPassId = if (expandedPassId == "pin_pass") null else "pin_pass"
                },
                pinCode = uiState.pinCode,
                pinExpiresAt = uiState.pinExpiresAt,
            )

            // Device Credential cards (this device's mobile BLE credentials only)
            uiState.mobileCredentials.filter { it.status == "active" && it.platform == "android" }.forEach { cred ->
                PassCard(
                    passId = cred.id,
                    passType = PassType.DEVICE_CREDENTIAL,
                    organizationName = uiState.organizationName,
                    placeName = null,
                    holderName = cred.deviceModel ?: cred.platform,
                    credentialExpiresAt = cred.expiresAt,
                    isExpanded = expandedPassId == cred.id,
                    onTap = {
                        expandedPassId = if (expandedPassId == cred.id) null else cred.id
                    },
                )
            }

            // Wallet pass cards
            uiState.credentials.filter { it.status == "active" }.forEach { cred ->
                PassCard(
                    passId = cred.id,
                    passType = PassType.DEVICE_CREDENTIAL,
                    organizationName = uiState.organizationName,
                    placeName = null,
                    holderName = cred.cardNumber ?: cred.credentialKind,
                    isExpanded = expandedPassId == cred.id,
                    onTap = {
                        expandedPassId = if (expandedPassId == cred.id) null else cred.id
                    },
                )
            }

        }

        Spacer(modifier = Modifier.height(MistyBottomNavInset))
    }
}

@Composable
internal fun QrDoorSelector(
    doors: List<AccessibleDoor>,
    selectedDoorId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        doors.forEach { door ->
            val selected = door.id == selectedDoorId
            Surface(
                onClick = { onSelect(door.id) },
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                border = if (selected) null else androidx.compose.foundation.BorderStroke(
                    0.7.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                ),
            ) {
                Text(
                    text = door.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PassCard(
    passId: String,
    passType: PassType,
    organizationName: String,
    placeName: String?,
    holderName: String? = null,
    isExpanded: Boolean,
    onTap: () -> Unit,
    qrToken: String? = null,
    qrExpiresAt: Instant? = null,
    qrErrorMessage: String? = null,
    isQrLoading: Boolean = false,
    onRefreshQr: () -> Unit = {},
    pinCode: String? = null,
    pinExpiresAt: Instant? = null,
    credentialExpiresAt: String? = null,
) {
    val bgColor = when (passType) {
        PassType.ACCESS_PASS -> AccessPassBg
        PassType.PIN_PASS -> PinPassBg
        PassType.DEVICE_CREDENTIAL -> DeviceCredentialBg
    }
    val cardHeight by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 200.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "passCardHeight",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.985f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "passCardScale",
    )
    val cardShadow by animateDpAsState(
        targetValue = if (isExpanded) 12.dp else 5.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "passCardShadow",
    )

    Surface(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        shadowElevation = cardShadow,
    ) {
        Column {
            // Pass body
            Column(
                modifier = Modifier
                    .height(cardHeight)
                    .padding(20.dp),
            ) {
                // Header row: icon + org name + type badge
                HeaderRow(passType, organizationName)
                Spacer(modifier = Modifier.weight(1f))
                // Primary field
                PrimaryField(passType, holderName)
                Spacer(modifier = Modifier.height(8.dp))
                // Secondary row
                SecondaryRow(passType, placeName, credentialExpiresAt)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Barcode strip (expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                BarcodeStrip(
                    passType = passType,
                    bgColor = bgColor,
                    qrToken = qrToken,
                    qrExpiresAt = qrExpiresAt,
                    qrErrorMessage = qrErrorMessage,
                    isQrLoading = isQrLoading,
                    onRefreshQr = onRefreshQr,
                    pinCode = pinCode,
                    pinExpiresAt = pinExpiresAt,
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(passType: PassType, organizationName: String) {
    val badge = when (passType) {
        PassType.ACCESS_PASS -> "ACCESS"
        PassType.PIN_PASS -> "PIN"
        PassType.DEVICE_CREDENTIAL -> "DEVICE"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            when (passType) {
                PassType.PIN_PASS -> Text(
                    text = "#",
                    color = CardFg,
                    fontSize = 28.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(22.dp),
                )
                PassType.ACCESS_PASS -> Icon(
                    imageVector = MistyDoorIcon,
                    contentDescription = null,
                    tint = CardFg,
                    modifier = Modifier.size(22.dp),
                )
                PassType.DEVICE_CREDENTIAL -> Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = CardFg,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = organizationName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = CardFg,
                maxLines = 1,
            )
        }

        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = CardFg.copy(alpha = 0.8f),
            modifier = Modifier
                .background(CardFg.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PrimaryField(passType: PassType, holderName: String?) {
    val label = when (passType) {
        PassType.ACCESS_PASS -> "ACCESS PASS"
        PassType.PIN_PASS -> "PIN CODE"
        PassType.DEVICE_CREDENTIAL -> "CREDENTIAL"
    }
    val value = when (passType) {
        PassType.ACCESS_PASS -> "Mistyislet Pass"
        PassType.PIN_PASS -> "Door PIN"
        PassType.DEVICE_CREDENTIAL -> holderName ?: "Device Credential"
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CardLabel,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = CardFg,
            maxLines = 1,
        )
    }
}

@Composable
private fun SecondaryRow(
    passType: PassType,
    placeName: String?,
    credentialExpiresAt: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        if (!placeName.isNullOrBlank()) {
            FieldColumn(label = "LOCATION", value = placeName)
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(1f))

        when (passType) {
            PassType.ACCESS_PASS -> FieldColumn(label = "TYPE", value = "QR Access")
            PassType.PIN_PASS -> FieldColumn(label = "TYPE", value = "PIN Code")
            PassType.DEVICE_CREDENTIAL -> credentialExpiresAt?.let {
                FieldColumn(label = "EXPIRES", value = formatPassDate(it))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        FieldColumn(label = "STATUS", value = "Active")
    }
}

private fun formatPassDate(raw: String): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    return runCatching {
        Instant.parse(raw).atZone(ZoneId.systemDefault()).format(formatter)
    }.recoverCatching {
        LocalDate.parse(raw.take(10)).format(formatter)
    }.getOrElse {
        raw.take(10)
    }
}

@Composable
private fun FieldColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
            ),
            color = CardLabel,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = CardFg,
            maxLines = 1,
        )
    }
}

@Composable
private fun BarcodeStrip(
    passType: PassType,
    bgColor: Color,
    qrToken: String?,
    qrExpiresAt: Instant?,
    qrErrorMessage: String?,
    isQrLoading: Boolean,
    onRefreshQr: () -> Unit,
    pinCode: String?,
    pinExpiresAt: Instant?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(color = CardFg.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(12.dp))

        when (passType) {
            PassType.ACCESS_PASS -> {
                if (isQrLoading && qrToken == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = CardFg,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.qrcode_loading),
                        style = MaterialTheme.typography.labelMedium,
                        color = CardLabel,
                    )
                } else if (qrToken != null) {
                    val bitmap = remember(qrToken) { generateQRCode(qrToken, 200) }
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(12.dp),
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(200.dp),
                            )
                        }
                    }

                    if (qrExpiresAt != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ExpiryTimer(expiresAt = qrExpiresAt)
                    }
                } else {
                    Text(
                        text = "#",
                        fontSize = 48.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.Medium,
                        color = CardFg.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pass_qr_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        color = CardLabel,
                    )
                    qrErrorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelSmall,
                            color = CardFg.copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pass_present_to_scanner),
                    style = MaterialTheme.typography.labelSmall,
                    color = CardLabel,
                )
                MistyPillActionButton(
                    text = stringResource(R.string.pass_refresh_qr),
                    icon = Icons.Default.Refresh,
                    onClick = onRefreshQr,
                    tint = CardFg,
                    containerColor = CardFg.copy(alpha = 0.14f),
                    contentColor = CardFg,
                )
            }

            PassType.PIN_PASS -> {
                if (pinCode != null) {
                    PinDisplay(pin = pinCode)

                    if (pinExpiresAt != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ExpiryTimer(expiresAt = pinExpiresAt)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = CardFg.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pass_pin_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        color = CardLabel,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pass_enter_at_keypad),
                    style = MaterialTheme.typography.labelSmall,
                    color = CardLabel,
                )
            }

            PassType.DEVICE_CREDENTIAL -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = IosGreen,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.pass_ble_active),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = CardFg,
                        )
                        Text(
                            text = stringResource(R.string.pass_keystore_protected),
                            style = MaterialTheme.typography.labelMedium,
                            color = CardLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiryTimer(expiresAt: Instant, totalSeconds: Int = 30) {
    var remaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(expiresAt) {
        while (true) {
            remaining = ((expiresAt.toEpochMilli() - System.currentTimeMillis()) / 1000)
                .toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    val color = when {
        remaining > 15 -> IosGreen
        remaining > 5 -> IosYellow
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
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (remaining > 0) stringResource(R.string.pass_refreshes_in) + " ${remaining}s"
                   else stringResource(R.string.pass_refreshing),
            style = MaterialTheme.typography.labelSmall,
            color = CardFg.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PinDisplay(pin: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        pin.forEach { digit ->
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = digit.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.Black,
                )
            }
        }
    }
}

internal fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val pxSize = size * 3
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, pxSize, pxSize)
        val bitmap = Bitmap.createBitmap(pxSize, pxSize, Bitmap.Config.ARGB_8888)
        val black = Color.Black.toArgb()
        val white = Color.White.toArgb()
        for (x in 0 until pxSize) {
            for (y in 0 until pxSize) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) black else white)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

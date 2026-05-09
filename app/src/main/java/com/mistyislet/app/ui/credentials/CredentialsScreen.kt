package com.mistyislet.app.ui.credentials

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mistyislet.app.R
import kotlinx.coroutines.delay
import java.time.Instant

private val AccessPassBg = Color(0xFF1A1F36)
private val PinPassBg = Color(0xFF0F2027)
private val DeviceCredentialBg = Color(0xFF2C3E50)
private val CardFg = Color.White
private val CardLabel = Color.White.copy(alpha = 0.6f)

enum class PassType { ACCESS_PASS, PIN_PASS, DEVICE_CREDENTIAL }

@Composable
fun CredentialsScreen(
    onNavigateToBindCard: () -> Unit = {},
    viewModel: CredentialsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.pass_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )

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

            // Add to Google Wallet section
            GoogleWalletSection()
        }

        Spacer(modifier = Modifier.height(100.dp))
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
    pinCode: String? = null,
    pinExpiresAt: Instant? = null,
) {
    val bgColor = when (passType) {
        PassType.ACCESS_PASS -> AccessPassBg
        PassType.PIN_PASS -> PinPassBg
        PassType.DEVICE_CREDENTIAL -> DeviceCredentialBg
    }

    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Column {
            // Pass body
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                // Header row: icon + org name + type badge
                HeaderRow(passType, organizationName)
                Spacer(modifier = Modifier.height(24.dp))
                // Primary field
                PrimaryField(passType, holderName)
                Spacer(modifier = Modifier.height(12.dp))
                // Secondary row
                SecondaryRow(passType, placeName)
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
                    pinCode = pinCode,
                    pinExpiresAt = pinExpiresAt,
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(passType: PassType, organizationName: String) {
    val icon: ImageVector = when (passType) {
        PassType.ACCESS_PASS -> Icons.Default.QrCode2
        PassType.PIN_PASS -> Icons.Default.Pin
        PassType.DEVICE_CREDENTIAL -> Icons.Default.PhoneAndroid
    }
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CardFg,
                modifier = Modifier.size(22.dp),
            )
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
private fun SecondaryRow(passType: PassType, placeName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (!placeName.isNullOrBlank()) {
            FieldColumn(label = "LOCATION", value = placeName)
        }

        when (passType) {
            PassType.ACCESS_PASS -> FieldColumn(label = "TYPE", value = "QR Access")
            PassType.PIN_PASS -> FieldColumn(label = "TYPE", value = "PIN Code")
            PassType.DEVICE_CREDENTIAL -> {}
        }

        FieldColumn(label = "STATUS", value = "Active")
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
                if (qrToken != null) {
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
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = CardFg.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pass_qr_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        color = CardLabel,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pass_present_to_scanner),
                    style = MaterialTheme.typography.labelSmall,
                    color = CardLabel,
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
                        imageVector = Icons.Default.Pin,
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
                        tint = Color(0xFF4CAF50),
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
private fun ExpiryTimer(expiresAt: Instant) {
    var remaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(expiresAt) {
        while (true) {
            remaining = ((expiresAt.toEpochMilli() - System.currentTimeMillis()) / 1000)
                .toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    val dotColor = when {
        remaining > 15 -> Color(0xFF4CAF50)
        remaining > 5 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${stringResource(R.string.pass_refreshes_in)} ${remaining}s",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Normal,
            ),
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
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun GoogleWalletSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isInIndonesia = remember {
        val tm = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        val simCountry = tm?.simCountryIso?.lowercase() ?: ""
        val networkCountry = tm?.networkCountryIso?.lowercase() ?: ""
        simCountry == "id" || networkCountry == "id"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Box(
            modifier = Modifier
                .width(250.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Color.Black.copy(alpha = if (isInIndonesia) 0.2f else 0.8f))
                .clickable(enabled = !isInIndonesia) { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.add_to_google_wallet),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.pass_wallet_region_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

private fun generateQRCode(content: String, size: Int): Bitmap? {
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

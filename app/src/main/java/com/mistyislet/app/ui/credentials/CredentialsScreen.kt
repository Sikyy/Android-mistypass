package com.mistyislet.app.ui.credentials

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mistyislet.app.R

private val AccessPassGradient = listOf(Color(0xFF4F55FF), Color(0xFF6C63FF))
private val GoogleWalletGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
private val VisitorPassGradient = listOf(Color(0xFF0F9D58), Color(0xFF0B8043))
private val PhysicalCardGradient = listOf(Color(0xFF37474F), Color(0xFF263238))

@Composable
fun CredentialsScreen(
    onNavigateToBindCard: () -> Unit = {},
    viewModel: CredentialsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedCardIndex by remember { mutableStateOf<Int?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasNfc = remember { android.nfc.NfcAdapter.getDefaultAdapter(context) != null }
    val isGoogleWalletAvailable = remember {
        val tm = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        val simCountry = tm?.simCountryIso?.lowercase() ?: ""
        val networkCountry = tm?.networkCountryIso?.lowercase() ?: ""
        simCountry != "id" && networkCountry != "id"
    }

    val walletCards = remember(uiState.userId, uiState.credentials, uiState.dynamicQrContent) {
        buildWalletCards(uiState.userId, uiState.dynamicQrContent, uiState.credentials)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.credentials_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            )

            // Stacked wallet view
            WalletStack(
                cards = walletCards,
                onCardTap = { index -> expandedCardIndex = index },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bind Physical Card button (only if device has NFC)
            if (hasNfc) {
                OutlinedButton(
                    onClick = onNavigateToBindCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.bind_physical_card), style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Google Wallet button — hidden in regions where Google Wallet is unavailable (e.g. Indonesia)
            if (isGoogleWalletAvailable) {
                OutlinedButton(
                    onClick = { /* TODO: Google Wallet integration */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_to_google_wallet), style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Overlay when card is expanded
        AnimatedVisibility(
            visible = expandedCardIndex != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            // Scrim - tap anywhere to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        expandedCardIndex = null
                    },
            )
        }

        // Expanded card overlay
        AnimatedVisibility(
            visible = expandedCardIndex != null,
            enter = fadeIn(tween(250)) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                expandFrom = Alignment.CenterVertically,
            ),
            exit = fadeOut(tween(200)) + shrinkVertically(
                animationSpec = tween(250),
                shrinkTowards = Alignment.CenterVertically,
            ),
            modifier = Modifier.align(Alignment.Center),
        ) {
            expandedCardIndex?.let { index ->
                if (index < walletCards.size) {
                    ExpandedWalletCard(
                        card = walletCards[index],
                        onCollapse = { expandedCardIndex = null },
                    )
                }
            }
        }
    }
}

data class WalletCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val qrContent: String?,
    val type: CardType,
)

enum class CardType { ACCESS_PASS, GOOGLE_WALLET, VISITOR_PASS, PHYSICAL_CARD }

private fun buildWalletCards(
    userId: String?,
    dynamicQrContent: String?,
    credentials: List<com.mistyislet.app.domain.model.Credential>,
): List<WalletCard> {
    val cards = mutableListOf<WalletCard>()

    cards.add(
        WalletCard(
            id = "access_pass",
            title = "Mistyislet Pass",
            subtitle = userId ?: "Access Pass",
            gradient = AccessPassGradient,
            qrContent = dynamicQrContent ?: "mistyislet://access/${userId ?: "unknown"}",
            type = CardType.ACCESS_PASS,
        )
    )

    credentials.forEach { cred ->
        cards.add(
            WalletCard(
                id = cred.id,
                title = when (cred.credentialKind) {
                    "google_wallet" -> "Google Wallet Pass"
                    "physical_card" -> "Physical Card"
                    else -> "Access Link"
                },
                subtitle = cred.cardNumber ?: cred.status,
                gradient = when (cred.credentialKind) {
                    "google_wallet" -> GoogleWalletGradient
                    "physical_card" -> PhysicalCardGradient
                    else -> VisitorPassGradient
                },
                qrContent = cred.saveLink,
                type = when (cred.credentialKind) {
                    "google_wallet" -> CardType.GOOGLE_WALLET
                    "physical_card" -> CardType.PHYSICAL_CARD
                    else -> CardType.VISITOR_PASS
                },
            )
        )
    }

    return cards
}

@Composable
private fun WalletStack(
    cards: List<WalletCard>,
    onCardTap: (Int) -> Unit,
) {
    val stackOffset = 56

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(((cards.size - 1) * stackOffset + 200).dp),
    ) {
        cards.forEachIndexed { index, card ->
            val yOffset = index * stackOffset

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (yOffset * density).toInt()) }
                    .zIndex((cards.size - index).toFloat())
                    .clickable { onCardTap(index) },
            ) {
                WalletCardCompact(card = card)
            }
        }
    }
}

@Composable
private fun WalletCardCompact(card: WalletCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(card.gradient)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (card.type) {
                                CardType.ACCESS_PASS -> Icons.Default.QrCode2
                                CardType.GOOGLE_WALLET -> Icons.Default.Wallet
                                CardType.VISITOR_PASS -> Icons.Default.Person
                                CardType.PHYSICAL_CARD -> Icons.Default.CreditCard
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }

                Column {
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 1.sp),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    if (card.type == CardType.ACCESS_PASS) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TAP TO SHOW QR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedWalletCard(
    card: WalletCard,
    onCollapse: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { /* consume click so scrim doesn't trigger */ },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.linearGradient(card.gradient)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (card.type) {
                                CardType.ACCESS_PASS -> Icons.Default.QrCode2
                                CardType.GOOGLE_WALLET -> Icons.Default.Wallet
                                CardType.VISITOR_PASS -> Icons.Default.Person
                                CardType.PHYSICAL_CARD -> Icons.Default.CreditCard
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                        Text(
                            text = card.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // QR Code
                if (card.qrContent != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                    ) {
                        QRCodeImage(content = card.qrContent, size = 220)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = when (card.type) {
                            CardType.ACCESS_PASS -> stringResource(R.string.qr_my_code_hint)
                            else -> card.subtitle
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No QR code available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.tap_to_close),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun QRCodeImage(content: String, size: Int) {
    val bitmap = remember(content, size) { generateQRCode(content, size) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(size.dp),
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
    } catch (e: Exception) {
        null
    }
}

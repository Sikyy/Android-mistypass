package com.mistyislet.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.ble.BLEAuthClient
import com.mistyislet.app.core.ble.KeystoreManager
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Developer-only screen for testing BLE authentication via TCP simulator.
 * Connects to gateway-agent's TCP listener on the Mac and performs the
 * full v2 challenge-response handshake.
 */
@HiltViewModel
class TCPAuthTestViewModel @Inject constructor(
    private val bleClient: BLEAuthClient,
    private val keystoreManager: KeystoreManager,
) : ViewModel() {

    private val _result = MutableStateFlow("")
    val result = _result.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _publicKeyPEM = MutableStateFlow("")
    val publicKeyPEM = _publicKeyPEM.asStateFlow()

    private val _keystoreLevel = MutableStateFlow("")
    val keystoreLevel = _keystoreLevel.asStateFlow()

    init {
        loadKeyInfo()
    }

    private fun loadKeyInfo() {
        try {
            if (!keystoreManager.hasKeyPair()) {
                keystoreManager.generateKeyPair()
            }
            _publicKeyPEM.value = keystoreManager.getPublicKeyPEM()
            _keystoreLevel.value = keystoreManager.getKeystoreLevel()
        } catch (e: Exception) {
            _publicKeyPEM.value = "Error: ${e.message}"
        }
    }

    fun generateNewKeyPair() {
        try {
            keystoreManager.generateKeyPair()
            _publicKeyPEM.value = keystoreManager.getPublicKeyPEM()
            _keystoreLevel.value = keystoreManager.getKeystoreLevel()
            _result.value = "New key pair generated (${_keystoreLevel.value})"
        } catch (e: Exception) {
            _result.value = "Key gen error: ${e.message}"
        }
    }

    fun runTCPAuth(host: String, port: Int, userId: String) {
        if (_isRunning.value) return
        _isRunning.value = true
        _result.value = "Connecting to $host:$port..."

        viewModelScope.launch {
            val authResult = bleClient.authenticateViaTCP(host, port, userId)
            _result.value = when (authResult) {
                is BLEAuthClient.AuthResult.Granted -> "0x01 — ACCESS GRANTED: ${authResult.reason}"
                is BLEAuthClient.AuthResult.Denied -> "0x${
                    String.format("%02X", authResult.code)
                } — DENIED: ${authResult.reason}"
                is BLEAuthClient.AuthResult.Error -> "ERROR: ${authResult.message}"
            }
            _isRunning.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TCPAuthTestScreen(
    onBack: () -> Unit,
    viewModel: TCPAuthTestViewModel = hiltViewModel(),
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9900") }
    var userId by remember { mutableStateOf("usr_test_001") }

    val result by viewModel.result.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val publicKeyPEM by viewModel.publicKeyPEM.collectAsState()
    val keystoreLevel by viewModel.keystoreLevel.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = "TCP Auth Test",
                onBack = onBack,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = MistyGroupedListPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MistyGroupedSection(title = "Connection") {
                    InlineEditRow(label = "Host", value = host, onValueChange = { host = it }, placeholder = "Gateway IP")
                    HorizontalDivider(modifier = Modifier.padding(start = 66.dp))
                    InlineEditRow(label = "Port", value = port, onValueChange = { port = it }, placeholder = "9900")
                }
            }

            item {
                MistyGroupedSection(title = "Device Identity") {
                    InlineEditRow(label = "User ID", value = userId, onValueChange = { userId = it }, placeholder = "(none)")
                    if (keystoreLevel.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        InfoRow(label = "Keystore", value = keystoreLevel)
                    }
                    if (publicKeyPEM.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Public Key (PEM)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = { clipboard.setText(AnnotatedString(publicKeyPEM)) },
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                ) {
                                    Text("Copy", fontSize = 12.sp)
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = publicKeyPEM,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    maxLines = 8,
                                )
                            }
                        }
                    } else {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        TextButton(
                            onClick = { viewModel.generateNewKeyPair() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text("Generate Key Pair")
                        }
                    }
                }
            }

            item {
                MistyGroupedSection(title = "Test") {
                    TextButton(
                        onClick = { viewModel.runTCPAuth(host, port.toIntOrNull() ?: 9900, userId) },
                        enabled = !isRunning && host.isNotBlank() && userId.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isRunning) "Authenticating..." else "Run TCP Auth")
                    }
                    if (result.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        Text(
                            text = result,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (result.contains("GRANTED")) IosGreen else IosRed,
                        )
                    }
                }
            }

            item {
                MistyGroupedSection(title = "Protocol Info") {
                    InfoRow(label = "Challenge", value = "52 bytes (v2)")
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    InfoRow(label = "Signing", value = "SHA256(nonce||userId||'BLE')")
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    InfoRow(label = "Curve", value = "P-256 (Android Keystore)")
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    InfoRow(label = "Transport Tag", value = "BLE")
                }
            }
        }
    }
}

@Composable
private fun InlineEditRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(64.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(0.58f),
        )
    }
}

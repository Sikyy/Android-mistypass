package com.mistyislet.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.ble.BLEAuthClient
import com.mistyislet.app.core.ble.KeystoreManager
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
        topBar = {
            TopAppBar(
                title = { Text("TCP Auth Test") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Connection settings
            Text("Connection", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Gateway Host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                )
            }

            HorizontalDivider()

            // Key info
            Text("Device Identity", style = MaterialTheme.typography.titleSmall)
            Text("Keystore: $keystoreLevel", style = MaterialTheme.typography.bodySmall)

            if (publicKeyPEM.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Public Key (PEM)", style = MaterialTheme.typography.labelSmall)
                            TextButton(
                                onClick = { clipboard.setText(AnnotatedString(publicKeyPEM)) },
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text("Copy", fontSize = 12.sp)
                            }
                        }
                        SelectionContainer {
                            Text(
                                text = publicKeyPEM.take(120) + "...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.generateNewKeyPair() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                ) {
                    Text("Regen Key")
                }
                Button(
                    onClick = { viewModel.runTCPAuth(host, port.toIntOrNull() ?: 9900, userId) },
                    enabled = !isRunning && host.isNotBlank() && userId.isNotBlank(),
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isRunning) "Connecting..." else "Run TCP Auth")
                }
            }

            HorizontalDivider()

            // Result
            if (result.isNotEmpty()) {
                Text("Result", style = MaterialTheme.typography.titleSmall)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            result.contains("GRANTED") -> MaterialTheme.colorScheme.primaryContainer
                            result.contains("DENIED") || result.contains("ERROR") ->
                                MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Protocol info
            Text("Protocol v2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                "Challenge: 52B | Sign: SHA256(nonce||userId||'BLE') | Curve: P-256",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 11.sp,
            )
        }
    }
}

package com.mistyislet.app.ui.admin

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.domain.model.Camera
import com.mistyislet.app.domain.model.CameraCloudToken
import com.mistyislet.app.domain.model.CameraRecording
import com.mistyislet.app.domain.model.CameraVideoLink
import com.mistyislet.app.ui.admin.components.KpiItem
import com.mistyislet.app.ui.admin.components.StatusSummaryRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminCamerasViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<Camera>>(emptyList())
    val items: StateFlow<List<Camera>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _streamLink = MutableStateFlow<CameraVideoLink?>(null)
    val streamLink: StateFlow<CameraVideoLink?> = _streamLink
    private val _streamLoading = MutableStateFlow(false)
    val streamLoading: StateFlow<Boolean> = _streamLoading
    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError
    private val _cloudState = MutableStateFlow(CameraCloudDataState())
    val cloudState: StateFlow<CameraCloudDataState> = _cloudState

    init {
        viewModelScope.launch { loadData() }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData()
            _isRefreshing.value = false
        }
    }

    fun loadStream(cameraId: String) {
        viewModelScope.launch {
            _streamLoading.value = true
            _streamError.value = null
            _streamLink.value = null
            when (val result = adminRepository.getCameraStream(cameraId)) {
                is ApiResult.Success -> _streamLink.value = result.data
                is ApiResult.Error -> _streamError.value = result.message
                is ApiResult.Exception -> _streamError.value = result.throwable.localizedMessage
            }
            _streamLoading.value = false
        }
    }

    fun clearStream() {
        _streamLink.value = null
        _streamError.value = null
    }

    fun loadCloudStatus(cameraId: String) {
        viewModelScope.launch {
            _cloudState.value = CameraCloudDataState(isLoading = true)

            var token: CameraCloudToken? = null
            var tokenError: String? = null
            when (val result = adminRepository.getCameraCloudToken(cameraId)) {
                is ApiResult.Success -> token = result.data
                is ApiResult.Error -> tokenError = result.message
                is ApiResult.Exception -> tokenError = result.throwable.localizedMessage
            }

            var recordings = emptyList<CameraRecording>()
            var recordingsError: String? = null
            when (val result = adminRepository.getCameraRecordings(cameraId)) {
                is ApiResult.Success -> recordings = result.data
                is ApiResult.Error -> recordingsError = result.message
                is ApiResult.Exception -> recordingsError = result.throwable.localizedMessage
            }

            _cloudState.value = CameraCloudDataState(
                token = token,
                recordings = recordings,
                isLoading = false,
                tokenError = tokenError,
                recordingsError = recordingsError,
            )
        }
    }

    fun clearCloudStatus() {
        _cloudState.value = CameraCloudDataState()
    }

    fun renameCamera(cameraId: String, name: String) {
        viewModelScope.launch {
            adminRepository.renameCamera(cameraId, name)
            loadData()
        }
    }

    fun takeSnapshot(cameraId: String) {
        viewModelScope.launch {
            adminRepository.takeCameraSnapshot(cameraId)
        }
    }

    private suspend fun loadData() {
        when (val result = adminRepository.getCameras()) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

data class CameraCloudDataState(
    val token: CameraCloudToken? = null,
    val recordings: List<CameraRecording> = emptyList(),
    val isLoading: Boolean = false,
    val tokenError: String? = null,
    val recordingsError: String? = null,
)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCamerasScreen(
    onBack: () -> Unit,
    viewModel: AdminCamerasViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var selectedCamera by remember { mutableStateOf<Camera?>(null) }
    var renameTarget by remember { mutableStateOf<Camera?>(null) }
    var renameText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val online = items.count { it.status.lowercase() == "online" }
    val offline = items.count { it.status.lowercase() == "offline" }
    val errorCount = items.count { it.status.lowercase() == "error" }

    AdminListScreen(
        title = stringResource(R.string.dashboard_cameras),
        items = items.map { camera ->
            AdminListItem(
                id = camera.id,
                title = camera.name,
                subtitle = listOfNotNull(camera.provider.ifBlank { null }, camera.host).joinToString(" · ").ifBlank { null },
                trailing = camera.status.replaceFirstChar { it.uppercase() },
                trailingColor = when (camera.status.lowercase()) {
                    "online" -> Color(0xFF35A853)
                    "offline" -> Color(0xFFD93025)
                    "error" -> Color(0xFFFF9800)
                    else -> null
                },
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            val camera = items.find { it.id == item.id }
            if (camera != null) {
                selectedCamera = camera
                viewModel.loadCloudStatus(camera.id)
                if (camera.status.lowercase() == "online") {
                    viewModel.loadStream(camera.id)
                }
            }
        },
        headerContent = {
            StatusSummaryRow(
                items = listOfNotNull(
                    KpiItem(online.toString(), stringResource(R.string.admin_online), Color(0xFF35A853)),
                    KpiItem(offline.toString(), stringResource(R.string.admin_offline), Color(0xFFD93025)),
                    if (errorCount > 0) KpiItem(errorCount.toString(), stringResource(R.string.admin_error), Color(0xFFFF9800)) else null,
                    KpiItem(items.size.toString(), stringResource(R.string.admin_total), Color(0xFF4285F4)),
                ),
            )
        },
    )

    selectedCamera?.let { camera ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedCamera = null
                viewModel.clearStream()
                viewModel.clearCloudStatus()
            },
            sheetState = sheetState,
        ) {
            CameraDetailSheet(
                camera = camera,
                viewModel = viewModel,
                onRename = {
                    renameText = camera.name
                    renameTarget = camera
                    selectedCamera = null
                    viewModel.clearStream()
                    viewModel.clearCloudStatus()
                },
            )
        }
    }

    renameTarget?.let { camera ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.admin_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.admin_enter_new_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameCamera(camera.id, renameText.trim())
                            renameTarget = null
                        }
                    },
                    enabled = renameText.isNotBlank(),
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CameraDetailSheet(
    camera: Camera,
    viewModel: AdminCamerasViewModel,
    onRename: () -> Unit = {},
) {
    val streamLink by viewModel.streamLink.collectAsStateWithLifecycle()
    val streamLoading by viewModel.streamLoading.collectAsStateWithLifecycle()
    val streamError by viewModel.streamError.collectAsStateWithLifecycle()
    val cloudState by viewModel.cloudState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = camera.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_rename), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Video area
        if (camera.status.lowercase() == "online") {
            if (streamLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (streamLink != null) {
                VideoPlayer(
                    url = streamLink!!.videoUrl,
                    onReload = { viewModel.loadStream(camera.id) },
                    onSnapshot = { viewModel.takeSnapshot(camera.id) },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.camera_tap_to_stream),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.camera_offline),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }

        streamError?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD93025),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CloudStatusSection(cloudState = cloudState)

        Spacer(modifier = Modifier.height(16.dp))

        // Camera info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.camera_status), camera.status.replaceFirstChar { it.uppercase() },
                    valueColor = when (camera.status.lowercase()) {
                        "online" -> Color(0xFF35A853)
                        "offline" -> Color(0xFFD93025)
                        "error" -> Color(0xFFFF9800)
                        else -> null
                    })
                InfoRow(stringResource(R.string.camera_vendor), camera.provider)
                camera.host?.let { InfoRow(stringResource(R.string.camera_ip), if (camera.port > 0) "$it:${camera.port}" else it) }
                camera.doorId?.let { InfoRow(stringResource(R.string.camera_door), it) }
            }
        }
    }
}

@Composable
private fun CloudStatusSection(cloudState: CameraCloudDataState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.camera_cloud),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                cloudState.isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                cloudState.tokenError != null -> CameraErrorText(cloudState.tokenError)
                cloudState.token != null -> {
                    val tokenValue = cloudState.token.token ?: cloudState.token.cloudToken
                    InfoRow(
                        label = stringResource(R.string.camera_cloud_token),
                        value = if (tokenValue.isNullOrBlank()) {
                            cloudState.token.status.ifBlank { stringResource(R.string.admin_active_now) }
                        } else {
                            stringResource(R.string.camera_cloud_token_available)
                        },
                    )
                    cloudState.token.expiresAt?.let {
                        InfoRow(stringResource(R.string.camera_cloud_expires), it.take(19))
                    }
                }
                else -> Text(
                    text = stringResource(R.string.camera_cloud_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.camera_recordings),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            when {
                cloudState.isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                cloudState.recordingsError != null -> CameraErrorText(cloudState.recordingsError)
                cloudState.recordings.isEmpty() -> Text(
                    text = stringResource(R.string.camera_no_recordings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> cloudState.recordings.take(3).forEach { recording ->
                    InfoRow(
                        label = recording.title?.ifBlank { null } ?: recording.id,
                        value = listOfNotNull(
                            recording.startedAt?.take(10),
                            recording.durationSeconds?.let { stringResource(R.string.camera_recording_seconds, it) },
                        ).joinToString(" · ").ifBlank { stringResource(R.string.camera_recording_ready) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    url: String,
    onReload: () -> Unit,
    onSnapshot: () -> Unit = {},
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    DisposableEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
        onDispose {}
    }

    Column {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    isPlaying = !isPlaying
                    exoPlayer.playWhenReady = isPlaying
                },
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            IconButton(onClick = onReload) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            IconButton(onClick = onSnapshot) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

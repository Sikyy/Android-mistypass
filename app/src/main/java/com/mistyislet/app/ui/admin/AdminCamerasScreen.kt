package com.mistyislet.app.ui.admin

import android.net.Uri
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import com.mistyislet.app.ui.components.MistyAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminCamerasViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val demoFallback: AdminDemoFallback,
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
        val state = demoFallback.resolveList(adminRepository.getCameras()) { AdminDemoData.cameras }
        _items.value = state.items
        _error.value = state.error
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

private fun Camera.vendorLabel(): String = vendor.ifBlank { provider }

private fun Camera.ipDisplayText(): String? {
    ipAddress?.takeIf { it.isNotBlank() }?.let { return it }
    return host?.takeIf { it.isNotBlank() }?.let { if (port > 0) "$it:$port" else it }
}

private fun Camera.doorDisplayName(): String? =
    doorName?.takeIf { it.isNotBlank() } ?: doorId?.takeIf { it.isNotBlank() }

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

    val online = items.count { it.status.lowercase() == "online" }
    val offline = items.count { it.status.lowercase() == "offline" }
    val errorCount = items.count { it.status.lowercase() == "error" }

    selectedCamera?.let { camera ->
        BackHandler {
            selectedCamera = null
            viewModel.clearStream()
            viewModel.clearCloudStatus()
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                MistyNavigationTopBar(
                    title = camera.name,
                    onBack = {
                        selectedCamera = null
                        viewModel.clearStream()
                        viewModel.clearCloudStatus()
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CameraDetailSheet(
                    camera = camera,
                    viewModel = viewModel,
                    showHeader = false,
                )
            }
        }
        return
    }

    AdminListScreen(
        title = stringResource(R.string.dashboard_cameras),
        items = items.map { camera ->
            AdminListItem(
                id = camera.id,
                title = camera.name,
                subtitle = listOfNotNull(
                    camera.vendorLabel().takeIf { it.isNotBlank() },
                    camera.doorDisplayName(),
                ).joinToString(" · ").ifBlank { null },
                trailing = camera.status.replaceFirstChar { it.uppercase() },
                trailingColor = when (camera.status.lowercase()) {
                    "online" -> IosGreen
                    "offline" -> IosRed
                    "error" -> IosOrange
                    else -> null
                },
                trailingChip = true,
                leadingDotColor = when (camera.status.lowercase()) {
                    "online" -> IosGreen
                    "offline" -> IosRed
                    "error" -> IosOrange
                    else -> IosGray
                },
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.camera_no_cameras),
        emptyDescription = stringResource(R.string.camera_no_cameras_description),
        emptyIcon = Icons.Default.VideocamOff,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        listSectionTitle = stringResource(R.string.hardware_all_cameras),
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
        onItemLongClick = { item ->
            val camera = items.find { it.id == item.id }
            if (camera != null) {
                renameText = camera.name
                renameTarget = camera
            }
        },
        headerContent = {
            StatusSummaryRow(
                items = listOfNotNull(
                    KpiItem(online.toString(), stringResource(R.string.admin_online), IosGreen),
                    KpiItem(offline.toString(), stringResource(R.string.admin_offline), if (offline > 0) IosRed else IosGray),
                    if (errorCount > 0) KpiItem(errorCount.toString(), stringResource(R.string.admin_error), IosOrange) else null,
                    KpiItem(items.size.toString(), stringResource(R.string.admin_total), MaterialTheme.colorScheme.onSurface),
                ),
            )
        },
    )

    renameTarget?.let { camera ->
        MistyAlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.admin_rename)) },
            text = {
                MistyFormTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = stringResource(R.string.admin_enter_new_name),
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
    showHeader: Boolean = true,
) {
    val streamLink by viewModel.streamLink.collectAsStateWithLifecycle()
    val streamLoading by viewModel.streamLoading.collectAsStateWithLifecycle()
    val streamError by viewModel.streamError.collectAsStateWithLifecycle()
    val cloudState by viewModel.cloudState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = MistyGroupedListPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showHeader) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = camera.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRename) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.admin_rename),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        item {
            CameraVideoArea(
                camera = camera,
                streamLink = streamLink,
                streamLoading = streamLoading,
                onLoadStream = { viewModel.loadStream(camera.id) },
                onSnapshot = { viewModel.takeSnapshot(camera.id) },
            )
        }

        streamError?.let { err ->
            item { CameraErrorText(err) }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.camera_info)) {
                InfoRow(
                    stringResource(R.string.camera_status),
                    camera.status.replaceFirstChar { it.uppercase() },
                    valueColor = when (camera.status.lowercase()) {
                        "online" -> IosGreen
                        "offline" -> IosRed
                        "error" -> IosOrange
                        else -> null
                    },
                )
                if (camera.vendorLabel().isNotBlank()) {
                    DetailDivider()
                    InfoRow(stringResource(R.string.camera_vendor), camera.vendorLabel())
                }
                camera.model?.takeIf { it.isNotBlank() }?.let {
                    DetailDivider()
                    InfoRow(stringResource(R.string.camera_model), it)
                }
                camera.ipDisplayText()?.let {
                    DetailDivider()
                    InfoRow(stringResource(R.string.camera_ip), it)
                }
                camera.doorDisplayName()?.let {
                    DetailDivider()
                    InfoRow(stringResource(R.string.camera_door), it)
                }
            }
        }

        item { CloudAccessSection(cloudState = cloudState) }
        item { RecordingsSection(cloudState = cloudState) }
    }
}

@Composable
private fun CameraVideoArea(
    camera: Camera,
    streamLink: CameraVideoLink?,
    streamLoading: Boolean,
    onLoadStream: () -> Unit,
    onSnapshot: () -> Unit,
) {
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
                url = streamLink.videoUrl,
                onReload = onLoadStream,
                onSnapshot = onSnapshot,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(onClick = onLoadStream),
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
}

@Composable
private fun CloudAccessSection(cloudState: CameraCloudDataState) {
    MistyGroupedSection(title = stringResource(R.string.camera_cloud)) {
        when {
            cloudState.isLoading -> DetailLoadingRow()
            cloudState.tokenError != null -> CameraErrorText(cloudState.tokenError)
            cloudState.token != null -> {
                val tokenValue = cloudState.token.token ?: cloudState.token.cloudToken
                cloudState.token.provider?.takeIf { it.isNotBlank() }?.let {
                    InfoRow(
                        label = stringResource(R.string.camera_cloud_provider),
                        value = it,
                    )
                    DetailDivider()
                }
                InfoRow(
                    label = stringResource(R.string.camera_cloud_token),
                    value = if (tokenValue.isNullOrBlank()) {
                        cloudState.token.status.ifBlank { stringResource(R.string.admin_active_now) }
                    } else {
                        stringResource(R.string.camera_cloud_token_available)
                    },
                )
                cloudState.token.expiresAt?.let {
                    DetailDivider()
                    InfoRow(stringResource(R.string.camera_cloud_expires), it.take(19))
                }
            }
            else -> DetailEmptyText(stringResource(R.string.camera_cloud_unavailable))
        }
    }
}

@Composable
private fun RecordingsSection(cloudState: CameraCloudDataState) {
    MistyGroupedSection(title = stringResource(R.string.camera_recordings)) {
        when {
            cloudState.isLoading -> DetailLoadingRow()
            cloudState.recordingsError != null -> CameraErrorText(cloudState.recordingsError)
            cloudState.recordings.isEmpty() -> DetailEmptyText(stringResource(R.string.camera_no_recordings))
            else -> cloudState.recordings.take(3).forEachIndexed { index, recording ->
                InfoRow(
                    label = recording.title?.ifBlank { null } ?: recording.id,
                    value = listOfNotNull(
                        recording.startedAt?.take(10),
                        recording.durationSeconds?.let { stringResource(R.string.camera_recording_seconds, it) },
                    ).joinToString(" · ").ifBlank { stringResource(R.string.camera_recording_ready) },
                )
                if (index < cloudState.recordings.take(3).lastIndex) {
                    DetailDivider()
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
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun DetailLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DetailEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 20.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
    )
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
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

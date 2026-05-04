package com.mistyislet.app.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AccessApi
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.usecase.GetMyDoorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QRCodeUiState(
    val doors: List<AccessibleDoor> = emptyList(),
    val selectedDoor: AccessibleDoor? = null,
    val qrBitmap: Bitmap? = null,
    val remainingSeconds: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val accessApi: AccessApi,
    private val getMyDoorsUseCase: GetMyDoorsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QRCodeUiState())
    val uiState: StateFlow<QRCodeUiState> = _uiState

    private var countdownJob: Job? = null
    private var tokenExpiresIn: Int = 0

    init {
        loadDoors()
    }

    private fun loadDoors() {
        viewModelScope.launch {
            getMyDoorsUseCase.getCached().collect { doors ->
                val unlockableDoors = doors.filter { it.canUnlock }
                _uiState.value = _uiState.value.copy(doors = unlockableDoors)
                // 自动选择第一个门
                if (_uiState.value.selectedDoor == null && unlockableDoors.isNotEmpty()) {
                    selectDoor(unlockableDoors.first())
                }
            }
        }
    }

    fun selectDoor(door: AccessibleDoor) {
        _uiState.value = _uiState.value.copy(selectedDoor = door, qrBitmap = null, error = null)
        generateQRCode(door)
    }

    private fun generateQRCode(door: AccessibleDoor) {
        countdownJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = safeApiCall { accessApi.getBleToken() }) {
                is ApiResult.Success -> {
                    val token = result.data
                    tokenExpiresIn = token.expiresIn
                    // QR 内容格式: mistyislet://qr/{token}?lock_id={lock_id}
                    val qrContent = "mistyislet://qr/${token.bleToken}?lock_id=${door.id}"
                    val bitmap = generateQRBitmap(qrContent, 512)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        qrBitmap = bitmap,
                        remainingSeconds = tokenExpiresIn,
                    )
                    startCountdown()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.throwable.localizedMessage ?: "Network error",
                    )
                }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = tokenExpiresIn
            while (remaining > 0) {
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
                delay(1000)
                remaining--
            }
            // Token 过期，自动刷新
            _uiState.value.selectedDoor?.let { generateQRCode(it) }
        }
    }

    fun refresh() {
        _uiState.value.selectedDoor?.let { generateQRCode(it) }
    }

    private fun generateQRBitmap(content: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }
}

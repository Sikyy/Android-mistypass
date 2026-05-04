package com.mistyislet.app.core.ble

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.mistyislet.app.MainActivity
import com.mistyislet.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service for BLE scanning.
 * Discovers Mistyislet Readers advertising SERVICE_UUID.
 *
 * Duty cycle:
 * - When DoorsScreen is in foreground: LOW_LATENCY (continuous)
 * - When app is open but on other screen: LOW_POWER
 * - Background: OPPORTUNISTIC
 */
class BLEScanService : Service() {

    companion object {
        private const val TAG = "BLEScanService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ble_scanning"

        // Shared flow for discovered devices
        private val _discoveredDevices = MutableStateFlow<List<DiscoveredReader>>(emptyList())
        val discoveredDevices: StateFlow<List<DiscoveredReader>> = _discoveredDevices

        fun clearDevices() {
            _discoveredDevices.value = emptyList()
        }
    }

    data class DiscoveredReader(
        val device: BluetoothDevice,
        val rssi: Int,
        val timestampMs: Long = System.currentTimeMillis(),
    )

    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(MistyisletBleManager.SERVICE_UUID))
        .build()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .setReportDelay(0)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val reader = DiscoveredReader(
                device = result.device,
                rssi = result.rssi,
            )
            val current = _discoveredDevices.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.device.address == reader.device.address }
            if (existingIndex >= 0) {
                current[existingIndex] = reader
            } else {
                current.add(reader)
            }
            // Remove stale entries (> 30 seconds old)
            val now = System.currentTimeMillis()
            _discoveredDevices.value = current.filter { now - it.timestampMs < 30_000 }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error: $errorCode")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        scanner = btManager?.adapter?.bluetoothLeScanner
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startScanning()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
    }

    private fun startScanning() {
        if (isScanning) return
        if (scanner == null) {
            Log.w(TAG, "BLE scanner not available")
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "BLUETOOTH_SCAN permission not granted")
            return
        }

        try {
            scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            isScanning = true
            Log.i(TAG, "BLE scanning started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan", e)
        }
    }

    private fun stopScanning() {
        if (!isScanning) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            scanner?.stopScan(scanCallback)
            isScanning = false
            Log.i(TAG, "BLE scanning stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop BLE scan", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE Scanning",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Scanning for nearby door readers"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Scanning for nearby doors")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

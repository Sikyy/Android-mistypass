package com.mistyislet.app.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mistyislet.app.MainActivity
import com.mistyislet.app.R
import com.mistyislet.app.core.storage.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@AndroidEntryPoint
class MistyisletMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var httpClient: OkHttpClient

    companion object {
        private const val TAG = "FCM"

        const val CHANNEL_SECURITY = "security_alerts"
        const val CHANNEL_CREDENTIAL = "credential_alerts"
        const val CHANNEL_ACCESS = "access_updates"
        const val CHANNEL_VISITOR = "visitor_updates"

        fun createNotificationChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channels = listOf(
                NotificationChannel(CHANNEL_SECURITY, "Security Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Security-related notifications"
                },
                NotificationChannel(CHANNEL_CREDENTIAL, "Credential Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Credential status changes"
                },
                NotificationChannel(CHANNEL_ACCESS, "Access Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Door access updates"
                },
                NotificationChannel(CHANNEL_VISITOR, "Visitor Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Visitor arrival notifications"
                },
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: ${token.take(10)}...")
        if (tokenStore.accessToken == null) return

        val deviceId = "${Build.BOARD}_${Build.FINGERPRINT.hashCode().toUInt()}"
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = """{"fcm_token":"$token","device_id":"$deviceId","device_model":"$deviceModel","platform":"android"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                val httpRequest = Request.Builder()
                    .url("${com.mistyislet.app.BuildConfig.API_BASE_URL}app/devices/register")
                    .post(body)
                    .build()
                val response = httpClient.newCall(httpRequest).execute()
                if (response.isSuccessful) {
                    Log.i(TAG, "FCM token registered with backend")
                } else {
                    Log.w(TAG, "FCM register failed: ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "FCM register error", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message received: ${message.data}")

        val type = message.data["type"] ?: return
        val title = message.data["title"] ?: message.notification?.title ?: ""
        val body = message.data["body"] ?: message.notification?.body ?: ""

        val channel = when (type) {
            "door_unlocked" -> CHANNEL_ACCESS
            "visitor_arrived" -> CHANNEL_VISITOR
            "credential_updated", "credential_revoked", "credential_expiring" -> CHANNEL_CREDENTIAL
            "access_changed", "access_revoked" -> CHANNEL_ACCESS
            "door_held_open" -> CHANNEL_SECURITY
            else -> CHANNEL_ACCESS
        }

        showNotification(title, body, channel)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

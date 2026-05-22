package com.mistyislet.app.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mistyislet.app.MainActivity
import com.mistyislet.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MistyisletMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenRegistrar: PushTokenRegistrar

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
        pushTokenRegistrar.registerToken(token)
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

package com.mistyislet.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mistyislet.app.MainActivity
import com.mistyislet.app.core.storage.AppDatabase

/**
 * Compact quick-unlock widget — designed for small placement areas.
 * Shows a single door with a prominent unlock action.
 * Suitable as a home screen shortcut or paired with Android 14+ lock screen shortcuts.
 */
class QuickUnlockWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val topDoor = db.doorDao().getTopDoors().firstOrNull()

        provideContent {
            GlanceTheme {
                CompactWidgetContent(
                    doorName = topDoor?.name,
                    online = topDoor?.gatewayStatus == "online",
                )
            }
        }
    }

    @Composable
    private fun CompactWidgetContent(doorName: String?, online: Boolean) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            if (doorName != null) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "🔓",
                        style = TextStyle(fontSize = 24.sp),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = doorName,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurface,
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (online) "●" else "○",
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = if (online) GlanceTheme.colors.primary else GlanceTheme.colors.outline,
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = if (online) "Ready" else "Offline",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.secondary,
                            ),
                        )
                    }
                }
            } else {
                Column(
                    modifier = GlanceModifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "🔒",
                        style = TextStyle(fontSize = 24.sp),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Mistyislet",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurface,
                        ),
                    )
                }
            }
        }
    }
}

class QuickUnlockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickUnlockWidget()
}

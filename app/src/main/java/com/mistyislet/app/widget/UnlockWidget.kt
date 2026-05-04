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
 * 桌面快速开门 Widget。
 * 从 Room 数据库读取最近的门列表并展示。
 */
class UnlockWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 从 Room 读取真实门数据
        val db = AppDatabase.getInstance(context)
        val doors = db.doorDao().getTopDoors()

        provideContent {
            GlanceTheme {
                WidgetContent(
                    doors = doors.map { DoorItem(it.name, it.gatewayStatus == "online") },
                )
            }
        }
    }

    data class DoorItem(val name: String, val online: Boolean)

    @Composable
    private fun WidgetContent(doors: List<DoorItem>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.widgetBackground),
        ) {
            Text(
                text = "Mistyislet",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (doors.isEmpty()) {
                Text(
                    text = "No doors available",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.secondary,
                    ),
                )
            } else {
                doors.forEachIndexed { index, door ->
                    DoorWidgetItem(name = door.name, online = door.online)
                    if (index < doors.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun DoorWidgetItem(name: String, online: Boolean) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(8.dp)
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (online) "●" else "○",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = if (online) GlanceTheme.colors.primary else GlanceTheme.colors.outline,
                ),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = name,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.onSecondaryContainer,
                ),
            )
        }
    }
}

class UnlockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UnlockWidget()
}

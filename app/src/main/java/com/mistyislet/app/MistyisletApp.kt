package com.mistyislet.app

import android.app.Application
import com.mistyislet.app.core.push.MistyisletMessagingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MistyisletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MistyisletMessagingService.createNotificationChannels(this)
    }
}

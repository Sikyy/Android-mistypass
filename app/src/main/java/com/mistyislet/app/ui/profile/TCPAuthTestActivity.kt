package com.mistyislet.app.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Standalone activity for TCP auth testing.
 * Bypasses login — launch directly via:
 *   adb shell am start -n com.mistyislet.app.debug/com.mistyislet.app.ui.profile.TCPAuthTestActivity
 */
@AndroidEntryPoint
class TCPAuthTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TCPAuthTestScreen(onBack = { finish() })
            }
        }
    }
}

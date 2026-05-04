package com.mistyislet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.mistyislet.app.core.auth.BiometricHelper
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.ui.navigation.AppNavigation
import com.mistyislet.app.ui.profile.ProfileViewModel
import com.mistyislet.app.ui.theme.MistyisletTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var biometricHelper: BiometricHelper

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private var isAuthenticated by mutableStateOf(false)
    private var biometricRequired by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            val enabled = prefs[ProfileViewModel.KEY_BIOMETRIC_ENABLED] ?: false
            val isLoggedIn = authRepository.isLoggedIn()

            if (enabled && isLoggedIn && biometricHelper.isAvailable()) {
                biometricRequired = true
                val success = biometricHelper.authenticate(
                    activity = this@MainActivity,
                    title = "Mistyislet",
                    subtitle = "Verify to access your doors",
                )
                isAuthenticated = success
                if (!success) {
                    // 用户取消验证，关闭 App
                    finish()
                }
            } else {
                isAuthenticated = true
            }
        }

        setContent {
            MistyisletTheme {
                if (isAuthenticated) {
                    AppNavigation(authRepository = authRepository)
                } else {
                    // 等待生物识别验证时显示空白
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                }
            }
        }
    }
}

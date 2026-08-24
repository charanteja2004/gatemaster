package com.gatemaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.gatemaster.app.ui.GateMasterApp
import com.gatemaster.app.ui.theme.GateMasterTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Hold the splash until we know whether this is a first run, so the
        // user never sees the home screen flash before the paper picker.
        var startDestinationReady = false
        var startOnBranchPicker = false
        splash.setKeepOnScreenCondition { !startDestinationReady }

        val preferences = (application as GateMasterApplication).container.userPreferences

        lifecycleScope.launch {
            startOnBranchPicker = !preferences.hasChosenBranch.first()
            startDestinationReady = true

            setContent {
                GateMasterTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        GateMasterApp(startOnBranchPicker = startOnBranchPicker)
                    }
                }
            }
        }
    }
}

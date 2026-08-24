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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.gatemaster.app.ui.GateMasterApp
import com.gatemaster.app.core.model.ThemeMode
import com.gatemaster.app.ui.theme.GateMasterTheme
import com.gatemaster.app.ui.theme.isDark
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
                val themeMode by preferences.themeMode
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
                val dark = themeMode.isDark()
                val view = LocalView.current

                // enableEdgeToEdge picks the bar icon colour from the *system*
                // theme, so choosing Light in-app left white status-bar icons
                // on a white background. Follow the app's theme instead.
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }

                GateMasterTheme(darkTheme = dark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        GateMasterApp(
                            startOnBranchPicker = startOnBranchPicker,
                            isDarkTheme = dark,
                        )
                    }
                }
            }
        }
    }
}

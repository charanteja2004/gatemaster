package com.gatemaster.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.animation.doOnEnd
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

        // Hand-off animation: the splash icon lifts and fades while the app
        // scales up underneath, so launching reads as one continuous motion
        // rather than a splash that vanishes and a screen that appears.
        splash.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            val lift = ObjectAnimator.ofFloat(icon, View.TRANSLATION_Y, 0f, -icon.height * 0.35f)
            val fade = ObjectAnimator.ofFloat(icon, View.ALPHA, 1f, 0f)
            val zoomX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.35f)
            val zoomY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.35f)

            AnimatorSet().apply {
                playTogether(lift, fade, zoomX, zoomY)
                duration = 380L
                interpolator = AccelerateInterpolator(1.6f)
                doOnEnd { provider.remove() }
                start()
            }
        }

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

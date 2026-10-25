package com.tokinote.app

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokinote.app.data.AppViewModel
import com.tokinote.app.data.ThemeMode
import com.tokinote.app.ui.TokiNoteApp
import com.tokinote.app.ui.LocalMotionEnabled
import com.tokinote.app.ui.settings.PendingUpdate
import com.tokinote.app.ui.theme.TokiNoteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        PendingUpdate.installIfPending(this)

        setContent {
            val viewModel: AppViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val view = LocalView.current

            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
                @Suppress("DEPRECATION")
                window.statusBarColor = AndroidColor.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = AndroidColor.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarDividerColor = AndroidColor.TRANSPARENT
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }

            TokiNoteTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                amoled = settings.amoled,
                colorPreset = settings.colorPreset,
            ) {
                CompositionLocalProvider(LocalMotionEnabled provides !settings.reduceMotion) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TokiNoteApp(viewModel)
                    }
                }
            }
        }
    }
}

package com.tokinote.app

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokinote.app.data.AppViewModel
import com.tokinote.app.data.ThemeMode
import com.tokinote.app.ui.TokiNoteApp
import com.tokinote.app.ui.LocalMotionEnabled
import com.tokinote.app.ui.theme.TokiNoteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

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
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
                @Suppress("DEPRECATION")
                window.statusBarColor = AndroidColor.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = AndroidColor.TRANSPARENT
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
                        StatusBarScrim(darkTheme = darkTheme)
                    }
                }
            }
        }
    }
}

/** 状态栏区域从微弱深色过渡到透明，避免和内容出现硬切线。 */
@androidx.compose.runtime.Composable
private fun StatusBarScrim(darkTheme: Boolean) {
    val statusHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(statusHeight + 6.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (darkTheme) 0.42f else 0.08f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

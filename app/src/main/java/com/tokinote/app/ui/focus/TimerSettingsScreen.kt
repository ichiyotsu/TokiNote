package com.tokinote.app.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import com.tokinote.app.ui.components.GlassIconButton
import com.tokinote.app.ui.components.PillGroup
import com.tokinote.app.ui.formatClock

private val focusPresets = listOf(15, 25, 45)
private val shortPresets = listOf(3, 5, 10)
private val longPresets = listOf(10, 15, 30)

/**
 * 计时设置（照 Tomato 的简洁布局）：顶部一个大号时间预览，
 * 三个场景切换，下面只剩预设胶囊、长休息间隔和自动开始。
 */
@Composable
fun TimerSettingsScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    var mode by remember { mutableStateOf(FocusMode.Focus) }
    var showPicker by remember { mutableStateOf(false) }

    val minutes = mode.minutes(settings)
    val presets = when (mode) {
        FocusMode.Focus -> focusPresets
        FocusMode.ShortBreak -> shortPresets
        FocusMode.LongBreak -> longPresets
    }

    fun applyMinutes(value: Int) {
        when (mode) {
            FocusMode.Focus -> onUpdate { it.copy(focusMinutes = value) }
            FocusMode.ShortBreak -> onUpdate { it.copy(shortBreakMinutes = value) }
            FocusMode.LongBreak -> onUpdate { it.copy(longBreakMinutes = value) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
            Text(
                text = stringResource(R.string.settings_timer),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatClock(minutes * 60),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 76.sp,
                    lineHeight = 80.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(22.dp))
            PillGroup(
                options = listOf(
                    FocusMode.Focus to stringResource(R.string.stats_focus),
                    FocusMode.ShortBreak to stringResource(R.string.focus_short_break),
                    FocusMode.LongBreak to stringResource(R.string.focus_long_break),
                ),
                selected = mode,
                onSelect = { mode = it },
            )

            Spacer(Modifier.height(20.dp))
            PillGroup(
                options = presets.map { it to it.toString() },
                selected = minutes.takeIf { it in presets },
                onSelect = { value -> applyMinutes(value) },
                customLabel = stringResource(R.string.timer_custom),
                customSelected = minutes !in presets,
                onCustom = { showPicker = true },
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.timer_sessions),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.sessions_value, settings.sessionsBeforeLongBreak),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StepButton(Icons.Filled.Remove) {
                    onUpdate { it.copy(sessionsBeforeLongBreak = (it.sessionsBeforeLongBreak - 1).coerceAtLeast(1)) }
                }
                Text(
                    text = settings.sessionsBeforeLongBreak.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(64.dp),
                )
                StepButton(Icons.Filled.Add) {
                    onUpdate { it.copy(sessionsBeforeLongBreak = (it.sessionsBeforeLongBreak + 1).coerceAtMost(12)) }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.timer_auto_start),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.timer_auto_start_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.autoStartNext,
                    onCheckedChange = { value -> onUpdate { it.copy(autoStartNext = value) } },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showPicker) {
        TimePickerHost(
            minutes = minutes,
            onApply = {
                applyMinutes(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(44.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}


@androidx.compose.runtime.Composable
private fun TimePickerHost(
    minutes: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    com.tokinote.app.ui.components.M3TimePickerDialog(
        initialHour = minutes / 60,
        initialMinute = minutes % 60,
        title = "选择时长",
        confirmText = "确定",
        cancelText = "取消",
        onDismiss = onDismiss,
        onConfirm = { hour, minute -> onApply((hour * 60 + minute).coerceAtLeast(1)) },
    )
}

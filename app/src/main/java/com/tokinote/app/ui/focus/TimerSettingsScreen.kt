package com.tokinote.app.ui.focus

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.components.PillGroup

private val focusPresets = listOf(15, 25, 45)
private val shortPresets = listOf(3, 5, 10)
private val longPresets = listOf(10, 15, 30)
private val sessionPresets = listOf(2, 4, 6)

/**
 * 专注页专属的计时设置：预设改成胶囊按钮组，末端「自定义」调用系统时间选择器。
 */
@Composable
fun TimerSettingsScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    val context = LocalContext.current
    var showSessionsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        ExpressiveTopBar(
            title = stringResource(R.string.settings_timer),
            alignStart = true,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionHeader(
                title = stringResource(R.string.timer_focus_length),
                value = stringResource(R.string.minutes_value, settings.focusMinutes),
            )
            PillGroup(
                options = focusPresets.map { it to it.toString() },
                selected = settings.focusMinutes.takeIf { it in focusPresets },
                onSelect = { value -> onUpdate { it.copy(focusMinutes = value) } },
                customLabel = stringResource(R.string.timer_custom),
                customSelected = settings.focusMinutes !in focusPresets,
                onCustom = {
                    pickMinutes(
                        context = context,
                        initial = settings.focusMinutes,
                        title = context.getString(R.string.timer_pick_time),
                    ) { minutes -> onUpdate { it.copy(focusMinutes = minutes) } }
                },
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader(
                title = stringResource(R.string.timer_short_length),
                value = stringResource(R.string.minutes_value, settings.shortBreakMinutes),
            )
            PillGroup(
                options = shortPresets.map { it to it.toString() },
                selected = settings.shortBreakMinutes.takeIf { it in shortPresets },
                onSelect = { value -> onUpdate { it.copy(shortBreakMinutes = value) } },
                customLabel = stringResource(R.string.timer_custom),
                customSelected = settings.shortBreakMinutes !in shortPresets,
                onCustom = {
                    pickMinutes(
                        context = context,
                        initial = settings.shortBreakMinutes,
                        title = context.getString(R.string.timer_pick_time),
                    ) { minutes -> onUpdate { it.copy(shortBreakMinutes = minutes) } }
                },
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader(
                title = stringResource(R.string.timer_long_length),
                value = stringResource(R.string.minutes_value, settings.longBreakMinutes),
            )
            PillGroup(
                options = longPresets.map { it to it.toString() },
                selected = settings.longBreakMinutes.takeIf { it in longPresets },
                onSelect = { value -> onUpdate { it.copy(longBreakMinutes = value) } },
                customLabel = stringResource(R.string.timer_custom),
                customSelected = settings.longBreakMinutes !in longPresets,
                onCustom = {
                    pickMinutes(
                        context = context,
                        initial = settings.longBreakMinutes,
                        title = context.getString(R.string.timer_pick_time),
                    ) { minutes -> onUpdate { it.copy(longBreakMinutes = minutes) } }
                },
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader(
                title = stringResource(R.string.timer_sessions),
                value = stringResource(R.string.sessions_value, settings.sessionsBeforeLongBreak),
            )
            PillGroup(
                options = sessionPresets.map { it to it.toString() },
                selected = settings.sessionsBeforeLongBreak.takeIf { it in sessionPresets },
                onSelect = { value -> onUpdate { it.copy(sessionsBeforeLongBreak = value) } },
                customLabel = stringResource(R.string.timer_custom),
                customSelected = settings.sessionsBeforeLongBreak !in sessionPresets,
                onCustom = { showSessionsDialog = true },
            )

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
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

    if (showSessionsDialog) {
        var draft by remember { mutableFloatStateOf(settings.sessionsBeforeLongBreak.toFloat()) }
        AlertDialog(
            onDismissRequest = { showSessionsDialog = false },
            title = { Text(stringResource(R.string.timer_custom_sessions)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.sessions_value, draft.toInt()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        valueRange = 1f..12f,
                        steps = 10,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdate { it.copy(sessionsBeforeLongBreak = draft.toInt()) }
                        showSessionsDialog = false
                    },
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionsDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun pickMinutes(
    context: Context,
    initial: Int,
    title: String,
    onPicked: (Int) -> Unit,
) {
    val safe = initial.coerceIn(1, 180)
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked((hour * 60 + minute).coerceIn(1, 180)) },
        safe / 60,
        safe % 60,
        true,
    ).apply {
        setTitle(title)
    }.show()
}

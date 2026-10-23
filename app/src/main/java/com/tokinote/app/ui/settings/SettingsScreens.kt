package com.tokinote.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import com.tokinote.app.data.ThemeMode
import com.tokinote.app.ui.SettingsPage
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.components.SegmentedColumn
import com.tokinote.app.ui.components.SegmentedListItem
import com.tokinote.app.ui.components.SettingsSectionTitle
import com.tokinote.app.ui.formatMinutes

@Composable
fun SettingsScreen(
    settings: AppSettings,
    noteCount: Int,
    contentPadding: PaddingValues,
    onOpen: (SettingsPage) -> Unit,
) {
    val itemCount = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        ExpressiveTopBar(
            title = stringResource(R.string.settings_title),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            SegmentedColumn {
                SegmentedListItem(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = stringResource(R.string.settings_appearance_sub),
                    icon = Icons.Filled.Palette,
                    index = 0,
                    count = itemCount,
                    onClick = { onOpen(SettingsPage.Appearance) },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.settings_timer),
                    subtitle = stringResource(R.string.settings_timer_sub),
                    icon = Icons.Filled.AccessTime,
                    index = 1,
                    count = itemCount,
                    onClick = { onOpen(SettingsPage.Timer) },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.settings_data),
                    subtitle = stringResource(R.string.settings_data_sub),
                    icon = Icons.Filled.Storage,
                    index = 2,
                    count = itemCount,
                    onClick = { onOpen(SettingsPage.Data) },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_sub, versionName()),
                    icon = Icons.Filled.Info,
                    index = 3,
                    count = itemCount,
                    onClick = { onOpen(SettingsPage.About) },
                    trailing = { Chevron() },
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "${stringResource(R.string.app_name)} · ${stringResource(R.string.notes_count, noteCount)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SettingsDetailHeader(title: String, onBack: () -> Unit) {
    ExpressiveTopBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
    )
}

@Composable
fun AppearanceSettings(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        SettingsDetailHeader(stringResource(R.string.settings_appearance), onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            ChoicePills(
                options = listOf(
                    ThemeMode.System to stringResource(R.string.theme_system),
                    ThemeMode.Light to stringResource(R.string.theme_light),
                    ThemeMode.Dark to stringResource(R.string.theme_dark),
                ),
                selected = settings.themeMode,
                onSelect = { mode -> onUpdate { it.copy(themeMode = mode) } },
            )

            Spacer(Modifier.height(20.dp))
            SettingSwitch(
                title = stringResource(R.string.setting_amoled),
                description = stringResource(R.string.setting_amoled_desc),
                checked = settings.amoled,
                onCheckedChange = { value -> onUpdate { it.copy(amoled = value) } },
            )
            Spacer(Modifier.height(10.dp))
            SettingSwitch(
                title = stringResource(R.string.setting_dynamic),
                description = stringResource(R.string.setting_dynamic_desc),
                checked = settings.dynamicColor,
                onCheckedChange = { value -> onUpdate { it.copy(dynamicColor = value) } },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun TimerSettings(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        SettingsDetailHeader(stringResource(R.string.settings_timer), onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            SettingSlider(
                title = stringResource(R.string.timer_focus_length),
                valueText = stringResource(R.string.minutes_value, settings.focusMinutes),
                value = settings.focusMinutes.toFloat(),
                valueRange = 5f..90f,
                steps = 16,
                onValueChange = { value -> onUpdate { it.copy(focusMinutes = value.toInt()) } },
            )
            SettingSlider(
                title = stringResource(R.string.timer_short_length),
                valueText = stringResource(R.string.minutes_value, settings.shortBreakMinutes),
                value = settings.shortBreakMinutes.toFloat(),
                valueRange = 1f..30f,
                steps = 28,
                onValueChange = { value -> onUpdate { it.copy(shortBreakMinutes = value.toInt()) } },
            )
            SettingSlider(
                title = stringResource(R.string.timer_long_length),
                valueText = stringResource(R.string.minutes_value, settings.longBreakMinutes),
                value = settings.longBreakMinutes.toFloat(),
                valueRange = 5f..60f,
                steps = 10,
                onValueChange = { value -> onUpdate { it.copy(longBreakMinutes = value.toInt()) } },
            )
            SettingSlider(
                title = stringResource(R.string.timer_sessions),
                valueText = stringResource(R.string.sessions_value, settings.sessionsBeforeLongBreak),
                value = settings.sessionsBeforeLongBreak.toFloat(),
                valueRange = 2f..8f,
                steps = 5,
                onValueChange = { value -> onUpdate { it.copy(sessionsBeforeLongBreak = value.toInt()) } },
            )

            Spacer(Modifier.height(12.dp))
            SettingSwitch(
                title = stringResource(R.string.timer_auto_start),
                description = stringResource(R.string.timer_auto_start_desc),
                checked = settings.autoStartNext,
                onCheckedChange = { value -> onUpdate { it.copy(autoStartNext = value) } },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DataSettings(
    noteCount: Int,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    exportJson: () -> String,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        SettingsDetailHeader(stringResource(R.string.settings_data), onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            SegmentedColumn {
                SegmentedListItem(
                    title = stringResource(R.string.data_notes),
                    subtitle = null,
                    icon = Icons.Filled.Storage,
                    index = 0,
                    count = 1,
                    onClick = {},
                    trailing = {
                        Text(
                            text = noteCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(exportJson()))
                    Toast.makeText(context, R.string.data_exported, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.data_export))
            }

            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.data_clear),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.data_clear_title)) },
            text = { Text(stringResource(R.string.data_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.data_clear),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        SettingsDetailHeader(stringResource(R.string.settings_about), onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(88.dp).fillMaxWidth(0.28f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "T",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.about_version, versionName()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.about_inspired),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

/* ----------------------------- building blocks ----------------------------- */

@Composable
private fun <T> ChoicePills(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Composable
private fun versionName(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
}

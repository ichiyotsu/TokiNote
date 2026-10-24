package com.tokinote.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import com.tokinote.app.data.ColorPreset
import com.tokinote.app.data.ReleaseInfo
import com.tokinote.app.data.ThemeMode
import com.tokinote.app.data.UpdateChecker
import com.tokinote.app.ui.SettingsPage
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.components.PillGroup
import com.tokinote.app.ui.components.SegmentedColumn
import com.tokinote.app.ui.components.SegmentedListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    settings: AppSettings,
    noteCount: Int,
    contentPadding: PaddingValues,
    onOpen: (SettingsPage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(28.dp))
            SegmentedColumn {
                SegmentedListItem(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = stringResource(R.string.settings_appearance_sub),
                    icon = Icons.Filled.Palette,
                    index = 0,
                    count = 3,
                    onClick = { onOpen(SettingsPage.Appearance) },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.settings_data),
                    subtitle = stringResource(R.string.settings_data_sub),
                    icon = Icons.Filled.Storage,
                    index = 1,
                    count = 3,
                    onClick = { onOpen(SettingsPage.Data) },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_sub, appVersionName(LocalContext.current)),
                    icon = Icons.Filled.Info,
                    index = 2,
                    count = 3,
                    onClick = { onOpen(SettingsPage.About) },
                    trailing = { Chevron() },
                )
            }
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
            PillGroup(
                options = listOf(
                    ThemeMode.System to stringResource(R.string.theme_system),
                    ThemeMode.Light to stringResource(R.string.theme_light),
                    ThemeMode.Dark to stringResource(R.string.theme_dark),
                ),
                selected = settings.themeMode,
                onSelect = { mode -> onUpdate { it.copy(themeMode = mode) } },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.color_preset),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            PillGroup(
                options = listOf(
                    ColorPreset.Matcha to stringResource(R.string.color_default),
                    ColorPreset.Blue to stringResource(R.string.color_blue),
                    ColorPreset.Purple to stringResource(R.string.color_purple),
                    ColorPreset.Orange to stringResource(R.string.color_orange),
                ),
                selected = settings.colorPreset,
                onSelect = { preset -> onUpdate { it.copy(colorPreset = preset) } },
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
            Spacer(Modifier.height(10.dp))
            SettingSwitch(
                title = stringResource(R.string.setting_show_time),
                description = stringResource(R.string.setting_show_time_desc),
                checked = settings.showTime,
                onCheckedChange = { value -> onUpdate { it.copy(showTime = value) } },
            )
            Spacer(Modifier.height(10.dp))
            SettingSwitch(
                title = stringResource(R.string.setting_motion),
                description = stringResource(R.string.setting_motion_desc),
                checked = settings.reduceMotion,
                onCheckedChange = { value -> onUpdate { it.copy(reduceMotion = value) } },
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
                    count = 3,
                    onClick = {},
                    trailing = {
                        Text(
                            text = noteCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
                SegmentedListItem(
                    title = stringResource(R.string.data_export),
                    subtitle = stringResource(R.string.data_export_sub),
                    icon = Icons.Filled.Code,
                    index = 1,
                    count = 3,
                    onClick = {
                        clipboard.setText(AnnotatedString(exportJson()))
                        Toast.makeText(context, R.string.data_exported, Toast.LENGTH_SHORT).show()
                    },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.data_clear),
                    subtitle = null,
                    icon = Icons.Filled.DeleteForever,
                    index = 2,
                    count = 3,
                    onClick = { showClearDialog = true },
                    trailing = { Chevron() },
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { appVersionName(context) }
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var statusRes by remember { mutableStateOf<Int?>(null) }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var failureRes by remember { mutableStateOf<Int?>(null) }

    fun checkForUpdate() {
        if (checking) return
        checking = true
        statusRes = null
        failureRes = null
        scope.launch {
            val info = withContext(Dispatchers.IO) {
                runCatching { UpdateChecker.latest() }.getOrNull()
            }
            checking = false
            statusRes = when {
                info == null -> R.string.about_check_failed
                UpdateChecker.isNewer(info.version, appVersionName(context)) -> {
                    release = info
                    null
                }

                else -> R.string.about_latest
            }
        }
    }

    fun startDownload(info: ReleaseInfo) {
        val url = info.apkUrl
        if (url == null) {
            failureRes = R.string.update_no_apk
            return
        }
        downloading = true
        progress = 0
        failureRes = null
        scope.launch {
            val destination = File(context.cacheDir, "tokinote-${info.tag}.apk")
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    UpdateChecker.download(url, destination) { percent -> progress = percent }
                }.getOrDefault(false)
            }
            downloading = false
            when {
                !ok -> failureRes = R.string.update_failed
                !ApkInstaller.canInstall(context) -> {
                    ApkInstaller.openPermissionSettings(context)
                    Toast.makeText(context, R.string.update_permission, Toast.LENGTH_LONG).show()
                }

                else -> {
                    ApkInstaller.install(context, destination)
                    release = null
                }
            }
        }
    }

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
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SegmentedColumn {
                SegmentedListItem(
                    title = "TokiNote $version",
                    subtitle = null,
                    icon = Icons.Filled.Info,
                    index = 0,
                    count = 3,
                    onClick = {},
                )
                SegmentedListItem(
                    title = stringResource(R.string.about_check_update),
                    subtitle = statusRes?.let { stringResource(it) },
                    icon = Icons.Filled.SystemUpdate,
                    index = 1,
                    count = 3,
                    onClick = { checkForUpdate() },
                    trailing = {
                        if (checking) {
                            androidx.compose.material3.LoadingIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Chevron()
                        }
                    },
                )
                SegmentedListItem(
                    title = stringResource(R.string.about_github),
                    subtitle = stringResource(R.string.about_github_desc),
                    icon = Icons.Filled.Code,
                    index = 2,
                    count = 3,
                    onClick = { openGitHub(context) },
                    trailing = { Chevron() },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (downloading) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            androidx.compose.material3.Surface(
                color = androidx.compose.ui.graphics.Color(0xFF141414),
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 48.dp)) {
                    Text(
                        text = stringResource(R.string.update_in_progress),
                        style = MaterialTheme.typography.headlineLarge,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                    Spacer(Modifier.height(24.dp))
                    WavyProgress(progress = progress / 100f)
                }
            }
        }
    }

    val currentRelease = release
    if (currentRelease != null && !downloading) {
        AlertDialog(
            onDismissRequest = { if (!downloading) release = null },
            icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
            title = { Text(stringResource(R.string.update_title, currentRelease.version)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.update_current, version),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (currentRelease.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = cleanReleaseNotes(currentRelease.notes),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (downloading) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.update_downloading, progress),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    failureRes?.let { res ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !downloading,
                    onClick = { startDownload(currentRelease) },
                ) {
                    Text(stringResource(R.string.update_download))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !downloading,
                    onClick = { release = null },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private fun cleanReleaseNotes(raw: String): String = raw
    .replace(Regex("(?m)^#{1,6}\\s*"), "")
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("(?m)^[-*]\\s+"), "· ")
    .trim()

private fun openGitHub(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ichiyotsu/TokiNote"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
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

private fun appVersionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "0.1.0"

@Composable
private fun WavyProgress(progress: Float) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
    ) {
        val stroke = 6.dp.toPx()
        val amplitude = 4.dp.toPx()
        val y = size.height / 2f
        drawRoundRect(
            color = androidx.compose.ui.graphics.Color(0x33FFFFFF),
            topLeft = androidx.compose.ui.geometry.Offset(0f, y - stroke / 2f),
            size = androidx.compose.ui.geometry.Size(size.width, stroke),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke / 2f),
        )
        val end = size.width * progress.coerceIn(0f, 1f)
        if (end > 0f) {
            val path = androidx.compose.ui.graphics.Path()
            var x = 0f
            path.moveTo(0f, y)
            while (x <= end) {
                val phase = (x / (18.dp.toPx())) * 2f * Math.PI
                path.lineTo(x, y + (kotlin.math.sin(phase).toFloat() * amplitude))
                x += 2.dp.toPx()
            }
            drawPath(
                path = path,
                color = androidx.compose.ui.graphics.Color(0xFFCFE4FF),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
    }
}
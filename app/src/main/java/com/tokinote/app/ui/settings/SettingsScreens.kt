package com.tokinote.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import com.tokinote.app.data.ColorPreset
import com.tokinote.app.data.ReleaseInfo
import com.tokinote.app.data.ThemeMode
import com.tokinote.app.data.UpdateChecker
import com.tokinote.app.ui.SettingsPage
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.components.GlassIconButton
import com.tokinote.app.ui.components.PillGroup
import com.tokinote.app.ui.components.SegmentedColumn
import com.tokinote.app.ui.components.SegmentedListItem
import com.tokinote.app.ui.components.segmentedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    settings: AppSettings,
    noteCount: Int,
    contentPadding: PaddingValues,
    onOpen: (SettingsPage) -> Unit,
    searchActive: Boolean = false,
    onCloseSearch: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val items = listOf(
        Triple(SettingsPage.Appearance, stringResource(R.string.settings_appearance), stringResource(R.string.settings_appearance_sub)),
        Triple(SettingsPage.Data, stringResource(R.string.settings_data), stringResource(R.string.settings_data_sub)),
        Triple(SettingsPage.About, stringResource(R.string.settings_about), stringResource(R.string.settings_about_sub, appVersionName(LocalContext.current))),
    ).filter { searchActive.not() || query.isBlank() || it.second.contains(query, true) || it.third.contains(query, true) }
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
            if (searchActive) {
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("搜索设置") },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = onCloseSearch) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Filled.Close,
                                contentDescription = null,
                            )
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
            SegmentedColumn {
                items.forEachIndexed { index, item ->
                    val icon = when (item.first) {
                        SettingsPage.Appearance -> Icons.Filled.Palette
                        SettingsPage.Data -> Icons.Filled.Storage
                        SettingsPage.About -> Icons.Filled.Info
                    }
                    SegmentedListItem(
                        title = item.second,
                        subtitle = item.third,
                        icon = icon,
                        index = index,
                        count = items.size.coerceAtLeast(1),
                        onClick = {
                            onCloseSearch()
                            onOpen(item.first)
                        },
                        trailing = { Chevron() },
                    )
                }
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
        titleStartPadding = 8.dp,
        navigationIcon = {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
                modifier = Modifier.padding(start = 20.dp),
            )
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
    val context = LocalContext.current
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
                enabled = !settings.dynamicColor,
                onDisabledClick = {
                    Toast.makeText(context, R.string.need_disable_dynamic, Toast.LENGTH_SHORT).show()
                },
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
    archiveCount: Int,
    trashCount: Int,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    exportJson: () -> String,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showClearDialog by remember { mutableStateOf(false) }
    var cacheBytes by remember { mutableStateOf(dirSize(context.cacheDir)) }

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
                    title = stringResource(R.string.data_archive),
                    subtitle = null,
                    icon = Icons.Filled.Archive,
                    index = 1,
                    count = 3,
                    onClick = onOpenArchive,
                    trailing = {
                        Text(
                            text = archiveCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SegmentedListItem(
                    title = stringResource(R.string.data_trash),
                    subtitle = null,
                    icon = Icons.Filled.Delete,
                    index = 2,
                    count = 3,
                    onClick = onOpenTrash,
                    trailing = {
                        Text(
                            text = trashCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            SegmentedColumn {
                SegmentedListItem(
                    title = stringResource(R.string.data_export),
                    subtitle = stringResource(R.string.data_export_sub),
                    icon = Icons.Filled.Code,
                    index = 0,
                    count = 4,
                    onClick = {
                        clipboard.setText(AnnotatedString(exportJson()))
                        Toast.makeText(context, R.string.data_exported, Toast.LENGTH_SHORT).show()
                    },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.data_cache),
                    subtitle = formatSize(cacheBytes),
                    icon = Icons.Filled.CleaningServices,
                    index = 1,
                    count = 4,
                    onClick = {
                        clearCache(context.cacheDir)
                        cacheBytes = 0L
                    },
                    trailing = { Chevron() },
                )
                SegmentedListItem(
                    title = stringResource(R.string.data_storage),
                    subtitle = stringResource(R.string.data_storage_desc),
                    icon = Icons.Filled.SdStorage,
                    index = 2,
                    count = 4,
                    onClick = {},
                    trailing = {
                        Text(
                            text = formatSize(dirSize(context.filesDir) + dirSize(context.cacheDir)),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SegmentedListItem(
                    title = stringResource(R.string.data_clear),
                    subtitle = null,
                    icon = Icons.Filled.DeleteForever,
                    index = 3,
                    count = 4,
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
                    Text(stringResource(R.string.data_clear), color = MaterialTheme.colorScheme.error)
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

private fun dirSize(dir: java.io.File): Long =
    if (!dir.exists()) 0L else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

private fun clearCache(dir: java.io.File) {
    dir.listFiles()?.forEach { file -> runCatching { file.deleteRecursively() } }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(java.util.Locale.US, "%.1f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> String.format(java.util.Locale.US, "%.0f KB", bytes / (1L shl 10).toDouble())
    else -> "$bytes B"
}
@Composable
private fun UpdateCheckRow(
    status: String?,
    checking: Boolean,
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onCheck: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .heightIn(min = 74.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onCheck)
                .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.about_check_update),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (status != null) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(34.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (checking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                UpdateChannelDropdown(settings = settings, onUpdate = onUpdate)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun UpdateChannelDropdown(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "updateChannelArrow",
    )
    val selectedLabel = if (settings.updateChannel == "beta") "Beta" else "Release"
    var menuAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            menuAppeared = false
            delay(16)
            menuAppeared = true
        } else {
            menuAppeared = false
        }
    }
    val menuScale by animateFloatAsState(
        targetValue = if (menuAppeared) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "updateMenuScale",
    )

    Box {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.graphicsLayer {
                scaleX = menuScale
                scaleY = menuScale
                transformOrigin = TransformOrigin(1f, 0f)
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
        ) {
            listOf("release" to "Release", "beta" to "Beta").forEach { (value, label) ->
                val selected = settings.updateChannel == value
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        onUpdate { it.copy(updateChannel = value) }
                        expanded = false
                    },
                    trailingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                        ),
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember(context) { appVersionName(context) }
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var statusRes by remember { mutableStateOf<Int?>(null) }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var failureRes by remember { mutableStateOf<Int?>(null) }
    var downloadedPath by remember { mutableStateOf<String?>(null) }
    var downloaded by remember { mutableStateOf(false) }
    var showUpdateHelp by remember { mutableStateOf(false) }

    fun checkForUpdate() {
        if (checking || downloading) return
        checking = true
        statusRes = null
        failureRes = null
        release = null
        downloaded = false
        downloadedPath = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { UpdateChecker.latest(settings.updateChannel) }
            }
            checking = false
            val info = result.getOrNull()
            statusRes = when {
                result.isFailure -> R.string.about_check_failed
                info == null -> R.string.about_no_release
                UpdateChecker.isNewer(info.version, appVersionName(context)) -> {
                    release = info
                    null
                }

                else -> R.string.about_latest
            }
        }
    }

    fun startDownload() {
        val info = release ?: return
        val url = info.apkUrl
        if (url == null) {
            failureRes = R.string.update_no_apk
            return
        }
        downloading = true
        progress = 0
        failureRes = null
        downloaded = false
        scope.launch {
            val destination = File(context.cacheDir, "tokinote-${info.tag}.apk")
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    UpdateChecker.download(url, destination) { percent -> progress = percent }
                }.getOrDefault(false)
            }
            downloading = false
            if (ok) {
                downloadedPath = destination.absolutePath
                downloaded = true
            } else {
                failureRes = R.string.update_failed
            }
        }
    }

    fun restartNow() {
        val path = downloadedPath ?: return
        val file = File(path)
        if (!file.exists()) {
            failureRes = R.string.update_failed
            return
        }
        if (!ApkInstaller.canInstall(context)) {
            ApkInstaller.openPermissionSettings(context)
            Toast.makeText(context, R.string.update_permission, Toast.LENGTH_LONG).show()
            return
        }
        if (!ApkInstaller.install(context, file)) {
            failureRes = R.string.update_failed
        }
    }

    fun installLater() {
        val path = downloadedPath ?: return
        PendingUpdate.schedule(context, path)
        Toast.makeText(context, R.string.update_pending_saved, Toast.LENGTH_LONG).show()
        downloaded = false
        downloadedPath = null
        release = null
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
                UpdateCheckRow(
                    status = statusRes?.let { stringResource(it) },
                    checking = checking,
                    settings = settings,
                    onUpdate = onUpdate,
                    onCheck = { checkForUpdate() },
                    shape = segmentedShape(1, 3),
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

            val currentRelease = release
            if (currentRelease != null || downloading || downloaded) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
                            .clickable { showUpdateHelp = !showUpdateHelp },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PriorityHigh,
                            contentDescription = stringResource(R.string.update_help_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    AnimatedVisibility(
                        visible = showUpdateHelp,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Text(
                            text = stringResource(R.string.update_help_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (currentRelease != null) {
                    UpdateStatusCard(
                        info = currentRelease,
                        currentVersion = version,
                        downloading = downloading,
                        progress = progress,
                        downloaded = downloaded,
                        failureRes = failureRes,
                        onDownload = { startDownload() },
                        onRestart = { restartNow() },
                        onLater = { installLater() },
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

}

@Composable
private fun UpdateStatusCard(
    info: ReleaseInfo,
    currentVersion: String,
    downloading: Boolean,
    progress: Int,
    downloaded: Boolean,
    failureRes: Int?,
    onDownload: () -> Unit,
    onRestart: () -> Unit,
    onLater: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (info.isPrerelease) {
                            "TokiNote ${info.version}"
                        } else {
                            stringResource(R.string.update_title, info.version)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.update_current, currentVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (info.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = cleanReleaseNotes(info.notes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }

            if (downloading) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.update_in_progress_short),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.update_downloading, progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (downloaded) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.update_ready),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onLater) {
                        Text(stringResource(R.string.update_later))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onRestart) {
                        Text(stringResource(R.string.update_restart))
                    }
                }
            } else {
                failureRes?.let { res ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.update_download))
                }
            }
        }
    }
}

internal fun cleanReleaseNotes(raw: String): String = raw
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

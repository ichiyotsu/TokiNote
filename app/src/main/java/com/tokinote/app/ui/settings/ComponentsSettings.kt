package com.tokinote.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import com.tokinote.app.data.FocusComponent
import com.tokinote.app.data.UpdateChecker
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ComponentsSettings(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onComponentChanged: () -> Unit,
    showHeader: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(FocusComponent.installed(context)) }
    var installedVersion by remember { mutableStateOf(FocusComponent.installedVersion(context)) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    // 之前只有一个 failed 布尔值，下载、校验、安装三种失败都显示同一句话，
    // 用户不知道该重试还是该等。现在按步骤区分，分别提示。
    var failureRes by remember { mutableStateOf<Int?>(null) }
    var justEnabled by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteFailed by remember { mutableStateOf(false) }

    fun refresh() {
        installed = FocusComponent.installed(context)
        installedVersion = FocusComponent.installedVersion(context)
        onComponentChanged()
    }

    fun download() {
        if (downloading) return
        downloading = true
        progress = 0
        failureRes = null
        justEnabled = false
        deleteFailed = false
        scope.launch {
            val zip = File(context.cacheDir, "focus-component.zip")
            // 先删掉上次残留的半截文件，避免这次失败时校验到旧包。
            zip.delete()
            val failure: Int? = withContext(Dispatchers.IO) {
                runCatching {
                    if (!UpdateChecker.download(FocusComponent.DOWNLOAD_URL, zip) { progress = it }) {
                        return@runCatching R.string.component_download_failed_download
                    }
                    if (!FocusComponent.matchesChecksum(zip, FocusComponent.SHA256)) {
                        android.util.Log.w(
                            "FocusComponent",
                            "checksum mismatch size=${zip.length()}",
                        )
                        return@runCatching R.string.component_download_failed_checksum
                    }
                    when (val result = FocusComponent.installDetailed(context, zip)) {
                        is FocusComponent.InstallResult.Success -> null
                        is FocusComponent.InstallResult.Failed -> {
                            android.util.Log.w("FocusComponent", "install failed: ${result.reason}")
                            R.string.component_download_failed_install
                        }
                    }
                }.getOrElse { R.string.component_download_failed_download }
            }
            downloading = false
            zip.delete()
            if (failure == null) {
                // 关键修复：下载完必须自动启用。之前下载成功后开关仍是关闭的，
                // focusAvailable = enabled && installed 恒为 false，底栏始终
                // 不出现专注入口，用户会以为组件“下载了也不能用”。
                // 开关仍在下面，随时可以关掉。
                onUpdate { it.copy(focusComponentEnabled = true) }
                justEnabled = true
                refresh()
            } else {
                failureRes = failure
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (showHeader) SettingsDetailHeader(stringResource(R.string.settings_components), onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp,
                ),
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.component_focus_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (installed) {
                                stringResource(
                                    R.string.component_status_installed,
                                    installedVersion ?: FocusComponent.VERSION,
                                )
                            } else {
                                stringResource(R.string.component_status_missing)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        if (installed) {
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.component_delete),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.component_focus_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = componentSizeText(FocusComponent.installedSizeBytes(context)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (installed) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.component_enabled),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.component_enabled_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = settings.focusComponentEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdate { it.copy(focusComponentEnabled = enabled) }
                                    onComponentChanged()
                                },
                            )
                        }
                        if (justEnabled && settings.focusComponentEnabled) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.component_download_enabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (!installed) {
                        Spacer(Modifier.height(18.dp))
                        if (downloading) {
                            Text(
                                text = stringResource(R.string.component_downloading),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            if (progress <= 0) {
                                // 总大小未知时 UpdateChecker 不回传进度，
                                // 显示不定进度条而不是卡在 0%。
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            Button(onClick = { download() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.component_download))
                            }
                        }
                    }

                    failureRes?.let { res ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { download() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.component_retry))
                        }
                    }
                    if (deleteFailed) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.component_delete_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.component_local_only),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.component_delete_title)) },
            text = { Text(stringResource(R.string.component_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = FocusComponent.delete(context)
                        confirmDelete = false
                        if (ok) {
                            installed = false
                            installedVersion = null
                            justEnabled = false
                            deleteFailed = false
                            onUpdate { it.copy(focusComponentEnabled = false) }
                            onComponentChanged()
                        } else {
                            deleteFailed = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * 组件实际占用的大小。0.3.7 的组件包只有 428 B，之前按 MB 取整
 * 恒显示“0 MB”，像没装上一样；现在按 B / KB / MB 分档显示。
 */
@Composable
private fun componentSizeText(bytes: Long): String = when {
    bytes < 1024L -> stringResource(R.string.component_size_b, bytes)
    bytes < 1024L * 1024L -> stringResource(R.string.component_size_kb, bytes / 1024f)
    else -> stringResource(R.string.component_size_mb, bytes / (1024f * 1024f))
}

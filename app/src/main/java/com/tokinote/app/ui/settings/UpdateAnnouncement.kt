package com.tokinote.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tokinote.app.R
import com.tokinote.app.data.ReleaseInfo
import com.tokinote.app.data.UpdateChecker
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App-entry update announcement. It only performs the existing GitHub update
 * check and opens a floating bottom card when a newer release exists.
 */
@Composable
fun UpdateAnnouncementHost(channel: String) {
    val context = LocalContext.current
    // Deliberately independent of the dialog composition: "下次一定" downloads
    // in the background after the sheet is dismissed.
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    var info by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val current = currentVersion(context)
        val latest = withContext(Dispatchers.IO) {
            runCatching { UpdateChecker.latest(channel) }.getOrNull()
        }
        if (latest != null && UpdateChecker.isNewer(latest.version, current)) {
            info = latest
        }
    }

    val currentInfo = info ?: return
    val current = currentVersion(context)

    fun download() {
        val url = currentInfo.apkUrl ?: run {
            errorRes = R.string.update_no_apk
            return
        }
        downloading = true
        progress = 0
        errorRes = null
        scope.launch {
            val destination = File(context.cacheDir, "tokinote-${currentInfo.tag}.apk")
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    UpdateChecker.download(url, destination) { progress = it }
                }.getOrDefault(false)
            }
            downloading = false
            when {
                !ok -> errorRes = R.string.update_failed
                !ApkInstaller.canInstall(context) -> {
                    ApkInstaller.openPermissionSettings(context)
                    Toast.makeText(context, R.string.update_permission, Toast.LENGTH_LONG).show()
                }

                ApkInstaller.install(context, destination) -> info = null
                else -> errorRes = R.string.update_failed
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!downloading) info = null },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.update_announcement),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.update_title, currentInfo.version),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.update_current, current),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (currentInfo.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 168.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                                    RoundedCornerShape(16.dp),
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = cleanReleaseNotes(currentInfo.notes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (downloading) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.update_downloading, progress),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    errorRes?.let { res ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            enabled = !downloading && currentInfo.apkUrl != null,
                            onClick = { download() },
                        ) {
                            Text(stringResource(R.string.update_download))
                        }
                    }
                }
            }
        }
    }
}

private fun currentVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "0.0.0"

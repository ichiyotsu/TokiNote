package com.tokinote.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.ReleaseInfo
import com.tokinote.app.data.UpdateChecker
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App-entry update announcement. It only performs the existing GitHub update
 * check and opens a bottom sheet when a newer release exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAnnouncementHost(channel: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = currentVersion(context)

    fun startDownload() {
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

    ModalBottomSheet(
        onDismissRequest = { if (!downloading) info = null },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
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
                Spacer(Modifier.height(14.dp))
                Text(
                    text = cleanReleaseNotes(currentInfo.notes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (downloading) {
                Spacer(Modifier.height(16.dp))
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
            errorRes?.let { res ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = !downloading,
                    onClick = { info = null },
                ) {
                    Text(stringResource(R.string.update_later))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = !downloading && currentInfo.apkUrl != null,
                    onClick = { startDownload() },
                ) {
                    Text(stringResource(R.string.update_download))
                }
            }
        }
    }
}

private fun currentVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "0.0.0"

package com.tokinote.app.ui.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.AlbumFolder
import com.tokinote.app.ui.theme.ExpressiveChipShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 相册里的单张照片（来自某条笔记的 media）。 */
data class AlbumPhoto(
    val noteId: String,
    val path: String,
    val index: Int,
    val date: java.time.LocalDate,
    val noteTitle: String,
)

private val folderDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)

/** 新建 / 编辑相册文件夹：命名 + 多选照片 + 查看添加时间。 */
@Composable
fun AlbumFolderSheet(
    folder: AlbumFolder?,
    photos: List<AlbumPhoto>,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, paths: Set<String>) -> Boolean,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var selected by remember(folder?.id) { mutableStateOf(folder?.paths ?: emptySet()) }
    var nameError by remember(folder?.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val trimmed = name.trim()
    val duplicate = existingNames.any {
        !it.equals(folder?.name, ignoreCase = true) && it.equals(trimmed, ignoreCase = true)
    }

    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(
            stringResource(
                if (folder == null) R.string.folder_new else R.string.folder_edit,
            ),
        )
        if (folder != null) {
            Text(
                text = stringResource(
                    R.string.folder_created_at,
                    Instant.ofEpochMilli(folder.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .format(folderDateFormatter),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
            )
        }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            singleLine = true,
            isError = nameError,
            label = { Text(stringResource(R.string.folder_name)) },
            placeholder = { Text(stringResource(R.string.folder_name_hint)) },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        if (nameError) {
            Text(
                text = stringResource(
                    if (duplicate) R.string.folder_name_exists else R.string.folder_need_name,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.folder_photos),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.group_selected_count, selected.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 330.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(photos, key = { it.path }) { photo ->
                val checked = photo.path in selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            selected = if (checked) {
                                selected - photo.path
                            } else {
                                selected + photo.path
                            }
                        },
                ) {
                    val image = rememberImage(photo.path)
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    if (checked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (folder != null && onDelete != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.folder_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = {
                    nameError = false
                    if (trimmed.isEmpty() || duplicate) {
                        nameError = true
                    } else if (onSave(trimmed, selected)) {
                        onDismiss()
                    } else {
                        nameError = true
                    }
                },
                shape = ExpressiveChipShape,
            ) {
                Text(
                    stringResource(
                        if (folder == null) R.string.folder_create else R.string.group_save,
                    ),
                )
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.folder_delete_title, folder?.name.orEmpty())) },
            text = { Text(stringResource(R.string.folder_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
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

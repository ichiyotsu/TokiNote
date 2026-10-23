package com.tokinote.app.ui.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.Attachments
import com.tokinote.app.data.JournalFile
import com.tokinote.app.data.JournalLocation
import com.tokinote.app.data.Label
import com.tokinote.app.data.Note
import com.tokinote.app.data.TextWeight
import com.tokinote.app.data.documentMimeFilter
import com.tokinote.app.data.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chipDateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

@Composable
fun JournalEditorScreen(
    note: Note,
    labels: List<Label>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onChange: (Note) -> Unit,
    onDelete: () -> Unit,
    onCreateLabel: (String) -> Label?,
    onDeleteLabel: (String) -> Unit,
    onPickLocation: () -> Unit,
    onOpenFile: (JournalFile) -> Unit,
    onOpenMedia: (List<String>, Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEmoji by remember { mutableStateOf(false) }
    var showAttach by remember { mutableStateOf(false) }
    var showFormat by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val paths = withContext(Dispatchers.IO) {
                    uris.mapNotNull { Attachments.copyImage(context, it) }
                }
                if (paths.isNotEmpty()) onChange(note.copy(media = note.media + paths))
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val files = withContext(Dispatchers.IO) {
                    uris.mapNotNull { Attachments.copyDocument(context, it) }
                }
                if (files.isNotEmpty()) onChange(note.copy(files = note.files + files))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(contentPadding)
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showEmoji = true }
                    .padding(4.dp),
            ) {
                EmojiBadge(note.emoji, 40)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { showAttach = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_add),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onBack,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
            IconButton(onClick = { showDelete = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            if (note.media.isNotEmpty()) {
                MediaStrip(
                    paths = note.media,
                    onOpen = { index -> onOpenMedia(note.media, index) },
                    onRemove = { path -> onChange(note.copy(media = note.media - path)) },
                )
                Spacer(Modifier.height(16.dp))
            }

            BasicTextField(
                value = note.title,
                onValueChange = { onChange(note.copy(title = it)) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (note.title.isEmpty()) {
                        Text(
                            text = stringResource(R.string.title_hint),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    inner()
                },
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoChip(
                    icon = Icons.Filled.CalendarMonth,
                    text = note.journalDate.format(chipDateFormatter),
                )
                if (note.labelIds.isNotEmpty()) {
                    InfoChip(
                        icon = Icons.Filled.Label,
                        text = stringResource(R.string.entry_count, note.labelIds.size),
                        onClick = { showLabels = true },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            BasicTextField(
                value = note.body,
                onValueChange = { onChange(note.copy(body = it)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (17 * note.bodySize.scale).sp,
                    lineHeight = (27 * note.bodySize.scale).sp,
                    fontWeight = note.bodyWeight.toFontWeight(),
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                decorationBox = { inner ->
                    if (note.body.isEmpty()) {
                        Text(
                            text = stringResource(R.string.editor_hint),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = (17 * note.bodySize.scale).sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    inner()
                },
            )

            note.files.forEach { file ->
                Spacer(Modifier.height(8.dp))
                FileCard(
                    file = file,
                    onClick = { onOpenFile(file) },
                    onRemove = { onChange(note.copy(files = note.files - file)) },
                )
            }

            note.location?.let { location ->
                Spacer(Modifier.height(10.dp))
                LocationCard(location = location, onRemove = { onChange(note.copy(location = null)) })
            }

            if (note.labelIds.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    labels.filter { it.id in note.labelIds }.forEach { label ->
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = label.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorTool(Icons.Filled.FormatSize, stringResource(R.string.action_format)) { showFormat = true }
            EditorTool(Icons.Filled.Photo, stringResource(R.string.attach_photo)) {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            EditorTool(Icons.Filled.Description, stringResource(R.string.attach_file)) {
                filePicker.launch(documentMimeFilter())
            }
            EditorTool(Icons.Filled.LocationOn, stringResource(R.string.attach_location)) { onPickLocation() }
            EditorTool(Icons.Filled.Label, stringResource(R.string.attach_tags)) { showLabels = true }
        }
    }

    if (showEmoji) {
        EmojiSheet(
            selected = note.emoji,
            onSelect = {
                onChange(note.copy(emoji = it))
                showEmoji = false
            },
            onDismiss = { showEmoji = false },
        )
    }

    if (showAttach) {
        AttachSheet(
            onDismiss = { showAttach = false },
            onPhoto = {
                showAttach = false
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onFile = {
                showAttach = false
                filePicker.launch(documentMimeFilter())
            },
            onLocation = {
                showAttach = false
                onPickLocation()
            },
            onTags = {
                showAttach = false
                showLabels = true
            },
        )
    }

    if (showFormat) {
        FormatSheet(note = note, onChange = onChange, onDismiss = { showFormat = false })
    }

    if (showLabels) {
        LabelSheet(
            labels = labels,
            selectedIds = note.labelIds,
            onToggle = { labelId ->
                val updated = if (labelId in note.labelIds) note.labelIds - labelId else note.labelIds + labelId
                onChange(note.copy(labelIds = updated))
            },
            onCreate = onCreateLabel,
            onDeleteLabel = onDeleteLabel,
            onDismiss = { showLabels = false },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun MediaStrip(
    paths: List<String>,
    onOpen: (Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        paths.take(3).forEachIndexed { index, path ->
            val image = rememberImage(path)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onOpen(index) },
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                IconButton(
                    onClick = { onRemove(path) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_remove),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        repeat((3 - paths.take(3).size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FileCard(
    file: JournalFile,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${file.name.substringAfterLast('.', "").uppercase()} · ${formatFileSize(file.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun LocationCard(location: JournalLocation, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = location.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "%.5f, %.5f".format(location.lat, location.lng),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.location_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EditorTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun TextWeight.toFontWeight(): FontWeight = when (this) {
    TextWeight.Normal -> FontWeight.Normal
    TextWeight.Medium -> FontWeight.Medium
    TextWeight.Bold -> FontWeight.Bold
}

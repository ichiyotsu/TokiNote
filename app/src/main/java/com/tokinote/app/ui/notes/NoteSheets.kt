package com.tokinote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.JournalEmojis
import com.tokinote.app.data.Label
import com.tokinote.app.data.Note
import com.tokinote.app.data.TextSize
import com.tokinote.app.data.TextWeight
import com.tokinote.app.ui.components.segmentedShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetContainer(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp),
    )
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    shape: Shape,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = tint)
    }
}

/** 添加内容：图片 / 文件 / 位置 / 标签。 */
@Composable
fun AttachSheet(
    onDismiss: () -> Unit,
    onPhoto: () -> Unit,
    onFile: () -> Unit,
    onLocation: () -> Unit,
    onTags: () -> Unit,
) {
    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.attach_title))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SheetAction(Icons.Filled.Photo, stringResource(R.string.attach_photo), segmentedShape(0, 4), onPhoto)
            SheetAction(Icons.Filled.Description, stringResource(R.string.attach_file), segmentedShape(1, 4), onFile)
            SheetAction(Icons.Filled.LocationOn, stringResource(R.string.attach_location), segmentedShape(2, 4), onLocation)
            SheetAction(Icons.Filled.Label, stringResource(R.string.attach_tags), segmentedShape(3, 4), onTags)
        }
    }
}

/** 笔记长按菜单。 */
@Composable
fun NoteOptionsSheet(
    note: Note,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    SheetContainer(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SheetAction(
                icon = Icons.Filled.PushPin,
                label = stringResource(if (note.pinned) R.string.action_unpin else R.string.action_pin),
                shape = segmentedShape(0, 2),
                onClick = onPin,
            )
            SheetAction(
                icon = Icons.Filled.Delete,
                label = stringResource(R.string.action_delete),
                shape = segmentedShape(1, 2),
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun EmojiSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.emoji_title))
        var custom by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it.take(8) },
                singleLine = true,
                label = { Text(stringResource(R.string.emoji_custom_hint)) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    if (custom.isNotBlank()) {
                        onSelect(custom.trim())
                        custom = ""
                    }
                },
                enabled = custom.isNotBlank(),
            ) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.emoji_custom))
            }
        }
        Spacer(Modifier.height(8.dp))
        JournalEmojis.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { emoji ->
                    val isSelected = emoji == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            )
                            .clickable { onSelect(emoji) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun <T> SegmentedOptions(
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

/** 字号 / 字重（保留原有的字号调整能力）。 */
@Composable
fun FormatSheet(
    note: Note,
    onChange: (Note) -> Unit,
    onDismiss: () -> Unit,
) {
    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.format_title))

        Text(
            text = stringResource(R.string.body_size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        SegmentedOptions(
            options = listOf(
                TextSize.Small to stringResource(R.string.size_small),
                TextSize.Medium to stringResource(R.string.size_medium),
                TextSize.Large to stringResource(R.string.size_large),
            ),
            selected = note.bodySize,
            onSelect = { onChange(note.copy(bodySize = it)) },
        )

        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.format_weight),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        SegmentedOptions(
            options = listOf(
                TextWeight.Normal to stringResource(R.string.weight_normal),
                TextWeight.Medium to stringResource(R.string.weight_medium),
                TextWeight.Bold to stringResource(R.string.weight_bold),
            ),
            selected = note.bodyWeight,
            onSelect = { onChange(note.copy(bodyWeight = it)) },
        )
    }
}

@Composable
fun LabelSheet(
    labels: List<Label>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Label?,
    onDeleteLabel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLabel by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Label?>(null) }

    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.labels_title))

        if (labels.isEmpty()) {
            Text(
                text = stringResource(R.string.labels_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            labels.forEach { label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggle(label.id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = label.id in selectedIds, onCheckedChange = { onToggle(label.id) })
                    Text(
                        text = label.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { pendingDelete = label }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.label_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                singleLine = true,
                label = { Text(stringResource(R.string.label_new_hint)) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onCreate(newLabel)
                    newLabel = ""
                },
                enabled = newLabel.isNotBlank(),
            ) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.label_add))
            }
        }
    }

    pendingDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_label_title, label.name)) },
            text = { Text(stringResource(R.string.delete_label_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteLabel(label.id)
                        pendingDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

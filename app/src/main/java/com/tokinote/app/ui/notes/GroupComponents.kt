package com.tokinote.app.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.Note
import com.tokinote.app.data.NoteGroup

/**
 * 首页顶部的分组筛选条：全部、各个分组，以及最右侧的新建按钮。
 * 长按分组可以编辑或删除。
 */
@Composable
fun NoteGroupSheet(
    group: NoteGroup?,
    notes: List<Note>,
    groups: List<NoteGroup>,
    onDismiss: () -> Unit,
    onSave: (name: String, noteIds: Set<String>) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember(group?.id) { mutableStateOf(group?.name.orEmpty()) }
    var selected by remember(group?.id) { mutableStateOf(group?.noteIds ?: emptySet()) }
    var nameError by remember(group?.id) { mutableStateOf<String?>(null) }
    var notesError by remember(group?.id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val duplicate = groups.any { it.id != group?.id && it.name.equals(trimmed, ignoreCase = true) }
    val needName = stringResource(R.string.group_need_name)
    val nameExists = stringResource(R.string.group_name_exists)
    val needNotes = stringResource(R.string.group_need_notes)

    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(
            stringResource(if (group == null) R.string.group_new else R.string.group_edit),
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null
            },
            singleLine = true,
            label = { Text(stringResource(R.string.group_name)) },
            placeholder = { Text(stringResource(R.string.group_name_hint)) },
            isError = nameError != null,
            supportingText = if (nameError != null) {
                {
                    Text(
                        text = nameError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                null
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.group_pick_notes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.group_selected_count, selected.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (notes.isEmpty()) {
            Text(
                text = stringResource(R.string.group_no_notes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    val checked = note.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selected = if (checked) {
                                    selected - note.id
                                } else {
                                    selected + note.id
                                }
                                notesError = null
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selected = if (checked) {
                                    selected - note.id
                                } else {
                                    selected + note.id
                                }
                                notesError = null
                            },
                        )
                        Text(
                            text = note.emoji,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(30.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title.ifBlank { stringResource(R.string.note_untitled) },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = note.journalDate.dayLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (notesError != null) {
            Text(
                text = notesError.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onDelete != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(
                        text = stringResource(R.string.group_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = {
                    nameError = null
                    notesError = null
                    when {
                        trimmed.isEmpty() -> nameError = needName
                        duplicate -> nameError = nameExists
                        selected.isEmpty() -> notesError = needNotes
                        else -> {
                            onSave(trimmed, selected)
                            onDismiss()
                        }
                    }
                },
            ) {
                Text(
                    stringResource(if (group == null) R.string.group_create else R.string.group_save),
                )
            }
        }
    }

    if (confirmDelete && group != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.group_delete_title, group.name)) },
            text = { Text(stringResource(R.string.group_delete_body)) },
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

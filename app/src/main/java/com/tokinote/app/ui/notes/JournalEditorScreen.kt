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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.Attachments
import com.tokinote.app.data.Label
import com.tokinote.app.data.Note
import com.tokinote.app.data.TextWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chipDateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
private val chipTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

@Composable
fun JournalEditorScreen(
    note: Note,
    labels: List<Label>,
    showTime: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onChange: (Note) -> Unit,
    onDelete: () -> Unit,
    onCreateLabel: (String) -> Label?,
    onDeleteLabel: (String) -> Unit,
    onOpenMedia: (List<String>, Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFormat by remember { mutableStateOf(false) }
    var showDateLabels by remember { mutableStateOf(false) }
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
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDelete = true }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.action_save),
                    tint = MaterialTheme.colorScheme.primary,
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
                val hero = rememberImage(note.media.first())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onOpenMedia(note.media, 0) },
                ) {
                    if (hero != null) {
                        Image(
                            bitmap = hero,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
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
                val labelNames = labels.filter { it.id in note.labelIds }.map { it.name }
                val chipText = buildString {
                    append(note.journalDate.format(chipDateFormatter))
                    if (showTime) {
                        append("  ")
                        append(
                            Instant.ofEpochMilli(note.updatedAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .format(chipTimeFormatter),
                        )
                    }
                    if (labelNames.isNotEmpty()) {
                        append("  ·  ")
                        append(labelNames.joinToString(" "))
                    }
                }
                InfoChip(
                    icon = Icons.Filled.CalendarMonth,
                    text = chipText,
                    onClick = { showDateLabels = true },
                )
            }

            if (note.checklist.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                note.checklist.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = item.done,
                            onCheckedChange = { checked ->
                                val list = note.checklist.toMutableList()
                                list[index] = item.copy(done = checked)
                                onChange(note.copy(checklist = list))
                            },
                        )
                        BasicTextField(
                            value = item.text,
                            onValueChange = { text ->
                                val list = note.checklist.toMutableList()
                                list[index] = item.copy(text = text)
                                onChange(note.copy(checklist = list))
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (item.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.checklist_empty_item),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                                inner()
                            },
                        )
                        IconButton(
                            onClick = {
                                onChange(note.copy(checklist = note.checklist.filterIndexed { i, _ -> i != index }))
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_remove),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { onChange(note.copy(checklist = note.checklist + com.tokinote.app.data.ChecklistItem())) },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.checklist_add))
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
                    .height(280.dp),
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
            EditorTool(Icons.Filled.Checklist, stringResource(R.string.checklist_title)) {
                onChange(note.copy(checklist = note.checklist + com.tokinote.app.data.ChecklistItem()))
            }
            EditorTool(Icons.Filled.CalendarMonth, stringResource(R.string.editor_date_labels)) {
                showDateLabels = true
            }
        }
    }

    if (showFormat) {
        FormatSheet(note = note, onChange = onChange, onDismiss = { showFormat = false })
    }

    if (showDateLabels) {
        DateLabelSheet(
            note = note,
            labels = labels,
            showTime = showTime,
            onChange = onChange,
            onCreate = onCreateLabel,
            onDeleteLabel = onDeleteLabel,
            onDismiss = { showDateLabels = false },
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 日期 + 时间 + 标签合在一个弹层里，改完原地同步。 */
@Composable
private fun DateLabelSheet(
    note: Note,
    labels: List<Label>,
    showTime: Boolean,
    onChange: (Note) -> Unit,
    onCreate: (String) -> Label?,
    onDeleteLabel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var newLabel by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val current = Instant.ofEpochMilli(note.updatedAt).atZone(zone)

    fun updateDate(date: LocalDate) {
        val time = current.toLocalTime()
        val updated = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
        onChange(note.copy(day = date.toEpochDay(), updatedAt = updated))
    }

    fun updateTime(time: LocalTime) {
        val date = note.journalDate
        val updated = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
        onChange(note.copy(updatedAt = updated))
    }

    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.editor_date_labels))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val dateButtonInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Surface(
                onClick = { showDatePicker = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                interactionSource = dateButtonInteraction,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = note.journalDate.format(chipDateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
            if (showTime) {
                Surface(
                    onClick = { showTimePicker = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = current.toLocalTime().format(chipTimeFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.labels_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        if (labels.isEmpty()) {
            Text(
                text = stringResource(R.string.labels_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            labels.forEach { label ->
                val selected = label.id in note.labelIds
                Surface(
                    onClick = {
                        val ids = if (selected) note.labelIds - label.id else note.labelIds + label.id
                        onChange(note.copy(labelIds = ids))
                    },
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Text(
                        text = label.name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.label_add))
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

@Composable
private fun EditorTimePicker(
    hour: Int,
    minute: Int,
    onApply: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    com.tokinote.app.ui.components.M3TimePickerDialog(
        initialHour = hour,
        initialMinute = minute,
        title = "选择时间",
        confirmText = "确定",
        cancelText = "取消",
        onDismiss = onDismiss,
        onConfirm = onApply,
    )
}
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun EditorDatePicker(
    initialDate: LocalDate,
    onApply: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onApply(
                            java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate(),
                        )
                    }
                },
            ) {
                androidx.compose.material3.Text("确定")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("取消")
            }
        },
    ) {
        androidx.compose.material3.DatePicker(state = state)
    }
}
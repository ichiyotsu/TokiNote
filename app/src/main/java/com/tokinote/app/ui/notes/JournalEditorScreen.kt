package com.tokinote.app.ui.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tokinote.app.R
import com.tokinote.app.data.Attachments
import com.tokinote.app.data.Label
import com.tokinote.app.data.Note
import com.tokinote.app.data.TextWeight
import com.tokinote.app.ui.TokiMotion
import com.tokinote.app.ui.components.GlassIconButton
import com.tokinote.app.ui.pressScale
import com.tokinote.app.ui.theme.LocalChipShape
import com.tokinote.app.ui.theme.LocalIslandShape
import com.tokinote.app.ui.theme.labelInk
import com.tokinote.app.ui.theme.labelTint
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
    onArchive: () -> Unit,
    onCreateLabel: (String) -> Label?,
    onDeleteLabel: (String) -> Unit,
    onOpenMedia: (List<String>, Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showFormat by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var toolsExpanded by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current
    val editorPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = 0.dp,
        end = contentPadding.calculateEndPadding(layoutDirection),
        bottom = contentPadding.calculateBottomPadding(),
    )

    val colors = MaterialTheme.colorScheme
    val palette = rememberNotePalette(
        path = note.media.firstOrNull(),
        fallbackAccent = colors.primary,
        surface = colors.surface,
        onSurface = colors.onSurface,
        toolbarBase = colors.surfaceContainerHigh,
    )
    val emphasis = TokiMotion.emphasizedEasing
    val pageTint by animateColorAsState(
        targetValue = palette.scrim,
        animationSpec = tween(650, easing = emphasis),
        label = "editorScrim",
    )
    val accent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(650, easing = emphasis),
        label = "editorAccent",
    )
    val onAccent by animateColorAsState(
        targetValue = palette.onAccent,
        animationSpec = tween(650, easing = emphasis),
        label = "editorOnAccent",
    )
    val toolbarTint by animateColorAsState(
        targetValue = palette.toolbar,
        animationSpec = tween(650, easing = emphasis),
        label = "editorToolbar",
    )
    val titleTint by animateColorAsState(
        targetValue = palette.title,
        animationSpec = tween(650, easing = emphasis),
        label = "editorTitle",
    )

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

    val scrollState = rememberScrollState()
    var collapsed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(scrollState.value) {
        if (scrollState.value > 72 && !collapsed) collapsed = true
        if (scrollState.value < 6 && collapsed) collapsed = false
    }
    val collapseProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.62f,
            stiffness = 380f,
        ),
        label = "editorCollapse",
    )
    val titleSize by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (collapsed) 23f else 30f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.62f,
            stiffness = 380f,
        ),
        label = "editorTitleSize",
    )
    val headerTint = MaterialTheme.colorScheme.surface
    var headerHeight by remember { mutableStateOf(0.dp) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(pageTint, MaterialTheme.colorScheme.surface)))
            .padding(editorPadding),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            val viewport = maxHeight
            val bodyMin = (viewport - 72.dp - 130.dp - headerHeight)
                .coerceAtLeast(150.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = 72.dp, bottom = 130.dp)
                    .padding(horizontal = 22.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
                ) {
                    if (note.media.isNotEmpty()) {
                        val hero = rememberImage(note.media.first())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(156.dp)
                                .clip(RoundedCornerShape(30.dp))
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
                        Spacer(Modifier.height(28.dp))
                    }
                    BasicTextField(
                        value = note.title,
                        onValueChange = { onChange(note.copy(title = it)) },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = titleSize.sp,
                            lineHeight = (titleSize + 4f).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp,
                            color = titleTint,
                        ),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (note.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.title_hint),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = titleSize.sp,
                                        lineHeight = (titleSize + 4f).sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.6).sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                )
                            }
                            inner()
                        },
                    )

                    AnimatedVisibility(
                        visible = !collapsed,
                        enter = expandVertically(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.55f,
                                stiffness = 460f,
                            ),
                        ) + fadeIn(tween(180)),
                        exit = shrinkVertically(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.8f,
                                stiffness = 420f,
                            ),
                        ) + fadeOut(tween(120)),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
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
                            }
                            InfoChip(
                                icon = Icons.Filled.CalendarMonth,
                                text = chipText,
                            )
                        }
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
                                    cursorBrush = SolidColor(accent),
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
                                        onChange(
                                            note.copy(
                                                checklist = note.checklist.filterIndexed { i, _ -> i != index },
                                            ),
                                        )
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
                            onClick = {
                                onChange(
                                    note.copy(
                                        checklist = note.checklist + com.tokinote.app.data.ChecklistItem(),
                                    ),
                                )
                            },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.checklist_add))
                        }
                    }
                }

                BasicTextField(
                    value = note.body,
                    onValueChange = { onChange(note.copy(body = it)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (17 * note.bodySize.scale).sp,
                        lineHeight = (27 * note.bodySize.scale).sp,
                        fontWeight = note.bodyWeight.toFontWeight(),
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = bodyMin)
                        .padding(top = 8.dp),
                    decorationBox = { inner ->
                        if (note.body.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_hint),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = (17 * note.bodySize.scale).sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        inner()
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f)
                .fillMaxWidth()
                .background(
                    color = headerTint.copy(alpha = 0.28f + 0.70f * collapseProgress),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onBack,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                shadowElevation = 0.dp,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = collapsed,
                    enter = expandHorizontally(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.55f,
                            stiffness = 420f,
                        ),
                    ) + fadeIn(tween(180)),
                    exit = shrinkHorizontally(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.8f,
                            stiffness = 420f,
                        ),
                    ) + fadeOut(tween(120)),
                ) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.note_untitled) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = titleTint,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(22.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val moreInteraction = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 22.dp,
                                topEnd = 6.dp,
                                bottomEnd = 6.dp,
                                bottomStart = 22.dp,
                            ),
                        )
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                        .clickable(
                            interactionSource = moreInteraction,
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = { showInfo = true },
                        )
                        .pressScale(moreInteraction, pressedScale = 0.92f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.editor_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                val saveInteraction = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 6.dp,
                                topEnd = 26.dp,
                                bottomEnd = 26.dp,
                                bottomStart = 6.dp,
                            ),
                        )
                        .background(accent)
                        .clickable(
                            interactionSource = saveInteraction,
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBack()
                            },
                        )
                        .pressScale(saveInteraction, pressedScale = 0.9f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.action_save),
                        tint = onAccent,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            val toggleInteraction = remember {
                androidx.compose.foundation.interaction.MutableInteractionSource()
            }
            val rotation by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (toolsExpanded) 45f else 0f,
                animationSpec = TokiMotion.emphasized,
                label = "toolToggle",
            )
            Row(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.72f,
                            stiffness = 380f,
                        ),
                    )
                    .clip(LocalIslandShape.current)
                    .background(toolbarTint.copy(alpha = 0.94f))
                    .border(1.dp, accent.copy(alpha = 0.22f), LocalIslandShape.current)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .pressScale(toggleInteraction, pressedScale = 0.86f)
                        .clickable(
                            interactionSource = toggleInteraction,
                            indication = ripple(color = accent),
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toolsExpanded = !toolsExpanded
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.editor_tools),
                        tint = onAccent,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = rotation }
                            .size(24.dp),
                    )
                }
                AnimatedVisibility(
                    visible = toolsExpanded,
                    enter = expandHorizontally(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.62f,
                            stiffness = 420f,
                        ),
                    ) + fadeIn(tween(TokiMotion.standard)),
                    exit = shrinkHorizontally(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.8f,
                            stiffness = 420f,
                        ),
                    ) + fadeOut(tween(TokiMotion.quick)),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditorTool(
                            icon = Icons.Filled.FormatSize,
                            label = stringResource(R.string.action_format),
                            selected = showFormat,
                            accent = accent,
                            onClick = {
                                showFormat = true
                            },
                        )
                        EditorTool(
                            icon = Icons.Filled.Photo,
                            label = stringResource(R.string.attach_photo),
                            selected = false,
                            accent = accent,
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                        EditorTool(
                            icon = Icons.Filled.Checklist,
                            label = stringResource(R.string.checklist_title),
                            selected = false,
                            accent = accent,
                            onClick = {
                                onChange(
                                    note.copy(
                                        checklist = note.checklist + com.tokinote.app.data.ChecklistItem(),
                                    ),
                                )
                                toolsExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }


    if (showFormat) {
        FormatSheet(
            note = note,
            onChange = onChange,
            onDismiss = { showFormat = false },
        )
    }

    if (showInfo) {
        NoteInfoSheet(
            note = note,
            labels = labels,
            showTime = showTime,
            onChange = onChange,
            onCreateLabel = onCreateLabel,
            onDeleteLabel = onDeleteLabel,
            onDismiss = { showInfo = false },
            onArchive = onArchive,
            onDelete = onDelete,
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
    val chipShape = LocalChipShape.current
    Surface(
        shape = chipShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        modifier = if (onClick != null) {
            Modifier
                .clip(chipShape)
                .clickable(onClick = onClick)
        } else {
            Modifier
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NoteInfoSheet(
    note: Note,
    labels: List<Label>,
    showTime: Boolean,
    onChange: (Note) -> Unit,
    onCreateLabel: (String) -> Label?,
    onDeleteLabel: (String) -> Unit,
    onDismiss: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
    }
    val current = Instant.ofEpochMilli(note.updatedAt).atZone(zone)

    fun updateDate(date: LocalDate) {
        val updated = LocalDateTime.of(date, current.toLocalTime())
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        onChange(note.copy(day = date.toEpochDay(), updatedAt = updated))
    }

    fun updateTime(time: LocalTime) {
        val updated = LocalDateTime.of(note.journalDate, time)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        onChange(note.copy(updatedAt = updated))
    }

    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.note_info))
        Text(
            text = note.title.ifBlank { stringResource(R.string.note_untitled) },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(14.dp))
        InfoRow(
            icon = Icons.Filled.CalendarMonth,
            label = stringResource(R.string.note_created),
            value = Instant.ofEpochMilli(note.createdAt).atZone(zone).format(formatter),
        )
        InfoRow(
            icon = Icons.Filled.Edit,
            label = stringResource(R.string.note_last_edited),
            value = Instant.ofEpochMilli(note.updatedAt).atZone(zone).format(formatter),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.editor_custom_datetime),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = { showDatePicker = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = note.journalDate.format(chipDateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.labels_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
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
            verticalAlignment = Alignment.CenterVertically,
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
                        labelTint(label.colorIndex)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onSurface
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
            Surface(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.label_add),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    onDismiss()
                    onArchive()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.preview_archive))
            }
            OutlinedButton(
                onClick = { confirmDelete = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_delete))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newLabel = ""
            },
            title = { Text(stringResource(R.string.labels_title)) },
            text = {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_new_hint)) },
                    shape = RoundedCornerShape(20.dp),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newLabel.isNotBlank(),
                    onClick = {
                        if (onCreateLabel(newLabel) != null) {
                            newLabel = ""
                            showAddDialog = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newLabel = ""
                    },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showDatePicker) {
        EditorDatePicker(
            initialDate = note.journalDate,
            onApply = {
                updateDate(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        EditorTimePicker(
            hour = current.hour,
            minute = current.minute,
            onApply = { hour, minute ->
                updateTime(LocalTime.of(hour, minute))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDismiss()
                        onDelete()
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

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EditorTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val interaction = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(LocalChipShape.current)
            .background(if (selected) accent.copy(alpha = 0.28f) else Color.Transparent)
            .pressScale(interaction, pressedScale = 0.86f)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = accent),
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(21.dp),
            tint = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
            },
        )
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

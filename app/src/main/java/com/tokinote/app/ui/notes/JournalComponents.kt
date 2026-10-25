package com.tokinote.app.ui.notes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.Label
import com.tokinote.app.data.Note
import com.tokinote.app.ui.pressScale
import com.tokinote.app.ui.theme.ExpressiveCardShape
import com.tokinote.app.ui.theme.labelInk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dayFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

fun LocalDate.dayLabel(): String {
    val weekday = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
    return format(dayFormatter) + " " + weekday
}

fun Long.timeLabel(): String =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).format(timeFormatter)

@Composable
fun rememberImage(path: String): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(path, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return image
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalCard(
    note: Note,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
    showTime: Boolean = true,
    labels: List<Label> = emptyList(),
) {
    val shape = ExpressiveCardShape
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val haptic = LocalHapticFeedback.current
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .clip(shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (bounds != Rect.Zero) onLongPress(bounds)
                },
            )
            .pressScale(interaction),
        shape = shape,
        color = if (note.pinned) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val image = note.media.firstOrNull()?.let { rememberImage(it) }
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val noteLabels = labels.filter { it.id in note.labelIds }
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.note_untitled) },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 25.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.pinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (note.body.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            fontWeight = note.bodyWeight.toFontWeight(),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (note.checklist.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    note.checklist.take(2).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.done) {
                                    androidx.compose.material.icons.Icons.Filled.CheckCircle
                                } else {
                                    androidx.compose.material.icons.Icons.Outlined.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (item.done) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    textDecoration = if (item.done) {
                                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    } else {
                                        null
                                    },
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (noteLabels.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        noteLabels.take(3).forEach { label ->
                            val ink = labelInk(label.colorIndex)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, ink.copy(alpha = 0.72f), CircleShape)
                                    .padding(horizontal = 9.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        letterSpacing = 0.2.sp,
                                    ),
                                    color = ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 76.dp),
                                )
                            }
                        }
                        if (noteLabels.size > 3) {
                            Text(
                                text = "+${noteLabels.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = buildString {
                        append(note.journalDate.dayLabel())
                        if (showTime) {
                            append("  ")
                            append(note.updatedAt.timeLabel())
                        }
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.25.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

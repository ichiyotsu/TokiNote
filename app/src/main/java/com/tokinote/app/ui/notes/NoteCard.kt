package com.tokinote.app.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
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

/**
 * Keep 风格卡片：有图时图片在顶部铺满，没有图时整个图片容器都不渲染，
 * 文字直接从卡片顶部开始，避免纯文本卡片出现空白占位。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    labels: List<Label>,
    showTime: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val shape = ExpressiveCardShape
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val imagePath = note.media.firstOrNull()
    val image = imagePath?.let { rememberImage(it) }
    val noteLabels = labels.filter { it.id in note.labelIds }

    Surface(
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
        Column {
            if (imagePath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (compact) 1.12f else 1.55f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(if (compact) 1.12f else 1.55f),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = if (compact) 14.dp else 18.dp,
                    end = if (compact) 14.dp else 18.dp,
                    top = if (compact) 13.dp else 16.dp,
                    bottom = if (compact) 14.dp else 18.dp,
                ),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.note_untitled) },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (compact) 19.sp else 25.sp,
                            lineHeight = if (compact) 23.sp else 29.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.pinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }

                if (note.body.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (compact) 13.sp else 15.sp,
                            lineHeight = if (compact) 19.sp else 21.sp,
                            fontWeight = note.bodyWeight.toFontWeight(),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 3 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (noteLabels.isNotEmpty()) {
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        noteLabels.take(3).forEach { label ->
                            val ink = labelInk(label.colorIndex)
                            Text(
                                text = label.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 0.2.sp,
                                ),
                                color = ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = if (compact) 62.dp else 76.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, ink.copy(alpha = 0.72f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
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

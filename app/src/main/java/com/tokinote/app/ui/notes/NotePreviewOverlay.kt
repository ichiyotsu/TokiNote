package com.tokinote.app.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.Label
import com.tokinote.app.data.Note
import com.tokinote.app.ui.TokiMotion
import com.tokinote.app.ui.theme.ExpressiveCardShape
import com.tokinote.app.ui.theme.ExpressiveChipShape
import com.tokinote.app.ui.theme.labelInk
import kotlinx.coroutines.launch

/**
 * 长按卡片的放大预览：从卡片在屏幕上的实际位置与尺寸出发，
 * 用 Emphasized easing 膨胀到屏幕中央，背景同步压暗。
 */
@Composable
fun NotePreviewOverlay(
    note: Note,
    labels: List<Label>,
    sourceBounds: Rect?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onArchive: (String) -> Unit,
    onTrash: (String) -> Unit,
) {
    val progress = remember(note.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var targetBounds by remember { mutableStateOf<Rect?>(null) }
    var closing by remember { mutableStateOf(false) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(260, easing = TokiMotion.emphasizedEasing),
            )
            onDismiss()
        }
    }

    BackHandler(onBack = ::close)

    LaunchedEffect(note.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(430, easing = TokiMotion.emphasizedEasing),
        )
    }

    val reveal = progress.value
    val noteLabels = labels.filter { it.id in note.labelIds }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f * reveal))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::close,
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val cardWidth = maxWidth * 0.9f
            val modifier = Modifier
                .width(cardWidth)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    if (bounds.width > 0f && bounds.height > 0f && targetBounds != bounds) {
                        targetBounds = bounds
                    }
                }
                .graphicsLayer {
                    val target = targetBounds
                    val source = sourceBounds
                    if (target != null && source != null &&
                        target.width > 0f && target.height > 0f
                    ) {
                        val startScale = minOf(
                            source.width / target.width,
                            source.height / target.height,
                        ).coerceIn(0.2f, 1f)
                        val scale = startScale + (1f - startScale) * reveal
                        scaleX = scale
                        scaleY = scale
                        translationX = (source.center.x - target.center.x) * (1f - reveal)
                        translationY = (source.center.y - target.center.y) * (1f - reveal)
                        alpha = reveal
                    } else {
                        scaleX = 1f
                        scaleY = 1f
                        alpha = reveal
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )

            Surface(
                modifier = modifier,
                shape = ExpressiveCardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = note.title.ifBlank { stringResource(R.string.note_untitled) },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = ::close, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(note.journalDate.dayLabel())
                            append("  ")
                            append(note.updatedAt.timeLabel())
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )

                    if (noteLabels.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            noteLabels.take(3).forEach { label ->
                                val ink = labelInk(label.colorIndex)
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = ink,
                                    modifier = Modifier
                                        .clip(ExpressiveChipShape)
                                        .background(ink.copy(alpha = 0.08f))
                                        .padding(horizontal = 9.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = note.body.ifBlank { stringResource(R.string.editor_hint) },
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(
                            onClick = { onOpen(note.id) },
                            shape = ExpressiveChipShape,
                        ) {
                            Text(stringResource(R.string.preview_open))
                        }
                        TextButton(onClick = { onArchive(note.id) }) {
                            Text(stringResource(R.string.preview_archive))
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { onTrash(note.id) }) {
                            Text(
                                text = stringResource(R.string.preview_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

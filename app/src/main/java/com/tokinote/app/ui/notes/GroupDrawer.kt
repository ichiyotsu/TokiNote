package com.tokinote.app.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.Note
import com.tokinote.app.data.NoteGroup
import com.tokinote.app.ui.TokiMotion
import com.tokinote.app.ui.pressScale
import com.tokinote.app.ui.theme.ExpressiveChipShape
import com.tokinote.app.ui.theme.Micro5Charted

/**
 * 笔记页左侧分组抽屉。分组从顶栏迁到这里，「全部」与每个分组都在同一列。
 * 面板从状态栏下方开始，结束在悬浮底栏上方。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupDrawer(
    open: Boolean,
    groups: List<NoteGroup>,
    selectedGroupId: String?,
    activeNotes: List<Note>,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onEdit: (NoteGroup) -> Unit,
) {
    BackHandler(enabled = open, onBack = onDismiss)

    Box(
        Modifier
            .fillMaxSize()
            .padding(top = topInset)
            .navigationBarsPadding(),
    ) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .combinedClickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = onDismiss,
                        onLongClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
            ) + fadeIn(tween(180)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f),
            ) + fadeOut(tween(160)),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.68f)
                    .widthIn(max = 320.dp)
                    .padding(start = 16.dp, top = 12.dp, bottom = 16.dp),
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 24.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                ) {
                    Text(
                        text = " TokiNote ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = Micro5Charted,
                            fontSize = 31.sp,
                            lineHeight = 35.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.drawer_groups),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                    )

                    DrawerRow(
                        label = stringResource(R.string.label_all),
                        count = activeNotes.size,
                        selected = selectedGroupId == null,
                        icon = Icons.Filled.Notes,
                        onClick = { onSelect(null) },
                    )
                    groups.forEach { group ->
                        DrawerRow(
                            label = group.name,
                            count = activeNotes.count { it.id in group.noteIds },
                            selected = selectedGroupId == group.id,
                            icon = Icons.Filled.Label,
                            onClick = { onSelect(group.id) },
                            onLongClick = { onEdit(group) },
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    val createInteraction = androidx.compose.runtime.remember {
                        MutableInteractionSource()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveChipShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .pressScale(createInteraction, pressedScale = 0.97f)
                            .combinedClickable(
                                interactionSource = createInteraction,
                                indication = null,
                                onClick = onCreate,
                                onLongClick = onCreate,
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.group_add),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.drawer_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerRow(
    label: String,
    count: Int,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = androidx.compose.runtime.remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(ExpressiveChipShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            },
        )
    }
}

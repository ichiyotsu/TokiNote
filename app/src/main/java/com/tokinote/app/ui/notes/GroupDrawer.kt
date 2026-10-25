package com.tokinote.app.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tokinote.app.R
import com.tokinote.app.data.AlbumFolder
import com.tokinote.app.data.Note
import com.tokinote.app.data.NoteGroup
import com.tokinote.app.ui.TokiMotion
import com.tokinote.app.ui.pressScale
import com.tokinote.app.ui.theme.ExpressiveChipShape
import com.tokinote.app.ui.theme.Micro5Charted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val folderRowDate = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

/**
 * 左侧悬浮导航抽屉：顶层 Tab（笔记 / 相册 / 统计）+ 可折叠的笔记分组与相册文件夹。
 * 分组不再出现在主页，主页只保留顶部搜索胶囊和内容。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GroupDrawer(
    open: Boolean,
    selectedTab: HomeTab,
    groups: List<NoteGroup>,
    folders: List<AlbumFolder>,
    selectedGroupId: String?,
    selectedFolderId: String?,
    activeNotes: List<Note>,
    photos: List<AlbumPhoto>,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    glass: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSelectTab: (HomeTab) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onCreateGroup: () -> Unit,
    onEditGroup: (NoteGroup) -> Unit,
    onCreateFolder: () -> Unit,
    onEditFolder: (AlbumFolder) -> Unit,
) {
    BackHandler(enabled = open, onBack = onDismiss)
    var notesExpanded by rememberSaveable { mutableStateOf(true) }
    var albumExpanded by rememberSaveable { mutableStateOf(true) }

    Box(
        Modifier
            .fillMaxSize()
            .zIndex(3f)
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
            enter = androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
            ) + fadeIn(tween(180)),
            exit = androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f),
            ) + fadeOut(tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.68f)
                    .widthIn(max = 320.dp)
                    .padding(start = 16.dp, top = 12.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(34.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(glass)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 22.dp),
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
                    Spacer(Modifier.height(20.dp))

                    DrawerTabRow(
                        label = stringResource(R.string.nav_notes),
                        icon = Icons.Filled.StickyNote2,
                        selected = selectedTab == HomeTab.Notes,
                        onClick = { onSelectTab(HomeTab.Notes) },
                    )
                    DrawerTabRow(
                        label = stringResource(R.string.nav_album),
                        icon = Icons.Filled.Photo,
                        selected = selectedTab == HomeTab.Album,
                        onClick = { onSelectTab(HomeTab.Album) },
                    )
                    DrawerTabRow(
                        label = stringResource(R.string.tab_stats),
                        icon = Icons.Filled.BarChart,
                        selected = selectedTab == HomeTab.Stats,
                        onClick = { onSelectTab(HomeTab.Stats) },
                    )

                    Spacer(Modifier.height(18.dp))
                    CollapsibleHeader(
                        title = stringResource(R.string.drawer_notes_section),
                        expanded = notesExpanded,
                        onToggle = { notesExpanded = !notesExpanded },
                    )
                    AnimatedVisibility(
                        visible = notesExpanded,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
                        ) + fadeIn(tween(160)),
                        exit = shrinkVertically(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                        ) + fadeOut(tween(120)),
                    ) {
                        Column {
                            DrawerRow(
                                label = stringResource(R.string.drawer_all_notes),
                                icon = Icons.Filled.StickyNote2,
                                selected = selectedTab == HomeTab.Notes && selectedGroupId == null,
                                count = activeNotes.size,
                                onClick = { onSelectGroup(null) },
                            )
                            groups.forEach { group ->
                                DrawerRow(
                                    label = group.name,
                                    icon = Icons.Filled.Label,
                                    selected = selectedTab == HomeTab.Notes && selectedGroupId == group.id,
                                    count = activeNotes.count { it.id in group.noteIds },
                                    onClick = { onSelectGroup(group.id) },
                                    onLongClick = { onEditGroup(group) },
                                )
                            }
                            CreateRow(
                                label = stringResource(R.string.group_add),
                                onClick = onCreateGroup,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    CollapsibleHeader(
                        title = stringResource(R.string.drawer_album_section),
                        expanded = albumExpanded,
                        onToggle = { albumExpanded = !albumExpanded },
                    )
                    AnimatedVisibility(
                        visible = albumExpanded,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
                        ) + fadeIn(tween(160)),
                        exit = shrinkVertically(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                        ) + fadeOut(tween(120)),
                    ) {
                        Column {
                            DrawerRow(
                                label = stringResource(R.string.drawer_all_photos),
                                icon = Icons.Filled.Photo,
                                selected = selectedTab == HomeTab.Album && selectedFolderId == null,
                                count = photos.size,
                                onClick = { onSelectFolder(null) },
                            )
                            folders.forEach { folder ->
                                DrawerRow(
                                    label = folder.name,
                                    icon = Icons.Filled.Folder,
                                    selected = selectedTab == HomeTab.Album && selectedFolderId == folder.id,
                                    count = folder.paths.size,
                                    subtitle = Instant.ofEpochMilli(folder.createdAt)
                                        .atZone(ZoneId.systemDefault())
                                        .format(folderRowDate),
                                    onClick = { onSelectFolder(folder.id) },
                                    onLongClick = { onEditFolder(folder) },
                                )
                            }
                            CreateRow(
                                label = stringResource(R.string.folder_add),
                                onClick = onCreateFolder,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.drawer_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerTabRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(ExpressiveChipShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
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
        )
    }
}

@Composable
private fun CollapsibleHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = TokiMotion.emphasized,
        label = "drawerSectionArrow",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveChipShape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier
                .size(17.dp)
                .graphicsLayer { rotationZ = rotation },
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DrawerRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    count: Int,
    subtitle: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(ExpressiveChipShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
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
            modifier = Modifier.size(19.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
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
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                )
            }
        }
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

@Composable
private fun CreateRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(ExpressiveChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

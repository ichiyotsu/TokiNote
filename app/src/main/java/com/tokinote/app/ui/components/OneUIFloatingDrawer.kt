package com.tokinote.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/** 侧边栏里的一个导航项。 */
data class OneUIDrawerEntry(
    val key: String,
    val label: String,
    val count: Int? = null,
    val icon: ImageVector,
)

/** 侧边栏里的文件夹项。 */
data class OneUIDrawerFolder(
    val id: String,
    val name: String,
    val count: Int = 0,
)

/**
 * One UI 悬浮侧边栏：不贴边、独立大圆角、深色玻璃，顶部设置，
 * 中部导航和文件夹，底部「管理文件夹」按钮。
 *
 * @param glass 由页面传入的 Haze 玻璃 Modifier，确保模糊只画一层。
 */
@Composable
fun OneUIFloatingDrawer(
    open: Boolean,
    onDismiss: () -> Unit,
    entries: List<OneUIDrawerEntry>,
    folders: List<OneUIDrawerFolder>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onManageFolders: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit,
    glass: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    if (!open) return

    Box(modifier = modifier.fillMaxSize().zIndex(20f)) {
        // 遮罩
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(enabled = true, onClick = onDismiss),
        )

        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val shape = RoundedCornerShape(30.dp)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.86f)
                        .widthIn(max = 340.dp)
                        .clip(shape)
                        .then(glass)
                        .background(Color(0xFF1B1B1B).copy(alpha = 0.72f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onSettings) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "设置",
                                tint = Color.White.copy(alpha = 0.9f),
                            )
                        }
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onInfo) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "关于",
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "笔记",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        entries.forEach { entry ->
                            DrawerRow(
                                label = entry.label,
                                count = entry.count,
                                icon = entry.icon,
                                selected = entry.key == selectedKey,
                                onClick = { onSelect(entry.key) },
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "文件夹",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
                        )
                        folders.forEach { folder ->
                            DrawerRow(
                                label = folder.name,
                                count = folder.count,
                                icon = Icons.Filled.Folder,
                                selected = false,
                                onClick = { onSelectFolder(folder.id) },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onManageFolders,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("管理文件夹", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerRow(
    label: String,
    count: Int?,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.82f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

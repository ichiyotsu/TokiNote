package com.tokinote.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.ui.Dest

private data class NavEntry(
    val dest: Dest,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: Int,
)

private val navEntries = listOf(
    NavEntry(Dest.Notes, Icons.Outlined.StickyNote2, Icons.Filled.StickyNote2, R.string.nav_notes),
    NavEntry(Dest.Focus, Icons.Outlined.Timer, Icons.Filled.Timer, R.string.nav_focus),
    NavEntry(Dest.Settings, Icons.Outlined.Settings, Icons.Filled.Settings, R.string.nav_settings),
)

/**
 * 底栏使用 M3 Expressive 的 HorizontalFloatingToolbar + ToggleButton（与 Tomato 一致），
 * 右侧独立按钮通过 floatingActionButton 槽位挂在工具栏尾部。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavBar(
    current: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier,
    glass: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    // 容器本身保持透明：背景完全交给 Haze 模糊效果，不再叠一层半透明 Surface。
    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors().copy(
        Color.Transparent,
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val toolbarModifier = Modifier.clip(shape).then(glass)
        if (action != null) {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = toolbarModifier,
                colors = colors,
                floatingActionButton = { action() },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                content = { NavEntries(current, onSelect) },
            )
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = toolbarModifier,
                colors = colors,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                content = { NavEntries(current, onSelect) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.NavEntries(
    current: Dest,
    onSelect: (Dest) -> Unit,
) {
    navEntries.forEach { entry ->
        val selected = current == entry.dest
        ToggleButton(
            checked = selected,
            onCheckedChange = { onSelect(entry.dest) },
            shapes = androidx.compose.material3.ToggleButtonShapes(
                androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                CircleShape,
                CircleShape,
            ),
            colors = androidx.compose.material3.ToggleButtonDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.primary,
                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.height(56.dp),
            icon = {
                Icon(
                    imageVector = if (selected) entry.selectedIcon else entry.icon,
                    contentDescription = stringResource(entry.label),
                    modifier = Modifier.size(20.dp),
                )
            },
            content = {
                if (selected) {
                    Text(
                        text = stringResource(entry.label),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            },
        )
    }
}

@Composable
fun NavActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(64.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

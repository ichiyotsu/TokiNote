package com.tokinote.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.ui.Dest
import com.tokinote.app.ui.pressScale

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
 * One UI 风格的单层毛玻璃胶囊：Haze 只画一次背景，选中项是胶囊内部的实色块，
 * 右侧独立 + 按钮留在胶囊外，保持原来的布局关系。
 */
@Composable
fun FloatingNavBar(
    current: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier,
    glass: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                    )
                    .clip(shape)
                    .then(glass)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navEntries.forEach { entry ->
                    NavItem(
                        entry = entry,
                        selected = current == entry.dest,
                        onSelect = onSelect,
                    )
                }
            }
            action?.invoke()
        }
    }
}

@Composable
private fun NavItem(
    entry: NavEntry,
    selected: Boolean,
    onSelect: (Dest) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(entry.dest)
                },
            )
            .pressScale(interaction, pressedScale = 0.92f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) entry.selectedIcon else entry.icon,
            contentDescription = stringResource(entry.label),
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 440f),
            ) + fadeIn(),
            exit = shrinkHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 440f),
            ) + fadeOut(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(entry.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun NavActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 2.dp,
        interactionSource = interaction,
        modifier = Modifier
            .size(64.dp)
            .pressScale(interaction, pressedScale = 0.9f),
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

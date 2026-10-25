package com.tokinote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.ui.pressScale
import kotlinx.coroutines.delay

/** Top action with a small M3 tooltip that appears below on long press. */
@Composable
fun TopActionButton(
    icon: ImageVector,
    contentDescription: String,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(15.dp),
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f),
) {
    var tooltipVisible by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(tooltipVisible) {
        if (tooltipVisible) {
            delay(1800)
            tooltipVisible = false
        }
    }
    Box {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = tint,
            shadowElevation = 2.dp,
            modifier = modifier.size(size).pressScale(interaction, pressedScale = 0.9f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onClick,
                        onLongClick = { tooltipVisible = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        TooltipMenu(
            expanded = tooltipVisible,
            text = tooltip,
            onDismiss = { tooltipVisible = false },
        )
    }
}

@Composable
fun TopAvatarButton(
    emoji: String,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tooltipVisible by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(tooltipVisible) {
        if (tooltipVisible) {
            delay(1800)
            tooltipVisible = false
        }
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .pressScale(interaction, pressedScale = 0.9f)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = onClick,
                    onLongClick = { tooltipVisible = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji.ifBlank { "🌿" }, fontSize = 18.sp)
        }
        TooltipMenu(
            expanded = tooltipVisible,
            text = tooltip,
            onDismiss = { tooltipVisible = false },
        )
    }
}

@Composable
private fun TooltipMenu(
    expanded: Boolean,
    text: String,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 6.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
            shadowElevation = 4.dp,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

package com.tokinote.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

/**
 * One UI 大标题折叠头：列表在顶部时显示大标题、计数和下方工具栏；
 * 向上滚动后收缩为标准 TopAppBar，并保留右侧操作。
 *
 * 用法：header 覆盖在列表上方，LazyColumn 的 contentPadding.top 使用 expandedHeight。
 */
@Composable
fun OneUICollapsingHeader(
    title: String,
    subtitle: String?,
    listState: LazyListState,
    onMenu: () -> Unit,
    onExportPdf: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    expandedHeight: Dp = 226.dp,
    appBarHeight: Dp = 64.dp,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val expandedPx = with(density) { expandedHeight.toPx() }
    val rawOffset = if (listState.firstVisibleItemIndex > 0) {
        expandedPx
    } else {
        listState.firstVisibleItemScrollOffset.toFloat()
    }
    val progress by animateFloatAsState(
        targetValue = (rawOffset / expandedPx).coerceIn(0f, 1f),
        animationSpec = tween(120),
        label = "oneUiHeaderProgress",
    )
    val currentHeight = lerp(expandedHeight, appBarHeight, progress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(currentHeight)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
    ) {
        // 折叠后的标准 TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(appBarHeight)
                .padding(horizontal = 12.dp)
                .graphicsLayer { alpha = progress },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.Menu, contentDescription = "菜单")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 1,
            )
        }

        // 展开态：大标题 + 计数 + 下方工具栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = appBarHeight)
                .graphicsLayer {
                    alpha = 1f - progress
                    translationY = -progress * (expandedHeight - appBarHeight).toPx() * 0.35f
                    scaleX = 1f - progress * 0.04f
                    scaleY = 1f - progress * 0.04f
                },
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp),
                maxLines = 1,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = "菜单")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onExportPdf) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "导出 PDF")
                }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索")
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                }
            }
        }
    }
}

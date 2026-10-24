package com.tokinote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R

enum class HomeTab { Notes, Album, Stats }

enum class HomeViewMode { List, Grid }

/**
 * 主页顶部悬浮胶囊：菜单、搜索、视图切换、排序、头像都收在这一条里。
 * 背景由外部传入的 Haze 涂成半透明磨砂，这里只负责内容和 1dp 边缘高光。
 */
@Composable
fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    viewMode: HomeViewMode,
    onToggleView: () -> Unit,
    sortAscending: Boolean,
    onToggleSort: () -> Unit,
    avatarEmoji: String,
    showViewSort: Boolean,
    onMenu: () -> Unit,
    onAvatar: () -> Unit,
    glass: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .then(glass)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                shape = CircleShape,
            )
            .padding(start = 4.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.drawer_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_tokinote),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (showViewSort) {
            IconButton(onClick = onToggleView, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (viewMode == HomeViewMode.Grid) {
                        Icons.Filled.ViewAgenda
                    } else {
                        Icons.Filled.GridView
                    },
                    contentDescription = stringResource(
                        if (viewMode == HomeViewMode.Grid) R.string.view_list else R.string.view_grid,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onToggleSort, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = stringResource(
                        if (sortAscending) R.string.sort_descending else R.string.sort_ascending,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onAvatar),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarEmoji.ifBlank { "🌿" },
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(2.dp))
    }
}

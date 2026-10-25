package com.tokinote.app.ui.notes

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
 * 主页控制区：导航、搜索、查看方式、排序、头像各自独立。
 * 只有搜索框有半透明底（无描边），其余都是独立悬浮按钮。
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
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopActionButton(
            icon = Icons.Filled.Menu,
            contentDescription = stringResource(R.string.drawer_open),
            tooltip = stringResource(R.string.tooltip_navigation),
            onClick = onMenu,
        )
        Spacer(Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(CircleShape)
                .then(glass)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
        if (showViewSort) {
            Spacer(Modifier.width(4.dp))
            TopActionButton(
                icon = if (viewMode == HomeViewMode.Grid) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                contentDescription = stringResource(
                    if (viewMode == HomeViewMode.Grid) R.string.view_list else R.string.view_grid,
                ),
                tooltip = stringResource(R.string.tooltip_view_mode),
                onClick = onToggleView,
                size = 40.dp,
                iconSize = 19.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            TopActionButton(
                icon = Icons.Filled.SwapVert,
                contentDescription = stringResource(
                    if (sortAscending) R.string.sort_descending else R.string.sort_ascending,
                ),
                tooltip = stringResource(R.string.tooltip_sort),
                onClick = onToggleSort,
                size = 40.dp,
                iconSize = 19.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        TopAvatarButton(
            emoji = avatarEmoji,
            tooltip = stringResource(R.string.tooltip_profile),
            onClick = onAvatar,
        )
    }
}

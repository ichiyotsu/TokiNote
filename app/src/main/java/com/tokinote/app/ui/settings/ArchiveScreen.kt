package com.tokinote.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.Note
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.components.GlassIconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/** 归档 / 回收站共用列表；归档不可编辑，回收站保留 14 天。 */
@Composable
fun ArchiveListScreen(
    title: String,
    subtitle: String,
    notes: List<Note>,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    deleteLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(),
    ) {
        ExpressiveTopBar(
            title = title,
            subtitle = subtitle,
            alignStart = true,
            navigationIcon = {
                GlassIconButton(
                    icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                    modifier = Modifier.padding(start = 16.dp),
                )
            },
        )

        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.archive_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(notes, key = { it.id }) { note ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = note.title.ifBlank { stringResource(R.string.note_untitled) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (note.body.isNotBlank()) {
                            Text(
                                text = note.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { onRestore(note.id) }) {
                                Text(stringResource(R.string.archive_restore))
                            }
                            Spacer(Modifier.width(4.dp))
                            TextButton(onClick = { onDelete(note.id) }) {
                                Text(
                                    text = deleteLabel,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

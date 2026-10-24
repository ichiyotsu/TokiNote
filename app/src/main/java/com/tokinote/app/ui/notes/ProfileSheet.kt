package com.tokinote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import com.tokinote.app.ui.theme.ExpressiveChipShape

private val avatarChoices = listOf("🌿", "🌙", "🌸", "☕", "🐈", "🍃", "✨", "🪐")

/** 个人资料：头像 emoji + 用户 ID，点击顶栏头像打开。 */
@Composable
fun ProfileSheet(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var emoji by remember { mutableStateOf(settings.avatarEmoji.ifBlank { "🌿" }) }
    var userId by remember { mutableStateOf(settings.userId) }

    SheetContainer(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.profile_title))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 34.sp)
            }
            Spacer(Modifier.size(18.dp))
            Column {
                Text(
                    text = userId.ifBlank { stringResource(R.string.profile_anonymous) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.profile_local_only),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.profile_avatar),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            avatarChoices.forEach { choice ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ExpressiveChipShape)
                        .background(
                            if (choice == emoji) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        )
                        .clickable { emoji = choice },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = choice, fontSize = 21.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            singleLine = true,
            label = { Text(stringResource(R.string.profile_user_id)) },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                onUpdate {
                    it.copy(
                        avatarEmoji = emoji,
                        userId = userId.trim().ifBlank { "tokinote" },
                    )
                }
                onDismiss()
            },
            shape = ExpressiveChipShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_save))
        }
        Spacer(Modifier.height(8.dp))
    }
}

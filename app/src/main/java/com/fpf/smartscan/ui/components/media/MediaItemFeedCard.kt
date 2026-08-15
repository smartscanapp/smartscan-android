package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.core.media.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaItemFeedCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onItemClick: (MediaItem) -> Unit,
    content: @Composable () -> Unit,
    onShowItemMenu: (MediaItem) -> Unit,
) {
    val description = remember(item.description) {
        item.description?.trim()?.replaceFirstChar { it.uppercase() }.orEmpty()
    }

    val formattedDate = remember(item.dateAdded) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.dateAdded * 1000L))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onItemClick(item) },
            )
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = "Lightbulb icon",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if(item.isHidden) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = "hide icon",
                            modifier = Modifier.size(20.dp).alpha(0.7f)
                        )
                    }

                    IconButton(
                        onClick = { onShowItemMenu(item) },
                        modifier = Modifier.size(24.dp)
                        ){
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Item menu icon",
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }

                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box {
                    content()

                }
            }

            HorizontalDivider()
        }
    }
}
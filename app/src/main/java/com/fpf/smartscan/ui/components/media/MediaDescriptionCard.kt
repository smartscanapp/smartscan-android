package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.ui.components.common.CircularCheckbox

@Composable
fun MediaDescriptionCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onItemClick: (MediaItem) -> Unit,
    isSelecting: Boolean = false,
    onItemLongClick: ((MediaItem) -> Unit)? = null,
    isChecked: (() -> Boolean)? = null,
    onToggleSelected: ((MediaItem) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    val description = remember(item.description) {
        item.description?.trim().orEmpty()
    }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape
            )
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (isSelecting) {
                        onToggleSelected?.invoke(item)
                    } else {
                        onItemClick(item)
                    }
                },
                onLongClick = if (isSelecting || onItemLongClick == null) {
                    null
                } else {
                    { onItemLongClick(item) }
                }
            )
    ) {
        Column {
            Box {
                ImageDisplay(
                    maxSize = 864,
                    uri = item.uri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f),
                    contentScale = ContentScale.Crop,
                    mediaType = item.type,
                    onError = onError
                )

                if (isSelecting) {
                    CircularCheckbox(
                        checked = isChecked?.invoke() ?: false,
                        onCheckedChange = {
                            onToggleSelected?.invoke(item)
                        },
                        modifier = Modifier
                            .offset(x = 8.dp, y = 8.dp)
                            .align(Alignment.TopStart)
                    )

                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onItemClick(item)
                            }
                            .offset((-8).dp, (-8).dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expand item",
                            modifier = Modifier
                                .size(20.dp)
                                .padding(2.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.5f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            if (description.isNotBlank()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
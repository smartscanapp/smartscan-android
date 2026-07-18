package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.HorizontalDivider
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
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.components.common.CircularCheckbox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConceptMediaItemCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onItemClick: (MediaItem) -> Unit,
    playbackPosition: Long? = null,
    isSelecting: Boolean = false,
    onItemLongClick: ((MediaItem) -> Unit)? = null,
    isChecked: (() -> Boolean)? = null,
    onToggleSelected: ((MediaItem) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
    onSavePlaybackPosition: ((Long) -> Unit)? = null,
    ) {
    val description = remember(item.description) {
        item.description?.trim()?.replaceFirstChar { it.uppercase() }.orEmpty()
    }

    val formattedDate = remember(item.dateAdded) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(item.dateAdded * 1000L))
    }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isSelecting) {
                        onToggleSelected?.invoke(item)
                    } else {
                        onItemClick(item)
                    }
                },
                onLongClick = if (!isSelecting && onItemLongClick != null) {
                    { onItemLongClick(item) }
                } else {
                    null
                }
            )
    ) {
        Column() {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    if (isSelecting) {
                        Spacer(modifier = Modifier.width(12.dp))

                        CircularCheckbox(
                            checked = isChecked?.invoke() ?: false,
                            onCheckedChange = {
                                onToggleSelected?.invoke(item)
                            }
                        )
                    }
                }

                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(top = 12.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.padding(top = 16.dp))

                Box {
                    if(item.type == MediaType.IMAGE){
                    ImageDisplay(
                        maxSize = 864,
                        uri = item.uri,
                        mediaType = item.type,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .aspectRatio(1.25f),
                        onError = onError
                    ) }else{
                        VideoDisplay(uri = item.uri,
                            onPlaybackPositionChanged = onSavePlaybackPosition ,
                            playbackPosition=playbackPosition?: 0L,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .aspectRatio(1.25f),
                        )

                    }

                    if (isSelecting) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset((-8).dp, (-8).dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onItemClick(item)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Expand item",
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(3.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
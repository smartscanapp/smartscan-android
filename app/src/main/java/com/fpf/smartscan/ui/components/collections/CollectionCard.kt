package com.fpf.smartscan.ui.components.collections

import androidx.compose.runtime.Composable


import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.ui.components.common.CircularCheckbox
import com.fpf.smartscan.ui.components.media.ImageDisplay


@Composable
fun CollectionCard(
    item: MediaCollection,
    modifier: Modifier = Modifier,
    onItemClick: (MediaCollection) -> Unit,
    onLongItemClick: ((item: MediaCollection) -> Unit)? = null,
    isChecked: () -> Boolean,
    isSelecting: Boolean = false,
) {
    Column{
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .clip(MaterialTheme.shapes.large)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), MaterialTheme.shapes.large)
                .combinedClickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { onItemClick(item) },
                    onLongClick =if (isSelecting) {
                        null
                    } else {
                        { onLongItemClick?.invoke(item) }
                    }
                )
        ) {
            ImageDisplay(
                uri = item.thumbNail,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                mediaType = MediaType.IMAGE
            )

            if (isSelecting) {
                CircularCheckbox(
                    checked = isChecked(),
                    onCheckedChange = { onItemClick(item) },
                    modifier = Modifier
                        .offset(x = 8.dp, y = 8.dp)
                        .align(Alignment.TopStart),
                )
            }
        }
        Text(
            text = item.name.replaceFirstChar { char -> char.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding( start = 8.dp),
            color= MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.size.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp),
            color= MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
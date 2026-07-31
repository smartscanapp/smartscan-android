package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.ui.components.common.CircularCheckbox


@Composable
fun MediaItemCard(
    item: MediaItem,
    isSelecting: Boolean,
    isChecked: () -> Boolean,
    onToggleSelected: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSelectionMode: () -> Unit,
    onError:((AsyncImagePainter.State.Error) -> Unit)? = null,
){
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .border(1.dp, Color.Gray.copy(alpha = 0.2f))
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (isSelecting) onToggleSelected(item) else onItemClick(item)
                },
                onLongClick = {
                    if (!isSelecting) {
                        onToggleSelectionMode()
                        onToggleSelected(item)
                    }
                }
            )
    ) {
        ImageDisplay(
            uri = item.uri,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            mediaType = item.type,
            onError=onError
        )
        if(isSelecting) {
            CircularCheckbox(
                checked = isChecked() ,
                onCheckedChange = { onToggleSelected(item) },
                modifier = Modifier
                    .offset(x = 8.dp, y = 8.dp)
                    .align(Alignment.TopStart),
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
                Icon(Icons.Filled.OpenInFull, contentDescription = "Expand item", modifier = Modifier
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
}
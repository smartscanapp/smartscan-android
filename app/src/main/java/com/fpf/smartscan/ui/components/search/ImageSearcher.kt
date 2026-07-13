package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.components.media.ImageDisplay
import com.fpf.smartscan.ui.components.pickers.OptionPicker

@Composable
fun ImageSearcher(
    uri: Uri?,
    mediaType: MediaType,
    mediaTypeSelectorEnabled: Boolean,
    onMediaTypeChange: (type: MediaType) -> Unit,
    onSearch: () -> Unit,
    onRemoveImage: () -> Unit,
    imageSize: Dp = 150.dp
){
    if(uri == null) return

    var currentMediaType by remember { mutableStateOf(mediaType) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(mediaType) {
        if(currentMediaType == mediaType) return@LaunchedEffect
        onSearch()
        currentMediaType = mediaType
    }

    Row (
        modifier = Modifier.fillMaxWidth().background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium
        ).padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .heightIn(max = 160.dp)
                .size(imageSize)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                .dropShadow(
                    shape = RoundedCornerShape(4.dp),
                    shadow = Shadow(
                        radius = 4.dp,
                        spread = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.15f),
                        offset = DpOffset(x = 2.dp, 2.dp)
                    )
                )
        ) {
            ImageDisplay(
                maxSize = 720,
                uri = uri,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = { onRemoveImage() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    .size(16.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove Image",
                    tint = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            OutlinedButton (
                enabled = mediaTypeSelectorEnabled,
                onClick = { showPicker = true },
                modifier = Modifier.widthIn(max = 140.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mediaTypeOptions[mediaType]!!,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface

                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint =MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ImageSearch,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "ImageSearch",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha= 0.1f)
            )
        }
    }

    OptionPicker(
        isVisible = showPicker,
        title = stringResource(R.string.media_type_title),
        options = mediaTypeOptions.values.toList(),
        selectedOption = mediaTypeOptions[mediaType]!!,
        onSelect = { selected ->
            val newMode = mediaTypeOptions.entries
                .find { it.value == selected }
                ?.key ?: MediaType.IMAGE
            onMediaTypeChange(newMode)
            showPicker = false
        },
        onClose = {showPicker = false}
    )
}
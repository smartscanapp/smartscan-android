package com.fpf.smartscan.ui.components.collections


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.fpf.smartscan.core.collections.MediaCollection
import com.fpf.smartscan.ui.components.common.Grid

@Composable
fun MultiCollectionPicker(
    collections: List<MediaCollection>,
    onClose: () -> Unit,
    onSaveSelectedCollections: () -> Unit,
    onItemClick: (MediaCollection) -> Unit,
    isChecked: (MediaCollection) -> Boolean,
    ) {

    Popup(
        onDismissRequest = { onClose() },
        properties = PopupProperties(
            dismissOnBackPress = true,
            focusable = true)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    TextButton(
                        onClick = { onSaveSelectedCollections() }
                    ) {
                        Text(text = "Save", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Grid(
                        items = collections,
                        numGridColumns = 3,
                        key = {it.id},
                        isVisible = true,
                        itemContent = { item ->
                            CollectionCard(
                                item=item,
                                isSelecting = true,
                                onItemClick= onItemClick,
                                isChecked = {isChecked(item)}
                            )
                        }
                    )
                }
            }
        }
    }
}
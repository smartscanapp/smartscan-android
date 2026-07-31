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
import com.fpf.smartscan.core.media.MediaCollection

@Composable
fun CollectionPicker(
    collections: List<MediaCollection>,
    onClose: () -> Unit,
    onSelectCollection: (collection: MediaCollection) -> Unit,
    onCreateNewCollection: (() -> Unit)? = null
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
                    if(onCreateNewCollection != null) {
                        TextButton(
                            onClick = { onCreateNewCollection.invoke() }
                        ) {
                            Text(text = "Create", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    MediaCollectionsList(
                        isVisible = true,
                        numGridColumns = 3,
                        items = collections,
                        onItemClick = { onSelectCollection(it);  },
                    )
                }
            }
        }
    }
}
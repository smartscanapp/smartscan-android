package com.fpf.smartscan.ui.screens.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.CollectionType

@Composable
fun EmptyCollectionScreen(
    isVisible: Boolean,
    isMainScanRequired: Boolean,
    collectionType: CollectionType
    ) {
    if (!isVisible) return

    val message = when(collectionType){
        CollectionType.TAG -> stringResource(R.string.collections_tag_collections_description)
        CollectionType.CLUSTER -> stringResource(R.string.collections_auto_collections_description)
    }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = "Album icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )

            Text(
                text = stringResource(R.string.collections_no_collections_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if( isMainScanRequired){
                Text(text = stringResource(R.string.alert_initial_scan_required), color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

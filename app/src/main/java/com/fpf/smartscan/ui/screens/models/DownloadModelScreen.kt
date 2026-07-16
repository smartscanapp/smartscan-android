package com.fpf.smartscan.ui.screens.models

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R

@Composable
fun DownloadModelScreen(
    message: String,
    onDownload: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = "Download icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = stringResource(R.string.download_model_action),
                textAlign = TextAlign.Left,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = message,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDownload
            ) {
                Text(
                    text = stringResource(R.string.download_model_action)
                )
            }
        }
    }
}
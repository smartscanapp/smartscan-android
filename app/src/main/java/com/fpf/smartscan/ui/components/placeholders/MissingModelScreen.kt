package com.fpf.smartscan.ui.components.placeholders

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R

@Composable
fun MissingModelScreen(
    message: String,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
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
            modifier=Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = message,
            textAlign = TextAlign.Left
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
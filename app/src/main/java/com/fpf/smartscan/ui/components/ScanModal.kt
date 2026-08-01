package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.MediaType

@Composable
fun ScanModal(
    isVisible: Boolean,
    title: String,
    description: String,
    onClose: () -> Unit,
    onConfirm: (mediaTypes: List<MediaType>) -> Unit,
    mediaType: MediaType? = null
) {
    if (!isVisible) return

    var selectedMediaTypes by remember {
        mutableStateOf<List<MediaType>>(emptyList())
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(title)
        },
        text = {
            Column {
                Text(description)

                if (mediaType == null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.media_types_to_scan),
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MediaType.entries.forEach { type ->
                        val checked = type in selectedMediaTypes

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedMediaTypes =
                                        if (isChecked) {
                                            selectedMediaTypes + type
                                        } else {
                                            selectedMediaTypes - type
                                        }
                                }
                            )

                            Text(
                                text = type.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose
            ) {
                Text(stringResource(R.string.cancel_action))
            }
        },
        confirmButton = {
            TextButton(
                enabled = mediaType != null || selectedMediaTypes.isNotEmpty(),
                onClick = {
                    onConfirm(
                        if (mediaType != null) {
                            listOf(mediaType)
                        } else {
                            selectedMediaTypes
                        }
                    )
                }
            ) {
                Text(stringResource(R.string.confirm_action))
            }
        }
    )
}
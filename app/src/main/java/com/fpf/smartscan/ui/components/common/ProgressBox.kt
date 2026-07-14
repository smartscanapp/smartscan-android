package com.fpf.smartscan.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProgressBox(
    isVisible: Boolean,
    title: String,
    progress: Int,
    modifier: Modifier = Modifier,
    label: String = "",
    onCancel: () -> Unit,
) {
    if (!isVisible) return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        title = {
            Text(text = title)
        },
        text = {
            Column {
                ProgressBar(
                    isVisible = true,
                    label = "$label $progress%",
                    progress = progress.toFloat() / 100
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {}
    )
}
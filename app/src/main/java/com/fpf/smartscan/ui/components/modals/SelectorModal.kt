package com.fpf.smartscan.ui.components.modals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.ui.components.pickers.OptionPicker

@Composable
fun <T> SelectorModal(
    isVisible: Boolean,
    title: String,
    options: List<Pair<String, T>>,
    onClose: () -> Unit,
    onConfirm: (T) -> Unit,
    label: String,
    initialOption: T? = null
) {
    if (!isVisible) return

    var selectedOption by remember { mutableStateOf(initialOption) }
    var showPicker by remember { mutableStateOf(false) }

    val selectedLabel = options.find { it.second == selectedOption }?.first ?: "Select option"

    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label)
                OutlinedButton(
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
                            text = selectedLabel,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedOption != null,
                onClick = { selectedOption?.let(onConfirm) }
            ) {
                Text("Confirm")
            }
        }
    )

    OptionPicker(
        isVisible = showPicker,
        title = label,
        options = options,
        selectedOption = selectedOption,
        onSelect = {
            selectedOption = it
            showPicker = false
        },
        onClose = { showPicker = false }
    )
}
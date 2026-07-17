package com.fpf.smartscan.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder:  @Composable (() -> Unit)? = null,
    description: String? = null,
    enabled: Boolean = true,
    isNumeric: Boolean = false
) {
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = textColor, modifier = Modifier.padding(bottom = 4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.small,
                keyboardOptions = if (isNumeric) {
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions.Default
                }
            )
            if (description != null) {
                Text(text = description, fontSize = 12.sp, color = textColor)
            }
        }
    }
}



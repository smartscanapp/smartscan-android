package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MediaViewerDescriptionView(
    description: String?,
    onSave: (String) -> Unit
) {
    var editing by remember {
        mutableStateOf(description.isNullOrBlank())
    }

    var editedDescription by remember(description) {
        mutableStateOf(description.orEmpty())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {

        DescriptionHeader(
            editing = editing,
            onEdit = {
                editing = true
            }
        )

        Spacer(Modifier.height(20.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(Modifier.height(20.dp))

        if (editing) {
            DescriptionEditor(
                value = editedDescription,
                onChange = {
                    editedDescription = it
                }
            )

            Spacer(Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = editedDescription.isNotBlank(),
                onClick = {
                    onSave(editedDescription.trim())
                    editing = false
                }
            ) {
                Text("Save")
            }
        } else {
            Text(
                text = description.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DescriptionHeader(
    editing: Boolean,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        if (!editing) {
            FilledTonalIconButton(
                onClick = onEdit
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit description"
                )
            }
        }
    }
}

@Composable
private fun DescriptionEditor(
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 5,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = "Describe what this is about and why it's important or useful.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
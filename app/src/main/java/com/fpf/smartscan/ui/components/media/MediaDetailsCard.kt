package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.media.CollectionType

@Composable
fun MediaDetailsCard(
    description: String?,
    collections: List<Triple<Long, String, CollectionType>>,
    modifier: Modifier = Modifier,
    onCollectionClick: (Long, CollectionType) -> Unit,
    onSave: (String) -> Unit
) {
    var editing by remember {
        mutableStateOf(description.isNullOrBlank())
    }

    var editedDescription by remember(description) {
        mutableStateOf(
            TextFieldValue(
                text = description.orEmpty(),
                selection = TextRange(description.orEmpty().length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(editing) {
        if (editing && !description.isNullOrBlank()) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            if (editing) {
                TextButton(
                    enabled = editedDescription.text.isNotBlank() &&
                            description != editedDescription.text.trim(),
                    onClick = {
                        onSave(editedDescription.text.trim())
                        editing = false
                    }
                ) {
                    Text("Save")
                }
            } else {
                TextButton(
                    onClick = {
                        editedDescription = editedDescription.copy(
                            selection = TextRange(editedDescription.text.length)
                        )
                        editing = true
                    }
                ) {
                    Text("Edit")
                }
            }
        }

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        if (editing) {
            BasicTextField(
                value = editedDescription,
                onValueChange = {
                    editedDescription = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                minLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (editedDescription.text.isEmpty()) {
                        Text(
                            text = "Describe what this is about and or why it's important or useful.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        } else {
            Text(
                text = description.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        if (collections.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                collections.forEach { (id, name, type) ->

                    AssistChip(
                        onClick = {
                            onCollectionClick(id, type)
                        },
                        label = {
                            Text("#$name")
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (type) {
                                CollectionType.CLUSTER ->
                                    MaterialTheme.colorScheme.primaryContainer

                                CollectionType.TAG ->
                                    MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = when (type) {
                                CollectionType.CLUSTER ->
                                    MaterialTheme.colorScheme.primary

                                CollectionType.TAG ->
                                    MaterialTheme.colorScheme.secondary
                            }
                        )
                    )
                }
            }
        }
    }
}
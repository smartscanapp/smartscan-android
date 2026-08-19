package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.CollectionType

@Composable
fun MediaDetailsCard(
    description: String?,
    collections: List<Triple<Long, String, CollectionType>>,
    modifier: Modifier = Modifier,
    onCollectionClick: ((Long, CollectionType) -> Unit)? = null,
    onEditDescription: (() -> Unit)? = null,
) {
    val viewCollectionEnabled = onCollectionClick != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.description),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            onEditDescription?.let {
                TextButton(onClick = it) {
                    Text(if (description.isNullOrBlank()) stringResource(R.string.add_description) else stringResource(R.string.edit))
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

        Text(
            text = description?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.placeholders_media_detail_description),
            style = MaterialTheme.typography.bodyLarge,
            color = if (description.isNullOrBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (collections.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                collections.forEach { (id, name, type) ->
                    AssistChip(
                        onClick = {
                            if (viewCollectionEnabled) {
                                onCollectionClick(id, type)
                            }
                        },
                        label = { Text("#$name") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (type) {
                                CollectionType.CLUSTER -> MaterialTheme.colorScheme.primaryContainer
                                CollectionType.TAG -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = when (type) {
                                CollectionType.CLUSTER -> MaterialTheme.colorScheme.primary
                                CollectionType.TAG -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    )
                }
            }
        }
    }
}
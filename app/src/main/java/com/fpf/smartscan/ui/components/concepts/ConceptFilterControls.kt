package com.fpf.smartscan.ui.components.concepts

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.media.MediaType

@Composable
fun ConceptFilterControls(
    filter: MediaFilter,
    label: String,
    onSetMediaType: (MediaType?) -> Unit,
    onSetShowHidden: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.media_type_title),
                    style = MaterialTheme.typography.labelLarge,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val mediaOptions = MediaType.entries + null

                    mediaOptions.forEach { mediaType ->
                        val selected = filter.mediaType == mediaType

                        Button(
                            onClick = { onSetMediaType(mediaType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = mediaType?.name
                                    ?.lowercase()
                                    ?.replaceFirstChar { it.uppercase() }
                                    ?: "All",
                                fontSize = 12.sp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.inverseOnSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.show_hidden_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Switch(
                    checked = filter.showHidden == null,
                    onCheckedChange = onSetShowHidden
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onResetFilters
            ) {
                Text(stringResource(R.string.reset_filters_action))
            }
        }
    }
}
package com.fpf.smartscan.ui.components.collections

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.media.MediaType


@Composable
fun CollectionFilterControls(
    filter: MediaFilter,
    label: String,
    onSetDuplicateFilter: (Boolean?) -> Unit,
    onSetMediaType: (MediaType?) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        val color = if (filter.mediaType == mediaType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        val textColor = if (filter.mediaType == mediaType) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface

                        Button(
                            onClick = { onSetMediaType(mediaType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                mediaType?.name?.lowercase()?.replaceFirstChar { it.uppercase() }?: "All",
                                fontSize = 12.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.duplicate_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val duplicateFilterOptions = listOf(null, false, true)

                    duplicateFilterOptions.forEach { duplicateFilter ->
                        val color = if (filter.isDuplicate == duplicateFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        val textColor = if (filter.isDuplicate == duplicateFilter) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
                        val label = when(duplicateFilter){
                            true -> stringResource(R.string.only_button)
                            false ->  stringResource(R.string.exclude_button)
                            else ->  stringResource(R.string.include_button)
                        }

                        Button(
                            onClick = { onSetDuplicateFilter(duplicateFilter) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton  (
                onClick = {
                    onResetFilters()
                }
            ) {
                Text(stringResource(R.string.reset_filters_action))
            }
        }
    }
}
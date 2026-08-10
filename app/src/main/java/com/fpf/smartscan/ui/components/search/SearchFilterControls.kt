package com.fpf.smartscan.ui.components.search

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.utils.formatDate

@Composable
fun SearchFilterControls(
    filter: SearchFilter,
    label: String,
    onSetMediaType: (MediaType) -> Unit,
    onSetDuplicateFilter: (Boolean?) -> Unit,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit,
    onRemoveStartDate: () -> Unit,
    onRemoveEndDate: () -> Unit,
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
                    MediaType.entries.forEach { mediaType ->
                        val color =
                            if (filter.mediaType == mediaType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        val textColor =
                            if (filter.mediaType == mediaType) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface

                        Button(
                            onClick = { onSetMediaType(mediaType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                mediaType.name.lowercase().replaceFirstChar { it.uppercase() },
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.search_date_range_label),
                    style = MaterialTheme.typography.labelLarge,
                    )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shape = MaterialTheme.shapes.extraLarge
                    Row(
                        modifier = Modifier
                            .clip(shape)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.extraLarge
                            )
                            .clickable { onSelectStartDate() }
                            .padding(ButtonDefaults.ContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter.startDate?.let { formatDate(it) }
                                ?: stringResource(R.string.search_start_date_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )

                        if (filter.startDate != null) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear start date",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        onRemoveStartDate()
                                    },
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .clip(shape)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.extraLarge
                            )
                            .clickable { onSelectEndDate() }
                            .padding(ButtonDefaults.ContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            filter.endDate?.let { formatDate(it) }
                                ?: stringResource(R.string.search_end_date_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )

                        if (filter.endDate != null) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear start date",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        onRemoveEndDate()
                                    },
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
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
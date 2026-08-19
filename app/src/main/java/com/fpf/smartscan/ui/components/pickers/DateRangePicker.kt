package com.fpf.smartscan.ui.components.pickers

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.R
import com.fpf.smartscan.utils.formatDate

@Composable
fun DateRangePicker(
    label: String,
    startDate: Long?,
    endDate: Long?,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit,
    onRemoveStartDate: () -> Unit,
    onRemoveEndDate: () -> Unit,
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
                            text = startDate?.let { formatDate(it) }
                                ?: stringResource(R.string.search_start_date_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )

                        if (startDate != null) {
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
                            endDate?.let { formatDate(it) }
                                ?: stringResource(R.string.search_end_date_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )

                        if (endDate != null) {
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
    }
}
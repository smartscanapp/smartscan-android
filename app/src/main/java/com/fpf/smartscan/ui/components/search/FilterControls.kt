package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.ui.components.common.Collapsable
import com.fpf.smartscan.ui.components.pickers.FlowRowPicker

@Composable
fun FilterControls(
    dateFilter: Long?,
    mediaTypeFilter: MediaType?,
    duplicateFilter: Boolean?,
    collapse: Boolean? = null,
    mediaTypeOptions: List<Pair<String, MediaType>>,
    duplicateOptions: List<Pair<String, Boolean>>,
    dateOptions: List<Pair<String, Long>>,
    onMediaTypeChange: (MediaType) -> Unit,
    onDuplicateChange: (Boolean) -> Unit,
    onDateChange: (Long) -> Unit,
) {
    Collapsable(
        initialValue = true,
        collapse = collapse,
        label = "Filters",
        modifier = Modifier.fillMaxWidth(),
        headerModifier  = Modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRowPicker(
                label = stringResource(R.string.media_type_title),
                selectedOption = mediaTypeFilter,
                options = mediaTypeOptions,
                onClick = onMediaTypeChange
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRowPicker(
                label = stringResource(R.string.duplicate_label),
                selectedOption =duplicateFilter,
                options =duplicateOptions,
                onClick = onDuplicateChange,
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRowPicker(
                label = "Date",
                selectedOption = dateFilter,
                options = dateOptions,
                onClick =onDateChange,
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
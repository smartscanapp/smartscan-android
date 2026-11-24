package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RecentSearchesList(
    isVisible: Boolean,
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    if (!isVisible) return

    val screenHeight = LocalWindowInfo.current.containerSize.height.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight / 2)
            .verticalScroll(rememberScrollState())
            .clip(MaterialTheme.shapes.medium)
    ) {

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ){
            label?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium)
            }

            TextButton(
                onClick = onClearAll
            ) {
                Text(
                    text = "Clear all",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }


        BoxWithConstraints(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            val itemMaxWidth = maxWidth

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentSearches.forEach { query ->
                    Row(
                        modifier = Modifier
                            .widthIn(max = itemMaxWidth)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onSelect(query) }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = query,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(
                                weight = 1f,
                                fill = false
                            )
                        )

                        IconButton(
                            onClick = {
                                onRemoveRecentSearch(query)
                            },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove recent search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.fpf.smartscan.ui.components.concepts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.ui.components.common.CircularCheckbox


@Composable
fun ConceptCard(
    item: Concept,
    isSelecting: Boolean,
    isChecked: () -> Boolean,
    onToggleSelected: (Concept) -> Unit,
    onItemClick: (Concept) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSelectionMode: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(shape)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape
            )
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (isSelecting) {
                        onToggleSelected(item)
                    } else {
                        onItemClick(item)
                    }
                },
                onLongClick = {
                    if (!isSelecting) {
                        onToggleSelectionMode()
                        onToggleSelected(item)
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier=Modifier.height(16.dp))

            Text(
                text = "${item.size} items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelecting) {
            CircularCheckbox(
                checked = isChecked(),
                onCheckedChange = { onToggleSelected(item) },
                modifier = Modifier
                    .offset(x = 8.dp, y = 8.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}
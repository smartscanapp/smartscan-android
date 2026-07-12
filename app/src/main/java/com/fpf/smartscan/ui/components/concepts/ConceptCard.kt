package com.fpf.smartscan.ui.components.concepts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onItemClick: (Concept) -> Unit,
    onItemLongClick: (Concept) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .heightIn(max = 216.dp)
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
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier=Modifier.weight(1f, fill = false),
                text = item.description.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
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
                onCheckedChange = { onItemClick(item) },
                modifier = Modifier
                    .offset(x = 8.dp, y = 8.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}
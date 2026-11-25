package com.fpf.smartscan.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun Collapsable(
    label: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    initialValue: Boolean = false,
    collapse: Boolean? = null,
    children: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initialValue) }

    LaunchedEffect(collapse) {
        if(collapse == true){
            expanded = false
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = headerModifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp)
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp, top = 8.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) + fadeOut()
        ) {
            children()
        }
    }
}
package com.fpf.smartscan.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.action.MenuActionConfig

@Composable
fun DropDownMenuWrapper(
    actions: List<MenuActionConfig>,
    expanded: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(x = 0.dp, y = (-40).dp),
){
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = { onClose() },
        offset = offset,
        shape= RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        for (action in actions){
            if(!action.enabled && action.hideIfDisabled) continue
            when(action){
                is MenuActionConfig.Button -> DropdownMenuItem(
                    enabled = action.enabled,
                    text = {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyLarge
                        ) },
                    onClick = {
                        onClose()
                        action.onClick()
                    }
                )
                is MenuActionConfig.Switch -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = action.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        enabled = action.enabled,
                        checked = action.checked,
                        onCheckedChange = action.onCheckedChange,
                        modifier = Modifier.scale(0.8f),
                        )
                }
            }
        }
    }
}
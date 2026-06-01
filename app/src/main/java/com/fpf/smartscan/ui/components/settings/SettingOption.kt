package com.fpf.smartscan.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.action.SettingActionConfig

@Composable
fun SettingOption(config: SettingActionConfig, modifier: Modifier = Modifier) {
    val minHeight = 48
    when(config){
        is SettingActionConfig.Button -> {
            val textColor = if (config.enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = config.enabled,
                        onClick = config.onClick
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = modifier
                        .heightIn(min = minHeight.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = config.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                    config.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.fillMaxWidth().alpha(0.8f)
                        )
                    }
                }
            }
        }

        is SettingActionConfig.Switch -> {
            val textColor = if (config.enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = config.enabled,
                        onClick = { config.onCheckedChange(!config.checked) }
                    )
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier.heightIn(min = minHeight.dp).fillMaxWidth()
                ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = config.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                    config.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.fillMaxWidth(0.8f).alpha(0.8f)
                            )
                        }
                    }
                    Switch(
                        enabled = config.enabled,
                        checked = config.checked,
                        onCheckedChange = config.onCheckedChange,
                    )
                }
            }
        }
    }
}
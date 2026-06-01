package com.fpf.smartscan.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.action.SettingActionConfig

@Composable
fun SettingSection(
    title: String,
    settingActionConfigs: List<SettingActionConfig>,
    ) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Column(
        modifier = Modifier.drawBehind {
            val stroke = 1.dp.toPx()
            drawLine(
                color = borderColor,
                start = Offset(0f, size.height - stroke / 2),
                end = Offset(size.width, size.height - stroke / 2),
                strokeWidth = stroke
            )
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )

        for (setting in settingActionConfigs) {
            SettingOption(setting, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}
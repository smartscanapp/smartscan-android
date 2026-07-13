package com.fpf.smartscan.ui.action

import androidx.compose.ui.graphics.vector.ImageVector

data class ActionConfig(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val hideIfDisabled: Boolean = false,
    val icon: ImageVector? = null,
)
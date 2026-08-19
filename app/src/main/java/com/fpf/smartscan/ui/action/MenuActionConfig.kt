package com.fpf.smartscan.ui.action

import androidx.compose.ui.graphics.vector.ImageVector

sealed interface MenuActionConfig {
    val enabled: Boolean
    val hideIfDisabled: Boolean

    data class Button(
        val label: String,
        val onClick: () -> Unit,
        val icon: ImageVector? = null,
        override val enabled: Boolean = true,
        override val hideIfDisabled: Boolean = false
    ) : MenuActionConfig

    data class Switch(
        val label: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        override val enabled: Boolean = true,
        override val hideIfDisabled: Boolean = false
    ) : MenuActionConfig
}
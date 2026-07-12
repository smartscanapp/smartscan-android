package com.fpf.smartscan.ui.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun ActionRowWithFade(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else -30f,
        animationSpec = tween(durationMillis = 300)
    )

    if (alpha > 0f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(-1f)
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translationY
                }
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            content()
        }
    }
}
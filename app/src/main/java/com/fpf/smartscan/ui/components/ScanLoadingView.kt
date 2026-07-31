package com.fpf.smartscan.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.core.index.IndexingStatus
import com.fpf.smartscan.ui.components.common.ProgressBar

@Composable
fun ScanLoadingView(
    title: String,
    isIndexing: Boolean,
    imageIndexStatus: IndexingStatus,
    videoIndexStatus: IndexingStatus,
    imageIndexProgress: Float,
    videoIndexProgress: Float,
    message: String? = null
) {
    if (!isIndexing) return

    val infiniteTransition = rememberInfiniteTransition(label = "scan_rotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = TweenSpec(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ){}
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = "Scanning media",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp).rotate(rotation)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            message?.let {
                Text(
                    text = it,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ProgressBar(
                label = "${stringResource(R.string.search_image_scan_progress_bar_label)} ${
                    "%.0f".format(
                        imageIndexProgress * 100
                    )
                }%",
                isVisible = imageIndexStatus == IndexingStatus.ACTIVE,
                progress = imageIndexProgress
            )

            ProgressBar(
                label = "${stringResource(R.string.search_video_scan_progress_bar_label)} ${
                    "%.0f".format(
                        videoIndexProgress * 100
                    )
                }%",
                isVisible = videoIndexStatus == IndexingStatus.ACTIVE,
                progress = videoIndexProgress
            )
        }
    }
}
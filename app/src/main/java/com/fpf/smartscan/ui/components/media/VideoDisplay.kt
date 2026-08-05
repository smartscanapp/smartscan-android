package com.fpf.smartscan.ui.components.media

import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
@Composable
fun VideoDisplay(
    videoId: Long,
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    showControls: Boolean = false,
    onSizeChanged: ((Int, Int) -> Unit)? = null,
    onTap: () -> Unit = {},
) {
    var playerView by remember {
        mutableStateOf<CustomPlayerView?>(null)
    }

    DisposableEffect(player, videoId) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                onSizeChanged?.invoke(size.width, size.height)
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(showControls, playerView) {
        playerView?.apply {
            if (showControls) {
                controllerAutoShow = true
                showController()
            } else {
                controllerAutoShow = false
                hideController()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            CustomPlayerView(ctx).apply {
                this.player = player
                useController = true
                this.onTap = onTap
                playerView = this

                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }

            view.onTap = onTap
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
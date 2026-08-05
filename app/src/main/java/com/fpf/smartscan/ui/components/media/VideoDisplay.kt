package com.fpf.smartscan.ui.components.media

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
@Composable
fun VideoDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    showControls: Boolean = false,
    playbackPosition: Long = 0L,
    pause: Boolean = false,
    onPlaybackPositionChanged: ((Long) -> Unit)? = null,
    onSizeChanged: ((Int, Int) -> Unit)? = null,
    onTap: () -> Unit = {},
) {
    val context = LocalContext.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    var playerView by remember {
        mutableStateOf<CustomPlayerView?>(null)
    }

    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.seekTo(playbackPosition)
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                onSizeChanged?.invoke(size.width, size.height)
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            onPlaybackPositionChanged?.invoke(exoPlayer.currentPosition)
        }
    }

    LaunchedEffect(pause) {
        exoPlayer.playWhenReady = !pause
        if (pause) {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(showControls, playerView) {
        playerView?.apply {
            if (showControls) {
                showController()
                controllerAutoShow = true
            } else {
                hideController()
                controllerAutoShow = false
            }
        }
    }


    AndroidView(
        factory = { ctx ->
            CustomPlayerView(ctx).apply {
                player = exoPlayer
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
            if (view.player !== exoPlayer) {
                view.player = exoPlayer
            }
            view.onTap = onTap
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    )
}
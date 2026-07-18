package com.fpf.smartscan.ui.components.media

import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun VideoDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    playbackPosition: Long = 0L,
    onPlaybackPositionChanged: ((Long) -> Unit)? = null,
    onTap: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    ) {
    val context = LocalContext.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.seekTo(playbackPosition)
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            onPlaybackPositionChanged?.invoke(exoPlayer.currentPosition)
        }
    }

    AndroidView(
        factory = { ctx ->
            SwipeablePlayerView(ctx).apply {
                player = exoPlayer
                useController = true

                this.onTap = onTap
                this.onSwipeLeft = onSwipeLeft
                this.onSwipeRight = onSwipeRight
                this.onSwipeUp = onSwipeUp
                this.onSwipeDown = onSwipeDown

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
            view.onSwipeLeft = onSwipeLeft
            view.onSwipeRight = onSwipeRight
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
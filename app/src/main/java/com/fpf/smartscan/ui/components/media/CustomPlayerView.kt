package com.fpf.smartscan.ui.components.media

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.OptIn
import androidx.core.view.isNotEmpty
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

class CustomPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PlayerView(context, attrs) {

    private var currentScale = 1f
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    var onTap: (() -> Unit)? = null

    @OptIn(UnstableApi::class)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        if (scaleGestureDetector.isInProgress) {
            return true
        }

        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun setScale(scale: Float, animate: Boolean = false) {
        currentScale = scale

        if (isNotEmpty()) {
            getChildAt(0).apply {
                if (animate) {
                    animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(200)
                        .start()
                } else {
                    scaleX = scale
                    scaleY = scale
                }
            }
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            setScale((currentScale * detector.scaleFactor).coerceIn(1f, 5f))
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onTap?.invoke()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            setScale(if (currentScale == 1f) 3f else 1f, true)
            return true
        }
    }
}
package com.fpf.smartscan.ui.components.media

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlin.math.abs

class SwipeablePlayerView(
    context: Context,
    attrs: AttributeSet? = null
) : PlayerView(context, attrs) {

    private var downX = 0f
    private var downY = 0f

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onTap: (() -> Unit)? = null


    @OptIn(UnstableApi::class)
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                showController()
            }

            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY

                val absX = abs(dx)
                val absY = abs(dy)
                val threshold = 100f

                if (absX > absY && absX > threshold) {
                    if (dx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                    return true
                }

                if (absY > absX && absY > threshold) {
                    if (dy < 0) onSwipeUp?.invoke() else onSwipeDown?.invoke()
                    return true
                }

                onTap?.invoke()
            }
        }

        return super.dispatchTouchEvent(event)
    }
}

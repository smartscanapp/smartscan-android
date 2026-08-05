package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.mediaViewerGestures(
    gestureKey: Long,
    isZoomed: Boolean,
    onTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    threshold: Float = 100f
): Modifier {
    return this
        .pointerInput(gestureKey) {
            detectTapGestures(
                onTap = { onTap() },
                onDoubleTap = { onDoubleTap() }
            )
        }
        .pointerInput(isZoomed, threshold) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startPosition = down.position
                var endPosition = startPosition
                var hasMovedEnough = false
                var hasMultiplePointers = false

                while (true) {
                    val event = awaitPointerEvent()

                    if (event.changes.count { it.pressed } > 1) {
                        hasMultiplePointers = true
                    }

                    val change = event.changes.firstOrNull {
                        it.id == down.id
                    }

                    if (change != null) {
                        endPosition = change.position

                        val dx = endPosition.x - startPosition.x
                        val dy = endPosition.y - startPosition.y

                        if (
                            abs(dx) > threshold ||
                            abs(dy) > threshold
                        ) {
                            hasMovedEnough = true
                        }

                        if (change.changedToUpIgnoreConsumed()) {
                            break
                        }
                    }

                    if (event.changes.none { it.pressed }) {
                        break
                    }
                }

                if (
                    isZoomed ||
                    hasMultiplePointers ||
                    !hasMovedEnough
                ) {
                    return@awaitEachGesture
                }

                val dx = endPosition.x - startPosition.x
                val dy = endPosition.y - startPosition.y

                val absX = abs(dx)
                val absY = abs(dy)

                when {
                    absX > absY && absX > threshold -> {
                        if (dx < 0f) {
                            onSwipeLeft()
                        } else {
                            onSwipeRight()
                        }
                    }

                    absY > absX && absY > threshold -> {
                        if (dy < 0f) {
                            onSwipeUp()
                        } else {
                            onSwipeDown()
                        }
                    }
                }
            }
        }
}
package com.fpf.smartscan.ui.components.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp


@Composable
fun ScannerLoadingIndicator(
    modifier: Modifier = Modifier,
    loading: Boolean
) {
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "scan")

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Canvas(modifier.height(24.dp)) {
        val l = 2.dp.toPx()
        val r = size.width - l
        val t = 2.dp.toPx()
        val b = size.height - t
        val c = size.height / 2f
        val s = 6.dp.toPx()

        val path = Path().apply {
            moveTo(l, t + s); lineTo(l, t); lineTo(l + s, t)
            moveTo(r - s, t); lineTo(r, t); lineTo(r, t + s)
            moveTo(l, b - s); lineTo(l, b); lineTo(l + s, b)
            moveTo(r - s, b); lineTo(r, b); lineTo(r, b - s)
        }

        drawPath(path, accent.copy(alpha = 0.35f), style = Stroke(1.4.dp.toPx()))

        drawLine(
            accent.copy(alpha = 0.12f),
            Offset(l, c),
            Offset(r, c),
            1.dp.toPx()
        )

        val x = if (loading) l + (r - l) * progress else size.width / 2f

        if (loading) {
            drawLine(
                accent.copy(alpha = 0.7f),
                Offset((x - 10.dp.toPx()).coerceAtLeast(l), c),
                Offset((x + 10.dp.toPx()).coerceAtMost(r), c),
                1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            accent.copy(alpha = if (loading) 0.7f else 0.4f),
            radius = if (loading) 3.dp.toPx() else 2.dp.toPx(),
            center = Offset(x, c)
        )
    }
}
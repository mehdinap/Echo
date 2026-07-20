package com.example.echo.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.sin

/**
 * Waveform, drawn frame by frame with Canvas — no Lottie, no GIF, no bitmap.
 *
 * Real amplitudes would need Visualizer/RECORD_AUDIO permission, which is a bad trade for a
 * decorative element. Instead the bars are a sum of three sine waves at incommensurate
 * frequencies: the pattern never visibly repeats, and it stalls the moment playback pauses.
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 48,
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "phase",
    )
    // Bars settle to a flat line when paused rather than freezing mid-jump.
    val energy by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.08f,
        animationSpec = tween(600),
        label = "energy",
    )

    Canvas(modifier) {
        drawBars(barCount, phase, energy, color)
    }
}

private fun DrawScope.drawBars(barCount: Int, phase: Float, energy: Float, color: Color) {
    val barWidth = size.width / (barCount * 2f)
    val centerY = size.height / 2f
    val maxAmp = size.height / 2f

    repeat(barCount) { i ->
        val t = i / barCount.toFloat()
        val wave =
            sin(t * 12f + phase) * 0.5f +
            sin(t * 27f - phase * 1.7f) * 0.3f +
            sin(t * 41f + phase * 0.6f) * 0.2f

        // Taper the ends so the waveform reads as one shape, not a wall of bars.
        val envelope = sin(t * Math.PI).toFloat()
        val amplitude = (abs(wave) * envelope * energy).coerceIn(0.02f, 1f)
        val barHeight = maxAmp * amplitude

        val x = size.width * t + barWidth / 2f
        drawLine(
            color = color.copy(alpha = 0.35f + 0.65f * amplitude),
            start = Offset(x, centerY - barHeight),
            end = Offset(x, centerY + barHeight),
            strokeWidth = barWidth,
            cap = StrokeCap.Round,
        )
    }
}

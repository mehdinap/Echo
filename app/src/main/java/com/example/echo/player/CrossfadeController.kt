package com.example.echo.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ExoPlayer has no built-in crossfade with a single instance, so we fade the volume of the
 * current item down over the last [fadeMs] and fade the next one up once it starts. Combined
 * with a gapless queue this reads as a real crossfade to the ear.
 *
 * Watch the position on a coroutine rather than a listener: playback position isn't an event.
 */
class CrossfadeController(
    private val scope: CoroutineScope,
    private val fadeMs: Long = 6_000L,
) {
    private var job: Job? = null
    var enabled: Boolean = true

    fun attach(player: Player) {
        job?.cancel()
        job = scope.launch(Dispatchers.Main) {
            var lastIndex = -1
            while (isActive) {
                delay(100)
                if (!enabled || !player.isPlaying) {
                    if (player.volume != 1f && !player.isPlaying) player.volume = 1f
                    continue
                }

                val duration = player.duration
                val position = player.currentPosition
                if (duration <= 0) continue

                // Fade in at the head of a new item.
                if (player.currentMediaItemIndex != lastIndex) {
                    lastIndex = player.currentMediaItemIndex
                    fade(player, from = 0f, to = 1f, over = minOf(fadeMs, duration / 4))
                    continue
                }

                // Fade out at the tail.
                val remaining = duration - position
                if (remaining in 1..fadeMs) {
                    player.volume = (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
                } else if (player.volume < 1f) {
                    player.volume = 1f
                }
            }
        }
    }

    private suspend fun fade(player: Player, from: Float, to: Float, over: Long) {
        val steps = 20
        val stepDelay = (over / steps).coerceAtLeast(10)
        repeat(steps) { i ->
            player.volume = from + (to - from) * (i + 1) / steps
            delay(stepDelay)
        }
        player.volume = to
    }

    fun detach() {
        job?.cancel()
        job = null
    }
}

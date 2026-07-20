package com.example.echo.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pure-coroutine sleep timer: one cancellable Job that counts down and stops playback.
 * No AlarmManager, no service restart — cancelling the job cancels the timer.
 */
class SleepTimer(private val scope: CoroutineScope) {

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs

    private var job: Job? = null

    val isActive: Boolean get() = job?.isActive == true

    fun start(durationMs: Long, onFinish: () -> Unit) {
        cancel()
        _remainingMs.value = durationMs
        job = scope.launch(Dispatchers.Main) {
            val tick = 1_000L
            while (isActive && _remainingMs.value > 0) {
                delay(tick)
                _remainingMs.value = (_remainingMs.value - tick).coerceAtLeast(0)
            }
            if (_remainingMs.value <= 0L) onFinish()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0
    }
}

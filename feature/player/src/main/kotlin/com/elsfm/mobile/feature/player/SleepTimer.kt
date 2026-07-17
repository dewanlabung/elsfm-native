package com.elsfm.mobile.feature.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SLEEP_TIMER_TICK_MS = 1_000L

/**
 * Pure countdown timer, decoupled from Media3/MediaController so it's unit-testable with a
 * TestDispatcher instead of requiring a real Android Context or MediaController.
 */
internal class SleepTimer(private val scope: CoroutineScope) {
    private var job: Job? = null

    /** Starts counting down from [minutes]; replaces any timer already running. */
    fun start(minutes: Int, onTick: (millisLeft: Long) -> Unit, onFinished: () -> Unit) {
        job?.cancel()
        var millisLeft = minutes * 60_000L
        onTick(millisLeft)

        job = scope.launch {
            while (millisLeft > 0 && isActive) {
                delay(SLEEP_TIMER_TICK_MS)
                millisLeft = (millisLeft - SLEEP_TIMER_TICK_MS).coerceAtLeast(0)
                // isActive guards this write - cancellation is cooperative and only checked
                // at delay()'s suspension point, so without this check a job already
                // cancelled by a subsequent cancel()/start() call could still run one more
                // line and clobber the state that call just set.
                if (isActive) onTick(millisLeft)
            }
            if (isActive) onFinished()
        }
    }

    /** Cancels a running timer, if any. */
    fun cancel() {
        job?.cancel()
        job = null
    }
}

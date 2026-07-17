package com.elsfm.mobile.feature.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    @Test
    fun `ticks down once per second and reports each value`() = runTest {
        val timer = SleepTimer(this)
        val ticks = mutableListOf<Long>()

        timer.start(minutes = 1, onTick = { ticks.add(it) }, onFinished = {})
        advanceTimeBy(3_500)

        // First tick is the initial value fired synchronously by start(), then one per
        // second as the coroutine's delay(1000) elapses.
        assertEquals(listOf(60_000L, 59_000L, 58_000L, 57_000L), ticks)
    }

    @Test
    fun `calls onFinished exactly once when the countdown reaches zero`() = runTest {
        val timer = SleepTimer(this)
        var finishedCount = 0

        timer.start(minutes = 1, onTick = {}, onFinished = { finishedCount++ })
        advanceUntilIdle()

        assertEquals(1, finishedCount)
    }

    @Test
    fun `cancel stops ticking and never calls onFinished`() = runTest {
        val timer = SleepTimer(this)
        val ticks = mutableListOf<Long>()
        var finished = false

        timer.start(minutes = 1, onTick = { ticks.add(it) }, onFinished = { finished = true })
        advanceTimeBy(2_500)
        timer.cancel()
        advanceUntilIdle()

        assertTrue(ticks.isNotEmpty())
        val ticksAtCancel = ticks.size
        assertEquals(false, finished)

        // Advancing further must not produce any more ticks or a late onFinished call -
        // this guards the isActive check inside the loop against firing one more time
        // after cancellation.
        advanceTimeBy(120_000)
        assertEquals(ticksAtCancel, ticks.size)
        assertEquals(false, finished)
    }

    @Test
    fun `starting a new timer cancels the previous one without it clobbering state`() = runTest {
        val timer = SleepTimer(this)
        val ticks = mutableListOf<Long>()

        timer.start(minutes = 1, onTick = { ticks.add(it) }, onFinished = {})
        advanceTimeBy(1_500)
        val ticksFromFirstTimer = ticks.size

        timer.start(minutes = 5, onTick = { ticks.add(it) }, onFinished = {})
        advanceUntilIdle()

        // The restart's initial tick (5 minutes) must be the last recorded value - the
        // cancelled first timer must not sneak in one more stale tick afterwards.
        assertTrue(ticks.size > ticksFromFirstTimer)
        assertEquals(0L, ticks.last())
    }

    @Test
    fun `onTick receives null-free countdown down to zero then onFinished fires`() = runTest {
        val timer = SleepTimer(this)
        var lastTick: Long? = null
        var finished = false

        timer.start(minutes = 1, onTick = { lastTick = it }, onFinished = { finished = true })
        advanceUntilIdle()

        assertEquals(0L, lastTick)
        assertTrue(finished)
    }
}

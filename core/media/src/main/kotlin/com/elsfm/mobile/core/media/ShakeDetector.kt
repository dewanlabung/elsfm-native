package com.elsfm.mobile.core.media

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects intentional shake gestures while ignoring walking, pocket movement,
 * and vehicle vibrations.
 *
 * ─── Algorithm ───────────────────────────────────────────────────────────────
 *
 * Step 1 – High-pass filter (gravity removal)
 *   Raw accelerometer data always includes a ~9.8 m/s² gravity component.
 *   Without removal, gravity inflates the magnitude so that mild tilts or
 *   walking easily exceed a fixed threshold.
 *
 *   A simple IIR low-pass filter tracks the slowly-changing gravity vector:
 *       gravity[i] = α·gravity[i-1] + (1−α)·raw[i]
 *   Subtracting it yields linear (motion-only) acceleration:
 *       linear[i] = raw[i] − gravity[i]
 *
 *   α = 0.8 (HP_FILTER_ALPHA) is the Android-recommended value:
 *   high enough that the gravity estimate follows slow orientation changes,
 *   low enough that brief impulsive shakes are preserved in the remainder.
 *
 * Step 2 – Single-shake detection
 *   If the magnitude of the linear acceleration vector exceeds the sensitivity
 *   preset's threshold (in m/s²), a "shake event" timestamp is recorded.
 *   A minimum inter-shake gap (INTER_SHAKE_MIN_MS) prevents one sustained
 *   jolt from counting as multiple events.
 *
 * Step 3 – Multi-shake window
 *   Shake events older than [ShakeSensitivity.windowMs] are evicted. When
 *   the remaining count reaches [ShakeSensitivity.requiredCount], a skip fires.
 *   This requires deliberate, repeated shaking — walking or a single bump
 *   cannot accumulate enough events in the window.
 *
 * Step 4 – Post-skip cooldown
 *   After a skip, SKIP_COOLDOWN_MS (4 s) must pass before another can fire.
 *
 * Step 5 – Proximity guard (optional)
 *   If a proximity sensor is present, shakes are silently ignored when the
 *   sensor reports "near" (phone face-down in a pocket).
 *
 * ─── Battery efficiency ───────────────────────────────────────────────────────
 *   Sensor is registered at SENSOR_DELAY_UI (~16 ms / ~60 Hz) instead of the
 *   previous SENSOR_DELAY_GAME (~20 ms / 50 Hz at maximum rate). This is more
 *   than adequate for gesture detection and saves CPU wake-ups.
 *   [start]/[stop] are called by PlaybackService based on playback state and
 *   screen state, so the sensor is fully off when it isn't needed.
 *
 * All sensor callbacks are delivered on the main thread (the thread that called
 * [start]); no synchronisation is needed.
 */
class ShakeDetector(
    private val sensorManager: SensorManager,
    initialSensitivity: ShakeSensitivity,
    private val onShake: () -> Unit,
) : SensorEventListener {

    companion object {
        // Time that must pass after a successful skip before another can trigger.
        private const val SKIP_COOLDOWN_MS = 4_000L

        // IIR low-pass filter coefficient for the gravity estimate.
        // Must be in (0, 1); 0.8 is the Android SDK recommendation.
        private const val HP_FILTER_ALPHA = 0.8f

        // Two sensor samples separated by less than this gap are treated as
        // one continuous event rather than two distinct shakes.
        private const val INTER_SHAKE_MIN_MS = 120L
    }

    // Sensitivity can be updated live without restarting the detector.
    @Volatile var sensitivity: ShakeSensitivity = initialSensitivity

    // Smoothed gravity estimate used by the high-pass filter.
    private val gravity = FloatArray(3)

    // Timestamps of recent individual shake events within the rolling window.
    private val recentShakeTimes = ArrayDeque<Long>()

    // Tracks the end of the last single shake event for inter-shake debouncing.
    private var lastSingleShakeMs = 0L

    // Tracks when the last skip was fired to enforce the post-skip cooldown.
    private var lastSkipMs = 0L

    // True when the proximity sensor reports "near" (phone likely in pocket).
    private var isInPocket = false
    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            isInPocket = event.values[0] < event.sensor.maximumRange
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    private var isRunning = false

    /**
     * Registers sensor listeners and begins detection. Safe to call repeatedly;
     * subsequent calls while already running are no-ops.
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        gravity.fill(0f)
        recentShakeTimes.clear()

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            // SENSOR_DELAY_UI ≈ 60 Hz — enough for gesture detection, lower CPU overhead
            // than SENSOR_DELAY_GAME and far lower than SENSOR_DELAY_FASTEST.
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Proximity is optional; silently skipped on devices without the sensor.
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { sensor ->
            sensorManager.registerListener(
                proximityListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL,
            )
        }
    }

    /**
     * Unregisters all sensor listeners and resets transient state. Safe to
     * call when already stopped.
     */
    fun stop() {
        if (!isRunning) return
        isRunning = false
        sensorManager.unregisterListener(this)
        sensorManager.unregisterListener(proximityListener)
        gravity.fill(0f)
        recentShakeTimes.clear()
        isInPocket = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()

        // ── Step 1: Update gravity estimate and compute linear acceleration ──
        //
        // IIR low-pass on raw readings → gravity estimate.
        // Remainder is the high-frequency (motion-only) component.
        gravity[0] = HP_FILTER_ALPHA * gravity[0] + (1f - HP_FILTER_ALPHA) * event.values[0]
        gravity[1] = HP_FILTER_ALPHA * gravity[1] + (1f - HP_FILTER_ALPHA) * event.values[1]
        gravity[2] = HP_FILTER_ALPHA * gravity[2] + (1f - HP_FILTER_ALPHA) * event.values[2]

        val lx = event.values[0] - gravity[0]
        val ly = event.values[1] - gravity[1]
        val lz = event.values[2] - gravity[2]
        val magnitude = sqrt(lx * lx + ly * ly + lz * lz)

        // ── Step 2: Early exits ───────────────────────────────────────────────
        val preset = sensitivity
        if (isInPocket) return                                    // proximity guard
        if (now - lastSkipMs < SKIP_COOLDOWN_MS) return          // post-skip cooldown
        if (magnitude < preset.linearThreshold) return            // below threshold
        if (now - lastSingleShakeMs < INTER_SHAKE_MIN_MS) return // same shake, different sample
        lastSingleShakeMs = now

        // ── Step 3: Accumulate in the rolling window ──────────────────────────
        //
        // Evict stale entries outside the window, then append the new timestamp.
        while (recentShakeTimes.isNotEmpty() &&
            now - recentShakeTimes.first() > preset.windowMs
        ) {
            recentShakeTimes.removeFirst()
        }
        recentShakeTimes.addLast(now)

        // ── Step 4: Fire when the required count is reached ───────────────────
        if (recentShakeTimes.size >= preset.requiredCount) {
            recentShakeTimes.clear()
            lastSkipMs = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}

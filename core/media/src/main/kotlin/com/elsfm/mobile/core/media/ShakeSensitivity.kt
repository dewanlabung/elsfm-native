package com.elsfm.mobile.core.media

/**
 * User-configurable shake sensitivity presets.
 *
 * All thresholds operate on linear acceleration (gravity subtracted via
 * high-pass filter), so the values are independent of device orientation.
 *
 * @param linearThreshold  Minimum linear-acceleration magnitude in m/s²
 *   that registers as one shake event. Higher = harder to trigger.
 * @param requiredCount    Number of distinct shake events required within
 *   [windowMs] before a skip is fired.
 * @param windowMs         Rolling time window in ms for counting shakes.
 */
enum class ShakeSensitivity(
    val linearThreshold: Float,
    val requiredCount: Int,
    val windowMs: Long,
) {
    // Requires 3 strong shakes (≈1.5 G) within 1.5 s — effectively immune
    // to walking, running, or vehicle vibration at any speed.
    LOW(linearThreshold = 15f, requiredCount = 3, windowMs = 1500L),

    // Requires 2 firm shakes (≈1.1 G) within 1.2 s — the recommended
    // default; mimics Poweramp's behaviour.
    MEDIUM(linearThreshold = 11f, requiredCount = 2, windowMs = 1200L),

    // Requires 2 moderate shakes (≈0.8 G) within 1.0 s — responsive
    // but may occasionally trigger on vigorous running.
    HIGH(linearThreshold = 8f, requiredCount = 2, windowMs = 1000L),
}

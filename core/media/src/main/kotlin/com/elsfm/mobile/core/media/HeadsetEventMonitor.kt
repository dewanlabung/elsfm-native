package com.elsfm.mobile.core.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Listens for headset plug/unplug events and resumes playback when headphones
 * are re-inserted after having been unplugged mid-play.
 *
 * [getIsPlaying] is called at unplug time to snapshot the playing state
 * before ExoPlayer's own AUDIO_BECOMING_NOISY handler pauses it.
 * [onResumeRequested] is called when the headset is re-plugged only if we
 * recorded a mid-play unplug.
 *
 * ACTION_HEADSET_PLUG is sticky so the first delivery on registration reflects
 * current state — safe because [wasPlayingBeforeUnplug] starts false.
 */
class HeadsetEventMonitor(
    private val getIsPlaying: () -> Boolean,
    private val onResumeRequested: () -> Unit,
) {
    private var wasPlayingBeforeUnplug = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_HEADSET_PLUG) return
            when (intent.getIntExtra("state", -1)) {
                0 -> wasPlayingBeforeUnplug = getIsPlaying()
                1 -> {
                    if (wasPlayingBeforeUnplug) {
                        wasPlayingBeforeUnplug = false
                        onResumeRequested()
                    }
                }
            }
        }
    }

    fun start(context: Context) {
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }

    fun stop(context: Context) {
        runCatching { context.unregisterReceiver(receiver) }
    }
}

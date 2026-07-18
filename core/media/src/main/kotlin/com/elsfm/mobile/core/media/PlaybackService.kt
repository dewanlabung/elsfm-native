package com.elsfm.mobile.core.media

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.elsfm.mobile.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val USER_AGENT = "ElsfmMobile/1.0 (Android)"

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var shakePreferences: ShakePreferences

    private var mediaSession: MediaSession? = null
    private var shakeDetector: ShakeDetector? = null
    private var headsetMonitor: HeadsetEventMonitor? = null
    private var keyguardManager: KeyguardManager? = null

    // Tracks latest screen-on/off state for the shake-detection gate.
    private var isScreenOn = true

    // Re-evaluates whether shake detection should run whenever the relevant
    // conditions change: playback state, screen state, lock state, or settings.
    private fun updateShakeDetector(player: Player) {
        val isLocked = keyguardManager?.isKeyguardLocked == true
        val shouldRun = shakePreferences.isEnabled &&
            player.isPlaying &&
            isScreenOn &&
            !isLocked
        if (shouldRun) shakeDetector?.start() else shakeDetector?.stop()
    }

    // Receives screen-off / screen-on / keyguard-dismissed broadcasts.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> isScreenOn = false
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> isScreenOn = true
            }
            mediaSession?.player?.let { updateShakeDetector(it) }
        }
    }

    // Kept so a future settings screen can expose band-level EQ controls off this same
    // audio-session-scoped instance; re-created whenever the session id changes (e.g. on
    // route changes to a different output device) since an Equalizer is bound to one id.
    private var equalizer: Equalizer? = null

    override fun onCreate() {
        super.onCreate()
        // Falls back to the authenticated download endpoint (see MediaItemFactory) whenever a
        // track has no `src` - that endpoint requires a Bearer token, but ExoPlayer's HTTP data
        // source never goes through the app's Ktor client/AuthPlugin, so the token has to be
        // attached here instead. Read fresh per data source (called once per track load, on a
        // background loading thread) rather than once at service creation, so a token saved
        // after a later login is picked up without restarting playback.
        val httpDataSourceFactory = object : HttpDataSource.Factory {
            override fun createDataSource(): HttpDataSource {
                val token = runBlocking { sessionManager.currentToken() }
                return DefaultHttpDataSource.Factory()
                    .setUserAgent(USER_AGENT)
                    .apply {
                        if (token != null) {
                            setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
                        }
                    }
                    .createDataSource()
            }

            override fun setDefaultRequestProperties(
                defaultRequestProperties: MutableMap<String, String>,
            ): HttpDataSource.Factory = this
        }
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        // handleAudioFocus = true activates Media3's default AudioFocusManager: pauses on
        // transient loss (e.g. a phone call) and ducks on transient-loss-with-ducking,
        // without any extra listener code here. setHandleAudioBecomingNoisy pauses playback
        // when headphones are unplugged, matching standard platform media-player behavior.
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            // Improves render-loop timing precision, reducing the chance of a scheduler
            // gap between consecutive media items. ExoPlayer already strips gapless
            // metadata from LAME-MP3 and AAC frames; this flag tightens the scheduling
            // side so the transition arrives at the DAC on time.
            .experimentalSetDynamicSchedulingEnabled(true)
            .build()
        attachEqualizer(player)
        val sessionBuilder = MediaSession.Builder(this, player)
        // Without this, tapping the media notification does nothing - `core:media` can't
        // reference `app`'s MainActivity directly (that would be a backwards module
        // dependency), so this uses the launcher intent for our own package instead, which
        // is the standard Media3 pattern for this exact constraint.
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            sessionBuilder.setSessionActivity(pendingIntent)
        }
        mediaSession = sessionBuilder.build()

        keyguardManager = getSystemService(KeyguardManager::class.java)

        shakeDetector = ShakeDetector(
            sensorManager = getSystemService(SensorManager::class.java),
            initialSensitivity = shakePreferences.sensitivity,
            onShake = {
                player.seekToNextMediaItem()
                if (!player.isPlaying) player.play()
            },
        )

        // Pause/resume detection as playback state changes so the sensor is
        // off whenever music is paused (saves battery and avoids false triggers).
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Also sync sensitivity in case the user changed it while paused.
                shakeDetector?.sensitivity = shakePreferences.sensitivity
                updateShakeDetector(player)
            }
        })

        // React to screen-off / screen-on / keyguard-dismissed events.
        // These broadcasts are not deliverable via the manifest, so dynamic
        // registration is required.
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
        )

        // Kick off detection immediately if all conditions are already met.
        updateShakeDetector(player)

        headsetMonitor = HeadsetEventMonitor(
            getIsPlaying = { player.isPlaying },
            onResumeRequested = { player.play() },
        ).also { it.start(this) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        shakeDetector?.stop()
        runCatching { unregisterReceiver(screenReceiver) }
        headsetMonitor?.stop(this)
        equalizer?.release()
        equalizer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /**
     * Wires up the audio-effect chain for [player]'s audio session. Equalizer construction can
     * legitimately fail on devices without a compatible effect (`getOrNull` handles that), and
     * the session id changes whenever the output route changes, so a fresh instance is bound on
     * every transition.
     */
    private fun attachEqualizer(player: ExoPlayer) {
        equalizer = runCatching { Equalizer(0, player.audioSessionId) }.getOrNull()?.apply { enabled = true }
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                equalizer?.release()
                equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()?.apply { enabled = true }
            }
        })
    }
}

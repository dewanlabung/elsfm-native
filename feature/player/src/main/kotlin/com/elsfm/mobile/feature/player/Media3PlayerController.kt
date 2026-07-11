package com.elsfm.mobile.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.elsfm.mobile.core.media.PlaybackService
import com.elsfm.mobile.core.media.toMediaItem
import com.elsfm.mobile.core.model.Track
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val POSITION_POLL_INTERVAL_MS = 500L

@Singleton
class Media3PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerController {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    private var mediaController: MediaController? = null
    private var currentQueue: List<Track> = emptyList()

    // Application-lifetime scope: this controller is a @Singleton with no natural
    // cancellation point, so the ticker below runs for the process's lifetime.
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                attachListener()
            },
            MoreExecutors.directExecutor(),
        )
        startPositionTicker()
    }

    private fun attachListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val track = currentQueue.find { it.id.toString() == mediaItem?.mediaId }
                _state.value = _state.value.copy(
                    currentTrack = track,
                    durationMs = track?.durationMs ?: 0,
                    positionMs = 0,
                )
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.value = _state.value.copy(error = error.message)
            }
        })
    }

    /**
     * ExoPlayer only pushes position updates via listener callbacks on seeks/track
     * changes, never on a timer — without this, the player screen's elapsed-time
     * text and seek bar freeze at 0 while a track plays. Polls current position on
     * a fixed interval instead.
     */
    private fun startPositionTicker() {
        scope.launch {
            while (true) {
                delay(POSITION_POLL_INTERVAL_MS)
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _state.value = _state.value.copy(positionMs = controller.currentPosition.coerceAtLeast(0))
                    }
                }
            }
        }
    }

    override fun play(track: Track, queue: List<Track>) {
        currentQueue = queue
        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = queue.map { it.toMediaItem() }
        _state.value = _state.value.copy(queue = queue, currentTrack = track, durationMs = track.durationMs)
        mediaController?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
    }

    override fun togglePlayPause() {
        mediaController?.apply { if (isPlaying) pause() else play() }
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    override fun skipPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    override fun jumpToQueueItem(track: Track) {
        val index = currentQueue.indexOfFirst { it.id == track.id }
        if (index < 0) return
        mediaController?.apply {
            seekTo(index, 0)
            play()
        }
    }

    override fun addToQueue(track: Track) {
        if (currentQueue.any { it.id == track.id }) return
        currentQueue = currentQueue + track
        _state.value = _state.value.copy(queue = currentQueue)
        mediaController?.addMediaItem(track.toMediaItem())
    }

    override fun toggleShuffle() {
        val newValue = !_state.value.shuffleEnabled
        mediaController?.shuffleModeEnabled = newValue
        _state.value = _state.value.copy(shuffleEnabled = newValue)
    }

    override fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            PlayerRepeatMode.OFF -> PlayerRepeatMode.ALL
            PlayerRepeatMode.ALL -> PlayerRepeatMode.ONE
            PlayerRepeatMode.ONE -> PlayerRepeatMode.OFF
        }
        mediaController?.repeatMode = when (nextMode) {
            PlayerRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            PlayerRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            PlayerRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
    }
}

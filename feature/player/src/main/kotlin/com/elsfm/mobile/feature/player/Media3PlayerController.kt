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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Media3PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerController {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    private var mediaController: MediaController? = null
    private var currentQueue: List<Track> = emptyList()

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
}

package com.elsfm.mobile.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.elsfm.mobile.core.common.PersistedPlaybackState
import com.elsfm.mobile.core.common.PlaybackStateStore
import com.elsfm.mobile.core.media.LocalRecommendationEngine
import com.elsfm.mobile.core.media.PlaybackService
import com.elsfm.mobile.core.media.RecentTracksStore
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
private const val MIN_PLAYBACK_SPEED = 0.5f
private const val MAX_PLAYBACK_SPEED = 2f

@Singleton
class Media3PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackStateStore: PlaybackStateStore,
    private val recentTracksStore: RecentTracksStore,
    private val recommendationEngine: LocalRecommendationEngine,
) : PlayerController {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    private var mediaController: MediaController? = null
    private var currentQueue: List<Track> = emptyList()

    // Application-lifetime scope: this controller is a @Singleton with no natural
    // cancellation point, so the ticker below runs for the process's lifetime.
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val sleepTimer = SleepTimer(scope)

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                attachListener()
                scope.launch { restorePersistedState() }
            },
            MoreExecutors.directExecutor(),
        )
        startPositionTicker()
    }

    private fun attachListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (!isPlaying) persistState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val track = currentQueue.find { it.id.toString() == mediaItem?.mediaId }
                _state.value = _state.value.copy(
                    currentTrack = track,
                    durationMs = track?.durationMs ?: 0,
                    positionMs = 0,
                )
                track?.let { recentTracksStore.record(it) }
                persistState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    autoPlayRecommendations()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.value = _state.value.copy(error = error.message)
            }
        })
    }

    private fun autoPlayRecommendations() {
        val excludeIds = currentQueue.map { it.id }.toSet()
        val recommendations = recommendationEngine.getRecommendations(excludeIds)
        if (recommendations.isEmpty()) return
        recommendations.forEach { track ->
            currentQueue = currentQueue + track
            track.toMediaItem()?.let { mediaController?.addMediaItem(it) }
        }
        _state.value = _state.value.copy(queue = currentQueue)
        mediaController?.seekToNextMediaItem()
        mediaController?.play()
    }

    /**
     * Saves the current queue/track/position/speed/volume so [restorePersistedState] can bring
     * it back on the next app launch. No-ops when nothing is playing yet.
     */
    private fun persistState() {
        val track = _state.value.currentTrack ?: return
        val positionMs = mediaController?.currentPosition?.coerceAtLeast(0) ?: _state.value.positionMs
        scope.launch {
            playbackStateStore.save(
                PersistedPlaybackState(
                    currentTrack = track,
                    queue = currentQueue,
                    positionMs = positionMs,
                    speed = _state.value.playbackSpeed,
                    volume = _state.value.volume,
                ),
            )
        }
    }

    override suspend fun restorePersistedState() {
        val persisted = playbackStateStore.restore() ?: return
        currentQueue = persisted.queue
        val startIndex = persisted.queue.indexOfFirst { it.id == persisted.currentTrack.id }.coerceAtLeast(0)
        val mediaItems = persisted.queue.mapNotNull { it.toMediaItem() }
        _state.value = _state.value.copy(
            queue = persisted.queue,
            currentTrack = persisted.currentTrack,
            durationMs = persisted.currentTrack.durationMs,
            positionMs = persisted.positionMs,
            playbackSpeed = persisted.speed,
            volume = persisted.volume,
        )
        mediaController?.apply {
            setMediaItems(mediaItems, startIndex, persisted.positionMs)
            prepare()
            playbackParameters = PlaybackParameters(persisted.speed)
            volume = persisted.volume
            // Deliberately not calling play() - restored state comes back paused/ready, matching
            // the plan's "restore in a paused state" requirement rather than auto-resuming audio.
        }
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
        val mediaItems = queue.mapNotNull { it.toMediaItem() }
        _state.value = _state.value.copy(queue = queue, currentTrack = track, durationMs = track.durationMs)
        mediaController?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
        persistState()
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
        track.toMediaItem()?.let { mediaController?.addMediaItem(it) }
    }

    override fun playNext(track: Track) {
        if (currentQueue.any { it.id == track.id }) return
        val insertIndex = (mediaController?.currentMediaItemIndex ?: 0) + 1
        val mutableQueue = currentQueue.toMutableList()
        mutableQueue.add(insertIndex.coerceAtMost(mutableQueue.size), track)
        currentQueue = mutableQueue
        _state.value = _state.value.copy(queue = currentQueue)
        track.toMediaItem()?.let { mediaController?.addMediaItem(insertIndex, it) }
    }

    override fun toggleShuffle() {
        val newValue = !_state.value.shuffleEnabled
        mediaController?.shuffleModeEnabled = newValue
        _state.value = _state.value.copy(shuffleEnabled = newValue)
    }

    override fun stop() {
        mediaController?.apply {
            stop()
            clearMediaItems()
        }
        currentQueue = emptyList()
        // Without this, a sleep timer started in a previous session (e.g. before logout)
        // keeps ticking on this app-lifetime singleton and can later pause/clobber state
        // for whichever session is active when it fires.
        sleepTimer.cancel()
        _state.value = PlayerState()
        // An explicit stop (e.g. logout) shouldn't leave stale state for restorePersistedState
        // to bring back on the next launch or next signed-in user.
        scope.launch { playbackStateStore.clear() }
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

    override fun startSleepTimer(minutes: Int) {
        sleepTimer.start(
            minutes = minutes,
            onTick = { millisLeft -> _state.value = _state.value.copy(sleepTimerMillisLeft = millisLeft) },
            onFinished = {
                mediaController?.pause()
                _state.value = _state.value.copy(sleepTimerMillisLeft = null)
            },
        )
    }

    override fun cancelSleepTimer() {
        sleepTimer.cancel()
        _state.value = _state.value.copy(sleepTimerMillisLeft = null)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
        mediaController?.playbackParameters = PlaybackParameters(clamped)
        _state.value = _state.value.copy(playbackSpeed = clamped)
        persistState()
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        mediaController?.volume = clamped
        _state.value = _state.value.copy(volume = clamped)
        persistState()
    }
}

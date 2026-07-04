package com.elsfm.mobile.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.media.PlayHistoryApi
import com.elsfm.mobile.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val playHistoryApi: PlayHistoryApi,
) : ViewModel() {

    val state: StateFlow<PlayerState> = playerController.state

    fun play(track: Track, queue: List<Track>) {
        playerController.play(track, queue)
        viewModelScope.launch { playHistoryApi.recordPlay(track.id) }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
}

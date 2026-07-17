package com.elsfm.mobile.feature.player.lyrics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.LyricsApi
import com.elsfm.mobile.feature.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val TRACK_ID_ARG = "trackId"
private const val POSITION_SHARING_STOP_TIMEOUT_MS = 5_000L

/**
 * Lyrics for a single track's dedicated Lyrics screen, reached from
 * [com.elsfm.mobile.core.designsystem.TrackContextMenu]'s "View lyrics" item. Backed by
 * `LyricsApi.getTrackLyrics()`. [PlayerController] is injected directly (it's a
 * `@Singleton`) rather than through [com.elsfm.mobile.feature.player.PlayerViewModel], so this
 * screen isn't coupled to another screen's ViewModel instance/scope, while still observing the
 * same live playback position for synced-lyrics highlighting.
 */
@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsApi: LyricsApi,
    private val playerController: PlayerController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val trackId: Int = requireNotNull(savedStateHandle[TRACK_ID_ARG]) { "trackId is required" }

    private val _state = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    val positionMs: StateFlow<Long> = playerController.state
        .map { it.positionMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(POSITION_SHARING_STOP_TIMEOUT_MS), 0L)

    init {
        loadLyrics()
    }

    fun retryLoad() {
        loadLyrics()
    }

    private fun loadLyrics() {
        viewModelScope.launch {
            _state.value = LyricsState.Loading
            when (val result = lyricsApi.getTrackLyrics(trackId)) {
                is ApiResult.Success -> {
                    val lyrics = result.data
                    _state.value = if (lyrics.isSynced) {
                        LyricsState.SyncedLyrics(lyrics.lines)
                    } else {
                        LyricsState.PlainLyrics(lyrics.lines.map { it.text })
                    }
                }
                is ApiResult.NetworkError -> {
                    _state.value = LyricsState.Error(result.cause.message ?: "Failed to load lyrics")
                }
                is ApiResult.ValidationError -> {
                    _state.value = LyricsState.Error("Validation error")
                }
                is ApiResult.Unauthorized -> {
                    _state.value = LyricsState.Error("Unauthorized")
                }
            }
        }
    }
}

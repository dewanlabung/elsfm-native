package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.PlaylistInfo
import com.elsfm.mobile.core.network.api.RepostApi
import com.elsfm.mobile.core.network.api.TrackListApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable, hoisted UI state for [PlaylistScreen].
 *
 * `core:network` has no `GET /playlists/{id}` endpoint that returns playlist
 * metadata (name/description/image) — only [TrackListApi.getPlaylistTracks]
 * exists. Until such an endpoint is added, [playlist] is populated from the
 * navigation argument passed into [PlaylistViewModel.loadPlaylist] and only
 * the [tracks] list is backed by a real API call.
 */
data class PlaylistDetailState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val likedTrackIds: Set<Int> = emptySet(),
    val likeLoadingTrackIds: Set<Int> = emptySet(),
    val downloadingTrackIds: Set<Int> = emptySet(),
    val downloadedTrackIds: Set<Int> = emptySet(),
    val isDownloadingPlaylist: Boolean = false,
    val hasMoreTracks: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRenamingPlaylist: Boolean = false,
    val isDeletingPlaylist: Boolean = false,
    val playlistDeleted: Boolean = false,
    val isPlaylistPickerVisible: Boolean = false,
    val isLoadingPlaylists: Boolean = false,
    val userPlaylists: List<PlaylistInfo> = emptyList(),
    val addToPlaylistTrackId: Int? = null,
    val addToPlaylistLoading: Boolean = false,
    val repostLoadingTrackIds: Set<Int> = emptySet(),
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val trackListApi: TrackListApi,
    private val playlistApi: PlaylistApi,
    private val trackLikeController: TrackLikeController,
    private val downloadRepository: DownloadRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val userDao: UserDao,
    private val repostApi: RepostApi,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistDetailState())
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    private var currentPage = 1

    fun loadPlaylist(playlist: Playlist) {
        currentPage = 1
        _state.update { it.copy(playlist = playlist, isLoading = true, error = null) }

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = trackListApi.getPlaylistTracks(playlist.id, currentPage)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            tracks = result.data.tracks,
                            hasMoreTracks = result.data.hasMore,
                            isLoading = false,
                        )
                    }
                    loadAllRemainingPages()
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isLoading = false, error = "Failed to load playlist tracks") }
                }
            }
        }
    }

    /**
     * Keeps fetching subsequent pages in the background, independent of scrolling, until
     * the whole playlist is loaded (e.g. all 120 tracks of a youth-camp playlist) - without
     * this, tapping Play right after opening a playlist only queued the first page (30
     * tracks), so background/lock-screen playback stopped there instead of continuing
     * through the rest of the playlist.
     *
     * Stops on the first failed page rather than retrying forever - a page that fails to
     * parse (or a real network error) would otherwise re-request the exact same page in a
     * tight loop indefinitely, since [hasMoreTracks] never changes on failure.
     */
    private suspend fun loadAllRemainingPages() {
        while (_state.value.hasMoreTracks) {
            val loadedNewPage = loadNextPageSuspending()
            if (!loadedNewPage) return
        }
    }

    /** Appends the next page of tracks; called as the user scrolls near the end of the list. */
    fun loadNextPage() {
        viewModelScope.launch(dispatcherProvider.io) { loadNextPageSuspending() }
    }

    /** Returns true if a new page was appended, false if there was nothing to do or it failed. */
    private suspend fun loadNextPageSuspending(): Boolean {
        val playlist = _state.value.playlist ?: return false
        if (_state.value.isLoadingMore || !_state.value.hasMoreTracks) return false

        _state.update { it.copy(isLoadingMore = true) }
        val nextPage = currentPage + 1

        return when (val result = trackListApi.getPlaylistTracks(playlist.id, nextPage)) {
            is ApiResult.Success -> {
                currentPage = nextPage
                // .update {} (not .value =) so this read-modify-write of `tracks` is atomic
                // against deleteTrack's concurrent optimistic-update/revert - both run on
                // dispatcherProvider.io and a plain `.value = .value.copy(...)` here could
                // silently discard a delete (or vice versa) if they interleave.
                _state.update {
                    it.copy(
                        tracks = it.tracks + result.data.tracks,
                        hasMoreTracks = result.data.hasMore,
                        isLoadingMore = false,
                    )
                }
                true
            }
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> {
                _state.update { it.copy(isLoadingMore = false) }
                false
            }
        }
    }

    /**
     * Removes a track from the playlist. Previously this only updated local state, so the
     * track reappeared the next time the playlist loaded - it never actually called the
     * backend's `tracks/remove` endpoint.
     */
    fun deleteTrack(trackId: Int) {
        val playlist = _state.value.playlist ?: return
        val removedTrack = _state.value.tracks.find { it.id == trackId } ?: return

        _state.update { it.copy(tracks = it.tracks.filterNot { track -> track.id == trackId }) }

        viewModelScope.launch(dispatcherProvider.io) {
            val result = playlistApi.removeTrackFromPlaylist(playlist.id, trackId)
            if (result !is ApiResult.Success) {
                // Reinsert into whatever the *current* list looks like rather than
                // overwriting with the snapshot taken above - background pagination runs
                // concurrently on this same dispatcher and may have appended pages in the
                // meantime, which a blind revert-to-snapshot would silently discard.
                _state.update { current ->
                    val tracks = if (current.tracks.any { it.id == trackId }) {
                        current.tracks
                    } else {
                        current.tracks + removedTrack
                    }
                    current.copy(tracks = tracks, error = "Failed to remove track from playlist")
                }
            }
        }
    }

    /** Renames the playlist. Allowed for the owner/editor without any special permission. */
    fun renamePlaylist(name: String) {
        val playlist = _state.value.playlist ?: return
        _state.update { it.copy(isRenamingPlaylist = true) }

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = playlistApi.updatePlaylist(playlist.id, name)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(playlist = playlist.copy(name = result.data.name), isRenamingPlaylist = false)
                    }
                }
                is ApiResult.ValidationError -> {
                    _state.update {
                        it.copy(
                            isRenamingPlaylist = false,
                            error = result.fields.values.flatten().firstOrNull() ?: "Failed to rename playlist",
                        )
                    }
                }
                is ApiResult.NetworkError,
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isRenamingPlaylist = false, error = "Failed to rename playlist") }
                }
            }
        }
    }

    /** Deletes the playlist. Allowed for the owner/editor without any special permission. */
    fun deletePlaylist() {
        val playlist = _state.value.playlist ?: return
        _state.update { it.copy(isDeletingPlaylist = true) }

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = playlistApi.deletePlaylist(playlist.id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isDeletingPlaylist = false, playlistDeleted = true) }
                }
                is ApiResult.ValidationError -> {
                    _state.update {
                        it.copy(
                            isDeletingPlaylist = false,
                            error = result.fields.values.flatten().firstOrNull() ?: "Failed to delete playlist",
                        )
                    }
                }
                is ApiResult.NetworkError,
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isDeletingPlaylist = false, error = "Failed to delete playlist") }
                }
            }
        }
    }

    fun toggleTrackLike(trackId: Int) {
        val currentlyLiked = _state.value.likedTrackIds.contains(trackId)
        _state.update { it.copy(likeLoadingTrackIds = it.likeLoadingTrackIds + trackId) }

        viewModelScope.launch(dispatcherProvider.io) {
            val newLikedState = trackLikeController.toggleLike(trackId, currentlyLiked)
            _state.update {
                it.copy(
                    likedTrackIds = when (newLikedState) {
                        true -> it.likedTrackIds + trackId
                        false -> it.likedTrackIds - trackId
                        null -> it.likedTrackIds
                    },
                    likeLoadingTrackIds = it.likeLoadingTrackIds - trackId,
                    error = if (newLikedState == null) "Failed to update library" else it.error,
                )
            }
        }
    }

    /** "Make available offline" for a single track. */
    fun downloadTrack(track: Track) {
        val playlist = _state.value.playlist
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(downloadingTrackIds = it.downloadingTrackIds + track.id) }
            val result = downloadRepository.downloadTrack(
                track,
                playlistId = playlist?.id,
                playlistName = playlist?.name,
            )
            _state.update {
                it.copy(
                    downloadingTrackIds = it.downloadingTrackIds - track.id,
                    downloadedTrackIds = if (result.isSuccess) it.downloadedTrackIds + track.id else it.downloadedTrackIds,
                    error = if (result.isFailure) "Failed to download track" else it.error,
                )
            }
        }
    }

    /**
     * "Make available offline" for the whole playlist - same per-track download
     * looped client-side as [downloadTrack], matching the real PWA's own
     * "Downloading N tracks..." behavior (there is no bulk backend endpoint).
     */
    fun downloadPlaylist() {
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return
        val playlist = _state.value.playlist

        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isDownloadingPlaylist = true) }
            for (track in tracks) {
                _state.update { it.copy(downloadingTrackIds = it.downloadingTrackIds + track.id) }
                val result = downloadRepository.downloadTrack(
                    track,
                    playlistId = playlist?.id,
                    playlistName = playlist?.name,
                )
                _state.update {
                    it.copy(
                        downloadingTrackIds = it.downloadingTrackIds - track.id,
                        downloadedTrackIds = if (result.isSuccess) it.downloadedTrackIds + track.id else it.downloadedTrackIds,
                    )
                }
            }
            _state.update { it.copy(isDownloadingPlaylist = false) }
        }
    }

    /** Opens the "Add to playlist" picker for [trackId] and loads the signed-in user's playlists. */
    fun showPlaylistPicker(trackId: Int) {
        _state.update {
            it.copy(
                isPlaylistPickerVisible = true,
                addToPlaylistTrackId = trackId,
            )
        }
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoadingPlaylists = true) }
            val userId = userDao.get()?.id
            if (userId == null) {
                _state.update { it.copy(isLoadingPlaylists = false, error = "Not signed in") }
                return@launch
            }
            when (val result = playlistApi.getUserPlaylists(userId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoadingPlaylists = false, userPlaylists = result.data) }
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isLoadingPlaylists = false, error = "Failed to load playlists") }
                }
            }
        }
    }

    fun hidePlaylistPicker() {
        _state.update { it.copy(isPlaylistPickerVisible = false) }
    }

    /** Adds the track picked in [showPlaylistPicker] to [targetPlaylistId]. */
    fun addTrackToPlaylist(targetPlaylistId: Int) {
        val trackId = _state.value.addToPlaylistTrackId ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(addToPlaylistLoading = true) }
            when (playlistApi.addTrackToPlaylist(targetPlaylistId, trackId)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(addToPlaylistLoading = false, isPlaylistPickerVisible = false, error = null)
                    }
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.update {
                        it.copy(addToPlaylistLoading = false, error = "Failed to add track to playlist")
                    }
                }
            }
        }
    }

    fun repostTrack(trackId: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(repostLoadingTrackIds = it.repostLoadingTrackIds + trackId) }
            when (repostApi.toggleTrackRepost(trackId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(repostLoadingTrackIds = it.repostLoadingTrackIds - trackId, error = null) }
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.update {
                        it.copy(
                            repostLoadingTrackIds = it.repostLoadingTrackIds - trackId,
                            error = "Failed to repost track",
                        )
                    }
                }
            }
        }
    }
}

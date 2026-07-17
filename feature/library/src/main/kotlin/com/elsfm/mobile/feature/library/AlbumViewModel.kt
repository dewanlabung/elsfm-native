package com.elsfm.mobile.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Comment
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AlbumApi
import com.elsfm.mobile.core.network.api.CommentApi
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.PlaylistInfo
import com.elsfm.mobile.core.network.api.RepostApi
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import com.elsfm.mobile.feature.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val ALBUM_ID_ARG = "albumId"
private const val COMMENTABLE_TYPE_ALBUM = "album"

/**
 * Immutable, hoisted UI state for [AlbumScreen]. Both [album] and [tracks]
 * come from the single real `GET /albums/{id}` call ([AlbumApi.getAlbum]) —
 * the backend has no separate tracks endpoint, it nests tracks in the album
 * response.
 *
 * There is no "is this album already liked/reposted" lookup endpoint (same
 * simplification already used for tracks), so [isAlbumLiked]/[isAlbumReposted]
 * start `false` each time the album loads and only reflect toggles made this
 * session.
 */
data class AlbumDetailState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val likedTrackIds: Set<Int> = emptySet(),
    val likeLoadingTrackIds: Set<Int> = emptySet(),
    val isAlbumLiked: Boolean = false,
    val isAlbumLikeLoading: Boolean = false,
    val isAlbumReposted: Boolean = false,
    val isAlbumRepostLoading: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentInput: String = "",
    val isPostingComment: Boolean = false,
    val downloadingTrackIds: Set<Int> = emptySet(),
    val downloadedTrackIds: Set<Int> = emptySet(),
    val isDownloadingAlbum: Boolean = false,
    val isPlaylistPickerVisible: Boolean = false,
    val isLoadingPlaylists: Boolean = false,
    val userPlaylists: List<PlaylistInfo> = emptyList(),
    val addToPlaylistTrackId: Int? = null,
    val addToPlaylistLoading: Boolean = false,
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumApi: AlbumApi,
    private val trackLikeController: TrackLikeController,
    private val userApi: UserApi,
    private val repostApi: RepostApi,
    private val commentApi: CommentApi,
    private val downloadRepository: DownloadRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val playerController: PlayerController,
    private val playlistApi: PlaylistApi,
    private val userDao: UserDao,
) : ViewModel() {
    private val albumId: Int = checkNotNull(savedStateHandle[ALBUM_ID_ARG]) {
        "AlbumViewModel requires an albumId argument"
    }

    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()

    init {
        loadAlbum()
        loadComments()
    }

    private fun loadAlbum() {
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = albumApi.getAlbum(albumId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        album = result.data,
                        tracks = result.data.tracks,
                        isLoading = false,
                    )
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load album",
                    )
                }
            }
        }
    }

    fun toggleTrackLike(trackId: Int) {
        val currentlyLiked = _state.value.likedTrackIds.contains(trackId)
        _state.value = _state.value.copy(
            likeLoadingTrackIds = _state.value.likeLoadingTrackIds + trackId,
        )

        viewModelScope.launch(dispatcherProvider.io) {
            val newLikedState = trackLikeController.toggleLike(trackId, currentlyLiked)
            _state.value = _state.value.copy(
                likedTrackIds = when (newLikedState) {
                    true -> _state.value.likedTrackIds + trackId
                    false -> _state.value.likedTrackIds - trackId
                    null -> _state.value.likedTrackIds
                },
                likeLoadingTrackIds = _state.value.likeLoadingTrackIds - trackId,
                error = if (newLikedState == null) "Failed to update library" else _state.value.error,
            )
        }
    }

    fun toggleAlbumLike() {
        val currentlyLiked = _state.value.isAlbumLiked
        _state.value = _state.value.copy(isAlbumLikeLoading = true)

        viewModelScope.launch(dispatcherProvider.io) {
            val result = if (currentlyLiked) {
                userApi.removeAlbumFromLibrary(albumId)
            } else {
                userApi.addAlbumToLibrary(albumId)
            }
            when (result) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isAlbumLiked = result.data, isAlbumLikeLoading = false)
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isAlbumLikeLoading = false,
                        error = "Failed to update library",
                    )
                }
            }
        }
    }

    fun toggleAlbumRepost() {
        _state.value = _state.value.copy(isAlbumRepostLoading = true)

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = repostApi.toggleAlbumRepost(albumId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isAlbumReposted = result.data.action == "added",
                        isAlbumRepostLoading = false,
                    )
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isAlbumRepostLoading = false,
                        error = "Failed to update repost",
                    )
                }
            }
        }
    }

    private fun loadComments() {
        _state.value = _state.value.copy(isLoadingComments = true)

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = commentApi.getComments(COMMENTABLE_TYPE_ALBUM, albumId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(comments = result.data, isLoadingComments = false)
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(isLoadingComments = false)
                }
            }
        }
    }

    fun onCommentInputChanged(input: String) {
        _state.value = _state.value.copy(commentInput = input)
    }

    fun postComment() {
        val content = _state.value.commentInput.trim()
        if (content.isEmpty()) return
        _state.value = _state.value.copy(isPostingComment = true)

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = commentApi.postComment(COMMENTABLE_TYPE_ALBUM, albumId, content)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        comments = listOf(result.data) + _state.value.comments,
                        commentInput = "",
                        isPostingComment = false,
                    )
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isPostingComment = false,
                        error = "Failed to post comment",
                    )
                }
            }
        }
    }

    fun addToQueue(trackId: Int) {
        val track = _state.value.tracks.find { it.id == trackId } ?: return
        playerController.addToQueue(track)
    }

    fun repostTrack(trackId: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            when (repostApi.toggleTrackRepost(trackId)) {
                is ApiResult.Success -> Unit
                else -> _state.value = _state.value.copy(error = "Failed to repost track")
            }
        }
    }

    fun showPlaylistPicker(trackId: Int) {
        _state.value = _state.value.copy(isPlaylistPickerVisible = true, addToPlaylistTrackId = trackId)
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isLoadingPlaylists = true)
            val userId = userDao.get()?.id
            if (userId == null) {
                _state.value = _state.value.copy(isLoadingPlaylists = false, error = "Not signed in")
                return@launch
            }
            when (val result = playlistApi.getUserPlaylists(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoadingPlaylists = false, userPlaylists = result.data)
                }
                else -> _state.value = _state.value.copy(isLoadingPlaylists = false, error = "Failed to load playlists")
            }
        }
    }

    fun hidePlaylistPicker() {
        _state.value = _state.value.copy(isPlaylistPickerVisible = false)
    }

    fun addTrackToPlaylist(playlistId: Int) {
        val trackId = _state.value.addToPlaylistTrackId ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(addToPlaylistLoading = true)
            when (playlistApi.addTrackToPlaylist(playlistId, trackId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        addToPlaylistLoading = false, isPlaylistPickerVisible = false, error = null,
                    )
                }
                else -> _state.value = _state.value.copy(addToPlaylistLoading = false, error = "Failed to add to playlist")
            }
        }
    }

    /**
     * Builds the shareable album URL client-side, mirroring the real web client's link
     * pattern (same approach as [com.elsfm.mobile.feature.artist.ArtistDetailViewModel]'s
     * `buildArtistShareUrl` — no backend "share" endpoint exists for albums either).
     */
    fun buildAlbumShareUrl(): String? {
        val album = _state.value.album ?: return null
        return "https://www.elsfm.com/album/${album.id}/${slugify(album.name)}"
    }

    private fun slugify(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
    }

    /** "Make available offline" for a single track. */
    fun downloadTrack(track: Track) {
        val album = _state.value.album
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(
                downloadingTrackIds = _state.value.downloadingTrackIds + track.id,
            )
            val result = downloadRepository.downloadTrack(track, albumId = album?.id, albumName = album?.name)
            _state.value = _state.value.copy(
                downloadingTrackIds = _state.value.downloadingTrackIds - track.id,
                downloadedTrackIds = if (result.isSuccess) {
                    _state.value.downloadedTrackIds + track.id
                } else {
                    _state.value.downloadedTrackIds
                },
                error = if (result.isFailure) "Failed to download track" else _state.value.error,
            )
        }
    }

    /**
     * "Make available offline" for the whole album - the real backend has no bulk
     * download endpoint, so this is the same per-track download looped client-side,
     * matching the real PWA's own "Downloading N tracks..." behavior.
     */
    fun downloadAlbum() {
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return
        val album = _state.value.album

        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isDownloadingAlbum = true)
            for (track in tracks) {
                _state.value = _state.value.copy(
                    downloadingTrackIds = _state.value.downloadingTrackIds + track.id,
                )
                val result = downloadRepository.downloadTrack(track, albumId = album?.id, albumName = album?.name)
                _state.value = _state.value.copy(
                    downloadingTrackIds = _state.value.downloadingTrackIds - track.id,
                    downloadedTrackIds = if (result.isSuccess) {
                        _state.value.downloadedTrackIds + track.id
                    } else {
                        _state.value.downloadedTrackIds
                    },
                )
            }
            _state.value = _state.value.copy(isDownloadingAlbum = false)
        }
    }
}

package com.elsfm.mobile.feature.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.repository.FollowStateRepository
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ArtistApi
import com.elsfm.mobile.core.network.api.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val artistApi: ArtistApi,
    private val followStateRepository: FollowStateRepository,
    private val userApi: UserApi,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val dispatcher = dispatcherProvider.io

    private val _state = MutableStateFlow(ArtistDetailState())
    val state: StateFlow<ArtistDetailState> = _state.asStateFlow()

    init {
        savedStateHandle.get<Int>("artistId")?.let { artistId ->
            loadArtistDetails(artistId)
        }
    }

    fun retryLoadArtistDetails() {
        savedStateHandle.get<Int>("artistId")?.let { artistId ->
            loadArtistDetails(artistId)
        }
    }

    private fun loadArtistDetails(id: Int) {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                coroutineScope {
                    val artistResult = artistApi.getArtist(id)
                    val tracksResult = artistApi.getArtistTracks(id)
                    val albumsResult = artistApi.getArtistAlbums(id)

                    when {
                        artistResult is ApiResult.Success && tracksResult is ApiResult.Success && albumsResult is ApiResult.Success -> {
                            _state.value = _state.value.copy(
                                artist = artistResult.data,
                                tracks = tracksResult.data,
                                albums = albumsResult.data,
                                isLoading = false
                            )
                        }
                        artistResult is ApiResult.Success && tracksResult is ApiResult.Success -> {
                            _state.value = _state.value.copy(
                                artist = artistResult.data,
                                tracks = tracksResult.data,
                                albums = emptyList(),
                                isLoading = false
                            )
                        }
                        artistResult is ApiResult.Success -> {
                            _state.value = _state.value.copy(
                                artist = artistResult.data,
                                tracks = emptyList(),
                                albums = emptyList(),
                                isLoading = false
                            )
                        }
                        else -> {
                            val errorMsg = when (artistResult) {
                                is ApiResult.NetworkError -> artistResult.cause.message ?: "Network error"
                                is ApiResult.ValidationError -> "Validation error"
                                is ApiResult.Unauthorized -> "Unauthorized"
                                else -> "Failed to load artist"
                            }
                            _state.value = _state.value.copy(error = errorMsg, isLoading = false)
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error",
                    isLoading = false
                )
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch(dispatcher) {
            val artistId = _state.value.artist?.id ?: return@launch
            _state.value = _state.value.copy(isFollowLoading = true)
            try {
                if (_state.value.followedByUser) {
                    followStateRepository.unfollow(artistId)
                } else {
                    followStateRepository.follow(artistId)
                }
                _state.value = _state.value.copy(
                    followedByUser = !_state.value.followedByUser,
                    isFollowLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFollowLoading = false,
                    error = e.message ?: "Failed to update follow state"
                )
            }
        }
    }

    /**
     * Switches the active profile tab. Followers are fetched lazily the first time that tab
     * is selected (`GET artists/{id}/followers`), matching the web client's per-tab data
     * loading instead of eagerly fetching data the user may never view.
     */
    fun selectTab(tab: ArtistTab) {
        _state.value = _state.value.copy(selectedTab = tab)
        if (tab == ArtistTab.FOLLOWERS && _state.value.followers.isEmpty() && !_state.value.isFollowersLoading) {
            loadFollowers()
        }
        // ALBUMS tab uses albums already loaded in loadArtistDetails — no lazy fetch needed.
    }

    private fun loadFollowers() {
        val artistId = _state.value.artist?.id ?: return
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isFollowersLoading = true, followersError = null)
            when (val result = artistApi.getArtistFollowers(artistId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        followers = result.data,
                        isFollowersLoading = false
                    )
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        isFollowersLoading = false,
                        followersError = result.cause.message ?: "Failed to load followers"
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(isFollowersLoading = false, followersError = "Validation error")
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(isFollowersLoading = false, followersError = "Unauthorized")
                }
            }
        }
    }

    /**
     * Follows/unfollows a *user* from the Followers tab's per-row Follow button. This is
     * user-to-user following (`UserApi.followUser`/`unfollowUser`, backed by the real
     * `POST users/{id}/follow` / `unfollow` routes) — distinct from [toggleFollow], which
     * follows the *artist* itself.
     */
    fun toggleFollowUser(userId: Int) {
        val wasFollowed = _state.value.followedUserIds.contains(userId)
        viewModelScope.launch(dispatcher) {
            val result = if (wasFollowed) userApi.unfollowUser(userId) else userApi.followUser(userId)
            when (result) {
                is ApiResult.Success -> {
                    val updatedIds = if (wasFollowed) {
                        _state.value.followedUserIds - userId
                    } else {
                        _state.value.followedUserIds + userId
                    }
                    _state.value = _state.value.copy(followedUserIds = updatedIds)
                }
                else -> {
                    _state.value = _state.value.copy(followersError = "Failed to update follow state")
                }
            }
        }
    }

    /**
     * Builds the shareable artist profile URL client-side, mirroring the real web client's
     * `getArtistLink()` (`resources/client/web-player/artists/artist-link.tsx`):
     * `{base_url}/artist/{id}/{slugified-name}`. No backend "share" endpoint exists for artists
     * (there is none in `routes/api.php`) so this is purely local, matching the PWA.
     */
    fun buildArtistShareUrl(): String? {
        val artist = _state.value.artist ?: return null
        return "https://www.elsfm.com/artist/${artist.id}/${slugify(artist.name)}"
    }

    private fun slugify(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
    }
}

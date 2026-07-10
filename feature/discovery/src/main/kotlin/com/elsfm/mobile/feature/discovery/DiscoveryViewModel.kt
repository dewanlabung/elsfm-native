package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ChannelContentResult
import com.elsfm.mobile.core.network.api.ProfileApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Real Channel 5 ("Nepali Christian Songs") sub-channel names, confirmed via
// a screen recording of the elsfm.com PWA and by calling the live backend
// directly (`GET api/v1/channel/5?loader=channelPage`). Matched by
// case-insensitive `contains` rather than a hardcoded channel id, since ids
// are backend data and the id->section mapping isn't guaranteed stable.
private const val CHANNEL_NAME_KEYWORD_KIDS_ZONE = "Kids Zone"
private const val CHANNEL_NAME_KEYWORD_EXPLORE_MORE = "Explore More"
private const val CHANNEL_NAME_KEYWORD_NEW_RELEASE = "New Release"
private const val CHANNEL_NAME_KEYWORD_MOSTLY_PLAYED = "Mostly Played"

// Fallback titles used only if a matching sub-channel can't be found (e.g. a
// section failed to load); real section headers always prefer the backend's
// verbatim channel.name once loaded.
private const val DEFAULT_KIDS_ZONE_TITLE = "Kids Zone"
private const val DEFAULT_EXPLORE_MORE_TITLE = "Explore More Channel"
private const val DEFAULT_NEW_RELEASES_TITLE = "New Releases"
private const val DEFAULT_MOSTLY_PLAYED_TITLE = "Mostly Played Songs"

/**
 * Immutable, hoisted UI state for [DiscoveryScreen].
 *
 * Every section is backed by real backend data. [kidsZone], [exploreMoreChannel],
 * [newReleases] and [mostlyPlayedSongs] all come from Channel 5's real nested
 * sub-channels (fetched via [ChannelApi.getChannels] and then
 * [ChannelApi.getChannelContent] for each sub-channel that matches the real
 * PWA's section names), while [recentlyPlayed] is a separate personalized
 * feature backed by [ProfileApi.getRecentlyPlayed].
 *
 * Section titles ([kidsZoneTitle], [exploreMoreChannelTitle], etc.) are the
 * verbatim `name` returned by the backend for that sub-channel, not
 * hardcoded strings, so the UI never drifts from what the PWA actually calls
 * these sections.
 */
data class DiscoveryUiState(
    val kidsZone: List<Playlist> = emptyList(),
    val kidsZoneTitle: String = DEFAULT_KIDS_ZONE_TITLE,
    val exploreMoreChannel: List<Playlist> = emptyList(),
    val exploreMoreChannelTitle: String = DEFAULT_EXPLORE_MORE_TITLE,
    val exploreMoreChannelId: Int? = null,
    val newReleases: List<Album> = emptyList(),
    val newReleasesTitle: String = DEFAULT_NEW_RELEASES_TITLE,
    val mostlyPlayedSongs: List<Track> = emptyList(),
    val mostlyPlayedSongsTitle: String = DEFAULT_MOSTLY_PLAYED_TITLE,
    val recentlyPlayed: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val channelApi: ChannelApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoveryUiState())
    val state: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            var loadError: String? = null

            coroutineScope {
                val recentlyPlayedDeferred = async { loadRecentlyPlayed() }
                val homeSectionsDeferred = async { loadHomeSections() }

                val recentlyPlayed = recentlyPlayedDeferred.await()
                val homeSections = homeSectionsDeferred.await()

                if (recentlyPlayed == null) {
                    loadError = "Failed to load recently played"
                }
                if (homeSections.isEmpty()) {
                    loadError = loadError ?: "Failed to load home sections"
                }

                _state.value = _state.value.copy(
                    kidsZone = homeSections.kidsZone.orEmpty(),
                    kidsZoneTitle = homeSections.kidsZoneTitle ?: DEFAULT_KIDS_ZONE_TITLE,
                    exploreMoreChannel = homeSections.exploreMoreChannel.orEmpty(),
                    exploreMoreChannelTitle = homeSections.exploreMoreChannelTitle ?: DEFAULT_EXPLORE_MORE_TITLE,
                    exploreMoreChannelId = homeSections.exploreMoreChannelId,
                    newReleases = homeSections.newReleases.orEmpty(),
                    newReleasesTitle = homeSections.newReleasesTitle ?: DEFAULT_NEW_RELEASES_TITLE,
                    mostlyPlayedSongs = homeSections.mostlyPlayedSongs.orEmpty(),
                    mostlyPlayedSongsTitle = homeSections.mostlyPlayedSongsTitle ?: DEFAULT_MOSTLY_PLAYED_TITLE,
                    recentlyPlayed = recentlyPlayed.orEmpty(),
                )
            }

            _state.value = _state.value.copy(isLoading = false, error = loadError)
        }
    }

    private suspend fun loadRecentlyPlayed(): List<Track>? {
        return when (val result = profileApi.getRecentlyPlayed()) {
            is ApiResult.Success -> result.data
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> null
        }
    }

    /**
     * Fetches Channel 5's real nested sub-channels, matches each one to a
     * known Discovery section by name, then loads each matched sub-channel's
     * own content in parallel. A section a match wasn't found for (or whose
     * content fetch failed) is simply left empty rather than failing the
     * whole load.
     */
    private suspend fun loadHomeSections(): HomeSections {
        val subChannels = when (val result = channelApi.getChannels()) {
            is ApiResult.Success -> result.data
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> return HomeSections()
        }

        val kidsZoneChannel = subChannels.findByNameContaining(CHANNEL_NAME_KEYWORD_KIDS_ZONE)
        val exploreMoreChannel = subChannels.findByNameContaining(CHANNEL_NAME_KEYWORD_EXPLORE_MORE)
        val newReleasesChannel = subChannels.findByNameContaining(CHANNEL_NAME_KEYWORD_NEW_RELEASE)
        val mostlyPlayedChannel = subChannels.findByNameContaining(CHANNEL_NAME_KEYWORD_MOSTLY_PLAYED)

        return coroutineScope {
            val kidsZoneDeferred = kidsZoneChannel?.let { async { channelApi.getChannelContent(it.id) } }
            val exploreMoreDeferred = exploreMoreChannel?.let { async { channelApi.getChannelContent(it.id) } }
            val newReleasesDeferred = newReleasesChannel?.let { async { channelApi.getChannelContent(it.id) } }
            val mostlyPlayedDeferred = mostlyPlayedChannel?.let { async { channelApi.getChannelContent(it.id) } }

            listOfNotNull(
                kidsZoneDeferred,
                exploreMoreDeferred,
                newReleasesDeferred,
                mostlyPlayedDeferred,
            ).awaitAll()

            HomeSections(
                kidsZone = kidsZoneDeferred?.await()?.asPlaylists(),
                kidsZoneTitle = kidsZoneChannel?.name,
                exploreMoreChannel = exploreMoreDeferred?.await()?.asPlaylists(),
                exploreMoreChannelTitle = exploreMoreChannel?.name,
                exploreMoreChannelId = exploreMoreChannel?.id,
                newReleases = newReleasesDeferred?.await()?.asAlbums(),
                newReleasesTitle = newReleasesChannel?.name,
                mostlyPlayedSongs = mostlyPlayedDeferred?.await()?.asTracks(),
                mostlyPlayedSongsTitle = mostlyPlayedChannel?.name,
            )
        }
    }

    private fun List<Channel>.findByNameContaining(keyword: String): Channel? {
        return firstOrNull { it.name.contains(keyword, ignoreCase = true) }
    }

    private fun ApiResult<ChannelContentResult>.asPlaylists(): List<Playlist>? {
        return ((this as? ApiResult.Success)?.data as? ChannelContentResult.Playlists)?.items
    }

    private fun ApiResult<ChannelContentResult>.asAlbums(): List<Album>? {
        return ((this as? ApiResult.Success)?.data as? ChannelContentResult.Albums)?.items
    }

    private fun ApiResult<ChannelContentResult>.asTracks(): List<Track>? {
        return ((this as? ApiResult.Success)?.data as? ChannelContentResult.Tracks)?.items
    }

    private data class HomeSections(
        val kidsZone: List<Playlist>? = null,
        val kidsZoneTitle: String? = null,
        val exploreMoreChannel: List<Playlist>? = null,
        val exploreMoreChannelTitle: String? = null,
        val exploreMoreChannelId: Int? = null,
        val newReleases: List<Album>? = null,
        val newReleasesTitle: String? = null,
        val mostlyPlayedSongs: List<Track>? = null,
        val mostlyPlayedSongsTitle: String? = null,
    ) {
        fun isEmpty(): Boolean {
            return kidsZone == null && exploreMoreChannel == null && newReleases == null && mostlyPlayedSongs == null
        }
    }
}

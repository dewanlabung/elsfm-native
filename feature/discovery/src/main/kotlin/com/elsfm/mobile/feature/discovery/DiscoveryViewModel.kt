package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.dao.DiscoveryCacheDao
import com.elsfm.mobile.core.database.entity.DiscoveryCache
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.DiscoverySections
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ChannelContentResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.connectivity.NetworkMonitor
import com.elsfm.mobile.core.network.elsfmJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
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
    val isOffline: Boolean = false,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val channelApi: ChannelApi,
    private val dispatcherProvider: DispatcherProvider,
    private val discoveryCacheDao: DiscoveryCacheDao,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoveryUiState())
    val state: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    init {
        // Paint any cached sections instantly (no spinner), then always refresh
        // from the network afterwards - performLoad() only shows the spinner
        // when there is nothing cached to show yet (e.g. a fresh install).
        viewModelScope.launch(dispatcherProvider.io) {
            loadCachedSectionsIfAvailable()
            performLoad()
        }

        // A user who loses connectivity mid-session and reconnects gets a
        // silent refresh without leaving the screen. `drop(1)` skips the
        // initial emission so this only reacts to actual transitions.
        viewModelScope.launch(dispatcherProvider.io) {
            networkMonitor.isOnline
                .drop(1)
                .distinctUntilChanged()
                .filter { it }
                .collect { performLoad() }
        }
    }

    fun loadHome() {
        viewModelScope.launch(dispatcherProvider.io) { performLoad() }
    }

    private suspend fun loadCachedSectionsIfAvailable() {
        val cached = discoveryCacheDao.get() ?: return
        val sections = runCatching {
            elsfmJson().decodeFromString(DiscoverySections.serializer(), cached.payloadJson)
        }.getOrNull() ?: return

        _state.value = _state.value.copy(
            kidsZone = sections.kidsZone,
            kidsZoneTitle = sections.kidsZoneTitle ?: DEFAULT_KIDS_ZONE_TITLE,
            exploreMoreChannel = sections.exploreMoreChannel,
            exploreMoreChannelTitle = sections.exploreMoreChannelTitle ?: DEFAULT_EXPLORE_MORE_TITLE,
            exploreMoreChannelId = sections.exploreMoreChannelId,
            newReleases = sections.newReleases,
            newReleasesTitle = sections.newReleasesTitle ?: DEFAULT_NEW_RELEASES_TITLE,
            mostlyPlayedSongs = sections.mostlyPlayedSongs,
            mostlyPlayedSongsTitle = sections.mostlyPlayedSongsTitle ?: DEFAULT_MOSTLY_PLAYED_TITLE,
            isLoading = false,
        )
    }

    /**
     * Loads recently-played + home sections and merges them into state. Only
     * shows the full-screen spinner ([DiscoveryUiState.isLoading]) when there
     * is no content on screen yet - once cache-or-fresh content has painted,
     * subsequent calls (background refresh, connectivity restore) update
     * silently and simply leave stale content up if the refresh itself fails.
     */
    private suspend fun performLoad() {
        val hadContentAlready = _state.value.hasAnySection()
        _state.value = _state.value.copy(isLoading = !hadContentAlready, error = null, isOffline = false)

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

            val previous = _state.value
            _state.value = previous.copy(
                // Fall back to whatever is already on screen (cache or a prior
                // successful load) rather than clearing sections a failed
                // refresh didn't actually touch.
                kidsZone = homeSections.kidsZone ?: previous.kidsZone,
                kidsZoneTitle = homeSections.kidsZoneTitle ?: previous.kidsZoneTitle,
                exploreMoreChannel = homeSections.exploreMoreChannel ?: previous.exploreMoreChannel,
                exploreMoreChannelTitle = homeSections.exploreMoreChannelTitle ?: previous.exploreMoreChannelTitle,
                exploreMoreChannelId = homeSections.exploreMoreChannelId ?: previous.exploreMoreChannelId,
                newReleases = homeSections.newReleases ?: previous.newReleases,
                newReleasesTitle = homeSections.newReleasesTitle ?: previous.newReleasesTitle,
                mostlyPlayedSongs = homeSections.mostlyPlayedSongs ?: previous.mostlyPlayedSongs,
                mostlyPlayedSongsTitle = homeSections.mostlyPlayedSongsTitle ?: previous.mostlyPlayedSongsTitle,
                recentlyPlayed = recentlyPlayed.orEmpty(),
            )

            if (!homeSections.isEmpty()) {
                cacheSections(_state.value)
            }
        }

        val hasContent = _state.value.hasAnySection()
        _state.value = _state.value.copy(
            isLoading = false,
            error = if (loadError != null && hasContent) null else loadError,
            isOffline = loadError != null && hasContent,
        )
    }

    private fun DiscoveryUiState.hasAnySection(): Boolean {
        return kidsZone.isNotEmpty() ||
            exploreMoreChannel.isNotEmpty() ||
            newReleases.isNotEmpty() ||
            mostlyPlayedSongs.isNotEmpty()
    }

    private suspend fun cacheSections(state: DiscoveryUiState) {
        val sections = DiscoverySections(
            kidsZone = state.kidsZone,
            kidsZoneTitle = state.kidsZoneTitle,
            exploreMoreChannel = state.exploreMoreChannel,
            exploreMoreChannelTitle = state.exploreMoreChannelTitle,
            exploreMoreChannelId = state.exploreMoreChannelId,
            newReleases = state.newReleases,
            newReleasesTitle = state.newReleasesTitle,
            mostlyPlayedSongs = state.mostlyPlayedSongs,
            mostlyPlayedSongsTitle = state.mostlyPlayedSongsTitle,
        )
        val payloadJson = elsfmJson().encodeToString(DiscoverySections.serializer(), sections)
        discoveryCacheDao.save(DiscoveryCache(payloadJson = payloadJson))
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

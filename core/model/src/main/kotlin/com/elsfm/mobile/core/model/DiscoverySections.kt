package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

/**
 * Cacheable snapshot of Discovery's non-personalized channel sections
 * (see [DiscoveryViewModel][com.elsfm.mobile.feature.discovery.DiscoveryViewModel]).
 * Deliberately excludes `recentlyPlayed` - that data is personalized per
 * account and is always fetched live rather than cached, to avoid leaking
 * one user's recently-played tracks into another account's cache on a
 * shared device.
 */
@Serializable
data class DiscoverySections(
    val kidsZone: List<Playlist> = emptyList(),
    val kidsZoneTitle: String? = null,
    val exploreMoreChannel: List<Playlist> = emptyList(),
    val exploreMoreChannelTitle: String? = null,
    val exploreMoreChannelId: Int? = null,
    val newReleases: List<Album> = emptyList(),
    val newReleasesTitle: String? = null,
    val mostlyPlayedSongs: List<Track> = emptyList(),
    val mostlyPlayedSongsTitle: String? = null,
)

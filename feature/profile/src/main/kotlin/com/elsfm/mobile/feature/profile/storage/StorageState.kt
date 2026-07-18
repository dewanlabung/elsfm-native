package com.elsfm.mobile.feature.profile.storage

data class CacheEntry(
    val label: String,
    val sizeBytes: Long,
)

data class StorageState(
    val discoveryCacheBytes: Long = 0L,
    val libraryCacheBytes: Long = 0L,
    val profileCacheBytes: Long = 0L,
    val downloadedTracksBytes: Long = 0L,
    val isExpanded: Boolean = false,
    val isClearing: Boolean = false,
) {
    val entries: List<CacheEntry>
        get() = listOf(
            CacheEntry("Discovery feed", discoveryCacheBytes),
            CacheEntry("Library snapshot", libraryCacheBytes),
            CacheEntry("Profile data", profileCacheBytes),
        )

    val totalCacheBytes: Long
        get() = discoveryCacheBytes + libraryCacheBytes + profileCacheBytes
}

package com.elsfm.mobile.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

private const val SINGLETON_ID = 0

/**
 * Single-row cache of Discovery's non-personalized channel sections, stored
 * as an opaque JSON blob (the caller encodes/decodes it - this module never
 * needs the kotlinx-serialization plugin just to store a [String]). Keyed by
 * a fixed [SINGLETON_ID] since there is only ever one cached snapshot.
 */
@Entity(tableName = "discovery_cache")
data class DiscoveryCache(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val payloadJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

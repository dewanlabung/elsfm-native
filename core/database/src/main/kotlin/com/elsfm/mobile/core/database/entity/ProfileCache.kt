package com.elsfm.mobile.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

private const val SINGLETON_ID = 0

/**
 * Single-row cache of the signed-in user's profile (UserProfile JSON blob).
 * Enables ProfileViewModel to paint the profile screen instantly from cache
 * before the network refresh arrives.
 */
@Entity(tableName = "profile_cache")
data class ProfileCache(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val payloadJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

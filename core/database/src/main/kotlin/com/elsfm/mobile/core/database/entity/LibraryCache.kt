package com.elsfm.mobile.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

private const val SINGLETON_ID = 0

@Entity(tableName = "library_cache")
data class LibraryCache(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val payloadJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

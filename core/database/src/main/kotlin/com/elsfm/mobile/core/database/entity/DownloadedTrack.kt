package com.elsfm.mobile.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey val trackId: Int,
    val title: String,
    val artist: String,
    val fileName: String,
    val fileSizeBytes: Long,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis(),
    val artworkUrl: String? = null,
    val albumId: Int? = null,
    val albumName: String? = null,
    val playlistId: Int? = null,
    val playlistName: String? = null,
)

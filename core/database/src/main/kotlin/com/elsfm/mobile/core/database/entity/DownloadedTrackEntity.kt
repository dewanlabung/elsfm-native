package com.elsfm.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey
    val trackId: Int,
    val title: String,
    val artist: String,
    val albumId: Int?,
    val playlistId: Int?,
    val artworkUrl: String?,
    val filePath: String,
    val downloadedAt: Long,
    val fileSize: Long,
    val status: String, // COMPLETED, DOWNLOADING, FAILED, PAUSED
    val progress: Int = 0 // 0-100
)

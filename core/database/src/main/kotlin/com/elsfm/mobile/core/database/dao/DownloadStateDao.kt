package com.elsfm.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elsfm.mobile.core.database.entity.DownloadedTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedTrack(track: DownloadedTrackEntity)

    @Query("SELECT * FROM downloaded_tracks WHERE status = 'COMPLETED'")
    fun getCompletedDownloads(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun getDownloadedTrack(trackId: Int): DownloadedTrackEntity?

    @Delete
    suspend fun deleteDownloadedTrack(track: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: Int)

    @Query("UPDATE downloaded_tracks SET progress = :progress WHERE trackId = :trackId")
    suspend fun updateDownloadProgress(trackId: Int, progress: Int)

    @Query("UPDATE downloaded_tracks SET status = :status WHERE trackId = :trackId")
    suspend fun updateDownloadStatus(trackId: Int, status: String)
}

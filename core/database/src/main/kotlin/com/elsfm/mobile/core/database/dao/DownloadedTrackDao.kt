package com.elsfm.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: DownloadedTrack)

    @Query("SELECT * FROM downloaded_tracks ORDER BY downloaded_at DESC")
    fun getAll(): Flow<List<DownloadedTrack>>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun getById(trackId: Int): DownloadedTrack?

    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun delete(trackId: Int)

    @Query("SELECT SUM(fileSizeBytes) FROM downloaded_tracks")
    fun getTotalSizeBytes(): Flow<Long?>
}

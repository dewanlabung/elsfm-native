package com.elsfm.mobile.core.database.repository

import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadRepository @Inject constructor(
    private val downloadedTrackDao: DownloadedTrackDao
) {
    fun getCompletedDownloads(): Flow<List<DownloadedTrack>> {
        return downloadedTrackDao.getAll()
    }

    suspend fun deleteDownloadedTrack(trackId: Int) {
        downloadedTrackDao.delete(trackId)
    }

    fun getTotalDownloadSize(): Flow<Long?> {
        return downloadedTrackDao.getTotalSizeBytes()
    }
}

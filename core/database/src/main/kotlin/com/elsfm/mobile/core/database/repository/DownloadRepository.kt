package com.elsfm.mobile.core.database.repository

import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.download.DownloadManager
import java.io.File
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Single entry point for "make available offline", callable from any feature (player,
 * album, playlist, liked songs) without each depending on `feature:downloads` directly.
 * Mirrors the real PWA: no bulk backend endpoint exists, so an album/playlist download
 * is just this same per-track call looped by the caller.
 */
open class DownloadRepository @Inject constructor(
    private val downloadedTrackDao: DownloadedTrackDao,
    private val downloadManager: DownloadManager,
) {
    fun getCompletedDownloads(): Flow<List<DownloadedTrack>> {
        return downloadedTrackDao.getAll()
    }

    open suspend fun isDownloaded(trackId: Int): Boolean {
        return downloadedTrackDao.getById(trackId) != null
    }

    open suspend fun downloadTrack(
        track: Track,
        albumId: Int? = null,
        albumName: String? = null,
        playlistId: Int? = null,
        playlistName: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit> {
        return downloadManager.downloadTrack(track, onProgress).map { file ->
            downloadedTrackDao.insert(
                DownloadedTrack(
                    trackId = track.id,
                    title = track.name,
                    artist = track.artists.firstOrNull()?.name ?: "Unknown",
                    fileName = file.name,
                    fileSizeBytes = file.length(),
                    artworkUrl = track.image,
                    albumId = albumId,
                    albumName = albumName,
                    playlistId = playlistId,
                    playlistName = playlistName,
                ),
            )
        }
    }

    suspend fun deleteDownloadedTrack(trackId: Int) {
        downloadManager.deleteDownload(trackId)
        downloadedTrackDao.delete(trackId)
    }

    fun getTotalDownloadSize(): Flow<Long?> {
        return downloadedTrackDao.getTotalSizeBytes()
    }

    /** The local on-disk file for a completed download, for offline playback. */
    open fun getLocalFile(fileName: String): File? {
        return downloadManager.getFile(fileName)
    }
}

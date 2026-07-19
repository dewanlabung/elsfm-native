package com.elsfm.mobile.core.database.repository

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.download.DownloadWorker
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.media.SessionPreferences
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.download.DownloadManager
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val BACKOFF_DELAY_SECONDS = 30L

/**
 * Single entry point for "make available offline", callable from any feature (player,
 * album, playlist, liked songs) without each depending on `feature:downloads` directly.
 * Mirrors the real PWA: no bulk backend endpoint exists, so an album/playlist download
 * is just this same per-track call looped by the caller.
 */
open class DownloadRepository @Inject constructor(
    private val downloadedTrackDao: DownloadedTrackDao,
    private val downloadManager: DownloadManager,
    private val workManager: WorkManager,
    private val sessionPreferences: SessionPreferences,
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

    /**
     * Enqueues a single-track download as a WorkManager job so it survives process death and
     * shows a foreground notification, instead of running on the caller's `viewModelScope`.
     * [ExistingWorkPolicy.KEEP] avoids duplicate work if the same track is already queued.
     */
    open fun enqueueDownload(
        track: Track,
        albumId: Int? = null,
        albumName: String? = null,
        playlistId: Int? = null,
        playlistName: String? = null,
    ) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_TRACK_ID to track.id,
                    DownloadWorker.KEY_TRACK_NAME to track.name,
                    DownloadWorker.KEY_ARTIST to (track.artists.firstOrNull()?.name ?: "Unknown"),
                    DownloadWorker.KEY_ARTWORK_URL to track.image,
                    DownloadWorker.KEY_ALBUM_ID to (albumId ?: -1),
                    DownloadWorker.KEY_ALBUM_NAME to albumName,
                    DownloadWorker.KEY_PLAYLIST_ID to (playlistId ?: -1),
                    DownloadWorker.KEY_PLAYLIST_NAME to playlistName,
                ),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS,
            )
            .addTag(DownloadWorker.TAG_DOWNLOAD)
            .addTag(DownloadWorker.uniqueName(track.id))
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorker.uniqueName(track.id),
            ExistingWorkPolicy.KEEP,
            workRequest,
        )
    }

    /** Enqueues every track of an album as individual, independently-retryable download jobs. */
    open fun enqueueAlbumDownload(
        albumId: Int,
        albumName: String,
        tracks: List<Track>,
    ) {
        tracks.forEach { track ->
            enqueueDownload(track, albumId = albumId, albumName = albumName)
        }
    }

    open fun cancelDownload(trackId: Int) {
        workManager.cancelUniqueWork(DownloadWorker.uniqueName(trackId))
    }

    /**
     * Emits current download progress (0f-1f) keyed by track id, for in-flight downloads only.
     * WorkManager keeps [WorkInfo] entries around (tagged, with their last-known progress) even
     * after a worker finishes - without filtering to non-terminal states, a SUCCEEDED or FAILED
     * download would stay stuck in this map at its last progress value forever, since nothing
     * ever removes it.
     */
    open fun observeDownloadProgress(): Flow<Map<Int, Float>> {
        return workManager.getWorkInfosByTagFlow(DownloadWorker.TAG_DOWNLOAD)
            .map { workInfoList: List<WorkInfo> ->
                workInfoList
                    .filter { !it.state.isFinished }
                    .mapNotNull { workInfo: WorkInfo ->
                        val trackId = workInfo.tags.firstNotNullOfOrNull(DownloadWorker::trackIdFromTag)
                        val progress = workInfo.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f)
                        trackId?.let { it to progress }
                    }.toMap()
            }
    }

    /** Emits the display name of each in-flight download, keyed by track id, for the Downloads UI. */
    open fun observeDownloadTrackNames(): Flow<Map<Int, String>> {
        return workManager.getWorkInfosByTagFlow(DownloadWorker.TAG_DOWNLOAD)
            .map { workInfoList: List<WorkInfo> ->
                workInfoList
                    .filter { !it.state.isFinished }
                    .mapNotNull { workInfo: WorkInfo ->
                        val trackId = workInfo.tags.firstNotNullOfOrNull(DownloadWorker::trackIdFromTag)
                        val trackName = workInfo.progress.getString(DownloadWorker.KEY_TRACK_NAME)
                        trackId?.let { id -> trackName?.let { id to it } }
                    }.toMap()
            }
    }
}

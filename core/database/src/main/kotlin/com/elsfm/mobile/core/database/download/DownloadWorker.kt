package com.elsfm.mobile.core.database.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.media.DownloadNotifications
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.download.DownloadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Lives in `core:database` (not `core:network`, as originally sketched) because it needs
 * [DownloadedTrackDao] and [DownloadNotifications]: `core:network` cannot depend on either
 * without creating a module cycle (`core:database` and `core:media` both already depend on
 * `core:network`). `core:database` already depends on `core:network`, so this is the only
 * module the worker can live in without restructuring the dependency graph.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadManager: DownloadManager,
    private val downloadedTrackDao: DownloadedTrackDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val trackId = inputData.getInt(KEY_TRACK_ID, -1)
        if (trackId < 0) return Result.failure()

        val trackName = inputData.getString(KEY_TRACK_NAME) ?: return Result.failure()
        val artist = inputData.getString(KEY_ARTIST) ?: "Unknown"
        val albumId = inputData.getInt(KEY_ALBUM_ID, -1).takeIf { it != -1 }
        val albumName = inputData.getString(KEY_ALBUM_NAME)
        val playlistId = inputData.getInt(KEY_PLAYLIST_ID, -1).takeIf { it != -1 }
        val playlistName = inputData.getString(KEY_PLAYLIST_NAME)
        val artworkUrl = inputData.getString(KEY_ARTWORK_URL)

        DownloadNotifications.createChannel(applicationContext)
        setForeground(DownloadNotifications.foregroundInfo(applicationContext, trackName))
        setProgressAsync(workDataOf(KEY_PROGRESS to 0f, KEY_TRACK_NAME to trackName))

        val track = Track(
            id = trackId,
            name = trackName,
            image = artworkUrl,
            durationMs = 0L,
            src = null,
            artists = emptyList(),
        )

        return downloadManager.downloadTrack(track) { progress ->
            DownloadNotifications.updateProgress(applicationContext, trackName, progress)
            setProgressAsync(workDataOf(KEY_PROGRESS to progress, KEY_TRACK_NAME to trackName))
        }.fold(
            onSuccess = { file ->
                downloadedTrackDao.insert(
                    DownloadedTrack(
                        trackId = trackId,
                        title = trackName,
                        artist = artist,
                        fileName = file.name,
                        fileSizeBytes = file.length(),
                        artworkUrl = artworkUrl,
                        albumId = albumId,
                        albumName = albumName,
                        playlistId = playlistId,
                        playlistName = playlistName,
                    ),
                )
                Result.success()
            },
            onFailure = {
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            },
        )
    }

    companion object {
        const val KEY_TRACK_ID = "track_id"
        const val KEY_TRACK_NAME = "track_name"
        const val KEY_ARTIST = "artist"
        const val KEY_ARTWORK_URL = "artwork_url"
        const val KEY_ALBUM_ID = "album_id"
        const val KEY_ALBUM_NAME = "album_name"
        const val KEY_PLAYLIST_ID = "playlist_id"
        const val KEY_PLAYLIST_NAME = "playlist_name"
        const val KEY_PROGRESS = "progress"
        private const val MAX_RETRIES = 2

        fun uniqueName(trackId: Int) = "download_track_$trackId"
        const val TAG_DOWNLOAD = "download"
        fun albumTag(albumId: Int) = "download_album_$albumId"
        fun playlistTag(playlistId: Int) = "download_playlist_$playlistId"

        /**
         * [WorkInfo] exposes only tags, progress, and output data - not the original
         * inputData - so per-track progress observation needs the track id recoverable
         * from a tag rather than [WorkerParameters.getInputData]. Reuses [uniqueName]'s
         * format as a tag applied to every request, and parses it back out here.
         */
        fun trackIdFromTag(tag: String): Int? = tag.removePrefix("download_track_").toIntOrNull()
    }
}

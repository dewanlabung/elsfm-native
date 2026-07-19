package com.elsfm.mobile.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.media.SessionPreferences
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.core.model.Track
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs periodically on WiFi to download recently played and liked tracks for offline use.
 * Skips silently when auto-cache is disabled in Settings, or when no user is logged in.
 */
@HiltWorker
class AutoCacheWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionPreferences: SessionPreferences,
    private val downloadRepository: DownloadRepository,
    private val profileApi: ProfileApi,
    private val userApi: UserApi,
    private val userDao: UserDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!sessionPreferences.isWifiAutoCacheEnabled) return Result.success()

        val userId = userDao.get()?.id ?: return Result.success()

        val tracksToCache = mutableSetOf<Track>()

        when (val result = profileApi.getRecentlyPlayed()) {
            is ApiResult.Success -> tracksToCache.addAll(result.data)
            else -> {}
        }

        when (val result = userApi.getLikedTracks(userId)) {
            is ApiResult.Success -> tracksToCache.addAll(result.data.take(MAX_LIKED_TRACKS))
            else -> {}
        }

        for (track in tracksToCache) {
            if (isStopped) break
            if (!downloadRepository.isDownloaded(track.id)) {
                downloadRepository.downloadTrack(track)
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "auto_cache_wifi"
        private const val MAX_LIKED_TRACKS = 20
    }
}

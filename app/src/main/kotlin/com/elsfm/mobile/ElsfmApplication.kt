package com.elsfm.mobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.elsfm.mobile.work.AutoCacheWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val MEMORY_CACHE_PERCENT = 0.25
private const val DISK_CACHE_BYTES = 512L * 1024 * 1024
private const val AUTO_CACHE_INTERVAL_HOURS = 6L

/**
 * Explicit Coil cache sizing (memory + a larger-than-default disk cache) so artwork
 * already seen once - Discovery thumbnails, artist/album art, track art - loads instantly
 * from cache on repeat visits instead of being re-fetched, which is what made switching
 * back to Home after browsing an Artist/Album page feel slow.
 *
 * Implements Configuration.Provider so HiltWorkerFactory is wired before WorkManager
 * initializes — required for @HiltWorker injection to work.
 */
@HiltAndroidApp
class ElsfmApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleAutoCache()
    }

    private fun scheduleAutoCache() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoCacheWorker>(AUTO_CACHE_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AutoCacheWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

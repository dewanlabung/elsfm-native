package com.elsfm.mobile.core.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo

object DownloadNotifications {
    private const val CHANNEL_ID = "downloads"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            )
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun foregroundInfo(context: Context, trackName: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Downloading")
            .setContentText(trackName)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .build()
        // targetSdk 35 rejects startForeground() with no declared type - must match the
        // foregroundServiceType="dataSync" override added for SystemForegroundService in
        // AndroidManifest.xml.
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    fun updateProgress(context: Context, trackName: String, progress: Float) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Downloading")
                .setContentText(trackName)
                .setProgress(100, (progress * 100).toInt(), false)
                .setOngoing(true)
                .build(),
        )
    }
}

package com.elsfm.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

/**
 * Ensures pending WorkManager download jobs survive a device reboot.
 * WorkManager reschedules periodic work automatically when RECEIVE_BOOT_COMPLETED
 * is declared, but one-time download tasks need an explicit re-enqueue trigger.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Touching the WorkManager instance is enough — it re-queues any surviving
        // enqueued work that wasn't marked finished before the reboot.
        WorkManager.getInstance(context)
    }
}

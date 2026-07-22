package com.elsfm.mobile.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.elsfm.mobile.core.media.PlaybackService

// ── 3×3 Artwork widget ─────────────────────────────────────────────────────

class ElsfmArtworkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ElsfmWidget()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PlaybackService.ACTION_WIDGET_UPDATE) {
            saveAndUpdate(context, intent, ElsfmArtworkWidgetReceiver::class.java)
        }
        super.onReceive(context, intent)
    }
}

// ── 4×2 Compact player ─────────────────────────────────────────────────────

class ElsfmCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ElsfmWidget()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PlaybackService.ACTION_WIDGET_UPDATE) {
            saveAndUpdate(context, intent, ElsfmCompactWidgetReceiver::class.java)
        }
        super.onReceive(context, intent)
    }
}

// ── 4×1 Wide player ────────────────────────────────────────────────────────

class ElsfmWideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ElsfmWidget()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PlaybackService.ACTION_WIDGET_UPDATE) {
            saveAndUpdate(context, intent, ElsfmWideWidgetReceiver::class.java)
        }
        super.onReceive(context, intent)
    }
}

// ── Shared helpers ──────────────────────────────────────────────────────────

private fun GlanceAppWidgetReceiver.saveAndUpdate(
    context: Context,
    intent: Intent,
    receiverClass: Class<*>,
) {
    WidgetPreferences.save(
        context = context,
        title = intent.getStringExtra(PlaybackService.EXTRA_TITLE) ?: "",
        artist = intent.getStringExtra(PlaybackService.EXTRA_ARTIST) ?: "",
        artworkUrl = intent.getStringExtra(PlaybackService.EXTRA_ARTWORK_URL),
        isPlaying = intent.getBooleanExtra(PlaybackService.EXTRA_IS_PLAYING, false),
    )
    // Trigger immediate Glance re-render for all instances of this receiver.
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass))
    if (ids.isNotEmpty()) {
        onUpdate(context, manager, ids)
    }
}

// ── Glance action callbacks (widget button clicks) ──────────────────────────

class PlayPauseCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.sendBroadcast(
            Intent(PlaybackService.ACTION_PLAY_PAUSE).setPackage(context.packageName)
        )
    }
}

class SkipNextCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.sendBroadcast(
            Intent(PlaybackService.ACTION_SKIP_NEXT).setPackage(context.packageName)
        )
    }
}

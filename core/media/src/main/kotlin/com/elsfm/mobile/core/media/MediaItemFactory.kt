package com.elsfm.mobile.core.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.elsfm.mobile.core.model.Track

fun Track.toMediaItem(
    baseUrl: String = "https://www.elsfm.com/",
    sessionPreferences: SessionPreferences? = null,
): MediaItem? {
    // image is already a fully-qualified URL by the time it reaches here - Track.image is
    // decoded via ImageUrlSerializer, which normalizes the backend's relative paths to full
    // URLs at deserialize time. Prefixing baseUrl again here produced a broken URL like
    // "https://www.elsfm.com/https://www.elsfm.com/storage/...", which is why the media
    // notification never showed album art.
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(artists.firstOrNull()?.name.orEmpty())
        .apply { image?.let { setArtworkUri(Uri.parse(it)) } }
        .build()

    // A locally downloaded track's src is already a full file:// URI - unlike streamed
    // tracks, it must not be prefixed with baseUrl.
    //
    // The Album/Channel API responses that populate most queues never include `src` at all
    // (confirmed live: the field is simply absent, authenticated or not) - falling back to
    // `baseUrl + null` produced the literal string ".../null", which the backend's SPA
    // catch-all route resolved to its own HTML homepage instead of audio, and ExoPlayer
    // failed with UnrecognizedInputFormatException. The authenticated download endpoint
    // (already used successfully by DownloadManager) reliably serves the real audio file by
    // track id alone, so it's used whenever `src` isn't available.
    //
    // When offline mode is enabled, only local files (file://) can be played. Streaming
    // tracks or remote endpoints return null to signal unavailability in offline mode.
    val uri = when {
        src?.startsWith("file://") == true -> src
        sessionPreferences?.isOfflineModeEnabled == true -> {
            // Offline mode: only allow local files
            return null
        }
        !src.isNullOrBlank() -> baseUrl + src
        else -> "${baseUrl}api/v1/tracks/$id/download"
    }

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

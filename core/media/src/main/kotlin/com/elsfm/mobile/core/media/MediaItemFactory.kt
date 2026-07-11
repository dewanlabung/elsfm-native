package com.elsfm.mobile.core.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.elsfm.mobile.core.model.Track

fun Track.toMediaItem(baseUrl: String = "https://www.elsfm.com/"): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(artists.firstOrNull()?.name.orEmpty())
        .apply { image?.let { setArtworkUri(Uri.parse(baseUrl + it)) } }
        .build()

    // A locally downloaded track's src is already a full file:// URI - unlike streamed
    // tracks, it must not be prefixed with baseUrl.
    val uri = if (src.orEmpty().startsWith("file://")) src else baseUrl + src

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

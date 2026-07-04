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

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(baseUrl + src)
        .setMediaMetadata(metadata)
        .build()
}

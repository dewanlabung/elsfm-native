package com.elsfm.mobile.core.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object SearchResultSerializer : JsonContentPolymorphicSerializer<SearchResult>(SearchResult::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SearchResult> {
        return when {
            "track" in element.jsonObject -> SearchResult.TrackResult.serializer()
            "playlist" in element.jsonObject -> SearchResult.PlaylistResult.serializer()
            "artist" in element.jsonObject -> SearchResult.ArtistResult.serializer()
            "channel" in element.jsonObject -> SearchResult.ChannelResult.serializer()
            else -> throw IllegalArgumentException("Unknown SearchResult type")
        } as DeserializationStrategy<SearchResult>
    }
}

@Serializable(with = SearchResultSerializer::class)
sealed class SearchResult {
    @Serializable
    @SerialName("track")
    data class TrackResult(val track: Track) : SearchResult()

    @Serializable
    @SerialName("playlist")
    data class PlaylistResult(val playlist: Playlist) : SearchResult()

    @Serializable
    @SerialName("artist")
    data class ArtistResult(val artist: Artist) : SearchResult()

    @Serializable
    @SerialName("channel")
    data class ChannelResult(val channel: Channel) : SearchResult()
}

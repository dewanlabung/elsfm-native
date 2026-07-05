package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class SearchResultPage<T>(val data: List<T> = emptyList())

@Serializable
private data class SearchResults(
    val tracks: SearchResultPage<Track>? = null,
    val artists: SearchResultPage<Artist>? = null,
    val playlists: SearchResultPage<Playlist>? = null,
)

@Serializable
private data class SearchResponse(val results: SearchResults)

class SearchApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun search(query: String): ApiResult<List<SearchResult>> {
        return try {
            val response = httpClient.get("api/v1/search") {
                parameter("query", query)
            }
            if (response.status.isSuccess()) {
                val results = response.body<SearchResponse>().results
                val combined = buildList {
                    results.tracks?.data?.forEach { add(SearchResult.TrackResult(it)) }
                    results.artists?.data?.forEach { add(SearchResult.ArtistResult(it)) }
                    results.playlists?.data?.forEach { add(SearchResult.PlaylistResult(it)) }
                }
                ApiResult.Success(combined)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}

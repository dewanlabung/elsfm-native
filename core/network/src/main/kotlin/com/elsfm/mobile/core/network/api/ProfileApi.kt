package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class UpdateProfileRequest(val name: String, val bio: String?)

@Serializable
private data class RecentlyPlayedTrackList(val data: List<Track>)

open class ProfileApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    open suspend fun getProfile(): ApiResult<UserProfile> {
        return try {
            val response = httpClient.get("api/v1/me/profile")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserProfile>())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun updateProfile(name: String, bio: String?): ApiResult<UserProfile> {
        return try {
            val response = httpClient.put("api/v1/me/profile") {
                contentType(ContentType.Application.Json)
                setBody(UpdateProfileRequest(name = name, bio = bio))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserProfile>())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun getRecentlyPlayed(): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/me/recently-played")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<RecentlyPlayedTrackList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}

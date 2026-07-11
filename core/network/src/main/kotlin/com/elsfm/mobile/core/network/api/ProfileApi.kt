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

// The public profile page (`GET user-profile/{id}`, `UserProfileController::show` ->
// `UserProfileLoader::toApiResource`) returns a curated, nested shape - not the flat
// [UserProfile] used by AccountApi's raw-Eloquent-model responses. Decoded here into
// this DTO, then mapped into [UserProfile] manually.
@Serializable
private data class UserProfilePageDto(
    val id: Int,
    val name: String,
    val username: String? = null,
    val image: String? = null,
    val profile: UserProfileDetailsDto? = null,
    @kotlinx.serialization.SerialName("followers_count") val followersCount: Int = 0,
    @kotlinx.serialization.SerialName("followed_users_count") val followedUsersCount: Int = 0,
)

@Serializable
private data class UserProfileDetailsDto(val description: String? = null)

@Serializable
private data class UserProfilePageResponse(val user: UserProfilePageDto)

private fun UserProfilePageDto.toUserProfile() = UserProfile(
    id = id,
    name = name,
    email = null,
    profileImage = image,
    bio = profile?.description,
    followersCount = followersCount,
    followedCount = followedUsersCount,
)

// Real shape for `PUT api/v1/users/profile/update` (`UserProfileController::update`):
// takes `user`, `profile` and `links` top-level keys, not a flat `{name, bio}` body.
@Serializable
private data class UpdateProfileRequest(
    val user: UpdateProfileUser,
    val profile: UserProfileDetailsDto,
    val links: List<String> = emptyList(),
)

@Serializable
private data class UpdateProfileUser(val name: String)

// `api/v1/me/recently-played` does not exist on the real backend (verified
// against routes/api.php). The real play-history endpoint is
// `GET api/v1/tracks/plays/{userId}` (TrackPlaysController::index), which
// accepts the literal string "me" for the current user and returns a
// standard paginated response, same shape as playlist/album track lists.
@Serializable
private data class RecentlyPlayedPagination(val data: List<Track>)

@Serializable
private data class RecentlyPlayedResponse(val pagination: RecentlyPlayedPagination)

open class ProfileApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    open suspend fun getProfile(userId: Int): ApiResult<UserProfile> {
        return try {
            val response = httpClient.get("api/v1/user-profile/$userId")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserProfilePageResponse>().user.toUserProfile())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * On success, re-fetches [getProfile] rather than parsing the update response body:
     * the update endpoint returns the raw `User` model (a different, unreliable shape),
     * while the profile page's curated resource is what the UI actually displays.
     */
    open suspend fun updateProfile(userId: Int, name: String, bio: String?): ApiResult<UserProfile> {
        return try {
            val response = httpClient.put("api/v1/users/profile/update") {
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateProfileRequest(
                        user = UpdateProfileUser(name = name),
                        profile = UserProfileDetailsDto(description = bio),
                    ),
                )
            }
            if (response.status.isSuccess()) {
                getProfile(userId)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun getRecentlyPlayed(): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/tracks/plays/me")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<RecentlyPlayedResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}

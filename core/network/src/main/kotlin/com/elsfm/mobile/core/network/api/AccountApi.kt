package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.FileEntry
import com.elsfm.mobile.core.model.LaravelValidationError
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

@Serializable
private data class FileEntryResponse(val fileEntry: FileEntry)

@Serializable
private data class UserResponse(val user: UserProfile)

@Serializable
private data class UpdateAccountDetailsRequest(
    val name: String? = null,
    val image: String? = null,
    val image_entry_id: Int? = null,
)

@Serializable
private data class ChangePasswordRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String,
)

/**
 * Backs the real "Update name and profile image" account setting
 * (`common/foundation/resources/client/auth/ui/account-settings/basic-info-panel` in the
 * Laravel/web source). The web client uses a two-step flow and this mirrors it exactly:
 *
 * 1. [uploadAvatar] - `POST api/v1/uploads` (multipart, `Common\Files\Controllers\FileEntriesController::store`)
 *    with the raw image bytes and `uploadType=avatars`. Returns a [FileEntry] (id + url).
 * 2. [updateAccountDetails] - `PUT api/v1/users/{id}` (`Common\Auth\Controllers\UserController::update`)
 *    with the new `name` and/or the `image`/`image_entry_id` from step 1.
 *
 * There is no dedicated single-request avatar-upload endpoint on this backend.
 */
open class AccountApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    open suspend fun uploadAvatar(
        bytes: ByteArray,
        filename: String,
        mimeType: String,
    ): ApiResult<FileEntry> {
        return try {
            val response = httpClient.post("api/v1/uploads") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("uploadType", "avatars")
                            append(
                                "file",
                                bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, mimeType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                                },
                            )
                        },
                    ),
                )
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created ->
                    ApiResult.Success(response.body<FileEntryResponse>().fileEntry)
                HttpStatusCode.UnprocessableEntity ->
                    ApiResult.ValidationError(emptyMap())
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ApiResult.Unauthorized
                else -> ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun updateAccountDetails(
        userId: Int,
        name: String? = null,
        image: String? = null,
        imageEntryId: Int? = null,
    ): ApiResult<UserProfile> {
        return try {
            val response = httpClient.put("api/v1/users/$userId") {
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateAccountDetailsRequest(
                        name = name,
                        image = image,
                        image_entry_id = imageEntryId,
                    ),
                )
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserResponse>().user)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Mirrors the PWA's "Update password" panel
     * (`common/foundation/resources/client/auth/ui/account-settings/password-panel`).
     * Sends [currentPassword], [newPassword], and its confirmation to
     * `PUT api/v1/users/{userId}`, which the backend's UserController::update handles
     * alongside name and avatar updates.
     */
    open suspend fun changePassword(
        userId: Int,
        currentPassword: String,
        newPassword: String,
    ): ApiResult<Unit> {
        return try {
            val response = httpClient.put("api/v1/users/$userId") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChangePasswordRequest(
                        current_password = currentPassword,
                        password = newPassword,
                        password_confirmation = newPassword,
                    ),
                )
            }
            when {
                response.status.isSuccess() -> ApiResult.Success(Unit)
                response.status == HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                response.status == HttpStatusCode.Unauthorized ||
                    response.status == HttpStatusCode.Forbidden -> ApiResult.Unauthorized
                else -> ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Matches the web client's "Remove image" link, which sends explicit
     * `image: null, image_entry_id: null` to clear the avatar. Sent as a raw
     * [JsonObject] rather than [UpdateAccountDetailsRequest] because the shared
     * [com.elsfm.mobile.core.network.elsfmJson] config uses `explicitNulls = false`,
     * which would otherwise omit (rather than null out) these fields.
     */
    open suspend fun removeAvatar(userId: Int): ApiResult<UserProfile> {
        return try {
            val response = httpClient.put("api/v1/users/$userId") {
                contentType(ContentType.Application.Json)
                setBody(JsonObject(mapOf("image" to JsonNull, "image_entry_id" to JsonNull)))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserResponse>().user)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}

package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

/**
 * A stored file returned by the Laravel backend's generic upload endpoint
 * (`POST api/v1/uploads`, `Common\Files\Controllers\FileEntriesController::store`).
 *
 * Only the fields the account-avatar flow needs are modeled here: after uploading raw
 * image bytes, the response's `id` and `url` are sent back to `PUT api/v1/users/{id}`
 * as `image_entry_id` and `image` to attach the file as the user's avatar.
 */
@Serializable
data class FileEntry(
    val id: Int,
    val url: String,
)

package com.elsfm.mobile.core.network.download

import android.content.Context
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

private const val DOWNLOADS_DIR = "downloads"
private const val BUFFER_SIZE = 4096

private const val TAG = "ElsfmDownload"
private const val ELSFM_BASE = "https://www.elsfm.com/"

/**
 * Downloads a track's audio file to app-private external storage.
 *
 * URL resolution mirrors MediaItemFactory so download and playback always use
 * the same source:
 *  - track.src is set → use it (relative paths are prefixed with ELSFM_BASE)
 *  - track.src is absent → fall back to the authenticated download endpoint
 */
@Singleton
open class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context?,
    private val httpClient: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val downloadDir: File?
        get() = context?.getExternalFilesDir(DOWNLOADS_DIR)

    open suspend fun downloadTrack(track: Track, onProgress: (Float) -> Unit): Result<File> =
        withContext(dispatcherProvider.io) {
            try {
                val dir = requireNotNull(downloadDir) { "Downloads directory unavailable" }
                dir.mkdirs()
                val file = File(dir, "${track.id}_${track.name.slugify()}.mp3")

                // Mirror MediaItemFactory's URL resolution so the audio source used for
                // downloading always matches what Media3 streams.
                val trackSrc = track.src  // local copy avoids cross-module smart cast failure
                val audioUrl: String = when {
                    !trackSrc.isNullOrBlank() ->
                        if (trackSrc.startsWith("http")) trackSrc
                        else ELSFM_BASE + trackSrc.removePrefix("/")
                    else -> "api/v1/tracks/${track.id}/download"
                }

                android.util.Log.d(TAG, "Downloading track ${track.id} from: $audioUrl")
                val response = httpClient.get(audioUrl)
                if (!response.status.isSuccess()) {
                    val msg = "HTTP ${response.status} for track ${track.id} ($audioUrl)"
                    android.util.Log.w(TAG, msg)
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                val totalBytes = response.headers[HttpHeaders.ContentLength]?.toLong() ?: 0L
                var bytesWritten = 0L

                val channel = response.bodyAsChannel()
                file.outputStream().use { fileOut ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead <= 0) break
                        fileOut.write(buffer, 0, bytesRead)
                        bytesWritten += bytesRead
                        if (totalBytes > 0) {
                            onProgress(bytesWritten.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
                android.util.Log.d(TAG, "Download complete: track ${track.id}, ${bytesWritten}B -> ${file.name}")
                Result.success(file)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Download exception for track ${track.id}: ${e.message}", e)
                Result.failure(e)
            }
        }

    open suspend fun deleteDownload(trackId: Int): Boolean =
        withContext(dispatcherProvider.io) {
            downloadDir?.listFiles()?.find { it.name.startsWith("${trackId}_") }
                ?.delete() ?: false
        }

    /** The local on-disk file for a completed download, for offline playback. */
    open fun getFile(fileName: String): File? {
        return downloadDir?.let { File(it, fileName) }
    }
}

private fun String.slugify(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

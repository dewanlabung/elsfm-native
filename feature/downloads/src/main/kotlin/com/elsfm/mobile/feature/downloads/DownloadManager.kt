package com.elsfm.mobile.feature.downloads

import android.content.Context
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

private const val DOWNLOADS_DIR = "downloads"
private const val BUFFER_SIZE = 4096

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

                val response = httpClient.get("tracks/${track.id}/download")
                val contentLength = response.contentLength() ?: 0L
                var bytesWritten = 0L

                val channel = response.bodyAsChannel()
                file.outputStream().use { fileOut ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead <= 0) break
                        fileOut.write(buffer, 0, bytesRead)
                        bytesWritten += bytesRead
                        if (contentLength > 0) {
                            onProgress(bytesWritten.toFloat() / contentLength.toFloat())
                        }
                    }
                }
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    open suspend fun deleteDownload(trackId: Int): Boolean =
        withContext(dispatcherProvider.io) {
            downloadDir?.listFiles()?.find { it.name.startsWith("${trackId}_") }
                ?.delete() ?: false
        }
}

private fun String.slugify(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

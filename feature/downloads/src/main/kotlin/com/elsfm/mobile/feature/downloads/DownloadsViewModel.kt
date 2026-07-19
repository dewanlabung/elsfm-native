package com.elsfm.mobile.feature.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AlbumApi
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.TrackListApi
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.feature.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val playerController: PlayerController,
    private val userApi: UserApi,
    private val albumApi: AlbumApi,
    private val playlistApi: PlaylistApi,
    private val trackListApi: TrackListApi,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsState())
    val state = _state.asStateFlow()

    private var allDownloads: List<DownloadedTrack> = emptyList()

    init {
        loadDownloads()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            downloadRepository.getCompletedDownloads().collectLatest { downloads ->
                allDownloads = downloads
                updateDownloadsList()
            }
        }
    }

    fun onEvent(event: DownloadsEvent) {
        when (event) {
            is DownloadsEvent.TabChanged -> _state.value = _state.value.copy(activeTab = event.tab)
            is DownloadsEvent.SearchQueryChanged -> {
                _state.value = _state.value.copy(searchQuery = event.query)
                updateDownloadsList()
            }
            is DownloadsEvent.SortChanged -> {
                _state.value = _state.value.copy(sortBy = event.sortBy)
                updateDownloadsList()
            }
            is DownloadsEvent.DeleteDownload -> deleteDownload(event.trackId)
            is DownloadsEvent.ShareDownload -> shareDownload(event.trackId)
            is DownloadsEvent.PlayTrack -> playGroup(allDownloads, startTrackId = event.trackId)
            is DownloadsEvent.PlayAlbum -> playGroup(allDownloads.filter { it.albumId == event.albumId })
            is DownloadsEvent.PlayPlaylist -> playGroup(allDownloads.filter { it.playlistId == event.playlistId })
            is DownloadsEvent.PlayAll -> playGroup(filteredDownloads())
            is DownloadsEvent.ShuffleAll -> playGroup(filteredDownloads().shuffled())
            is DownloadsEvent.DownloadLibrary -> downloadLibrary()
        }
    }

    private fun filteredDownloads(): List<DownloadedTrack> {
        val q = _state.value.searchQuery
        return if (q.isBlank()) allDownloads
        else allDownloads.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
        }
    }

    private fun updateDownloadsList() {
        val currentState = _state.value
        val filtered = filteredDownloads().let { list ->
            when (currentState.sortBy) {
                SortBy.RECENTLY_ADDED -> list.sortedByDescending { it.downloadedAt }
                SortBy.A_TO_Z -> list.sortedBy { it.title }
                SortBy.RELEASE_DATE -> list.sortedByDescending { it.downloadedAt }
            }
        }

        val tracks = filtered.map { d ->
            DownloadedTrackUI(
                trackId = d.trackId,
                title = d.title,
                artist = d.artist,
                artworkUrl = d.artworkUrl,
                fileSize = formatFileSize(d.fileSizeBytes),
                downloadedAt = d.downloadedAt,
                isOffline = true,
            )
        }

        val albums = filtered
            .filter { it.albumId != null }
            .groupBy { it.albumId }
            .map { (albumId, group) ->
                DownloadedAlbumUI(
                    albumId = albumId!!,
                    name = group.first().albumName ?: "Unknown album",
                    artist = group.first().artist,
                    artworkUrl = group.first().artworkUrl,
                    trackCount = group.size,
                    trackIds = group.map { it.trackId },
                )
            }

        val playlists = filtered
            .filter { it.playlistId != null }
            .groupBy { it.playlistId }
            .map { (playlistId, group) ->
                DownloadedPlaylistUI(
                    playlistId = playlistId!!,
                    name = group.first().playlistName ?: "Unknown playlist",
                    artworkUrl = group.first().artworkUrl,
                    trackCount = group.size,
                    trackIds = group.map { it.trackId },
                )
            }

        _state.value = currentState.copy(
            downloadedTracks = tracks,
            downloadedAlbums = albums,
            downloadedPlaylists = playlists,
        )
    }

    /**
     * Downloads all albums and playlists from the user's library using the same
     * per-track `GET api/v1/tracks/{id}/download` mechanism as AlbumViewModel/PlaylistViewModel.
     *
     * Flow:
     * 1. Fetch user's liked albums → for each album, fetch full album details (which nest tracks)
     *    → download each track with albumId/albumName stored
     * 2. Fetch user's playlists → for each playlist, page through tracks
     *    → download each track with playlistId/playlistName stored
     *
     * Tracks already downloaded are skipped via [DownloadRepository.isDownloaded].
     */
    private fun downloadLibrary() {
        if (_state.value.isDownloadingLibrary) return
        viewModelScope.launch {
            val userId = userDao.get()?.id
            if (userId == null) {
                _state.value = _state.value.copy(error = "Not signed in")
                return@launch
            }

            _state.value = _state.value.copy(isDownloadingLibrary = true, libraryDownloadStatus = "Loading library…")

            // --- Albums ---
            when (val albumsResult = userApi.getLikedAlbums(userId)) {
                is ApiResult.Success -> {
                    val albums = albumsResult.data
                    albums.forEachIndexed { index, albumSummary ->
                        _state.value = _state.value.copy(
                            libraryDownloadStatus = "Album ${index + 1}/${albums.size}: ${albumSummary.name}…",
                        )
                        // The album summary from the library endpoint may not include tracks;
                        // we must fetch the full album detail which nests all tracks.
                        when (val detailResult = albumApi.getAlbum(albumSummary.id)) {
                            is ApiResult.Success -> {
                                val album = detailResult.data
                                album.tracks.forEachIndexed { tIdx, track ->
                                    if (!downloadRepository.isDownloaded(track.id)) {
                                        _state.value = _state.value.copy(
                                            libraryDownloadStatus = "Album ${index + 1}/${albums.size}: track ${tIdx + 1}/${album.tracks.size} — ${track.name}",
                                        )
                                        downloadRepository.downloadTrack(
                                            track,
                                            albumId = album.id,
                                            albumName = album.name,
                                        )
                                    }
                                }
                            }
                            else -> { /* skip albums that fail to load */ }
                        }
                    }
                }
                else -> { /* skip album step on error, still try playlists */ }
            }

            // --- Playlists ---
            when (val playlistsResult = playlistApi.getUserPlaylists(userId)) {
                is ApiResult.Success -> {
                    val playlists = playlistsResult.data
                    playlists.forEachIndexed { index, playlistInfo ->
                        _state.value = _state.value.copy(
                            libraryDownloadStatus = "Playlist ${index + 1}/${playlists.size}: ${playlistInfo.name}…",
                        )
                        // Page through all tracks in the playlist
                        var page = 1
                        var hasMore = true
                        while (hasMore) {
                            when (val tracksResult = trackListApi.getPlaylistTracks(playlistInfo.id, page)) {
                                is ApiResult.Success -> {
                                    tracksResult.data.tracks.forEachIndexed { tIdx, track ->
                                        if (!downloadRepository.isDownloaded(track.id)) {
                                            _state.value = _state.value.copy(
                                                libraryDownloadStatus = "Playlist ${index + 1}/${playlists.size}: track ${tIdx + 1} — ${track.name}",
                                            )
                                            downloadRepository.downloadTrack(
                                                track,
                                                playlistId = playlistInfo.id,
                                                playlistName = playlistInfo.name,
                                            )
                                        }
                                    }
                                    hasMore = tracksResult.data.hasMore
                                    page++
                                }
                                else -> hasMore = false
                            }
                        }
                    }
                }
                else -> { /* skip playlist step on error */ }
            }

            _state.value = _state.value.copy(
                isDownloadingLibrary = false,
                libraryDownloadStatus = null,
            )
        }
    }

    private fun deleteDownload(trackId: Int) {
        viewModelScope.launch { downloadRepository.deleteDownloadedTrack(trackId) }
    }

    private fun shareDownload(trackId: Int) {
        val download = allDownloads.find { it.trackId == trackId } ?: return
        val file = downloadRepository.getLocalFile(download.fileName) ?: return
        if (!file.exists()) {
            _state.value = _state.value.copy(error = "File not found")
            return
        }
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Cannot share: ${e.message}")
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, download.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${download.title}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun playGroup(downloads: List<DownloadedTrack>, startTrackId: Int? = null) {
        if (downloads.isEmpty()) return
        val queue = downloads.mapNotNull { d ->
            val file = downloadRepository.getLocalFile(d.fileName) ?: return@mapNotNull null
            Track(
                id = d.trackId,
                name = d.title,
                image = d.artworkUrl,
                durationMs = 0L,
                src = "file://${file.absolutePath}",
                artists = listOf(Artist(id = 0, name = d.artist)),
            )
        }
        if (queue.isEmpty()) return
        val startTrack = startTrackId?.let { id -> queue.find { it.id == id } } ?: queue.first()
        playerController.play(startTrack, queue)
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}

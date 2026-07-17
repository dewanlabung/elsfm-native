package com.elsfm.mobile.feature.library

import app.cash.turbine.test
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadLibrary updates state with playlists, albums, artists and channels`() = runTest {
        val testArtist = Artist(id = 1, name = "ELShaddai Kalimpong")
        val testChannel = Channel(id = 1, name = "Sunday School", modelType = "channel")
        val repository = FakeLibraryApiRepository(
            playlists = listOf(Playlist(id = 8, name = "All Sunday School Songs", image = null)),
            albums = listOf(Album(id = 460, name = "2026 EL Shaddai Youth Camp Songs", image = null)),
            artists = listOf(testArtist),
            channels = listOf(testChannel),
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.state.test {
            // Initial state (empty)
            assertEquals(LibraryState(), awaitItem())

            // Loading state
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            // Loaded state
            val loadedState = awaitItem()
            assertTrue(loadedState.playlists.isNotEmpty())
            assertTrue(loadedState.albums.isNotEmpty())
            assertEquals(1, loadedState.artists.size)
            assertEquals("ELShaddai Kalimpong", loadedState.artists[0].name)
            assertEquals(1, loadedState.channels.size)
            assertEquals("Sunday School", loadedState.channels[0].name)
            assertEquals(false, loadedState.isLoading)
            assertNull(loadedState.error)
        }
    }

    @Test
    fun `loadLibrary sets error when repository fails`() = runTest {
        val repository = FakeLibraryApiRepository(
            error = RuntimeException("Network error"),
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.state.test {
            assertEquals(LibraryState(), awaitItem())
            awaitItem() // loading

            val loadedState = awaitItem()
            assertEquals(false, loadedState.isLoading)
            assertNotNull(loadedState.error)
            assertEquals("Failed to load library", loadedState.error)
        }
    }

    @Test
    fun `selectFilter updates selectedFilter in state`() = runTest {
        val repository = FakeLibraryApiRepository()
        val viewModel = LibraryViewModel(repository)

        viewModel.selectFilter(LibraryFilter.PLAYLISTS)

        assertEquals(LibraryFilter.PLAYLISTS, viewModel.state.value.selectedFilter)
    }

    @Test
    fun `createPlaylist appends the new playlist and signals playlistCreated`() = runTest {
        val newPlaylist = Playlist(id = 42, name = "Road Trip", image = null)
        val repository = FakeLibraryApiRepository(
            createPlaylistResult = ApiResult.Success(newPlaylist),
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // loaded (empty library)

            viewModel.createPlaylist("Road Trip")

            val creatingState = awaitItem()
            assertTrue(creatingState.isCreatingPlaylist)

            val createdState = awaitItem()
            assertEquals(false, createdState.isCreatingPlaylist)
            assertTrue(createdState.playlistCreated)
            assertTrue(createdState.playlists.contains(newPlaylist))

            viewModel.consumePlaylistCreatedEvent()
            val consumedState = awaitItem()
            assertEquals(false, consumedState.playlistCreated)
        }
    }

    @Test
    fun `createPlaylist surfaces a validation error without touching the playlist list`() = runTest {
        val errors = mapOf("name" to listOf("You have already created a playlist with this name."))
        val repository = FakeLibraryApiRepository(
            createPlaylistResult = ApiResult.ValidationError(errors),
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // loaded (empty library)

            viewModel.createPlaylist("Road Trip")

            awaitItem() // isCreatingPlaylist = true

            val errorState = awaitItem()
            assertEquals(false, errorState.isCreatingPlaylist)
            assertEquals(false, errorState.playlistCreated)
            assertEquals("You have already created a playlist with this name.", errorState.createPlaylistError)
            assertTrue(errorState.playlists.isEmpty())
        }
    }
}

package com.elsfm.mobile.feature.artist

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ArtistApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class FakeArtistApi(
    private val artist: Artist?,
    private val tracks: List<Track>?,
    private val artistError: Boolean = false,
    private val tracksError: Boolean = false,
) : ArtistApi(HttpClient(MockEngine { respond("{}") })) {
    override suspend fun getArtist(id: Int): ApiResult<Artist> {
        return if (artistError || artist == null) {
            ApiResult.NetworkError(RuntimeException("Failed to load artist"))
        } else {
            ApiResult.Success(artist)
        }
    }

    override suspend fun getArtistTracks(id: Int): ApiResult<List<Track>> {
        return if (tracksError || tracks == null) {
            ApiResult.NetworkError(RuntimeException("Failed to load tracks"))
        } else {
            ApiResult.Success(tracks)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val testArtist = Artist(id = 1, name = "Test Artist")
    private val testTracks = listOf(
        Track(
            id = 1,
            name = "Track 1",
            image = null,
            durationMs = 180_000,
            src = "storage/track_1.mp3",
            artists = listOf(testArtist),
        ),
        Track(
            id = 2,
            name = "Track 2",
            image = null,
            durationMs = 200_000,
            src = "storage/track_2.mp3",
            artists = listOf(testArtist),
        ),
    )

    @Test
    fun `loads artist and tracks on init`() = runTest(testDispatcher) {
        val artistApi = FakeArtistApi(testArtist, testTracks)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        savedStateHandle["artistId"] = 1
        val viewModel = ArtistDetailViewModel(artistApi, savedStateHandle, FakeDispatcherProvider(testDispatcher))
        delay(100) // Allow coroutines to execute

        val state = viewModel.state.value
        assertEquals(testArtist, state.artist)
        assertEquals(testTracks, state.tracks)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `handles artist loading error`() = runTest(testDispatcher) {
        val artistApi = FakeArtistApi(null, testTracks, artistError = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        savedStateHandle["artistId"] = 1
        val viewModel = ArtistDetailViewModel(artistApi, savedStateHandle, FakeDispatcherProvider(testDispatcher))
        delay(100) // Allow coroutines to execute

        val state = viewModel.state.value
        assertNull(state.artist)
        assertEquals(false, state.isLoading)
        assertEquals("Failed to load artist", state.error)
    }

    @Test
    fun `handles track loading error`() = runTest(testDispatcher) {
        val artistApi = FakeArtistApi(testArtist, null, tracksError = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        savedStateHandle["artistId"] = 1
        val viewModel = ArtistDetailViewModel(artistApi, savedStateHandle, FakeDispatcherProvider(testDispatcher))
        delay(100) // Allow coroutines to execute

        val state = viewModel.state.value
        assertEquals(testArtist, state.artist)
        assertEquals(emptyList<Track>(), state.tracks)
        assertEquals("Failed to load tracks", state.error)
    }

    @Test
    fun `initial state is empty when artistId is not provided`() = runTest(testDispatcher) {
        val artistApi = FakeArtistApi(testArtist, testTracks)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val viewModel = ArtistDetailViewModel(artistApi, savedStateHandle, FakeDispatcherProvider(testDispatcher))

        val state = viewModel.state.value
        assertNull(state.artist)
        assertEquals(emptyList<Track>(), state.tracks)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }
}

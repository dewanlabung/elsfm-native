package com.elsfm.mobile.feature.library

import app.cash.turbine.test
import com.elsfm.mobile.core.model.Channel
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
    fun `loadLibrary updates state with playlists, albums and channels`() = runTest {
        val testChannel = Channel(id = 1, name = "Sunday School", modelType = "channel")
        val repository = FakeLibraryApiRepository(
            playlists = SampleLibraryData.playlists,
            albums = SampleLibraryData.albums,
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
}

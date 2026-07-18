package com.elsfm.mobile.feature.profile.storage

import com.elsfm.mobile.core.database.dao.DiscoveryCacheDao
import com.elsfm.mobile.core.database.dao.LibraryCacheDao
import com.elsfm.mobile.core.database.dao.ProfileCacheDao
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeDiscoveryCacheDao : DiscoveryCacheDao {
    override suspend fun save(cache: com.elsfm.mobile.core.database.entity.DiscoveryCache) {}
    override suspend fun get() = null
    override suspend fun getSizeBytes() = 5000L
    override suspend fun clear() {}
}

class FakeLibraryCacheDao : LibraryCacheDao {
    override suspend fun save(cache: com.elsfm.mobile.core.database.entity.LibraryCache) {}
    override suspend fun get() = null
    override suspend fun getSizeBytes() = 10000L
    override suspend fun clear() {}
}

class FakeProfileCacheDao : ProfileCacheDao {
    override suspend fun save(cache: com.elsfm.mobile.core.database.entity.ProfileCache) {}
    override suspend fun get() = null
    override suspend fun getSizeBytes() = 2000L
    override suspend fun clear() {}
}

class FakeDownloadedTrackDao : DownloadedTrackDao {
    override suspend fun insert(track: com.elsfm.mobile.core.database.entity.DownloadedTrack) {}
    override suspend fun delete(id: Int) {}
    override suspend fun getAll() = emptyList()
    override suspend fun getById(id: Int) = null
    override fun getTotalSizeBytes() = flowOf(15000L)
}

class StorageViewModelTest {
    @Test
    fun `initial state loads cache sizes`() = runTest {
        val viewModel = StorageViewModel(
            FakeDiscoveryCacheDao(),
            FakeLibraryCacheDao(),
            FakeProfileCacheDao(),
            FakeDownloadedTrackDao(),
        )

        val state = viewModel.state.value
        assertEquals(5000L, state.discoveryCacheBytes)
        assertEquals(10000L, state.libraryCacheBytes)
        assertEquals(2000L, state.profileCacheBytes)
        assertEquals(15000L, state.downloadedTracksBytes)
        assertEquals(17000L, state.totalCacheBytes) // 5000 + 10000 + 2000
    }

    @Test
    fun `toggle expanded flips the isExpanded flag`() = runTest {
        val viewModel = StorageViewModel(
            FakeDiscoveryCacheDao(),
            FakeLibraryCacheDao(),
            FakeProfileCacheDao(),
            FakeDownloadedTrackDao(),
        )

        assertFalse(viewModel.state.value.isExpanded)
        viewModel.toggleExpanded()
        assertTrue(viewModel.state.value.isExpanded)
        viewModel.toggleExpanded()
        assertFalse(viewModel.state.value.isExpanded)
    }

    @Test
    fun `clear all caches resets cache bytes to zero`() = runTest {
        val viewModel = StorageViewModel(
            FakeDiscoveryCacheDao(),
            FakeLibraryCacheDao(),
            FakeProfileCacheDao(),
            FakeDownloadedTrackDao(),
        )

        assertEquals(17000L, viewModel.state.value.totalCacheBytes)
        viewModel.clearAllCaches()
        assertEquals(0L, viewModel.state.value.totalCacheBytes)
        assertEquals(0L, viewModel.state.value.discoveryCacheBytes)
        assertEquals(0L, viewModel.state.value.libraryCacheBytes)
        assertEquals(0L, viewModel.state.value.profileCacheBytes)
    }
}

package com.elsfm.mobile.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadedTrackDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: DownloadedTrackDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.downloadedTrackDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveDownload() = runTest {
        val track = DownloadedTrack(trackId = 1, fileName = "track.mp3", fileSizeBytes = 5_000_000)
        dao.insert(track)

        val retrieved = dao.getById(1)

        assertEquals("track.mp3", retrieved?.fileName)
    }

    @Test
    fun deleteDownload() = runTest {
        val track = DownloadedTrack(trackId = 1, fileName = "track.mp3", fileSizeBytes = 5_000_000)
        dao.insert(track)

        dao.delete(1)

        assertNull(dao.getById(1))
    }

    @Test
    fun getTotalSizeBytesSumsAllDownloadedTracks() = runTest {
        dao.insert(DownloadedTrack(trackId = 1, fileName = "track1.mp3", fileSizeBytes = 3_000_000))
        dao.insert(DownloadedTrack(trackId = 2, fileName = "track2.mp3", fileSizeBytes = 2_000_000))

        val totalSize = dao.getTotalSizeBytes().first()

        assertEquals(5_000_000L, totalSize)
    }
}

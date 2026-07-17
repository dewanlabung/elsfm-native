package com.elsfm.mobile.core.database.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.elsfm.mobile.core.common.PersistedPlaybackState
import com.elsfm.mobile.core.database.AppDatabase
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomPlaybackStateStoreTest {
    private lateinit var db: AppDatabase
    private lateinit var store: RoomPlaybackStateStore

    private val track = Track(
        id = 1,
        name = "Track 1",
        image = null,
        durationMs = 10_000,
        src = "storage/track_media/x.mp3",
        artists = listOf(Artist(id = 1, name = "Artist 1")),
    )
    private val queueTrack = track.copy(id = 2, name = "Track 2")

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = RoomPlaybackStateStore(db.playbackStateDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `restore returns null when nothing has been saved`() = runTest {
        assertNull(store.restore())
    }

    @Test
    fun `save then restore round-trips the persisted state`() = runTest {
        val persisted = PersistedPlaybackState(
            currentTrack = track,
            queue = listOf(track, queueTrack),
            positionMs = 5_000,
            speed = 1.25f,
            volume = 0.8f,
        )

        store.save(persisted)

        assertEquals(persisted, store.restore())
    }

    @Test
    fun `save replaces any previously saved state`() = runTest {
        store.save(
            PersistedPlaybackState(currentTrack = track, queue = listOf(track), positionMs = 1_000, speed = 1f, volume = 1f),
        )

        store.save(
            PersistedPlaybackState(currentTrack = queueTrack, queue = listOf(queueTrack), positionMs = 2_000, speed = 1.5f, volume = 0.5f),
        )

        val restored = store.restore()
        assertEquals(queueTrack, restored?.currentTrack)
        assertEquals(2_000L, restored?.positionMs)
    }

    @Test
    fun `clear removes the saved state`() = runTest {
        store.save(
            PersistedPlaybackState(currentTrack = track, queue = listOf(track), positionMs = 1_000, speed = 1f, volume = 1f),
        )

        store.clear()

        assertNull(store.restore())
    }
}

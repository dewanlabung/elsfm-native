package com.elsfm.mobile.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.elsfm.mobile.core.database.dao.DiscoveryCacheDao
import com.elsfm.mobile.core.database.entity.DiscoveryCache
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryCacheDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: DiscoveryCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.discoveryCacheDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getReturnsNullWhenNothingCached() = runTest {
        assertNull(dao.get())
    }

    @Test
    fun saveAndRetrieveCache() = runTest {
        dao.save(DiscoveryCache(payloadJson = "{\"kidsZone\":[]}"))

        val retrieved = dao.get()

        assertEquals("{\"kidsZone\":[]}", retrieved?.payloadJson)
    }

    @Test
    fun saveReplacesPreviousCacheRatherThanInserting() = runTest {
        dao.save(DiscoveryCache(payloadJson = "{\"first\":true}"))
        dao.save(DiscoveryCache(payloadJson = "{\"second\":true}"))

        val retrieved = dao.get()

        assertEquals("{\"second\":true}", retrieved?.payloadJson)
    }
}

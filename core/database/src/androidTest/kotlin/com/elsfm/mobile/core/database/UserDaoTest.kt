package com.elsfm.mobile.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertThenGetReturnsTheStoredUser() = runTest {
        val entity = UserEntity(id = 207, username = null, name = "ELSFM APP", email = "test.elsfm@gmail.com", avatarUrl = null)

        userDao.upsert(entity)
        val result = userDao.get()

        assertEquals(entity, result)
    }

    @Test
    fun upsertReplacesThePreviousSingleCachedUser() = runTest {
        userDao.upsert(UserEntity(id = 1, username = null, name = "First", email = "a@b.com", avatarUrl = null))
        userDao.upsert(UserEntity(id = 2, username = null, name = "Second", email = "c@d.com", avatarUrl = null))

        val result = userDao.get()

        assertEquals(2, result?.id)
    }

    @Test
    fun clearRemovesTheCachedUser() = runTest {
        userDao.upsert(UserEntity(id = 1, username = null, name = "First", email = "a@b.com", avatarUrl = null))

        userDao.clear()

        assertNull(userDao.get())
    }
}

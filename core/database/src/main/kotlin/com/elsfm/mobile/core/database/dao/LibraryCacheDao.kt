package com.elsfm.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elsfm.mobile.core.database.entity.LibraryCache

@Dao
interface LibraryCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cache: LibraryCache)

    @Query("SELECT * FROM library_cache WHERE id = 0")
    suspend fun get(): LibraryCache?

    @Query("SELECT LENGTH(CAST(payloadJson AS BLOB)) FROM library_cache WHERE id = 0")
    suspend fun getSizeBytes(): Long?

    @Query("DELETE FROM library_cache")
    suspend fun clear()
}

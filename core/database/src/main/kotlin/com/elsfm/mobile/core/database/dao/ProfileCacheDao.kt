package com.elsfm.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elsfm.mobile.core.database.entity.ProfileCache

@Dao
interface ProfileCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cache: ProfileCache)

    @Query("SELECT * FROM profile_cache WHERE id = 0")
    suspend fun get(): ProfileCache?

    @Query("DELETE FROM profile_cache")
    suspend fun clear()

    @Query("SELECT LENGTH(CAST(payloadJson AS BLOB)) FROM profile_cache WHERE id = 0")
    suspend fun getSizeBytes(): Long?
}

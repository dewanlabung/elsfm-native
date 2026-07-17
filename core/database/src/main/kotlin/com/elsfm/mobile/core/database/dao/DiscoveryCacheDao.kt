package com.elsfm.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elsfm.mobile.core.database.entity.DiscoveryCache

@Dao
interface DiscoveryCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cache: DiscoveryCache)

    @Query("SELECT * FROM discovery_cache WHERE id = 0")
    suspend fun get(): DiscoveryCache?
}

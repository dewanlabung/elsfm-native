package com.elsfm.mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM cached_user LIMIT 1")
    suspend fun get(): UserEntity?

    @Query("DELETE FROM cached_user")
    suspend fun clear()
}

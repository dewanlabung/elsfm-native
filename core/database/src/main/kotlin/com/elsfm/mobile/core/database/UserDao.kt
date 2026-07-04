package com.elsfm.mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UserDao {
    @Transaction
    suspend fun upsert(user: UserEntity) {
        clear()
        insertUser(user)
    }

    @Insert
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM cached_user LIMIT 1")
    suspend fun get(): UserEntity?

    @Query("DELETE FROM cached_user")
    suspend fun clear()
}

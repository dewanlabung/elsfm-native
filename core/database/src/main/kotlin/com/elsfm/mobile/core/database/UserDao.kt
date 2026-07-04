package com.elsfm.mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class UserDao {
    @Transaction
    open suspend fun upsert(user: UserEntity) {
        clear()
        insertUser(user)
    }

    @Insert
    protected abstract suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM cached_user LIMIT 1")
    abstract suspend fun get(): UserEntity?

    @Query("DELETE FROM cached_user")
    abstract suspend fun clear()
}

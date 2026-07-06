package com.elsfm.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elsfm.mobile.core.database.entity.TokenEntity

@Dao
interface TokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: TokenEntity)

    @Query("SELECT * FROM tokens LIMIT 1")
    suspend fun getToken(): TokenEntity?

    @Query("DELETE FROM tokens")
    suspend fun clearToken()
}

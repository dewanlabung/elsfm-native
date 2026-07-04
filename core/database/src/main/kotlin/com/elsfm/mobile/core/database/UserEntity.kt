package com.elsfm.mobile.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_user")
data class UserEntity(
    @PrimaryKey val cacheKey: Int = 0,
    val id: Int,
    val username: String?,
    val name: String?,
    val email: String,
    val avatarUrl: String?,
)

package com.elsfm.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey
    val userId: Int,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

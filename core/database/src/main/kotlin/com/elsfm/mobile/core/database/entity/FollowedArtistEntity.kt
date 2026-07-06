package com.elsfm.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_artists")
data class FollowedArtistEntity(
    @PrimaryKey
    val artistId: Int,
    val followedAt: Long = System.currentTimeMillis(),
)

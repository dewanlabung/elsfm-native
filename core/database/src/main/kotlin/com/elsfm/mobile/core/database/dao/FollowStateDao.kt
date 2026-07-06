package com.elsfm.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elsfm.mobile.core.database.entity.FollowedArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowStateDao {
    @Query("SELECT EXISTS(SELECT 1 FROM followed_artists WHERE artistId = :artistId)")
    fun observeIsFollowing(artistId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_artists WHERE artistId = :artistId)")
    suspend fun isFollowing(artistId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFollowed(entity: FollowedArtistEntity)

    @Delete
    suspend fun deleteFollowed(entity: FollowedArtistEntity)

    @Query("DELETE FROM followed_artists WHERE artistId = :artistId")
    suspend fun deleteFollowedByArtistId(artistId: Int)
}

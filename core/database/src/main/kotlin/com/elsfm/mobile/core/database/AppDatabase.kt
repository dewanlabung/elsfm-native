package com.elsfm.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.dao.FollowStateDao
import com.elsfm.mobile.core.database.dao.TokenDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.entity.FollowedArtistEntity
import com.elsfm.mobile.core.database.entity.TokenEntity

@Database(
    entities = [UserEntity::class, DownloadedTrack::class, FollowedArtistEntity::class, TokenEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun downloadedTrackDao(): DownloadedTrackDao
    abstract fun followStateDao(): FollowStateDao
    abstract fun tokenDao(): TokenDao
}

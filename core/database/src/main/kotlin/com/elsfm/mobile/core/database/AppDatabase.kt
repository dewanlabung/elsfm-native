package com.elsfm.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.dao.FollowStateDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.entity.FollowedArtistEntity

@Database(
    entities = [UserEntity::class, DownloadedTrack::class, FollowedArtistEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun downloadedTrackDao(): DownloadedTrackDao
    abstract fun followStateDao(): FollowStateDao
}

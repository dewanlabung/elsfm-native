package com.elsfm.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.elsfm.mobile.core.database.dao.DiscoveryCacheDao
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.dao.FollowStateDao
import com.elsfm.mobile.core.database.dao.LibraryCacheDao
import com.elsfm.mobile.core.database.dao.PlaybackStateDao
import com.elsfm.mobile.core.database.dao.TokenDao
import com.elsfm.mobile.core.database.entity.DiscoveryCache
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.entity.FollowedArtistEntity
import com.elsfm.mobile.core.database.entity.LibraryCache
import com.elsfm.mobile.core.database.entity.PlaybackStateEntity
import com.elsfm.mobile.core.database.entity.TokenEntity

@Database(
    entities = [
        UserEntity::class,
        DownloadedTrack::class,
        FollowedArtistEntity::class,
        TokenEntity::class,
        PlaybackStateEntity::class,
        DiscoveryCache::class,
        LibraryCache::class,
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun downloadedTrackDao(): DownloadedTrackDao
    abstract fun followStateDao(): FollowStateDao
    abstract fun tokenDao(): TokenDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun discoveryCacheDao(): DiscoveryCacheDao
    abstract fun libraryCacheDao(): LibraryCacheDao
}

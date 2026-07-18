package com.elsfm.mobile.core.database.di

import android.content.Context
import androidx.room.Room
import com.elsfm.mobile.core.common.PlaybackStateStore
import com.elsfm.mobile.core.database.AppDatabase
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.dao.DiscoveryCacheDao
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.dao.FollowStateDao
import com.elsfm.mobile.core.database.dao.LibraryCacheDao
import com.elsfm.mobile.core.database.dao.PlaybackStateDao
import com.elsfm.mobile.core.database.repository.RoomPlaybackStateStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "elsfm.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideDownloadedTrackDao(database: AppDatabase): DownloadedTrackDao = database.downloadedTrackDao()

    @Provides
    fun provideFollowStateDao(database: AppDatabase): FollowStateDao = database.followStateDao()

    @Provides
    fun providePlaybackStateDao(database: AppDatabase): PlaybackStateDao = database.playbackStateDao()

    @Provides
    fun provideDiscoveryCacheDao(database: AppDatabase): DiscoveryCacheDao = database.discoveryCacheDao()

    @Provides
    fun provideLibraryCacheDao(database: AppDatabase): LibraryCacheDao = database.libraryCacheDao()

    @Provides
    @Singleton
    fun providePlaybackStateStore(dao: PlaybackStateDao): PlaybackStateStore = RoomPlaybackStateStore(dao)
}

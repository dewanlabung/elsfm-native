package com.elsfm.mobile.core.database.di

import android.content.Context
import androidx.room.Room
import com.elsfm.mobile.core.database.AppDatabase
import com.elsfm.mobile.core.database.UserDao
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
        return Room.databaseBuilder(context, AppDatabase::class.java, "elsfm.db").build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}

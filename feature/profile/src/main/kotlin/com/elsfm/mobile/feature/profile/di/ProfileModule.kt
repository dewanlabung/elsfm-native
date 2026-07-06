package com.elsfm.mobile.feature.profile.di

import com.elsfm.mobile.feature.profile.ThemePreferences
import com.elsfm.mobile.feature.profile.ThemeStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds
    @Singleton
    abstract fun bindThemeStore(impl: ThemePreferences): ThemeStore
}

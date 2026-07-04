package com.elsfm.mobile.feature.player.di

import com.elsfm.mobile.feature.player.Media3PlayerController
import com.elsfm.mobile.feature.player.PlayerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: Media3PlayerController): PlayerController
}

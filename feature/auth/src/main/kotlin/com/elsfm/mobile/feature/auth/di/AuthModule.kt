package com.elsfm.mobile.feature.auth.di

import com.elsfm.mobile.feature.auth.data.GoogleSignInService
import com.elsfm.mobile.feature.auth.data.GoogleSignInServiceLike
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindGoogleSignInService(impl: GoogleSignInService): GoogleSignInServiceLike
}

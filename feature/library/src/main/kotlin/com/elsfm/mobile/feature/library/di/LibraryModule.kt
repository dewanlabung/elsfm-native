package com.elsfm.mobile.feature.library.di

import com.elsfm.mobile.feature.library.data.LibraryApiRepository
import com.elsfm.mobile.feature.library.data.LibraryApiRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class LibraryModule {
    @Binds
    abstract fun bindLibraryRepository(
        impl: LibraryApiRepositoryImpl,
    ): LibraryApiRepository
}

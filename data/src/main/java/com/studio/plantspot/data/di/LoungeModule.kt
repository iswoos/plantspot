package com.studio.plantspot.data.di

import com.studio.plantspot.data.repository.LoungeRepositoryImpl
import com.studio.plantspot.domain.repository.LoungeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoungeModule {

    @Binds
    @Singleton
    abstract fun bindLoungeRepository(
        impl: LoungeRepositoryImpl
    ): LoungeRepository
}

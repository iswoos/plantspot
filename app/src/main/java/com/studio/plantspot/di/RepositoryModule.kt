package com.studio.plantspot.di

import com.studio.plantspot.data.repository.DiagnosisRepositoryImpl
import com.studio.plantspot.data.repository.FileRepositoryImpl
import com.studio.plantspot.domain.repository.DiagnosisRepository
import com.studio.plantspot.domain.repository.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDiagnosisRepository(
        impl: DiagnosisRepositoryImpl
    ): DiagnosisRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        impl: FileRepositoryImpl
    ): FileRepository
}

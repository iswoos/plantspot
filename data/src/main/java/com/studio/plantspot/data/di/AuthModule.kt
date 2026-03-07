package com.studio.plantspot.data.di

import com.studio.plantspot.data.repository.AuthRepositoryImpl
import com.studio.plantspot.data.repository.DiagnosisRepositoryImpl
import com.studio.plantspot.domain.repository.AuthRepository
import com.studio.plantspot.domain.repository.DiagnosisRepository
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
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDiagnosisRepository(
        diagnosisRepositoryImpl: DiagnosisRepositoryImpl
    ): DiagnosisRepository
}

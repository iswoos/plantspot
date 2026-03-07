package com.studio.plantspot.data.di

import com.studio.plantspot.data.repository.PlantRepositoryImpl
import com.studio.plantspot.domain.repository.PlantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlantModule {

    @Binds
    @Singleton
    abstract fun bindPlantRepository(
        impl: PlantRepositoryImpl
    ): PlantRepository
}

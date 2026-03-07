package com.studio.plantspot.di

import com.studio.plantspot.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @OptIn(SupabaseInternal::class)
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Functions)
            
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000
                }
            }
        }
    }

    @Provides
    @Singleton
    @Named("googleWebClientId")
    fun provideGoogleWebClientId(): String {
        return BuildConfig.GOOGLE_WEB_CLIENT_ID
    }
}

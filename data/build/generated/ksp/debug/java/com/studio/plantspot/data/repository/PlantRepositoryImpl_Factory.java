package com.studio.plantspot.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class PlantRepositoryImpl_Factory implements Factory<PlantRepositoryImpl> {
  private final Provider<SupabaseClient> supabaseProvider;

  public PlantRepositoryImpl_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public PlantRepositoryImpl get() {
    return newInstance(supabaseProvider.get());
  }

  public static PlantRepositoryImpl_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new PlantRepositoryImpl_Factory(supabaseProvider);
  }

  public static PlantRepositoryImpl newInstance(SupabaseClient supabase) {
    return new PlantRepositoryImpl(supabase);
  }
}

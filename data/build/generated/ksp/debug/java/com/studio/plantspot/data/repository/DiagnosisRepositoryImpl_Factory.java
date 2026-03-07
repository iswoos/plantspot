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
public final class DiagnosisRepositoryImpl_Factory implements Factory<DiagnosisRepositoryImpl> {
  private final Provider<SupabaseClient> supabaseProvider;

  public DiagnosisRepositoryImpl_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public DiagnosisRepositoryImpl get() {
    return newInstance(supabaseProvider.get());
  }

  public static DiagnosisRepositoryImpl_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new DiagnosisRepositoryImpl_Factory(supabaseProvider);
  }

  public static DiagnosisRepositoryImpl newInstance(SupabaseClient supabase) {
    return new DiagnosisRepositoryImpl(supabase);
  }
}

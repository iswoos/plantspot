package com.studio.plantspot.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<SupabaseClient> supabaseProvider;

  private final Provider<Context> contextProvider;

  public AuthRepositoryImpl_Factory(Provider<SupabaseClient> supabaseProvider,
      Provider<Context> contextProvider) {
    this.supabaseProvider = supabaseProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(supabaseProvider.get(), contextProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<SupabaseClient> supabaseProvider,
      Provider<Context> contextProvider) {
    return new AuthRepositoryImpl_Factory(supabaseProvider, contextProvider);
  }

  public static AuthRepositoryImpl newInstance(SupabaseClient supabase, Context context) {
    return new AuthRepositoryImpl(supabase, context);
  }
}

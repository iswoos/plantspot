package com.studio.plantspot.presentation.ui.auth;

import com.studio.plantspot.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("javax.inject.Named")
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<String> googleWebClientIdProvider;

  public AuthViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<String> googleWebClientIdProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.googleWebClientIdProvider = googleWebClientIdProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(authRepositoryProvider.get(), googleWebClientIdProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<String> googleWebClientIdProvider) {
    return new AuthViewModel_Factory(authRepositoryProvider, googleWebClientIdProvider);
  }

  public static AuthViewModel newInstance(AuthRepository authRepository, String googleWebClientId) {
    return new AuthViewModel(authRepository, googleWebClientId);
  }
}

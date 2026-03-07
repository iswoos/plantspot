package com.studio.plantspot.presentation.ui.diagnosis;

import android.content.Context;
import com.studio.plantspot.domain.repository.DiagnosisRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class DiagnosisViewModel_Factory implements Factory<DiagnosisViewModel> {
  private final Provider<DiagnosisRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public DiagnosisViewModel_Factory(Provider<DiagnosisRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public DiagnosisViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static DiagnosisViewModel_Factory create(Provider<DiagnosisRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new DiagnosisViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static DiagnosisViewModel newInstance(DiagnosisRepository repository, Context context) {
    return new DiagnosisViewModel(repository, context);
  }
}

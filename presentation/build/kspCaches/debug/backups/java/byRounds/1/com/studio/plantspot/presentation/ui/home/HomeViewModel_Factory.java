package com.studio.plantspot.presentation.ui.home;

import com.studio.plantspot.domain.repository.PlantRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<PlantRepository> plantRepositoryProvider;

  public HomeViewModel_Factory(Provider<PlantRepository> plantRepositoryProvider) {
    this.plantRepositoryProvider = plantRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(plantRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<PlantRepository> plantRepositoryProvider) {
    return new HomeViewModel_Factory(plantRepositoryProvider);
  }

  public static HomeViewModel newInstance(PlantRepository plantRepository) {
    return new HomeViewModel(plantRepository);
  }
}

package com.nikhil.f1tracker.di

import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.data.repository.F1RepositoryImpl
import com.nikhil.f1tracker.data.repository.FavoritesRepository
import com.nikhil.f1tracker.data.repository.FavoritesRepositoryImpl
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
    abstract fun bindF1Repository(impl: F1RepositoryImpl): F1Repository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
}

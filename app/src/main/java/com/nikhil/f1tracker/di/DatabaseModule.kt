package com.nikhil.f1tracker.di

import android.content.Context
import androidx.room.Room
import com.nikhil.f1tracker.data.local.F1Database
import com.nikhil.f1tracker.data.local.dao.CircuitDao
import com.nikhil.f1tracker.data.local.dao.ConstructorDao
import com.nikhil.f1tracker.data.local.dao.ConstructorStandingDao
import com.nikhil.f1tracker.data.local.dao.DriverDao
import com.nikhil.f1tracker.data.local.dao.DriverStandingDao
import com.nikhil.f1tracker.data.local.dao.RaceDao
import com.nikhil.f1tracker.data.local.dao.ResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideF1Database(@ApplicationContext context: Context): F1Database =
        Room.databaseBuilder(context, F1Database::class.java, F1Database.DATABASE_NAME)
            // Room's cache holds only re-fetchable network data (favorites live in DataStore),
            // so destructive fallback is fine while the schema is still actively changing.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideDriverDao(database: F1Database): DriverDao = database.driverDao()

    @Provides
    fun provideConstructorDao(database: F1Database): ConstructorDao = database.constructorDao()

    @Provides
    fun provideCircuitDao(database: F1Database): CircuitDao = database.circuitDao()

    @Provides
    fun provideRaceDao(database: F1Database): RaceDao = database.raceDao()

    @Provides
    fun provideResultDao(database: F1Database): ResultDao = database.resultDao()

    @Provides
    fun provideDriverStandingDao(database: F1Database): DriverStandingDao = database.driverStandingDao()

    @Provides
    fun provideConstructorStandingDao(database: F1Database): ConstructorStandingDao =
        database.constructorStandingDao()
}

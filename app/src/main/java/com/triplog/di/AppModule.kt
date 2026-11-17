package com.triplog.di

import android.content.Context
import androidx.room.Room
import com.triplog.data.local.ALL_MIGRATIONS
import com.triplog.data.local.AppDatabase
import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.dao.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()

    @Provides
    fun provideFuelExpenseDao(db: AppDatabase): FuelExpenseDao = db.fuelExpenseDao()
}

package com.trueskies.android.di

import android.content.Context
import androidx.room.Room
import com.trueskies.android.data.local.TrueSkiesDatabase
import com.trueskies.android.data.local.dao.FlightDao
import com.trueskies.android.data.local.dao.FlightEventDao
import com.trueskies.android.data.local.dao.PersonalFlightDao
import com.trueskies.android.data.local.dao.SharedFlightDao
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
    fun provideDatabase(@ApplicationContext context: Context): TrueSkiesDatabase {
        return Room.databaseBuilder(
            context,
            TrueSkiesDatabase::class.java,
            "trueskies_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideFlightDao(database: TrueSkiesDatabase): FlightDao {
        return database.flightDao()
    }

    @Provides
    fun providePersonalFlightDao(database: TrueSkiesDatabase): PersonalFlightDao {
        return database.personalFlightDao()
    }

    @Provides
    fun provideSharedFlightDao(database: TrueSkiesDatabase): SharedFlightDao {
        return database.sharedFlightDao()
    }

    @Provides
    fun provideFlightEventDao(database: TrueSkiesDatabase): FlightEventDao {
        return database.flightEventDao()
    }
}

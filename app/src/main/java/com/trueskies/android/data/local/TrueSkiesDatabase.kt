package com.trueskies.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trueskies.android.data.local.dao.FlightDao
import com.trueskies.android.data.local.dao.PersonalFlightDao
import com.trueskies.android.data.local.entities.FlightEntity
import com.trueskies.android.data.local.entities.PersonalFlightEntity

@Database(
    entities = [FlightEntity::class, PersonalFlightEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TrueSkiesDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun personalFlightDao(): PersonalFlightDao
}

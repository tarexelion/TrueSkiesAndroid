package com.trueskies.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trueskies.android.data.local.dao.FlightDao
import com.trueskies.android.data.local.dao.FlightEventDao
import com.trueskies.android.data.local.dao.PersonalFlightDao
import com.trueskies.android.data.local.dao.SharedFlightDao
import com.trueskies.android.data.local.entities.FlightEntity
import com.trueskies.android.data.local.entities.FlightEventEntity
import com.trueskies.android.data.local.entities.PersonalFlightEntity
import com.trueskies.android.data.local.entities.SharedFlightEntity

@Database(
    entities = [
        FlightEntity::class,
        PersonalFlightEntity::class,
        SharedFlightEntity::class,
        FlightEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TrueSkiesDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun personalFlightDao(): PersonalFlightDao
    abstract fun sharedFlightDao(): SharedFlightDao
    abstract fun flightEventDao(): FlightEventDao
}

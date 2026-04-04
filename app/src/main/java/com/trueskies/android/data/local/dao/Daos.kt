package com.trueskies.android.data.local.dao

import androidx.room.*
import com.trueskies.android.data.local.entities.FlightEntity
import com.trueskies.android.data.local.entities.FlightEventEntity
import com.trueskies.android.data.local.entities.PersonalFlightEntity
import com.trueskies.android.data.local.entities.SharedFlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Query("SELECT * FROM flights WHERE flightNumber LIKE '%' || :query || '%' OR originCode LIKE '%' || :query || '%' OR destinationCode LIKE '%' || :query || '%' ORDER BY scheduledDeparture DESC LIMIT :limit")
    suspend fun searchFlights(query: String, limit: Int = 50): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE id = :id")
    suspend fun getFlightById(id: String): FlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlights(flights: List<FlightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity)

    @Query("DELETE FROM flights WHERE lastFetched < :olderThan")
    suspend fun deleteOldFlights(olderThan: Long)

    @Query("DELETE FROM flights")
    suspend fun deleteAllFlights()
}

@Dao
interface PersonalFlightDao {

    @Query("SELECT * FROM personal_flights ORDER BY addedAt DESC")
    fun getAllPersonalFlights(): Flow<List<PersonalFlightEntity>>

    @Query("SELECT * FROM personal_flights WHERE localId = :localId")
    suspend fun getPersonalFlight(localId: String): PersonalFlightEntity?

    @Query("SELECT * FROM personal_flights WHERE flightId = :flightId")
    suspend fun getPersonalFlightByFlightId(flightId: String): PersonalFlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalFlight(flight: PersonalFlightEntity)

    @Update
    suspend fun updatePersonalFlight(flight: PersonalFlightEntity)

    @Delete
    suspend fun deletePersonalFlight(flight: PersonalFlightEntity)

    @Query("DELETE FROM personal_flights WHERE localId = :localId")
    suspend fun deletePersonalFlightById(localId: String)

    @Query("SELECT COUNT(*) FROM personal_flights")
    suspend fun getPersonalFlightCount(): Int

    @Query("SELECT * FROM personal_flights ORDER BY addedAt DESC")
    suspend fun getAllPersonalFlightsSnapshot(): List<PersonalFlightEntity>
}

@Dao
interface SharedFlightDao {

    @Query("SELECT * FROM shared_flights WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveSharedFlights(): Flow<List<SharedFlightEntity>>

    @Query("SELECT * FROM shared_flights WHERE shareCode = :shareCode")
    suspend fun getSharedFlightByCode(shareCode: String): SharedFlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedFlight(flight: SharedFlightEntity)

    @Query("UPDATE shared_flights SET isActive = 0 WHERE shareCode = :shareCode")
    suspend fun deactivateSharedFlight(shareCode: String)

    @Query("DELETE FROM shared_flights WHERE id = :id")
    suspend fun deleteSharedFlight(id: String)

    @Query("DELETE FROM shared_flights")
    suspend fun deleteAllSharedFlights()

    @Query("SELECT * FROM shared_flights ORDER BY createdAt DESC")
    suspend fun getAllSharedFlightsSnapshot(): List<SharedFlightEntity>
}

@Dao
interface FlightEventDao {

    @Query("SELECT * FROM flight_events WHERE flightId = :flightId ORDER BY timestamp DESC")
    fun getEventsForFlight(flightId: String): Flow<List<FlightEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FlightEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<FlightEventEntity>)

    @Query("DELETE FROM flight_events WHERE flightId = :flightId")
    suspend fun deleteEventsForFlight(flightId: String)
}

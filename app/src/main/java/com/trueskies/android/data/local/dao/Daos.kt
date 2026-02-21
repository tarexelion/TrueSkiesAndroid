package com.trueskies.android.data.local.dao

import androidx.room.*
import com.trueskies.android.data.local.entities.FlightEntity
import com.trueskies.android.data.local.entities.PersonalFlightEntity
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
}

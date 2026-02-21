package com.trueskies.android.data.repository

import android.util.Log
import com.trueskies.android.data.local.dao.FlightDao
import com.trueskies.android.data.local.dao.PersonalFlightDao
import com.trueskies.android.data.local.entities.FlightEntity
import com.trueskies.android.data.local.entities.PersonalFlightEntity
import com.trueskies.android.data.remote.api.TrueSkiesApi
import com.trueskies.android.data.remote.models.BackendFlight
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.PersonalFlight
import com.trueskies.android.domain.models.SeatClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flight repository — combines remote API and local cache.
 * Ported from iOS TrueSkiesBackendService + PersonalFlightManager.
 */
@Singleton
class FlightRepository @Inject constructor(
    private val api: TrueSkiesApi,
    private val flightDao: FlightDao,
    private val personalFlightDao: PersonalFlightDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    companion object {
        private const val TAG = "FlightRepository"
    }

    // ── Search ──

    suspend fun searchFlights(query: String, date: String? = null): Result<List<Flight>> {
        return try {
            val response = api.searchFlights(query = query, date = date)
            if (response.isSuccessful) {
                val body = response.body()
                val flights = body?.flights?.map { Flight.fromBackend(it) } ?: emptyList()
                // Cache results
                cacheFlights(body?.flights ?: emptyList())
                Result.success(flights)
            } else {
                Log.w(TAG, "Search failed: ${response.code()} ${response.message()}")
                // Try enhanced search as fallback
                searchEnhancedFlights(query, date)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            // Fallback to local cache
            val cached = flightDao.searchFlights(query)
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomainFlight() })
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun searchEnhancedFlights(query: String, date: String? = null): Result<List<Flight>> {
        return try {
            val response = api.searchEnhancedFlights(query = query, date = date)
            if (response.isSuccessful) {
                val flights = response.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList()
                Result.success(flights)
            } else {
                Result.failure(Exception("Enhanced search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Flight Details ──

    suspend fun getFlightDetails(flightId: String, date: String? = null): Result<Flight> {
        return try {
            val response = api.getFlightDetails(flightId = flightId, date = date)
            if (response.isSuccessful) {
                val backendFlight = response.body()?.resolvedFlight
                if (backendFlight != null) {
                    Result.success(Flight.fromBackend(backendFlight))
                } else {
                    Result.failure(Exception("No flight data in response"))
                }
            } else {
                Result.failure(Exception("Details failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Flight details error", e)
            Result.failure(e)
        }
    }

    // ── Live Flights (for map) ──

    suspend fun getLiveFlights(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        limit: Int = 100
    ): Result<List<Flight>> {
        return try {
            // Try enhanced area flights first
            val response = api.getEnhancedAreaFlights(
                minLat = minLat, maxLat = maxLat,
                minLon = minLon, maxLon = maxLon,
                limit = limit
            )
            if (response.isSuccessful) {
                val flights = response.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList()
                Result.success(flights)
            } else {
                // Fallback to db-flights
                val dbResponse = api.getDbFlightsByBounds(
                    minLat = minLat, maxLat = maxLat,
                    minLon = minLon, maxLon = maxLon,
                    limit = limit
                )
                if (dbResponse.isSuccessful) {
                    val flights = dbResponse.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList()
                    Result.success(flights)
                } else {
                    Result.failure(Exception("Live flights failed: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Live flights error", e)
            Result.failure(e)
        }
    }

    // ── Health Check ──

    suspend fun checkHealth(): Boolean {
        return try {
            val response = api.healthCheck()
            response.isSuccessful && (response.body()?.success == true)
        } catch (e: Exception) {
            false
        }
    }

    // ── Personal Flights ──

    fun getPersonalFlightsFlow(): Flow<List<PersonalFlight>> {
        return personalFlightDao.getAllPersonalFlights().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    entity.toDomainPersonalFlight(json)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to convert personal flight ${entity.localId}", e)
                    null
                }
            }
        }
    }

    suspend fun addPersonalFlight(flight: Flight, notes: String? = null, seatNumber: String? = null): PersonalFlight {
        val personalFlight = PersonalFlight(
            flight = flight,
            notes = notes,
            seatNumber = seatNumber
        )
        val backendFlight = createBackendFlightFromDomain(flight)
        val entity = PersonalFlightEntity(
            localId = personalFlight.localId,
            flightId = flight.id,
            flightNumber = flight.flightNumber,
            originCode = flight.originCode,
            destinationCode = flight.destinationCode,
            airlineName = flight.airlineName,
            airlineIata = flight.airlineIata,
            status = flight.rawStatus,
            scheduledDeparture = flight.scheduledDeparture,
            scheduledArrival = flight.scheduledArrival,
            notes = notes,
            seatNumber = seatNumber,
            addedAt = personalFlight.addedAt,
            lastUpdated = personalFlight.lastUpdated,
            flightJson = json.encodeToString(backendFlight)
        )
        personalFlightDao.insertPersonalFlight(entity)
        return personalFlight
    }

    suspend fun deletePersonalFlight(localId: String) {
        personalFlightDao.deletePersonalFlightById(localId)
    }

    suspend fun refreshPersonalFlight(personalFlight: PersonalFlight): Result<PersonalFlight> {
        return try {
            val result = getFlightDetails(personalFlight.flight.id)
            result.map { updatedFlight ->
                personalFlight.copy(
                    flight = updatedFlight,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Private Helpers ──

    private suspend fun cacheFlights(flights: List<BackendFlight>) {
        val entities = flights.map { bf ->
            FlightEntity(
                id = bf.id,
                flightNumber = bf.flightNumber,
                originCode = bf.origin?.resolvedCode ?: "",
                destinationCode = bf.destination?.resolvedCode ?: "",
                airlineName = bf.airline?.name,
                airlineIata = bf.airline?.iata,
                status = bf.status,
                scheduledDeparture = bf.departure?.scheduled ?: bf.schedule?.departure?.scheduled,
                scheduledArrival = bf.arrival?.scheduled ?: bf.schedule?.arrival?.scheduled,
                latitude = bf.position?.latitude,
                longitude = bf.position?.longitude
            )
        }
        flightDao.insertFlights(entities)
    }

    private fun createBackendFlightFromDomain(flight: Flight): BackendFlight {
        return BackendFlight(
            id = flight.id,
            flightNumber = flight.flightNumber,
            status = flight.rawStatus,
            origin = com.trueskies.android.data.remote.models.BackendAirport(
                code = flight.originCode, name = flight.originName, city = flight.originCity,
                country = flight.originCountry, latitude = flight.originLat, longitude = flight.originLon,
                timezone = flight.originTimezone
            ),
            destination = com.trueskies.android.data.remote.models.BackendAirport(
                code = flight.destinationCode, name = flight.destinationName, city = flight.destinationCity,
                country = flight.destinationCountry, latitude = flight.destinationLat, longitude = flight.destinationLon,
                timezone = flight.destinationTimezone
            ),
            departure = com.trueskies.android.data.remote.models.BackendFlightTime(
                scheduled = flight.scheduledDeparture, estimated = flight.estimatedDeparture, actual = flight.actualDeparture
            ),
            arrival = com.trueskies.android.data.remote.models.BackendFlightTime(
                scheduled = flight.scheduledArrival, estimated = flight.estimatedArrival, actual = flight.actualArrival
            )
        )
    }
}

// ── Extension ──

private fun FlightEntity.toDomainFlight(): Flight {
    return Flight(
        id = id,
        flightNumber = flightNumber,
        displayFlightNumber = flightNumber.replace(Regex("([A-Z]{2,3})(\\d+)"), "$1 $2"),
        originCode = originCode,
        destinationCode = destinationCode,
        airlineName = airlineName,
        airlineIata = airlineIata,
        rawStatus = status,
        scheduledDeparture = scheduledDeparture,
        scheduledArrival = scheduledArrival,
        latitude = latitude,
        longitude = longitude
    )
}

private fun PersonalFlightEntity.toDomainPersonalFlight(json: Json): PersonalFlight {
    val flight = if (flightJson != null) {
        try {
            val bf = json.decodeFromString<BackendFlight>(flightJson)
            Flight.fromBackend(bf)
        } catch (e: Exception) {
            // Fallback: construct from entity fields
            Flight(
                id = flightId,
                flightNumber = flightNumber,
                displayFlightNumber = flightNumber.replace(Regex("([A-Z]{2,3})(\\d+)"), "$1 $2"),
                originCode = originCode,
                destinationCode = destinationCode,
                airlineName = airlineName,
                airlineIata = airlineIata,
                rawStatus = status,
                scheduledDeparture = scheduledDeparture,
                scheduledArrival = scheduledArrival
            )
        }
    } else {
        Flight(
            id = flightId,
            flightNumber = flightNumber,
            displayFlightNumber = flightNumber.replace(Regex("([A-Z]{2,3})(\\d+)"), "$1 $2"),
            originCode = originCode,
            destinationCode = destinationCode,
            airlineName = airlineName,
            airlineIata = airlineIata,
            rawStatus = status,
            scheduledDeparture = scheduledDeparture,
            scheduledArrival = scheduledArrival
        )
    }

    return PersonalFlight(
        localId = localId,
        flight = flight,
        notes = notes,
        seatNumber = seatNumber,
        seatClass = seatClass?.let { sc -> SeatClass.entries.find { it.name == sc } },
        bookingReference = bookingReference,
        customName = customName,
        isNotificationsEnabled = isNotificationsEnabled,
        addedAt = addedAt,
        lastUpdated = lastUpdated
    )
}

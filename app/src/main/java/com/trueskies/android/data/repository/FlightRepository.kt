package com.trueskies.android.data.repository

import android.util.Log
import com.trueskies.android.data.local.dao.FlightDao
import com.trueskies.android.data.local.dao.FlightEventDao
import com.trueskies.android.data.local.dao.PersonalFlightDao
import com.trueskies.android.data.local.dao.SharedFlightDao
import com.trueskies.android.data.local.entities.FlightEntity
import com.trueskies.android.data.local.entities.FlightEventEntity
import com.trueskies.android.data.local.entities.PersonalFlightEntity
import com.trueskies.android.data.local.entities.SharedFlightEntity
import com.trueskies.android.data.remote.api.TrueSkiesApi
import com.trueskies.android.data.remote.models.BackendFlight
import com.trueskies.android.data.remote.models.BackendJoinRequest
import com.trueskies.android.data.remote.models.BackendSharePermissions
import com.trueskies.android.data.remote.models.BackendShareRequest
import com.trueskies.android.data.remote.models.BackendShareUser
import com.trueskies.android.domain.models.*
import com.trueskies.android.util.AirlineCodeRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flight repository — combines remote API and local cache.
 * Ported from iOS TrueSkiesBackendService + PersonalFlightManager + FriendsAPIService.
 */
@Singleton
class FlightRepository @Inject constructor(
    private val api: TrueSkiesApi,
    private val flightDao: FlightDao,
    private val personalFlightDao: PersonalFlightDao,
    private val sharedFlightDao: SharedFlightDao,
    private val flightEventDao: FlightEventDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    companion object {
        private const val TAG = "FlightRepository"
    }

    // ── Search ──

    suspend fun searchFlights(query: String, date: String? = null): Result<List<Flight>> {
        // Generate ICAO/IATA variants (e.g. "WMT5YV" → ["WMT5YV", "W45YV", "WMT5", "W45"])
        val candidates = AirlineCodeRegistry.searchCandidates(query)
        Log.d(TAG, "Search candidates for '$query': $candidates")

        return try {
            // iOS approach: use enhanced search endpoint as primary (handles ATC callsigns etc.)
            // Try each candidate until we get results
            for (candidate in candidates) {
                Log.d(TAG, "Trying enhanced search for '$candidate'")
                val enhancedResult = searchEnhancedFlights(candidate, date)
                val flights = enhancedResult.getOrNull()
                if (!flights.isNullOrEmpty()) {
                    Log.d(TAG, "Enhanced search hit for '$candidate': ${flights.size} results")
                    return Result.success(flights)
                }
            }

            // Fallback: try schedules endpoint (returns complete airport data)
            for (candidate in candidates) {
                val scheduleFlights = trySchedulesSearch(candidate, date)
                if (scheduleFlights.isNotEmpty()) {
                    Log.d(TAG, "Schedules hit for '$candidate': ${scheduleFlights.size} results")
                    return Result.success(scheduleFlights)
                }
            }

            // Fallback: try basic search endpoint
            for (candidate in candidates) {
                val response = api.searchFlights(query = candidate, date = date)
                if (response.isSuccessful) {
                    val body = response.body()
                    val flights = body?.flights?.map { Flight.fromBackend(it) } ?: emptyList()
                    if (flights.isNotEmpty()) {
                        cacheFlights(body?.flights ?: emptyList())
                        return Result.success(flights)
                    }
                }
            }

            Log.w(TAG, "All candidates empty for '$query'")
            Result.success(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            val cached = flightDao.searchFlights(query)
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomainFlight() })
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Try the schedules endpoint first (like iOS PersonalFlightManager.findMatchingFlights).
     * This endpoint returns complete airport data including codes, names, cities, coordinates.
     */
    private suspend fun trySchedulesSearch(query: String, date: String?): List<Flight> {
        // Only try schedules if query looks like a flight number
        // iOS pattern: airline prefix (2-3 chars) + digits (1-5) + optional ATC suffix (0-3 letters)
        val trimmed = query.trim().uppercase()
        if (!trimmed.matches(Regex("^([A-Z]{2,3}|[A-Z][0-9]|[0-9][A-Z])\\s*\\d{1,5}[A-Z]{0,3}$"))) return emptyList()

        // Strip spaces for the API call (e.g., "PGT 7062" → "PGT7062")
        val flightNumber = trimmed.replace("\\s+".toRegex(), "")

        return try {
            val response = api.getFlightSchedules(
                flightNumber = flightNumber,
                date = date,
                maxPages = 2
            )
            if (response.isSuccessful) {
                val flights = response.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList()
                Log.d(TAG, "Schedules search for '$flightNumber': ${flights.size} results")
                flights
            } else {
                Log.d(TAG, "Schedules search failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Schedules search error", e)
            emptyList()
        }
    }


    suspend fun searchEnhancedFlights(
        query: String,
        date: String? = null,
        quick: Boolean? = null
    ): Result<List<Flight>> {
        return try {
            val response = api.searchEnhancedFlights(query = query, date = date, quick = quick)
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

    suspend fun getFlightDetails(
        flightId: String,
        date: String? = null,
        origin: String? = null,
        destination: String? = null
    ): Result<Flight> {
        return try {
            var flight: Flight? = null

            val response = api.getFlightDetails(flightId = flightId, date = date)
            if (response.isSuccessful) {
                val backendFlight = response.body()?.resolvedFlight
                if (backendFlight != null) {
                    flight = Flight.fromBackend(backendFlight)
                }
            }

            // Try enhanced if basic failed or returned incomplete data (??? codes)
            if (flight == null || flight.destinationCode == "???" || flight.originCode == "???") {
                try {
                    val enhancedResponse = api.getEnhancedFlightDetails(
                        faFlightId = flightId, origin = origin, destination = destination, date = date
                    )
                    if (enhancedResponse.isSuccessful) {
                        val bf = enhancedResponse.body()?.resolvedFlight
                        if (bf != null) {
                            val enhanced = Flight.fromBackend(bf)
                            flight = if (flight != null) {
                                // Merge: prefer enhanced data for missing fields
                                flight.copy(
                                    originCode = if (flight.originCode == "???") enhanced.originCode else flight.originCode,
                                    originName = flight.originName ?: enhanced.originName,
                                    originCity = flight.originCity ?: enhanced.originCity,
                                    originCountry = flight.originCountry ?: enhanced.originCountry,
                                    originLat = flight.originLat ?: enhanced.originLat,
                                    originLon = flight.originLon ?: enhanced.originLon,
                                    originTimezone = flight.originTimezone ?: enhanced.originTimezone,
                                    destinationCode = if (flight.destinationCode == "???") enhanced.destinationCode else flight.destinationCode,
                                    destinationName = flight.destinationName ?: enhanced.destinationName,
                                    destinationCity = flight.destinationCity ?: enhanced.destinationCity,
                                    destinationCountry = flight.destinationCountry ?: enhanced.destinationCountry,
                                    destinationLat = flight.destinationLat ?: enhanced.destinationLat,
                                    destinationLon = flight.destinationLon ?: enhanced.destinationLon,
                                    destinationTimezone = flight.destinationTimezone ?: enhanced.destinationTimezone,
                                    scheduledArrival = flight.scheduledArrival ?: enhanced.scheduledArrival,
                                    estimatedArrival = flight.estimatedArrival ?: enhanced.estimatedArrival,
                                    arrivalGate = flight.arrivalGate ?: enhanced.arrivalGate,
                                    arrivalTerminal = flight.arrivalTerminal ?: enhanced.arrivalTerminal
                                )
                            } else {
                                enhanced
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Enhanced details fallback failed", e)
                }
            }

            if (flight != null) {
                Result.success(flight)
            } else {
                Result.failure(Exception("No flight data in response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Flight details error", e)
            Result.failure(e)
        }
    }

    // ── Flight Position (iOS lightweight position endpoint) ──

    suspend fun getFlightPosition(faFlightId: String, maxStale: Int? = null): Result<Flight> {
        return try {
            val response = api.getFlightPosition(faFlightId, maxStale = maxStale)
            if (response.isSuccessful) {
                val pos = response.body()?.position
                if (pos != null) {
                    // Return a minimal flight with just position data
                    Result.success(
                        Flight(
                            id = pos.faFlightId ?: faFlightId,
                            flightNumber = faFlightId,
                            displayFlightNumber = faFlightId,
                            originCode = "",
                            destinationCode = "",
                            latitude = pos.latitude,
                            longitude = pos.longitude,
                            altitude = pos.altitude?.toDouble(),
                            heading = pos.heading?.toDouble(),
                            speed = pos.groundspeed?.toDouble(),
                            positionTimestamp = pos.timestamp
                        )
                    )
                } else {
                    Result.failure(Exception("No position data"))
                }
            } else {
                Result.failure(Exception("Position failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Flight Track (actual path traveled) ──

    suspend fun getFlightTrack(faFlightId: String): Result<List<TrackPoint>> {
        return try {
            val response = api.getFlightTrack(faFlightId)
            if (response.isSuccessful) {
                val positions = response.body()?.track?.positions
                if (!positions.isNullOrEmpty()) {
                    Result.success(positions.map {
                        TrackPoint(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            altitude = it.altitude?.toDouble(),
                            timestamp = it.timestamp
                        )
                    })
                } else {
                    Result.failure(Exception("No track data"))
                }
            } else {
                Result.failure(Exception("Track failed: ${response.code()}"))
            }
        } catch (e: Exception) {
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
            val response = api.getEnhancedAreaFlights(
                minLat = minLat, maxLat = maxLat,
                minLon = minLon, maxLon = maxLon,
                limit = limit
            )
            if (response.isSuccessful) {
                val flights = response.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList()
                Result.success(flights)
            } else {
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

    // ── Airport Operations (iOS) ──

    suspend fun getAirportDepartures(airportCode: String, limit: Int? = null): Result<List<Flight>> {
        return try {
            val response = api.getAirportDepartures(airportCode, limit = limit)
            if (response.isSuccessful) {
                Result.success(response.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList())
            } else {
                Result.failure(Exception("Airport departures failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAirportArrivals(airportCode: String, limit: Int? = null): Result<List<Flight>> {
        return try {
            val response = api.getAirportArrivals(airportCode, limit = limit)
            if (response.isSuccessful) {
                Result.success(response.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList())
            } else {
                Result.failure(Exception("Airport arrivals failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Schedules (iOS: 1yr future, 3mo past) ──

    suspend fun getFlightSchedules(
        flightNumber: String,
        start: String? = null,
        end: String? = null
    ): Result<List<Flight>> {
        return try {
            val response = api.getFlightSchedules(flightNumber, start = start, end = end)
            if (response.isSuccessful) {
                Result.success(response.body()?.flights?.map { Flight.fromBackend(it) } ?: emptyList())
            } else {
                Result.failure(Exception("Schedules failed: ${response.code()}"))
            }
        } catch (e: Exception) {
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

    suspend fun addPersonalFlight(
        flight: Flight,
        notes: String? = null,
        seatNumber: String? = null,
        seatClass: SeatClass? = null,
        bookingReference: String? = null
    ): PersonalFlight {
        // If origin or destination coordinates are missing, try to enrich from API details
        val enrichedFlight = if (flight.originLat == null || flight.originLon == null ||
            flight.destinationLat == null || flight.destinationLon == null) {
            Log.d(TAG, "Flight ${flight.flightNumber} missing coords (origin=${flight.originLat},${flight.originLon} dest=${flight.destinationLat},${flight.destinationLon}), fetching details...")
            try {
                val detailResult = getFlightDetails(
                    flightId = flight.id,
                    origin = flight.originCode,
                    destination = flight.destinationCode
                )
                detailResult.getOrNull()?.let { detailed ->
                    Log.d(TAG, "Enriched flight coords: origin=${detailed.originLat},${detailed.originLon} dest=${detailed.destinationLat},${detailed.destinationLon}")
                    // Merge: prefer detailed coords but keep original data where detailed is null
                    flight.copy(
                        originLat = detailed.originLat ?: flight.originLat,
                        originLon = detailed.originLon ?: flight.originLon,
                        originName = detailed.originName ?: flight.originName,
                        originCity = detailed.originCity ?: flight.originCity,
                        originCountry = detailed.originCountry ?: flight.originCountry,
                        originTimezone = detailed.originTimezone ?: flight.originTimezone,
                        destinationLat = detailed.destinationLat ?: flight.destinationLat,
                        destinationLon = detailed.destinationLon ?: flight.destinationLon,
                        destinationName = detailed.destinationName ?: flight.destinationName,
                        destinationCity = detailed.destinationCity ?: flight.destinationCity,
                        destinationCountry = detailed.destinationCountry ?: flight.destinationCountry,
                        destinationTimezone = detailed.destinationTimezone ?: flight.destinationTimezone,
                        latitude = detailed.latitude ?: flight.latitude,
                        longitude = detailed.longitude ?: flight.longitude,
                        altitude = detailed.altitude ?: flight.altitude,
                        heading = detailed.heading ?: flight.heading,
                        speed = detailed.speed ?: flight.speed,
                        progressPercent = detailed.progressPercent ?: flight.progressPercent,
                        trackPoints = if (detailed.trackPoints.isNotEmpty()) detailed.trackPoints else flight.trackPoints
                    )
                } ?: flight
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enrich flight details", e)
                flight
            }
        } else {
            flight
        }

        val personalFlight = PersonalFlight(
            flight = enrichedFlight,
            notes = notes,
            seatNumber = seatNumber,
            seatClass = seatClass,
            bookingReference = bookingReference
        )
        val backendFlight = createBackendFlightFromDomain(enrichedFlight)
        val entity = PersonalFlightEntity(
            localId = personalFlight.localId,
            flightId = enrichedFlight.id,
            flightNumber = enrichedFlight.flightNumber,
            originCode = enrichedFlight.originCode,
            destinationCode = enrichedFlight.destinationCode,
            airlineName = enrichedFlight.airlineName,
            airlineIata = enrichedFlight.airlineIata,
            status = enrichedFlight.rawStatus,
            scheduledDeparture = enrichedFlight.scheduledDeparture,
            scheduledArrival = enrichedFlight.scheduledArrival,
            notes = notes,
            seatNumber = seatNumber,
            seatClass = seatClass?.name,
            bookingReference = bookingReference,
            addedAt = personalFlight.addedAt,
            lastUpdated = personalFlight.lastUpdated,
            flightJson = json.encodeToString(backendFlight)
        )
        personalFlightDao.insertPersonalFlight(entity)
        return personalFlight
    }

    suspend fun deletePersonalFlight(localId: String) {
        // Get flight info before deleting so we can unsubscribe from backend
        val entity = personalFlightDao.getPersonalFlight(localId)

        // Delete locally first for immediate UI update
        personalFlightDao.deletePersonalFlightById(localId)

        // Unsubscribe from backend alerts (like iOS AlertSubscriptionService)
        // Try both IATA (e.g. TK7) and ICAO (e.g. THY7) idents, since the backend
        // worker canonicalizes to ICAO but we may only have the IATA variant stored.
        if (entity != null) {
            val departureDate = entity.scheduledDeparture?.take(10) // "2026-03-30T..." → "2026-03-30"

            // Collect unique ident variants to try
            val identsToTry = mutableSetOf<String>()

            // 1. The stored flight number (usually IATA, e.g. "TK7")
            identsToTry.add(entity.flightNumber.uppercase())

            // 2. Extract ICAO ident from flightId (FA flight ID format: "THY7-1680000000-schedule-0030")
            val icaoFromFlightId = entity.flightId.split("-").firstOrNull()?.uppercase()
            if (!icaoFromFlightId.isNullOrEmpty()) {
                identsToTry.add(icaoFromFlightId)
            }

            for (ident in identsToTry) {
                try {
                    Log.d(TAG, "Unsubscribing from flight alerts: $ident ($departureDate)")
                    api.unsubscribeFromFlight(
                        flightIdent = ident,
                        departureDate = departureDate
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unsubscribe ($ident) from flight alerts", e)
                }
            }
        }
    }

    suspend fun refreshPersonalFlight(personalFlight: PersonalFlight): Result<PersonalFlight> {
        return try {
            val result = getFlightDetails(
                flightId = personalFlight.flight.id,
                origin = personalFlight.flight.originCode,
                destination = personalFlight.flight.destinationCode
            )
            result.map { updatedFlight ->
                val refreshed = personalFlight.copy(
                    flight = updatedFlight,
                    lastUpdated = System.currentTimeMillis()
                )
                // Persist the updated data back to Room
                val backendFlight = createBackendFlightFromDomain(updatedFlight)
                personalFlightDao.insertPersonalFlight(
                    PersonalFlightEntity(
                        localId = refreshed.localId,
                        flightId = updatedFlight.id,
                        flightNumber = updatedFlight.flightNumber,
                        originCode = updatedFlight.originCode,
                        destinationCode = updatedFlight.destinationCode,
                        airlineName = updatedFlight.airlineName,
                        airlineIata = updatedFlight.airlineIata,
                        status = updatedFlight.rawStatus,
                        scheduledDeparture = updatedFlight.scheduledDeparture,
                        scheduledArrival = updatedFlight.scheduledArrival,
                        notes = refreshed.notes,
                        seatNumber = refreshed.seatNumber,
                        seatClass = refreshed.seatClass?.name,
                        bookingReference = refreshed.bookingReference,
                        customName = refreshed.customName,
                        isNotificationsEnabled = refreshed.isNotificationsEnabled,
                        sequenceNumber = refreshed.sequenceNumber,
                        boardingGroup = refreshed.boardingGroup,
                        cabinClass = refreshed.cabinClass,
                        isProvisional = refreshed.isProvisional,
                        provisionalUntil = refreshed.provisionalUntil,
                        addedAt = refreshed.addedAt,
                        lastUpdated = refreshed.lastUpdated,
                        flightJson = json.encodeToString(backendFlight)
                    )
                )
                Log.d(TAG, "Refreshed flight ${updatedFlight.flightNumber}: origin=${updatedFlight.originLat},${updatedFlight.originLon} dest=${updatedFlight.destinationLat},${updatedFlight.destinationLon}")
                refreshed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh personal flight error", e)
            Result.failure(e)
        }
    }

    // ── Shared Flights (iOS) ──

    fun getSharedFlightsFlow(): Flow<List<SharedFlightEntity>> {
        return sharedFlightDao.getActiveSharedFlights()
    }

    suspend fun createSharedFlight(
        flight: Flight,
        userId: String,
        displayName: String?
    ): Result<String> {
        return try {
            val request = BackendShareRequest(
                flightIdent = flight.flightNumber,
                origin = flight.originCode,
                destination = flight.destinationCode,
                airline = flight.airlineName ?: flight.airlineIata ?: flight.flightNumber.replace(Regex("\\d+$"), ""),
                departureDate = flight.scheduledDeparture,
                user = BackendShareUser(id = userId, displayName = displayName),
                permissions = BackendSharePermissions(),
                expiryHours = 48
            )
            val requestJson = json.encodeToString(request)
            Log.d("ShareFlight", "Request body: $requestJson")

            val response = api.createSharedFlight(request)
            Log.d("ShareFlight", "Response code: ${response.code()}")
            if (response.isSuccessful) {
                val shareCode = response.body()?.sharedFlight?.shareCode ?: response.body()?.shareCode
                if (shareCode != null) {
                    // Cache locally
                    sharedFlightDao.insertSharedFlight(
                        SharedFlightEntity(
                            id = response.body()?.sharedFlight?.id ?: shareCode,
                            flightIdent = flight.flightNumber,
                            shareCode = shareCode,
                            sharedByName = displayName,
                            origin = flight.originCode,
                            destination = flight.destinationCode,
                            airline = flight.airlineName,
                            status = flight.rawStatus,
                            departureDate = flight.scheduledDeparture,
                            arrivalDate = flight.scheduledArrival,
                            departureDelay = flight.departureDelay,
                            arrivalDelay = flight.arrivalDelay,
                            departureGate = flight.departureGate,
                            arrivalGate = flight.arrivalGate
                        )
                    )
                    Result.success(shareCode)
                } else {
                    Result.failure(Exception("No share code returned"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ShareFlight", "Share failed: ${response.code()} — $errorBody")
                Result.failure(Exception("Share failed: ${response.code()} — $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinSharedFlight(shareCode: String): Result<SharedFlightEntity?> {
        return try {
            Log.d("ShareFlight", "Joining share: $shareCode")
            val joinRequest = BackendJoinRequest(
                user = BackendShareUser(id = "android-user", displayName = "Android User")
            )
            val response = api.joinSharedFlight(shareCode, joinRequest)
            Log.d("ShareFlight", "Join response: ${response.code()}")
            if (response.isSuccessful) {
                val sf = response.body()?.sharedFlight
                Log.d("ShareFlight", "Join sharedFlight: $sf")
                if (sf != null) {
                    val entity = SharedFlightEntity(
                        id = sf.id,
                        flightIdent = sf.flightIdent,
                        shareCode = sf.shareCode,
                        sharedByName = sf.sharedBy,
                        origin = sf.origin,
                        destination = sf.destination,
                        airline = sf.airline,
                        status = sf.status,
                        departureDate = sf.departureDate,
                        arrivalDate = sf.arrivalDate,
                        departureDelay = sf.departureDelay,
                        arrivalDelay = sf.arrivalDelay,
                        departureGate = sf.departureGate,
                        arrivalGate = sf.arrivalGate
                    )
                    sharedFlightDao.insertSharedFlight(entity)
                    Result.success(entity)
                } else {
                    Result.success(null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ShareFlight", "Join failed: ${response.code()} — $errorBody")
                Result.failure(Exception("Join failed: ${response.code()} — $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ShareFlight", "Join exception", e)
            Result.failure(e)
        }
    }

    suspend fun stopSharedFlight(shareCode: String): Result<Unit> {
        // Remove locally first so the UI updates immediately
        sharedFlightDao.deactivateSharedFlight(shareCode)
        return try {
            api.stopSharedFlight(shareCode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    // ── Flight Events (iOS timeline) ──

    fun getFlightEventsFlow(flightId: String): Flow<List<FlightEventEntity>> {
        return flightEventDao.getEventsForFlight(flightId)
    }

    suspend fun recordFlightEvent(event: FlightEvent) {
        flightEventDao.insertEvent(
            FlightEventEntity(
                id = event.id,
                flightId = event.flightId,
                timestamp = event.timestamp,
                eventType = event.eventType.name,
                previousValue = event.previousValue,
                newValue = event.newValue,
                eventDescription = event.eventDescription
            )
        )
    }

    // ── Private Helpers ──

    private suspend fun cacheFlights(flights: List<BackendFlight>) {
        val entities = flights.map { bf ->
            FlightEntity(
                id = bf.id,
                flightNumber = bf.flightNumber,
                originCode = bf.origin?.resolvedCode ?: "",
                destinationCode = bf.destination?.resolvedCode ?: "",
                airlineName = bf.airline?.name ?: bf.operator?.name,
                airlineIata = bf.airline?.iata ?: bf.operator?.iata,
                airlineIcao = bf.airline?.icao ?: bf.operator?.icao,
                status = bf.status,
                scheduledDeparture = bf.departure?.scheduled ?: bf.schedule?.departure?.scheduled,
                scheduledArrival = bf.arrival?.scheduled ?: bf.schedule?.arrival?.scheduled,
                latitude = bf.position?.latitude,
                longitude = bf.position?.longitude,
                altitude = bf.position?.altitude,
                heading = bf.position?.heading,
                speed = bf.position?.speed,
                progressPercent = bf.progressPercent,
                departureDelay = bf.resolvedDepartureDelay,
                arrivalDelay = bf.resolvedArrivalDelay,
                diverted = bf.diverted == true,
                cancelled = bf.cancelled == true
            )
        }
        flightDao.insertFlights(entities)
    }

    private fun createBackendFlightFromDomain(flight: Flight): BackendFlight {
        return BackendFlight(
            id = flight.id,
            flightNumber = flight.flightNumber,
            callsign = flight.callsign,
            status = flight.rawStatus,
            detailedStatus = flight.detailedStatus,
            progressPercent = flight.progressPercent,
            departureDelay = flight.departureDelay,
            arrivalDelay = flight.arrivalDelay,
            diverted = flight.diverted,
            cancelled = flight.cancelled,
            blocked = flight.blocked,
            actualWheelsOff = flight.actualWheelsOff,
            actualWheelsOn = flight.actualWheelsOn,
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
            airline = com.trueskies.android.data.remote.models.BackendAirline(
                name = flight.airlineName, iata = flight.airlineIata, icao = flight.airlineIcao
            ),
            aircraft = com.trueskies.android.data.remote.models.BackendAircraft(
                registration = flight.aircraftRegistration, type = flight.aircraftType,
                icao = flight.aircraftIcao, iata = flight.aircraftIata
            ),
            departure = com.trueskies.android.data.remote.models.BackendFlightTime(
                scheduled = flight.scheduledDeparture, estimated = flight.estimatedDeparture,
                actual = flight.actualDeparture
            ),
            arrival = com.trueskies.android.data.remote.models.BackendFlightTime(
                scheduled = flight.scheduledArrival, estimated = flight.estimatedArrival,
                actual = flight.actualArrival
            ),
            route = com.trueskies.android.data.remote.models.BackendRoute(
                distance = flight.routeDistance, duration = flight.routeDuration
            ),
            marketingCarrier = if (flight.marketingFlightNumber != null)
                com.trueskies.android.data.remote.models.BackendMarketingCarrier(
                    flightNumber = flight.marketingFlightNumber,
                    airlineCode = flight.marketingAirlineCode,
                    iata = flight.marketingAirlineIata,
                    icao = flight.marketingAirlineIcao,
                    name = flight.marketingAirlineName
                ) else null
        )
    }
}

// ── Extensions ──

private fun FlightEntity.toDomainFlight(): Flight {
    return Flight(
        id = id,
        flightNumber = flightNumber,
        displayFlightNumber = flightNumber.replace(Regex("([A-Z]{2,3})(\\d+)"), "$1 $2"),
        originCode = originCode,
        destinationCode = destinationCode,
        airlineName = airlineName,
        airlineIata = airlineIata,
        airlineIcao = airlineIcao,
        rawStatus = status,
        scheduledDeparture = scheduledDeparture,
        scheduledArrival = scheduledArrival,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        heading = heading,
        speed = speed,
        progressPercent = progressPercent,
        departureDelay = departureDelay,
        arrivalDelay = arrivalDelay,
        diverted = diverted,
        cancelled = cancelled
    )
}

private fun PersonalFlightEntity.toDomainPersonalFlight(json: Json): PersonalFlight {
    val flight = if (flightJson != null) {
        try {
            val bf = json.decodeFromString<BackendFlight>(flightJson)
            Flight.fromBackend(bf)
        } catch (e: Exception) {
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
        sequenceNumber = sequenceNumber,
        boardingGroup = boardingGroup,
        cabinClass = cabinClass,
        isProvisional = isProvisional,
        provisionalUntil = provisionalUntil,
        addedAt = addedAt,
        lastUpdated = lastUpdated
    )
}

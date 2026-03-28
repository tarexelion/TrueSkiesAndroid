package com.trueskies.android.domain.models

import com.trueskies.android.data.remote.models.BackendFlight
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Domain Flight model — ported from iOS Flight.swift.
 * Represents a single flight with computed status, timing, and location properties.
 */
data class Flight(
    val id: String,
    val flightNumber: String,
    val displayFlightNumber: String,
    val callsign: String? = null,
    val icao24: String? = null,
    // Airline
    val airlineName: String? = null,
    val airlineIata: String? = null,
    val airlineIcao: String? = null,
    // Aircraft
    val aircraftRegistration: String? = null,
    val aircraftType: String? = null,
    val aircraftIcao: String? = null,
    val aircraftIata: String? = null,
    // Origin
    val originCode: String,
    val originName: String? = null,
    val originCity: String? = null,
    val originCountry: String? = null,
    val originLat: Double? = null,
    val originLon: Double? = null,
    val originTimezone: String? = null,
    // Destination
    val destinationCode: String,
    val destinationName: String? = null,
    val destinationCity: String? = null,
    val destinationCountry: String? = null,
    val destinationLat: Double? = null,
    val destinationLon: Double? = null,
    val destinationTimezone: String? = null,
    // Position
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val verticalRate: Double? = null,
    val positionTimestamp: String? = null,
    // Timing
    val scheduledDeparture: String? = null,
    val estimatedDeparture: String? = null,
    val actualDeparture: String? = null,
    val scheduledArrival: String? = null,
    val estimatedArrival: String? = null,
    val actualArrival: String? = null,
    val actualWheelsOff: String? = null,
    val actualWheelsOn: String? = null,
    val eta: String? = null,
    // Schedule (gate/terminal info)
    val departureGate: String? = null,
    val departureTerminal: String? = null,
    val arrivalGate: String? = null,
    val arrivalTerminal: String? = null,
    val arrivalBaggage: String? = null,
    // Status
    val rawStatus: String? = null,
    val detailedStatus: String? = null,
    val progressPercent: Int? = null,
    val departureDelay: Int? = null,
    val arrivalDelay: Int? = null,
    // Flags
    val diverted: Boolean = false,
    val cancelled: Boolean = false,
    val blocked: Boolean = false,
    // Diversion details (iOS)
    val divertedToAirportCode: String? = null,
    val divertedToAirportName: String? = null,
    val diversionReason: String? = null,
    val diversionTimestamp: String? = null,
    val diversionEstimatedArrival: String? = null,
    // Marketing carrier / codeshare (iOS)
    val marketingFlightNumber: String? = null,
    val marketingAirlineCode: String? = null,
    val marketingAirlineIata: String? = null,
    val marketingAirlineIcao: String? = null,
    val marketingAirlineName: String? = null,
    // Foresight factors (iOS)
    val foresightFactorsDeparture: List<String>? = null,
    val foresightFactorsArrival: List<String>? = null,
    // Route
    val routeDistance: Double? = null,
    val routeDuration: Int? = null,
    // Track
    val trackPoints: List<TrackPoint> = emptyList(),
    // Data source
    val dataSource: String? = null,
    val onGround: Boolean? = null
) {
    /** Computed flight status from raw backend string, with fallbacks */
    val status: FlightStatus
        get() {
            if (cancelled) return FlightStatus.CANCELLED
            if (diverted) return FlightStatus.DIVERTED
            val fromRaw = FlightStatus.fromBackendStatus(rawStatus)
            if (fromRaw != FlightStatus.UNKNOWN) return fromRaw
            // Try detailedStatus as fallback
            val fromDetailed = FlightStatus.fromBackendStatus(detailedStatus)
            if (fromDetailed != FlightStatus.UNKNOWN) return fromDetailed
            // Infer from position/timing data
            if (latitude != null && longitude != null && onGround != true) return FlightStatus.EN_ROUTE
            if (actualArrival != null) return FlightStatus.ARRIVED
            if (actualDeparture != null && latitude != null) return FlightStatus.EN_ROUTE
            if (actualDeparture != null) return FlightStatus.DEPARTED
            if (scheduledDeparture != null) return FlightStatus.SCHEDULED
            return FlightStatus.UNKNOWN
        }

    /** Best departure time: actual > estimated > scheduled */
    val bestDepartureTime: String?
        get() = actualDeparture ?: estimatedDeparture ?: scheduledDeparture

    /** Best arrival time: actual > estimated > scheduled */
    val bestArrivalTime: String?
        get() = actualArrival ?: estimatedArrival ?: scheduledArrival

    /** Whether the flight is currently in the air */
    val isAirborne: Boolean
        get() = status.isAirborne || (latitude != null && longitude != null && status.isActive)

    /** Whether the flight has a position */
    val hasPosition: Boolean
        get() = latitude != null && longitude != null

    /** Extract airline code prefix from flight number (e.g., "PGT5045" → "PGT") */
    val airlineCodeFromFlightNumber: String?
        get() = Regex("^([A-Z]{2,3})\\d").find(flightNumber)?.groupValues?.get(1)

    /** Route display string */
    val routeDisplay: String
        get() = "$originCode → $destinationCode"

    /** Whether this is a codeshare flight (iOS isCodeshare) */
    val isCodeshare: Boolean
        get() = marketingFlightNumber != null && marketingFlightNumber != flightNumber

    /** Current relevant gate (iOS currentRelevantGate) */
    val currentRelevantGate: String?
        get() = when {
            status.isCompleted || status == FlightStatus.ARRIVED || status == FlightStatus.LANDED ->
                arrivalGate
            else -> departureGate
        }

    /** Current relevant terminal (iOS currentRelevantTerminal) */
    val currentRelevantTerminal: String?
        get() = when {
            status.isCompleted || status == FlightStatus.ARRIVED || status == FlightStatus.LANDED ->
                arrivalTerminal
            else -> departureTerminal
        }

    /** Whether flight is live (has recent position data) */
    val isLive: Boolean
        get() = hasPosition && status.isActive

    /** Whether flight is grounded */
    val isGrounded: Boolean
        get() = onGround == true || status == FlightStatus.ARRIVED || status == FlightStatus.LANDED

    companion object {
        // Thread-safe confirmed rank cache — prevents status regression
        private val confirmedRankCache = ConcurrentHashMap<String, Int>()

        fun advanceConfirmedRank(flightId: String, newRank: Int) {
            confirmedRankCache.compute(flightId) { _, currentRank ->
                maxOf(newRank, currentRank ?: 0)
            }
        }

        fun getConfirmedRank(flightId: String): Int {
            return confirmedRankCache[flightId] ?: 0
        }

        fun clearRankCache(flightId: String) {
            confirmedRankCache.remove(flightId)
        }

        /** Create a domain Flight from a BackendFlight response */
        fun fromBackend(bf: BackendFlight): Flight {
            return Flight(
                id = bf.id,
                flightNumber = bf.flightNumber,
                displayFlightNumber = bf.displayFlightNumber,
                callsign = bf.callsign,
                airlineName = bf.airline?.name,
                airlineIata = bf.airline?.iata,
                airlineIcao = bf.airline?.icao,
                aircraftRegistration = bf.aircraft?.registration,
                aircraftType = bf.aircraft?.type,
                aircraftIcao = bf.aircraft?.icao,
                aircraftIata = bf.aircraft?.iata,
                originCode = bf.origin?.resolvedCode ?: "???",
                originName = bf.origin?.name,
                originCity = bf.origin?.city,
                originCountry = bf.origin?.country,
                originLat = bf.origin?.latitude,
                originLon = bf.origin?.longitude,
                originTimezone = bf.origin?.timezone,
                destinationCode = bf.destination?.resolvedCode ?: "???",
                destinationName = bf.destination?.name,
                destinationCity = bf.destination?.city,
                destinationCountry = bf.destination?.country,
                destinationLat = bf.destination?.latitude,
                destinationLon = bf.destination?.longitude,
                destinationTimezone = bf.destination?.timezone,
                latitude = bf.position?.latitude,
                longitude = bf.position?.longitude,
                altitude = bf.position?.altitude,
                heading = bf.position?.heading,
                speed = bf.position?.speed,
                positionTimestamp = bf.position?.timestamp,
                scheduledDeparture = bf.departure?.scheduled ?: bf.schedule?.departure?.scheduled,
                estimatedDeparture = bf.departure?.estimated ?: bf.schedule?.departure?.estimated,
                actualDeparture = bf.departure?.actual ?: bf.schedule?.departure?.actual,
                scheduledArrival = bf.arrival?.scheduled ?: bf.schedule?.arrival?.scheduled,
                estimatedArrival = bf.arrival?.estimated ?: bf.schedule?.arrival?.estimated,
                actualArrival = bf.arrival?.actual ?: bf.schedule?.arrival?.actual,
                actualWheelsOff = bf.actualWheelsOff,
                actualWheelsOn = bf.actualWheelsOn,
                departureGate = bf.schedule?.departure?.gate,
                departureTerminal = bf.schedule?.departure?.terminal,
                arrivalGate = bf.schedule?.arrival?.gate,
                arrivalTerminal = bf.schedule?.arrival?.terminal,
                arrivalBaggage = bf.schedule?.arrival?.baggage,
                rawStatus = bf.status,
                detailedStatus = bf.detailedStatus,
                progressPercent = bf.progressPercent,
                departureDelay = bf.resolvedDepartureDelay,
                arrivalDelay = bf.resolvedArrivalDelay,
                diverted = bf.diverted == true,
                cancelled = bf.cancelled == true,
                blocked = bf.blocked == true,
                divertedToAirportCode = bf.divertedToAirport?.resolvedCode,
                divertedToAirportName = bf.divertedToAirport?.name,
                marketingFlightNumber = bf.marketingCarrier?.flightNumber,
                marketingAirlineCode = bf.marketingCarrier?.airlineCode,
                marketingAirlineIata = bf.marketingCarrier?.iata,
                marketingAirlineIcao = bf.marketingCarrier?.icao,
                marketingAirlineName = bf.marketingCarrier?.name,
                foresightFactorsDeparture = bf.foresightFactorsDeparture,
                foresightFactorsArrival = bf.foresightFactorsArrival,
                routeDistance = bf.route?.distance,
                routeDuration = bf.route?.duration,
                trackPoints = bf.trackPoints?.map {
                    TrackPoint(it.latitude, it.longitude, it.altitude, it.timestamp)
                } ?: emptyList()
            )
        }
    }
}

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: String? = null
)

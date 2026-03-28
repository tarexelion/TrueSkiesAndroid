package com.trueskies.android.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Backend API response models — ported from iOS BackendModels.swift.
 * Uses kotlinx.serialization for JSON parsing.
 */

// ── Top-Level Responses ──

@Serializable
data class BackendFlightResponse(
    val success: Boolean,
    val flights: List<BackendFlight> = emptyList(),
    val pagination: BackendPagination? = null,
    val timestamp: String? = null,
    val metadata: BackendResponseMetadata? = null
)

@Serializable
data class BackendFlightDetailsResponse(
    val success: Boolean,
    val flight: List<BackendFlight>? = null,
    val flights: List<BackendFlight>? = null,
    val allFlights: List<BackendFlight>? = null,
    val metadata: BackendResponseMetadata? = null,
    val timestamp: String? = null
) {
    /** Returns the best flight, preferring flight array first, then flights array, then allFlights */
    val resolvedFlight: BackendFlight?
        get() = flight?.firstOrNull() ?: flights?.firstOrNull() ?: allFlights?.firstOrNull()
}

@Serializable
data class BackendLiveFlightsResponse(
    val success: Boolean,
    val flights: List<BackendFlight> = emptyList(),
    val metadata: BackendResponseMetadata? = null,
    val timestamp: String? = null
)

@Serializable
data class BackendPositionResponse(
    val success: Boolean,
    val position: BackendLivePosition? = null,
    val source: String? = null,
    val metadata: BackendPositionMetadata? = null
)

@Serializable
data class BackendTrackResponse(
    val success: Boolean,
    val track: BackendTrackData? = null,
    val source: String? = null,
    val metadata: BackendTrackMetadata? = null
)

@Serializable
data class HealthResponse(
    val success: Boolean,
    val message: String = "",
    val version: String = "",
    val services: Map<String, HealthServiceStatus>? = null,
    val operational: Boolean? = null
)

@Serializable
data class HealthServiceStatus(
    val status: String? = null,
    val operational: Boolean? = null,
    val available: Boolean? = null,
    val configured: Boolean? = null,
    val lastCheck: String? = null,
    val details: Map<String, String>? = null
)

// ── Shared Flight Responses (iOS) ──

@Serializable
data class BackendShareRequest(
    val flightIdent: String,
    val origin: String,
    val destination: String,
    val airline: String,
    val departureDate: String? = null,
    val user: BackendShareUser,
    val permissions: BackendSharePermissions? = null,
    val expiryHours: Int? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class BackendShareUser(
    val id: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class BackendSharePermissions(
    val canViewRealtime: Boolean = true,
    val canViewGate: Boolean = true,
    val canViewNotifications: Boolean = true,
    val canReshare: Boolean = false
)

@Serializable
data class BackendSharedFlightResponse(
    val success: Boolean,
    val sharedFlight: BackendSharedFlight? = null,
    val shareCode: String? = null,
    val metadata: BackendResponseMetadata? = null
)

@Serializable
data class BackendSharedFlight(
    val id: String,
    val flightIdent: String,
    val shareCode: String,
    val sharedBy: BackendShareUser? = null,
    val sharedWith: List<BackendShareUser>? = null,
    val origin: String? = null,
    val destination: String? = null,
    val airline: String? = null,
    val status: String? = null,
    val departureDate: String? = null,
    val arrivalDate: String? = null,
    val departureDelay: Int? = null,
    val arrivalDelay: Int? = null,
    val departureGate: String? = null,
    val arrivalGate: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: Double? = null,
    val altitude: Int? = null,
    val groundspeed: Int? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val isActive: Boolean? = null,
    val permissions: BackendSharePermissions? = null
)

// ── Service Status (iOS) ──

@Serializable
data class BackendServiceStatusResponse(
    val success: Boolean,
    val healthy: Boolean? = null,
    val rateLimited: Boolean? = null,
    val recommendations: List<String>? = null,
    val metadata: BackendResponseMetadata? = null
)

// ── Backend Flight ──

@Serializable
data class BackendFlight(
    val id: String,
    val flightNumber: String,
    val callsign: String? = null,
    val aircraft: BackendAircraft? = null,
    val airline: BackendAirline? = null,
    val origin: BackendAirport? = null,
    val destination: BackendAirport? = null,
    val position: BackendPosition? = null,
    val status: String? = null,
    val departure: BackendFlightTime? = null,
    val arrival: BackendFlightTime? = null,
    val route: BackendRoute? = null,
    val trackPoints: List<BackendTrackPoint>? = null,
    val schedule: BackendSchedule? = null,
    val detailedStatus: String? = null,
    val progressPercent: Int? = null,
    val departureDelay: Int? = null,
    val arrivalDelay: Int? = null,
    val diverted: Boolean? = null,
    val divertedToAirport: BackendAirport? = null,
    val diversionReason: String? = null,
    val diversionTimestamp: String? = null,
    val diversionEstimatedArrival: String? = null,
    val cancelled: Boolean? = null,
    val blocked: Boolean? = null,
    val foresightFactorsDeparture: List<String>? = null,
    val foresightFactorsArrival: List<String>? = null,
    val actualWheelsOff: String? = null,
    val actualWheelsOn: String? = null,
    val marketingCarrier: BackendMarketingCarrier? = null,
    val lastPosition: BackendLivePosition? = null,
    val liveActivity: BackendLiveActivity? = null,
    val operatorName: String? = null,
    val hybridDataSources: List<String>? = null
) {
    /** Resolved departure delay: top-level > nested */
    val resolvedDepartureDelay: Int?
        get() = departureDelay ?: departure?.delay

    /** Resolved arrival delay: top-level > nested */
    val resolvedArrivalDelay: Int?
        get() = arrivalDelay ?: arrival?.delay

    /** User-friendly flight number with space between airline code and number */
    val displayFlightNumber: String
        get() {
            val fn = marketingCarrier?.flightNumber ?: flightNumber
            return fn.replace(Regex("([A-Z]{2,3})(\\d+)"), "$1 $2")
        }
}

// ── Sub-Models ──

@Serializable
data class BackendAircraft(
    val registration: String? = null,
    val type: String? = null,
    val icao: String? = null,
    val iata: String? = null,
    val model: String? = null,
    val paintedAs: String? = null,
    val operatingAs: String? = null
)

@Serializable
data class BackendAirline(
    val name: String? = null,
    val icao: String? = null,
    val iata: String? = null
)

@Serializable
data class BackendMarketingCarrier(
    val flightNumber: String? = null,
    val airlineCode: String? = null,
    val iata: String? = null,
    val icao: String? = null,
    val name: String? = null
)

@Serializable
data class BackendAirport(
    val code: String? = null,
    val name: String? = null,
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    // Alternate code fields from different API formats
    val iata: String? = null,
    val icao: String? = null,
    val displayCode: String? = null,
    @SerialName("code_iata") val codeIata: String? = null,
    @SerialName("code_icao") val codeIcao: String? = null,
) {
    /** Resolves the best display code: code > displayCode > iata > codeIata > icao > codeIcao */
    val resolvedCode: String
        get() = code ?: displayCode ?: iata ?: codeIata ?: icao ?: codeIcao ?: "???"
}

@Serializable
data class BackendPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val timestamp: String? = null,
    val verticalRate: Double? = null
)

@Serializable
data class BackendTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: String? = null
)

@Serializable
data class BackendFlightTime(
    val scheduled: String? = null,
    val estimated: String? = null,
    val actual: String? = null,
    val delay: Int? = null
)

@Serializable
data class BackendRoute(
    val distance: Double? = null,
    val duration: Int? = null
)

@Serializable
data class BackendSchedule(
    val departure: BackendScheduleTime? = null,
    val arrival: BackendScheduleTime? = null
)

@Serializable
data class BackendScheduleTime(
    val scheduled: String? = null,
    val estimated: String? = null,
    val actual: String? = null,
    val gate: String? = null,
    val terminal: String? = null,
    val baggage: String? = null
)

@Serializable
data class BackendPagination(
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val hasMore: Boolean? = null
)

@Serializable
data class BackendResponseMetadata(
    val rateLimited: Boolean? = null,
    val breaker: String? = null,
    val breakerNextAttempt: String? = null,
    val recommendedRetrySeconds: Int? = null,
    val cached: Boolean? = null,
    val stale: Boolean? = null,
    val cacheAge: Int? = null,
    val dateAvailability: DateAvailabilityMetadata? = null,
    val provisional: Boolean? = null,
    val staleWarning: Boolean? = null,
    val dataConfidence: String? = null
)

@Serializable
data class DateAvailabilityMetadata(
    val requestedDate: String? = null,
    val actualDataDate: String? = null,
    val dateShiftRequired: Boolean? = null,
    val shiftDays: Int? = null,
    val isRecurringFlight: Boolean? = null,
    val scheduleConsistency: String? = null,
    val availableDates: List<String>? = null,
    val dataWindow: DataWindowInfo? = null,
    val provisionalUntil: String? = null,
    val isPastDate: Boolean? = null
)

@Serializable
data class DataWindowInfo(
    val end: String? = null,
    val requestedDateInWindow: Boolean? = null
)

// ── Position / Track ──

@Serializable
data class BackendLivePosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int? = null,
    val altitudeChange: String? = null,
    val groundspeed: Int? = null,
    val heading: Int? = null,
    val timestamp: String? = null,
    val updateType: String? = null,
    @SerialName("fa_flight_id") val faFlightId: String? = null
)

@Serializable
data class BackendPositionMetadata(
    val endpoint: String? = null,
    val faFlightId: String? = null,
    val cacheStatus: String? = null,
    val cacheLayer: String? = null,
    val positionAge: Double? = null,
    val stale: Boolean? = null,
    val fallbackReason: String? = null,
    val maxStale: Int? = null,
    val timestamp: String? = null
)

@Serializable
data class BackendTrackData(
    val positions: List<BackendTrackPositionFull>? = null
)

@Serializable
data class BackendTrackPositionFull(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int? = null,
    @SerialName("altitude_change") val altitudeChange: String? = null,
    val groundspeed: Int? = null,
    val heading: Int? = null,
    val timestamp: String? = null,
    @SerialName("update_type") val updateType: String? = null
)

@Serializable
data class BackendTrackMetadata(
    val endpoint: String? = null,
    val cacheStatus: String? = null,
    val timestamp: String? = null
)

// ── Live Activity (iOS push pre-computed data) ──

@Serializable
data class BackendLiveActivity(
    val flightNumber: String? = null,
    val status: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val gate: String? = null,
    val terminal: String? = null
)

// ── Error Response ──

@Serializable
data class BackendErrorResponse(
    val success: Boolean? = null,
    val error: String? = null,
    val metadata: BackendResponseMetadata? = null
)

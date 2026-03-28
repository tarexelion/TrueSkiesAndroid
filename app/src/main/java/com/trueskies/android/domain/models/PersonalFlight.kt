package com.trueskies.android.domain.models

import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Personal (tracked) flight — ported from iOS PersonalFlight.swift.
 * Wraps a Flight with user-specific metadata (notes, seat, custom name, boarding pass data).
 */
data class PersonalFlight(
    val localId: String = UUID.randomUUID().toString(),
    val flight: Flight,
    // User-specific fields
    val notes: String? = null,
    val seatNumber: String? = null,
    val seatClass: SeatClass? = null,
    val bookingReference: String? = null,
    val customName: String? = null,
    val isNotificationsEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    // Boarding pass fields (iOS)
    val sequenceNumber: String? = null,
    val boardingGroup: String? = null,
    val cabinClass: String? = null,
    // Provisional flight support (iOS)
    val isProvisional: Boolean = false,
    val provisionalUntil: String? = null,
    val userTargetDate: String? = null,
    // Ownership
    val isOwnFlight: Boolean = true
) {
    /** Confirmed lifecycle rank — prevents status from going backward (iOS hysteresis) */
    val confirmedStatus: FlightStatus
        get() {
            val currentStatus = flight.status
            val confirmedRank = Flight.getConfirmedRank(flight.id)
            return if (currentStatus.lifecycleRank >= confirmedRank) {
                // Advance rank
                Flight.advanceConfirmedRank(flight.id, currentStatus.lifecycleRank)
                currentStatus
            } else {
                // Status tried to regress; return last confirmed
                FlightStatus.entries.find { it.lifecycleRank == confirmedRank } ?: currentStatus
            }
        }

    /** Display name: custom name > flight number */
    val displayName: String
        get() = customName ?: flight.displayFlightNumber

    /** Route display: e.g. "JFK → LAX" */
    val routeDisplay: String
        get() = flight.routeDisplay

    /** Origin airport code shorthand */
    val originCode: String
        get() = flight.originCode

    /** Destination airport code shorthand */
    val destinationCode: String
        get() = flight.destinationCode

    /** Whether the flight is a codeshare */
    val isCodeshare: Boolean
        get() = flight.isCodeshare

    /** Whether the flight is international (iOS isInternational) */
    val isInternational: Boolean
        get() = flight.originCountry != null && flight.destinationCountry != null &&
                flight.originCountry != flight.destinationCountry

    /** Whether the flight is upcoming (not yet departed) */
    val isUpcoming: Boolean
        get() {
            val status = confirmedStatus
            return status == FlightStatus.SCHEDULED || status == FlightStatus.FILING ||
                   status == FlightStatus.BOARDING || status == FlightStatus.UNKNOWN
        }

    /** Whether the flight has a backend-reported delay */
    val hasBackendDelay: Boolean
        get() = (flight.departureDelay ?: 0) > 0 || (flight.arrivalDelay ?: 0) > 0

    /** Whether the flight is delayed */
    val isDelayed: Boolean
        get() = hasBackendDelay

    /** Effective departure delay in minutes */
    val effectiveDepartureDelay: Int?
        get() = flight.departureDelay

    /** Effective arrival delay in minutes */
    val effectiveArrivalDelay: Int?
        get() = flight.arrivalDelay

    /** Total delay minutes (max of departure and arrival) */
    val totalDelayMinutes: Int
        get() = maxOf(flight.departureDelay ?: 0, flight.arrivalDelay ?: 0)

    /** Best airline code for logo resolution (iOS logoAirlineCode) */
    val logoAirlineCode: String?
        get() = flight.marketingAirlineIata ?: flight.airlineIata ?: flight.airlineIcao

    /** Effective departure time: actual > estimated > scheduled */
    val effectiveDepartureTime: String?
        get() = flight.bestDepartureTime

    /** Effective arrival time: actual > estimated > scheduled */
    val effectiveArrivalTime: String?
        get() = flight.bestArrivalTime

    /** Effective landing time: wheelsOn > actual arrival */
    val effectiveLandingTime: String?
        get() = flight.actualWheelsOn ?: flight.actualArrival

    /** Time until departure in seconds, or null if not parseable */
    val timeUntilDeparture: Long?
        get() {
            val depTime = flight.bestDepartureTime ?: return null
            return try {
                val zdt = ZonedDateTime.parse(depTime)
                Duration.between(ZonedDateTime.now(), zdt).seconds
            } catch (e: Exception) {
                null
            }
        }
}

enum class SeatClass(val displayName: String) {
    ECONOMY("Economy"),
    PREMIUM_ECONOMY("Premium Economy"),
    BUSINESS("Business"),
    FIRST("First");
}

package com.trueskies.android.domain.models

import java.util.UUID

/**
 * Personal (tracked) flight — ported from iOS PersonalFlight.swift.
 * Wraps a Flight with user-specific metadata (notes, seat, custom name).
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
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /** Confirmed lifecycle rank — prevents status from going backward */
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
}

enum class SeatClass(val displayName: String) {
    ECONOMY("Economy"),
    PREMIUM_ECONOMY("Premium Economy"),
    BUSINESS("Business"),
    FIRST("First");
}

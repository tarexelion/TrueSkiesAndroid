package com.trueskies.android.domain.models

import java.util.UUID

/**
 * Shared flight — ported from iOS SharedFlight.swift.
 * Represents a flight shared between friends for collaborative tracking.
 */
data class SharedFlight(
    val id: String = UUID.randomUUID().toString(),
    val flightIdent: String,
    val shareCode: String,
    // Sharing info
    val sharedBy: SharedUser? = null,
    val sharedById: String? = null,
    val sharedWith: List<SharedUser> = emptyList(),
    // Timestamps
    val createdAt: String? = null,
    val expiresAt: String? = null,
    // Route
    val origin: String? = null,
    val destination: String? = null,
    val departureDate: String? = null,
    val arrivalDate: String? = null,
    val scheduledDepartureDate: String? = null,
    val scheduledArrivalDate: String? = null,
    // Delays
    val departureDelay: Int? = null,
    val arrivalDelay: Int? = null,
    // Timing
    val actualDeparture: String? = null,
    val actualArrival: String? = null,
    val actualWheelsOff: String? = null,
    val actualWheelsOn: String? = null,
    // Timezones
    val originTimezone: String? = null,
    val destinationTimezone: String? = null,
    // Flight info
    val airline: String? = null,
    val status: String? = null,
    // Gates
    val departureGate: String? = null,
    val arrivalGate: String? = null,
    // Aircraft
    val aircraftType: String? = null,
    val aircraftRegistration: String? = null,
    // Position cache
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: Double? = null,
    val groundspeed: Int? = null,
    val altitude: Int? = null,
    val lastPositionUpdate: String? = null,
    val faFlightId: String? = null,
    // Permissions
    val permissions: SharingPermissions = SharingPermissions(),
    val isActive: Boolean = true,
    val isOwnShare: Boolean = false
) {
    /** Route display string */
    val routeString: String
        get() {
            val o = origin ?: "???"
            val d = destination ?: "???"
            return "$o → $d"
        }

    /** Whether the share has expired */
    val isExpired: Boolean
        get() = false // TODO: parse expiresAt and compare

    /** Number of participants */
    val participantCount: Int
        get() = sharedWith.size + if (sharedBy != null) 1 else 0
}

data class SharedUser(
    val id: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val joinedAt: String? = null
)

data class SharingPermissions(
    val canViewRealtime: Boolean = true,
    val canViewGate: Boolean = true,
    val canViewNotifications: Boolean = true,
    val canReshare: Boolean = false
)

/**
 * Flight share link — used for deep linking (iOS FlightShareLink).
 */
data class FlightShareLink(
    val shareCode: String,
    val flightIdent: String
) {
    val url: String
        get() = "https://trueskiesapp.com/share/?flight=${java.net.URLEncoder.encode(flightIdent, "UTF-8")}&code=${java.net.URLEncoder.encode(shareCode, "UTF-8")}"

    val shareText: String
        get() = "Track my flight $flightIdent on TrueSkies! $url"

    companion object {
        /** Parse a share code from a URL or raw text */
        fun parse(from: String): FlightShareLink? {
            val trimmed = from.trim()
            // Match query-param format: ?flight=...&code=...
            val codeMatch = Regex("""[?&]code=([A-Za-z0-9]+)""").find(trimmed)
            val flightMatch = Regex("""[?&]flight=([A-Za-z0-9]+)""").find(trimmed)
            if (codeMatch != null) {
                return FlightShareLink(
                    shareCode = codeMatch.groupValues[1],
                    flightIdent = flightMatch?.groupValues?.get(1) ?: ""
                )
            }
            // Legacy path format: /share/CODE
            val urlMatch = Regex("""trueskiesapp\.com/share/([A-Za-z0-9]+)""").find(trimmed)
            if (urlMatch != null) {
                return FlightShareLink(shareCode = urlMatch.groupValues[1], flightIdent = "")
            }
            return null
        }
    }
}

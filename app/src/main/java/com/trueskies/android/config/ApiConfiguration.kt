package com.trueskies.android.config

import com.trueskies.android.BuildConfig

/**
 * API configuration — ported from iOS APIConfiguration.swift.
 * Reads the API key from BuildConfig (set via local.properties).
 */
object ApiConfiguration {

    val baseUrl: String = BuildConfig.API_BASE_URL

    val apiKey: String = BuildConfig.TRUESKIES_API_KEY

    val isDebug: Boolean = BuildConfig.IS_DEBUG

    val userAgent: String = "TrueSkies/1.0.0 (Android)"

    val environment: String
        get() = if (isDebug) "development" else "production"

    object Endpoints {
        // Health
        const val HEALTH = "/health"

        // Auth
        const val AUTH_TEST = "/api/auth/test"
        const val AUTH_ME = "/api/auth/me"

        // Flight discovery
        const val FLIGHTS_SEARCH = "/api/flights/search"
        const val FLIGHTS_DETAILS = "/api/flights" // + /{id}/details
        const val FLIGHTS_LIVE = "/api/flights/live"
        const val FLIGHTS_GLOBAL = "/api/flights/global"

        // Enhanced flights (AeroAPI)
        const val ENHANCED_SEARCH = "/api/enhanced-flights/search"
        const val ENHANCED_LIVE = "/api/enhanced-flights/area"
        const val ENHANCED_DETAILS = "/api/enhanced-flights" // + /{faFlightId}

        // AeroAPI specialized
        const val AERO_DEPARTURES = "/api/aeroapi/airports" // + /{code}/departures
        const val AERO_ARRIVALS = "/api/aeroapi/airports"   // + /{code}/arrivals
        const val AERO_SCHEDULES = "/api/aeroapi/schedules"  // + /{flightNumber}
        const val AERO_POSITION = "/api/aeroapi/flights"     // + /{faFlightId}/position
        const val AERO_STATUS = "/api/aeroapi/status"
        const val AERO_DELAYS = "/api/aeroapi/airports"      // + /{code}/delays

        // DB-cached flights
        const val DB_FLIGHTS_BOUNDS = "/api/db-flights/bounds"
        const val DB_FLIGHTS_ALL = "/api/db-flights/all"
        const val DB_FLIGHTS_SEARCH = "/api/db-flights/search"
    }
}

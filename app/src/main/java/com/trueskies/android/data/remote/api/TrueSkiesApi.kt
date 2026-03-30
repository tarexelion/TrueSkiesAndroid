package com.trueskies.android.data.remote.api

import com.trueskies.android.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * TrueSkies Retrofit API interface — ported from iOS TrueSkiesBackendService.swift.
 * Covers flight search, details, live positions, shared flights, and health check endpoints.
 */
interface TrueSkiesApi {

    // ── Health ──

    @GET("/health")
    suspend fun healthCheck(): Response<HealthResponse>

    // ── Flight Search ──

    @GET("/api/flights/search")
    suspend fun searchFlights(
        @Query("query") query: String,
        @Query("date") date: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<BackendFlightResponse>

    @GET("/api/enhanced-flights/search")
    suspend fun searchEnhancedFlights(
        @Query("query") query: String,
        @Query("date") date: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("quick") quick: Boolean? = null
    ): Response<BackendFlightResponse>

    // ── Flight Details ──

    @GET("/api/flights/{id}/details")
    suspend fun getFlightDetails(
        @Path("id") flightId: String,
        @Query("date") date: String? = null
    ): Response<BackendFlightDetailsResponse>

    @GET("/api/enhanced-flights/{faFlightId}")
    suspend fun getEnhancedFlightDetails(
        @Path("faFlightId") faFlightId: String,
        @Query("origin") origin: String? = null,
        @Query("destination") destination: String? = null,
        @Query("date") date: String? = null,
        @Query("fresh") fresh: Boolean? = null
    ): Response<BackendFlightDetailsResponse>

    // ── Live Flights ──

    @GET("/api/flights/live")
    suspend fun getLiveFlights(
        @Query("bounds") bounds: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<BackendLiveFlightsResponse>

    @GET("/api/flights/global")
    suspend fun getGlobalFlights(
        @Query("target") target: Int? = null
    ): Response<BackendLiveFlightsResponse>

    @GET("/api/enhanced-flights/area")
    suspend fun getEnhancedAreaFlights(
        @Query("minLat") minLat: Double,
        @Query("maxLat") maxLat: Double,
        @Query("minLon") minLon: Double,
        @Query("maxLon") maxLon: Double,
        @Query("limit") limit: Int? = null
    ): Response<BackendLiveFlightsResponse>

    @GET("/api/db-flights/bounds")
    suspend fun getDbFlightsByBounds(
        @Query("minLat") minLat: Double,
        @Query("maxLat") maxLat: Double,
        @Query("minLon") minLon: Double,
        @Query("maxLon") maxLon: Double,
        @Query("limit") limit: Int? = null
    ): Response<BackendFlightResponse>

    // ── Position & Track ──

    @GET("/api/aeroapi/flights/{faFlightId}/position")
    suspend fun getFlightPosition(
        @Path("faFlightId") faFlightId: String,
        @Query("maxStale") maxStale: Int? = null,
        @Query("fresh") fresh: Boolean? = null,
        @Query("date") date: String? = null
    ): Response<BackendPositionResponse>

    @GET("/api/aeroapi/flights/{faFlightId}/track")
    suspend fun getFlightTrack(
        @Path("faFlightId") faFlightId: String,
        @Query("tracking") tracking: String? = null
    ): Response<BackendTrackResponse>

    @GET("/api/aeroapi/flights/{faFlightId}/route")
    suspend fun getFlightRoute(
        @Path("faFlightId") faFlightId: String
    ): Response<BackendTrackResponse>

    // ── Airport Operations ──

    @GET("/api/aeroapi/airports/{code}/departures")
    suspend fun getAirportDepartures(
        @Path("code") airportCode: String,
        @Query("limit") limit: Int? = null,
        @Query("max_pages") maxPages: Int? = null,
        @Query("fresh") fresh: Boolean? = null
    ): Response<BackendFlightResponse>

    @GET("/api/aeroapi/airports/{code}/arrivals")
    suspend fun getAirportArrivals(
        @Path("code") airportCode: String,
        @Query("limit") limit: Int? = null,
        @Query("max_pages") maxPages: Int? = null,
        @Query("fresh") fresh: Boolean? = null
    ): Response<BackendFlightResponse>

    @GET("/api/aeroapi/airports/{code}/delays")
    suspend fun getAirportDelays(
        @Path("code") airportCode: String
    ): Response<HealthResponse>

    @GET("/api/aeroapi/airports/{code}/nearby")
    suspend fun getNearbyAirports(
        @Path("code") airportCode: String
    ): Response<BackendFlightResponse>

    // ── Schedules (iOS: 1yr future, 3mo past) ──

    @GET("/api/aeroapi/schedules/{flightNumber}")
    suspend fun getFlightSchedules(
        @Path("flightNumber") flightNumber: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("date") date: String? = null,
        @Query("max_pages") maxPages: Int? = null,
        @Query("fresh") fresh: Boolean? = null
    ): Response<BackendFlightResponse>

    // ── Shared Flights (iOS) ──

    @POST("/api/shared-flights")
    suspend fun createSharedFlight(
        @Body request: BackendShareRequest
    ): Response<BackendSharedFlightResponse>

    @GET("/api/shared-flights/{shareCode}")
    suspend fun getSharedFlight(
        @Path("shareCode") shareCode: String
    ): Response<BackendSharedFlightResponse>

    @POST("/api/shared-flights/{shareCode}/join")
    suspend fun joinSharedFlight(
        @Path("shareCode") shareCode: String,
        @Body request: BackendJoinRequest
    ): Response<BackendSharedFlightResponse>

    @POST("/api/shared-flights/{shareCode}/stop")
    suspend fun stopSharedFlight(
        @Path("shareCode") shareCode: String
    ): Response<BackendSharedFlightResponse>

    // ── Passport / Visa ──

    @GET("/api/passport-visa/check")
    suspend fun checkVisa(
        @Query("passport") passport: String,
        @Query("destination") destination: String
    ): Response<VisaCheckResponse>

    // ── Service Status ──

    @GET("/api/aeroapi/status")
    suspend fun getAeroApiStatus(): Response<HealthResponse>

    @GET("/api/enhanced-flights/service/status")
    suspend fun getServiceStatus(): Response<BackendServiceStatusResponse>

    // ── Alert Subscriptions ──

    @DELETE("/api/alerts/unsubscribe-by-flight")
    suspend fun unsubscribeFromFlight(
        @Query("flightIdent") flightIdent: String,
        @Query("departureDate") departureDate: String? = null
    ): Response<HealthResponse>

    // ── Auth (iOS device registration) ──

    @GET("/api/auth/test")
    suspend fun testAuth(): Response<HealthResponse>

    @GET("/api/auth/me")
    suspend fun getAuthMe(): Response<HealthResponse>
}

package com.trueskies.android.data.remote.api

import com.trueskies.android.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * TrueSkies Retrofit API interface — ported from iOS TrueSkiesBackendService.swift.
 * Covers flight search, details, live positions, and health check endpoints.
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
        @Query("limit") limit: Int? = null
    ): Response<BackendFlightResponse>

    // ── Flight Details ──

    @GET("/api/flights/{id}/details")
    suspend fun getFlightDetails(
        @Path("id") flightId: String,
        @Query("date") date: String? = null
    ): Response<BackendFlightDetailsResponse>

    @GET("/api/enhanced-flights/{faFlightId}")
    suspend fun getEnhancedFlightDetails(
        @Path("faFlightId") faFlightId: String
    ): Response<BackendFlightDetailsResponse>

    // ── Live Flights ──

    @GET("/api/flights/live")
    suspend fun getLiveFlights(
        @Query("bounds") bounds: String? = null,
        @Query("limit") limit: Int? = null
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
        @Path("faFlightId") faFlightId: String
    ): Response<BackendPositionResponse>

    @GET("/api/aeroapi/flights/{faFlightId}/track")
    suspend fun getFlightTrack(
        @Path("faFlightId") faFlightId: String
    ): Response<BackendTrackResponse>

    // ── Airport Operations ──

    @GET("/api/aeroapi/airports/{code}/departures")
    suspend fun getAirportDepartures(
        @Path("code") airportCode: String,
        @Query("limit") limit: Int? = null
    ): Response<BackendFlightResponse>

    @GET("/api/aeroapi/airports/{code}/arrivals")
    suspend fun getAirportArrivals(
        @Path("code") airportCode: String,
        @Query("limit") limit: Int? = null
    ): Response<BackendFlightResponse>

    // ── Schedules ──

    @GET("/api/aeroapi/schedules/{flightNumber}")
    suspend fun getFlightSchedules(
        @Path("flightNumber") flightNumber: String,
        @Query("date") date: String? = null
    ): Response<BackendFlightResponse>

    // ── Service Status ──

    @GET("/api/aeroapi/status")
    suspend fun getAeroApiStatus(): Response<HealthResponse>
}

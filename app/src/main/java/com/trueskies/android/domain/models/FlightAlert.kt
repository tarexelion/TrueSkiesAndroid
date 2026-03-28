package com.trueskies.android.domain.models

import java.util.UUID

/**
 * Flight alert — ported from iOS FlightAlert.swift.
 * Represents a push notification / alert for a tracked flight.
 */
data class FlightAlert(
    val id: String = UUID.randomUUID().toString(),
    val flightId: String,
    val type: FlightAlertType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

enum class FlightAlertType(val displayName: String) {
    DELAY("Delay"),
    GATE_CHANGE("Gate Change"),
    CANCELLATION("Cancellation"),
    BOARDING("Boarding"),
    DEPARTURE("Departure"),
    ARRIVAL("Arrival"),
    TERMINAL_CHANGE("Terminal Change"),
    WEATHER_DELAY("Weather Delay"),
    DIVERSION("Diversion"),
    FILED("Filed"),
    BACK_ON_TIME("Back on Time"),
    ETA("ETA Update"),
    TAXIING("Taxiing"),
    TAKEOFF("Takeoff"),
    LANDING("Landing"),
    AT_GATE("At Gate"),
    SCHEDULE_CHANGE("Schedule Change"),
    REINSTATED("Reinstated"),
    AIRCRAFT_CHANGE("Aircraft Change");
}

package com.trueskies.android.domain.models

import java.util.UUID

/**
 * Flight event — ported from iOS FlightEvent.swift.
 * Represents a timeline event for a tracked flight (gate change, delay, status change, etc.).
 */
data class FlightEvent(
    val id: String = UUID.randomUUID().toString(),
    val flightId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: FlightEventType,
    val previousValue: String? = null,
    val newValue: String? = null,
    val eventDescription: String? = null
) {
    /** Localized description of the event */
    val localizedDescription: String
        get() = eventDescription ?: buildChangeDescription()

    private fun buildChangeDescription(): String {
        return when (eventType) {
            FlightEventType.DEPARTURE_GATE_CHANGE ->
                "Departure gate changed${if (previousValue != null && newValue != null) " from $previousValue to $newValue" else ""}"
            FlightEventType.ARRIVAL_GATE_CHANGE ->
                "Arrival gate changed${if (previousValue != null && newValue != null) " from $previousValue to $newValue" else ""}"
            FlightEventType.DEPARTURE_TERMINAL_CHANGE ->
                "Departure terminal changed${if (newValue != null) " to $newValue" else ""}"
            FlightEventType.ARRIVAL_TERMINAL_CHANGE ->
                "Arrival terminal changed${if (newValue != null) " to $newValue" else ""}"
            FlightEventType.DELAY_UPDATE ->
                "Delay updated${if (newValue != null) " to $newValue minutes" else ""}"
            FlightEventType.BACK_ON_TIME -> "Flight is back on time"
            FlightEventType.SCHEDULE_CHANGE -> "Schedule updated"
            FlightEventType.STATUS_CHANGE ->
                "Status changed${if (newValue != null) " to $newValue" else ""}"
            FlightEventType.CANCELLATION -> "Flight cancelled"
            FlightEventType.REINSTATEMENT -> "Flight reinstated"
            FlightEventType.DIVERSION ->
                "Flight diverted${if (newValue != null) " to $newValue" else ""}"
            FlightEventType.AIRCRAFT_CHANGE ->
                "Aircraft changed${if (newValue != null) " to $newValue" else ""}"
            FlightEventType.DEPARTURE -> "Departed"
            FlightEventType.ARRIVAL -> "Arrived"
            FlightEventType.BOARDING -> "Boarding started"
        }
    }
}

enum class FlightEventType(val displayTitle: String, val symbolName: String) {
    DEPARTURE_GATE_CHANGE("Gate Change", "door.left.hand.open"),
    ARRIVAL_GATE_CHANGE("Arrival Gate", "door.right.hand.open"),
    DEPARTURE_TERMINAL_CHANGE("Terminal Change", "building.2"),
    ARRIVAL_TERMINAL_CHANGE("Arrival Terminal", "building.2"),
    DELAY_UPDATE("Delay Update", "clock.badge.exclamationmark"),
    BACK_ON_TIME("Back on Time", "clock.badge.checkmark"),
    SCHEDULE_CHANGE("Schedule Change", "calendar.badge.clock"),
    STATUS_CHANGE("Status Change", "arrow.triangle.2.circlepath"),
    CANCELLATION("Cancelled", "xmark.circle"),
    REINSTATEMENT("Reinstated", "checkmark.circle"),
    DIVERSION("Diverted", "arrow.uturn.right"),
    AIRCRAFT_CHANGE("Aircraft Change", "airplane"),
    DEPARTURE("Departed", "airplane.departure"),
    ARRIVAL("Arrived", "airplane.arrival"),
    BOARDING("Boarding", "person.fill.checkmark");
}

package com.trueskies.android.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached flights from the API.
 */
@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val id: String,
    val flightNumber: String,
    val originCode: String,
    val destinationCode: String,
    val airlineName: String? = null,
    val airlineIata: String? = null,
    val airlineIcao: String? = null,
    val status: String? = null,
    val scheduledDeparture: String? = null,
    val scheduledArrival: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val progressPercent: Int? = null,
    val departureDelay: Int? = null,
    val arrivalDelay: Int? = null,
    val diverted: Boolean = false,
    val cancelled: Boolean = false,
    val lastFetched: Long = System.currentTimeMillis()
)

/**
 * Room entity for user's personal (tracked) flights.
 */
@Entity(tableName = "personal_flights")
data class PersonalFlightEntity(
    @PrimaryKey val localId: String,
    val flightId: String,
    val flightNumber: String,
    val originCode: String,
    val destinationCode: String,
    val airlineName: String? = null,
    val airlineIata: String? = null,
    val status: String? = null,
    val scheduledDeparture: String? = null,
    val scheduledArrival: String? = null,
    val notes: String? = null,
    val seatNumber: String? = null,
    val seatClass: String? = null,
    val bookingReference: String? = null,
    val customName: String? = null,
    val isNotificationsEnabled: Boolean = true,
    // Boarding pass fields (iOS)
    val sequenceNumber: String? = null,
    val boardingGroup: String? = null,
    val cabinClass: String? = null,
    // Provisional flight support (iOS)
    val isProvisional: Boolean = false,
    val provisionalUntil: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    // Store the full flight JSON for offline reconstruction
    val flightJson: String? = null
)

/**
 * Room entity for shared flights (iOS SharedFlight).
 */
@Entity(tableName = "shared_flights")
data class SharedFlightEntity(
    @PrimaryKey val id: String,
    val flightIdent: String,
    val shareCode: String,
    val sharedByName: String? = null,
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
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for flight events / timeline (iOS FlightEvent).
 */
@Entity(tableName = "flight_events")
data class FlightEventEntity(
    @PrimaryKey val id: String,
    val flightId: String,
    val timestamp: Long,
    val eventType: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val eventDescription: String? = null
)

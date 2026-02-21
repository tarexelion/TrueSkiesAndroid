package com.trueskies.android.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached flights from the API.
 * Stores the raw JSON plus key indexed fields for querying.
 */
@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val id: String,
    val flightNumber: String,
    val originCode: String,
    val destinationCode: String,
    val airlineName: String? = null,
    val airlineIata: String? = null,
    val status: String? = null,
    val scheduledDeparture: String? = null,
    val scheduledArrival: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
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
    val addedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    // Store the full flight JSON for offline reconstruction
    val flightJson: String? = null
)

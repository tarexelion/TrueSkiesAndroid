package com.trueskies.android.domain.models

/**
 * Flight status enums — ported from iOS FlightStatus / PersonalFlightStatus.
 *
 * lifecycleRank ensures status can only advance forward (no regression).
 */
enum class FlightStatus(val lifecycleRank: Int, val displayName: String) {
    UNKNOWN(0, "Unknown"),
    SCHEDULED(1, "Scheduled"),
    FILING(2, "Filing"),
    BOARDING(3, "Boarding"),
    DEPARTED(4, "Departed"),
    TAXIING_OUT(5, "Taxiing"),
    EN_ROUTE(6, "En Route"),
    APPROACH(7, "Approach"),
    TAXIING_IN(8, "Taxiing In"),
    ARRIVED(9, "Arrived"),
    LANDED(10, "Landed"),
    COMPLETED(11, "Completed"),
    CANCELLED(99, "Cancelled"),
    DIVERTED(98, "Diverted");

    val isActive: Boolean
        get() = this in setOf(BOARDING, DEPARTED, TAXIING_OUT, EN_ROUTE, APPROACH, TAXIING_IN)

    val isAirborne: Boolean
        get() = this in setOf(EN_ROUTE, APPROACH)

    val isCompleted: Boolean
        get() = this in setOf(ARRIVED, LANDED, COMPLETED, CANCELLED)

    companion object {
        /** Parse status string from backend API to enum */
        fun fromBackendStatus(status: String?): FlightStatus {
            if (status == null) return UNKNOWN
            return when (status.lowercase().trim()) {
                "scheduled" -> SCHEDULED
                "filed", "filing" -> FILING
                "boarding" -> BOARDING
                "departed", "active_departure" -> DEPARTED
                "taxiing", "taxi", "taxiing_out" -> TAXIING_OUT
                "en route", "enroute", "active", "cruise", "climb" -> EN_ROUTE
                "approach", "landing" -> APPROACH
                "taxiing_in" -> TAXIING_IN
                "arrived" -> ARRIVED
                "landed" -> LANDED
                "completed", "result" -> COMPLETED
                "cancelled", "canceled" -> CANCELLED
                "diverted" -> DIVERTED
                else -> UNKNOWN
            }
        }
    }
}

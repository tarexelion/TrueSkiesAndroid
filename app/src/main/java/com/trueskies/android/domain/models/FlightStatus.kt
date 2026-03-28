package com.trueskies.android.domain.models

/**
 * Flight status enums — ported from iOS Flight.FlightStatus + PersonalFlightStatus.
 *
 * lifecycleRank ensures status can only advance forward (no regression).
 * Matches iOS lifecycleRank values.
 */
enum class FlightStatus(val lifecycleRank: Int, val displayName: String) {
    UNKNOWN(0, "Unknown"),
    SCHEDULED(1, "Scheduled"),
    FILING(2, "Filing"),
    BOARDING(3, "Boarding"),
    DEPARTED(4, "In Flight"),
    TAXIING_OUT(5, "Taxiing"),
    TAKEOFF(6, "Takeoff"),
    CLIMB(7, "Climb"),
    EN_ROUTE(8, "En Route"),
    CRUISE(9, "Cruise"),
    APPROACH(10, "Approach"),
    LANDING(11, "Landing"),
    TAXIING_IN(12, "Taxiing In"),
    ARRIVED(13, "Arrived"),
    LANDED(14, "Landed"),
    COMPLETED(15, "Completed"),
    CANCELLED(99, "Cancelled"),
    DIVERTED(98, "Diverted");

    val isActive: Boolean
        get() = this in setOf(BOARDING, DEPARTED, TAXIING_OUT, TAKEOFF, CLIMB, EN_ROUTE, CRUISE, APPROACH, LANDING, TAXIING_IN)

    val isAirborne: Boolean
        get() = this in setOf(TAKEOFF, CLIMB, EN_ROUTE, CRUISE, APPROACH, LANDING)

    val isCompleted: Boolean
        get() = this in setOf(ARRIVED, LANDED, COMPLETED, CANCELLED)

    val isTerminal: Boolean
        get() = this == CANCELLED || this == DIVERTED

    companion object {
        /** Parse status string from backend API to enum — matches iOS fromBackendStatus */
        fun fromBackendStatus(status: String?): FlightStatus {
            if (status == null) return UNKNOWN
            return when (status.lowercase().trim()) {
                "scheduled", "not operational" -> SCHEDULED
                "filed", "filing" -> FILING
                "delayed" -> SCHEDULED // iOS treats delayed as scheduled with delay
                "boarding", "gate_open" -> BOARDING
                "departed", "active_departure" -> DEPARTED
                "taxiing", "taxi", "taxiing_out", "taxi_out" -> TAXIING_OUT
                "takeoff", "took_off", "took off" -> TAKEOFF
                "climb", "climbing" -> CLIMB
                "en route", "enroute", "active", "cruise", "in_flight", "in flight",
                "in_air", "airborne" -> EN_ROUTE
                "approach", "approaching", "descending", "descent" -> APPROACH
                "landing", "on_approach", "on approach" -> LANDING
                "taxiing_in", "taxi_in" -> TAXIING_IN
                "arrived", "at_gate", "at gate" -> ARRIVED
                "landed", "on_ground" -> LANDED
                "completed", "result", "finished" -> COMPLETED
                "cancelled", "canceled" -> CANCELLED
                "diverted" -> DIVERTED
                else -> UNKNOWN
            }
        }
    }
}

package com.trueskies.android.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.preferences.UserPreferencesRepository
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.domain.models.FlightStatus
import com.trueskies.android.domain.models.PersonalFlight
import com.trueskies.android.ui.screens.FlightLogItem
import com.trueskies.android.ui.screens.FlightLogRange
import com.trueskies.android.ui.screens.FlightLogUiState
import com.trueskies.android.ui.screens.TravelStory
import com.trueskies.android.ui.theme.TrueSkiesColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class FlightLogViewModel @Inject constructor(
    private val repository: FlightRepository,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(FlightLogRange.ALL_TIME)

    val uiState: StateFlow<FlightLogUiState> = combine(
        repository.getPersonalFlightsFlow(),
        _selectedRange
    ) { flights, range ->
        buildUiState(flights.filter { it.isOwnFlight }, range)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlightLogUiState())

    fun selectRange(range: FlightLogRange) {
        _selectedRange.value = range
    }

    private fun buildUiState(flights: List<PersonalFlight>, range: FlightLogRange): FlightLogUiState {
        val filtered = filterByRange(flights, range)
        val availableRanges = computeAvailableRanges(flights)

        val origins = filtered.mapNotNull { it.flight.originCode }.toSet()
        val destinations = filtered.mapNotNull { it.flight.destinationCode }.toSet()
        val allAirportCodes = origins + destinations

        val stories = buildTravelStories(filtered)

        val logItems = filtered.sortedByDescending {
            parseDate(it.flight.scheduledDeparture)
        }.map { pf ->
            val f = pf.flight
            val status = f.status
            FlightLogItem(
                flightId = f.id,
                flightNumber = f.flightNumber,
                origin = f.originCode ?: "???",
                destination = f.destinationCode ?: "???",
                date = f.scheduledDeparture?.let { formatDate(it) } ?: "",
                statusText = status.displayName,
                statusColor = statusColor(status)
            )
        }

        return FlightLogUiState(
            userName = "Traveler", // TODO: get from user profile
            selectedRange = range,
            availableRanges = availableRanges,
            hasFlights = filtered.isNotEmpty(),
            totalFlights = filtered.size,
            countriesVisited = allAirportCodes.size.coerceAtMost(filtered.size),
            totalDistance = formatDistance(filtered),
            totalFlightTime = formatFlightTime(filtered),
            unlockedMilestones = computeMilestones(filtered.size),
            travelStories = stories,
            recentFlights = logItems,
            mapFlights = filtered.map { it.flight }
        )
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return 0L
        return try {
            Instant.parse(dateStr).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private fun filterByRange(flights: List<PersonalFlight>, range: FlightLogRange): List<PersonalFlight> {
        val now = Instant.now()
        val cutoff = when (range) {
            FlightLogRange.ALL_TIME -> 0L
            FlightLogRange.THIS_YEAR -> {
                val year = now.atZone(ZoneId.systemDefault()).year
                LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            FlightLogRange.THIS_MONTH -> {
                val zdt = now.atZone(ZoneId.systemDefault())
                LocalDate.of(zdt.year, zdt.month, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            FlightLogRange.LAST_90_DAYS -> now.minusSeconds(90L * 24 * 3600).toEpochMilli()
            FlightLogRange.LAST_30_DAYS -> now.minusSeconds(30L * 24 * 3600).toEpochMilli()
        }
        return flights.filter { pf ->
            parseDate(pf.flight.scheduledDeparture) >= cutoff
        }
    }

    private fun computeAvailableRanges(flights: List<PersonalFlight>): List<FlightLogRange> {
        if (flights.isEmpty()) return listOf(FlightLogRange.ALL_TIME)
        val ranges = mutableListOf(FlightLogRange.ALL_TIME)
        val now = Instant.now()
        val zdt = now.atZone(ZoneId.systemDefault())

        val yearStart = LocalDate.of(zdt.year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (flights.any { parseDate(it.flight.scheduledDeparture) >= yearStart }) {
            ranges.add(FlightLogRange.THIS_YEAR)
        }
        val monthStart = LocalDate.of(zdt.year, zdt.month, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (flights.any { parseDate(it.flight.scheduledDeparture) >= monthStart }) {
            ranges.add(FlightLogRange.THIS_MONTH)
        }
        if (flights.size > 1) {
            ranges.add(FlightLogRange.LAST_90_DAYS)
            ranges.add(FlightLogRange.LAST_30_DAYS)
        }
        return ranges
    }

    private fun buildTravelStories(flights: List<PersonalFlight>): List<TravelStory> {
        if (flights.isEmpty()) return emptyList()
        val stories = mutableListOf<TravelStory>()

        val longest = flights.maxByOrNull {
            val dep = parseDate(it.flight.scheduledDeparture)
            val arr = parseDate(it.flight.scheduledArrival)
            arr - dep
        }
        if (longest != null) {
            val f = longest.flight
            stories.add(
                TravelStory(
                    title = "Longest Flight",
                    subtitle = "${f.originCode ?: "?"} → ${f.destinationCode ?: "?"} (${f.flightNumber})",
                    icon = Icons.Default.Timeline,
                    iconTint = TrueSkiesColors.AccentCyan
                )
            )
        }

        val airports = flights.flatMap { listOfNotNull(it.flight.originCode, it.flight.destinationCode) }
        val topAirport = airports.groupBy { it }.maxByOrNull { it.value.size }
        if (topAirport != null) {
            stories.add(
                TravelStory(
                    title = "Most Visited",
                    subtitle = "${topAirport.key} (${topAirport.value.size} visits)",
                    icon = Icons.Default.LocationOn,
                    iconTint = TrueSkiesColors.DashboardPurple
                )
            )
        }

        return stories
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val instant = Instant.parse(dateStr)
            DateTimeFormatter.ofPattern("dd MMM yyyy")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (_: Exception) {
            dateStr
        }
    }

    private fun formatDistance(flights: List<PersonalFlight>): String {
        val estimate = flights.size * 1200 // rough average km per flight
        return if (estimate >= 1000) "${estimate / 1000}k km" else "$estimate km"
    }

    private fun formatFlightTime(flights: List<PersonalFlight>): String {
        val totalMinutes = flights.sumOf { pf ->
            val dep = parseDate(pf.flight.scheduledDeparture)
            val arr = parseDate(pf.flight.scheduledArrival)
            ((arr - dep) / 60_000).coerceAtLeast(0)
        }
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    private fun statusColor(status: FlightStatus) = when (status) {
        FlightStatus.SCHEDULED, FlightStatus.LANDED, FlightStatus.ARRIVED, FlightStatus.COMPLETED ->
            TrueSkiesColors.StatusOnTime
        FlightStatus.CANCELLED -> TrueSkiesColors.StatusCancelled
        FlightStatus.DIVERTED -> TrueSkiesColors.StatusDiverted
        else -> TrueSkiesColors.TextMuted
    }

    private fun computeMilestones(flightCount: Int): Int {
        var count = 0
        if (flightCount >= 1) count++
        if (flightCount >= 5) count++
        if (flightCount >= 10) count++
        if (flightCount >= 25) count++
        if (flightCount >= 50) count++
        if (flightCount >= 100) count++
        return count
    }
}

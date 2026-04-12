package com.trueskies.android.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.PersonalFlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class FlightsViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {

    companion object {
        private const val TAG = "FlightsViewModel"
        private const val REFRESH_INTERVAL_MS = 60_000L // Refresh active flights every 60s
        private const val COMPLETED_FLIGHT_HIDE_HOURS = 4L // Hide landed flights after 4 hours
    }

    data class FlightsUiState(
        val personalFlights: List<PersonalFlight> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(FlightsUiState())
    val uiState: StateFlow<FlightsUiState> = _uiState.asStateFlow()

    private var hasRefreshedOnce = false

    init {
        observePersonalFlights()
        startPeriodicRefresh()
    }

    private fun observePersonalFlights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getPersonalFlightsFlow().collect { flights ->
                val visibleFlights = flights.filter { !isCompletedAndExpired(it) }
                _uiState.value = _uiState.value.copy(
                    personalFlights = visibleFlights,
                    isLoading = false
                )
                // Auto-refresh from API on first load to get latest status/times
                if (!hasRefreshedOnce && flights.isNotEmpty()) {
                    hasRefreshedOnce = true
                    refreshFlightsFromApi()
                }
            }
        }
    }

    /**
     * Periodically refresh flights that are active (in-flight, boarding, etc.)
     * to keep status and arrival times up to date on the card.
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                val flights = _uiState.value.personalFlights
                val activeFlights = flights.filter { it.flight.status.isActive }
                if (activeFlights.isNotEmpty()) {
                    Log.d(TAG, "Periodic refresh: ${activeFlights.size} active flights")
                    for (pf in activeFlights) {
                        try {
                            repository.refreshPersonalFlight(pf)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to refresh ${pf.flight.flightNumber}", e)
                        }
                    }
                }
            }
        }
    }

    fun refreshFlights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            refreshFlightsFromApi()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun refreshFlightsFromApi() {
        val currentFlights = _uiState.value.personalFlights
        for (pf in currentFlights) {
            try {
                repository.refreshPersonalFlight(pf)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh ${pf.flight.flightNumber}", e)
            }
        }
    }

    /**
     * Returns true if a flight's status is completed/landed/arrived/cancelled
     * and its arrival time was more than [COMPLETED_FLIGHT_HIDE_HOURS] hours ago.
     */
    private fun isCompletedAndExpired(pf: PersonalFlight): Boolean {
        if (!pf.flight.status.isCompleted) return false
        val arrivalTimeStr = pf.flight.bestArrivalTime ?: return false
        return try {
            val arrivalTime = ZonedDateTime.parse(arrivalTimeStr)
            val hoursSinceArrival = Duration.between(arrivalTime, ZonedDateTime.now()).toHours()
            hoursSinceArrival >= COMPLETED_FLIGHT_HIDE_HOURS
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFlight(localId: String) {
        viewModelScope.launch {
            repository.deletePersonalFlight(localId)
        }
    }

    fun addFlight(flight: Flight) {
        viewModelScope.launch {
            repository.addPersonalFlight(flight)
        }
    }
}

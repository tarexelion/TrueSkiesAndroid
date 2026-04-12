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
class MapViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {

    data class MapUiState(
        val flights: List<Flight> = emptyList(),
        val personalFlights: List<Flight> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isBackendHealthy: Boolean = true
    ) {
        /** All flights to display on map: personal flights + area flights (deduplicated) */
        val allFlights: List<Flight>
            get() {
                val personalIds = personalFlights.map { it.id }.toSet()
                val areaOnly = flights.filter { it.id !in personalIds }
                return personalFlights + areaOnly
            }
    }

    companion object {
        private const val TAG = "MapViewModel"
        private const val POSITION_POLL_INTERVAL_MS = 15_000L // 15 seconds
        private const val COMPLETED_FLIGHT_HIDE_HOURS = 4L
    }

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** IDs of personal flights currently in Room — used to prevent race conditions */
    private val activePersonalFlightIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        checkBackendHealth()
        observePersonalFlights()
        startPositionPolling()
    }

    private fun observePersonalFlights() {
        viewModelScope.launch {
            var firstLoad = true
            repository.getPersonalFlightsFlow().collect { personalFlights ->
                val activeFlights = personalFlights
                    .filter { !isCompletedAndExpired(it) }
                    .map { it.flight }
                activePersonalFlightIds.value = activeFlights.map { it.id }.toSet()
                _uiState.value = _uiState.value.copy(
                    personalFlights = activeFlights
                )
                // Fetch live positions immediately on first load
                if (firstLoad && personalFlights.isNotEmpty()) {
                    firstLoad = false
                    refreshPersonalFlightPositions()
                }
            }
        }
    }

    /**
     * Periodically fetches live positions for personal flights that are active (en route).
     * Merges position data into the stored flight so the airplane marker moves on the map.
     */
    private fun startPositionPolling() {
        viewModelScope.launch {
            while (true) {
                delay(POSITION_POLL_INTERVAL_MS)
                refreshPersonalFlightPositions()
            }
        }
    }

    private suspend fun refreshPersonalFlightPositions() {
        val currentPersonal = _uiState.value.personalFlights
        if (currentPersonal.isEmpty()) return

        val updated = currentPersonal.map { flight ->
            try {
                // Fetch live position from API
                val posResult = repository.getFlightPosition(flight.id)
                val withPosition = posResult.getOrNull()?.let { posFlight ->
                    if (posFlight.latitude != null && posFlight.longitude != null) {
                        flight.copy(
                            latitude = posFlight.latitude,
                            longitude = posFlight.longitude,
                            altitude = posFlight.altitude ?: flight.altitude,
                            heading = posFlight.heading ?: flight.heading,
                            speed = posFlight.speed ?: flight.speed
                        )
                    } else flight
                } ?: flight

                // Fetch actual flight track (real path traveled)
                val trackResult = repository.getFlightTrack(flight.id)
                trackResult.getOrNull()?.let { trackPoints ->
                    if (trackPoints.isNotEmpty()) {
                        withPosition.copy(trackPoints = trackPoints)
                    } else withPosition
                } ?: withPosition
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch position/track for ${flight.flightNumber}", e)
                flight
            }
        }

        // Filter against Room source of truth to prevent re-adding deleted flights
        val validIds = activePersonalFlightIds.value
        val filtered = updated.filter { it.id in validIds }
        _uiState.value = _uiState.value.copy(personalFlights = filtered)
    }

    fun loadFlightsInBounds(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getLiveFlights(minLat, maxLat, minLon, maxLon)
            result.fold(
                onSuccess = { flights ->
                    _uiState.value = _uiState.value.copy(
                        flights = flights,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    private fun isCompletedAndExpired(pf: PersonalFlight): Boolean {
        if (!pf.flight.status.isCompleted) return false
        val arrivalTimeStr = pf.flight.bestArrivalTime ?: return false
        return try {
            val arrivalTime = ZonedDateTime.parse(arrivalTimeStr)
            Duration.between(arrivalTime, ZonedDateTime.now()).toHours() >= COMPLETED_FLIGHT_HIDE_HOURS
        } catch (e: Exception) {
            false
        }
    }

    private fun checkBackendHealth() {
        viewModelScope.launch {
            val healthy = repository.checkHealth()
            _uiState.value = _uiState.value.copy(isBackendHealthy = healthy)
        }
    }
}

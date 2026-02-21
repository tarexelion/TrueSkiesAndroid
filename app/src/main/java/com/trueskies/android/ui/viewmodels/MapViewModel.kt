package com.trueskies.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.domain.models.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {

    data class MapUiState(
        val flights: List<Flight> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isBackendHealthy: Boolean = true
    )

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        checkBackendHealth()
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

    private fun checkBackendHealth() {
        viewModelScope.launch {
            val healthy = repository.checkHealth()
            _uiState.value = _uiState.value.copy(isBackendHealthy = healthy)
        }
    }
}

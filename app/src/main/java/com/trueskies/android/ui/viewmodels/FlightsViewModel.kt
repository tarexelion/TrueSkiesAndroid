package com.trueskies.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.PersonalFlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlightsViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {

    data class FlightsUiState(
        val personalFlights: List<PersonalFlight> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(FlightsUiState())
    val uiState: StateFlow<FlightsUiState> = _uiState.asStateFlow()

    init {
        observePersonalFlights()
    }

    private fun observePersonalFlights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getPersonalFlightsFlow().collect { flights ->
                _uiState.value = _uiState.value.copy(
                    personalFlights = flights,
                    isLoading = false
                )
            }
        }
    }

    fun refreshFlights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val currentFlights = _uiState.value.personalFlights
            for (pf in currentFlights) {
                repository.refreshPersonalFlight(pf)
            }
            _uiState.value = _uiState.value.copy(isRefreshing = false)
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

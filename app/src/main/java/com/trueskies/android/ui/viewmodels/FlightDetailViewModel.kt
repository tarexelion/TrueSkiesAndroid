package com.trueskies.android.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
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
class FlightDetailViewModel @Inject constructor(
    private val repository: FlightRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class DetailUiState(
        val flight: Flight? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val isAddedToPersonal: Boolean = false,
        val seat: String = "",
        val notes: String = ""
    )

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val flightId: String = savedStateHandle.get<String>("flightId") ?: ""
    private var personalFlightLocalId: String? = null

    init {
        if (flightId.isNotEmpty()) {
            loadFlightDetails()
            observePersonalFlights()
        }
    }

    private fun observePersonalFlights() {
        viewModelScope.launch {
            repository.getPersonalFlightsFlow().collect { flights ->
                val matched = flights.find { it.flight.id == flightId }
                personalFlightLocalId = matched?.localId
                _uiState.value = _uiState.value.copy(isAddedToPersonal = matched != null)
            }
        }
    }

    private fun loadFlightDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getFlightDetails(flightId)
            result.fold(
                onSuccess = { flight ->
                    _uiState.value = _uiState.value.copy(
                        flight = flight,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load flight details"
                    )
                }
            )
        }
    }

    fun addToPersonalFlights() {
        val flight = _uiState.value.flight ?: return
        viewModelScope.launch {
            repository.addPersonalFlight(flight)
        }
    }

    fun deleteFromPersonalFlights() {
        val localId = personalFlightLocalId ?: return
        viewModelScope.launch {
            repository.deletePersonalFlight(localId)
        }
    }

    fun updateSeat(seat: String) {
        _uiState.value = _uiState.value.copy(seat = seat)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun refresh() {
        loadFlightDetails()
    }
}

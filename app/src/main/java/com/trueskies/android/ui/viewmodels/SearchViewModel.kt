package com.trueskies.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.domain.models.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {

    data class SearchUiState(
        val query: String = "",
        val results: List<Flight> = emptyList(),
        val isSearching: Boolean = false,
        val error: String? = null,
        val hasSearched: Boolean = false
    )

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        // Debounce search
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400) // 400ms debounce
                performSearch(query)
            }
        } else {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
        }
    }

    fun search() {
        val query = _uiState.value.query
        if (query.isNotEmpty()) {
            searchJob?.cancel()
            // Set isSearching synchronously so the UI knows immediately
            // (before the coroutine starts and before the keyboard hides)
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            searchJob = viewModelScope.launch {
                performSearch(query)
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, error = null)
        val result = repository.searchFlights(query)
        result.fold(
            onSuccess = { flights ->
                // Show results immediately
                _uiState.value = _uiState.value.copy(
                    results = flights,
                    isSearching = false,
                    hasSearched = true
                )
                // Enrich flights that have missing airport codes in the background
                enrichFlights(flights)
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = e.message,
                    hasSearched = true
                )
            }
        )
    }

    private fun enrichFlights(flights: List<Flight>) {
        val needsEnrichment = flights.filter {
            it.originCode == "???" || it.destinationCode == "???"
                    || (it.originCity == null && it.destinationCity == null)
        }
        if (needsEnrichment.isEmpty()) return

        viewModelScope.launch {
            val enriched = needsEnrichment.map { flight ->
                async {
                    repository.getFlightDetails(flight.id).getOrNull()?.let { detail ->
                        flight.id to detail
                    }
                }
            }.awaitAll().filterNotNull().toMap()

            if (enriched.isNotEmpty()) {
                val currentState = _uiState.value
                val updatedResults = currentState.results.map { flight ->
                    enriched[flight.id] ?: flight
                }
                _uiState.value = currentState.copy(results = updatedResults)
            }
        }
    }

    fun addToPersonalFlights(flight: Flight) {
        viewModelScope.launch {
            repository.addPersonalFlight(flight)
        }
    }
}

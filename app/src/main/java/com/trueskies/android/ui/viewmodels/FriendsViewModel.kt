package com.trueskies.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.ui.screens.FriendsUiState
import com.trueskies.android.ui.screens.SharedFlightUiItem
import com.trueskies.android.ui.theme.TrueSkiesColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {

    val uiState: StateFlow<FriendsUiState> = repository.getSharedFlightsFlow()
        .map { sharedFlights ->
            FriendsUiState(
                isLoading = false,
                sharedFlights = sharedFlights.map { sf ->
                    val statusStr = sf.status ?: "Unknown"
                    SharedFlightUiItem(
                        id = sf.id,
                        flightId = sf.id,
                        friendName = sf.sharedByName ?: "Friend",
                        flightNumber = sf.flightIdent,
                        origin = sf.origin ?: "???",
                        destination = sf.destination ?: "???",
                        statusText = statusStr.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        statusColor = when {
                            statusStr.contains("CANCEL", ignoreCase = true) -> TrueSkiesColors.StatusCancelled
                            statusStr.contains("DELAY", ignoreCase = true) -> TrueSkiesColors.StatusDelayed
                            statusStr.contains("DIVERT", ignoreCase = true) -> TrueSkiesColors.StatusDiverted
                            else -> TrueSkiesColors.StatusOnTime
                        }
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState(isLoading = true))

    fun joinSharedFlight(shareCode: String) {
        viewModelScope.launch {
            repository.joinSharedFlight(shareCode)
        }
    }

    fun removeSharedFlight(shareCode: String) {
        viewModelScope.launch {
            repository.stopSharedFlight(shareCode)
        }
    }
}

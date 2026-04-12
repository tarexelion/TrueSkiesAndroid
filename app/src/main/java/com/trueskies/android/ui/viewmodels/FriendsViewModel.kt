package com.trueskies.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.local.entities.SharedFlightEntity
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.PersonalFlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {

    /** Shared flights converted to PersonalFlight so FlightCard can render them */
    val sharedFlightsAsPersonal: StateFlow<List<SharedPersonalFlight>> =
        repository.getSharedFlightsFlow()
            .map { entities ->
                entities.map { sf -> SharedPersonalFlight(sf) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val personalFlights: StateFlow<List<PersonalFlight>> = repository.getPersonalFlightsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _shareResult = MutableStateFlow<ShareFlightResult>(ShareFlightResult.Idle)
    val shareResult: StateFlow<ShareFlightResult> = _shareResult.asStateFlow()

    private val _joinResult = MutableStateFlow<JoinFlightResult>(JoinFlightResult.Idle)
    val joinResult: StateFlow<JoinFlightResult> = _joinResult.asStateFlow()

    fun createShareForFlight(personalFlight: PersonalFlight) {
        viewModelScope.launch {
            _shareResult.value = ShareFlightResult.Loading
            val result = repository.createSharedFlight(
                flight = personalFlight.flight,
                userId = "android-user",
                displayName = "Android User"
            )
            _shareResult.value = result.fold(
                onSuccess = { shareCode ->
                    ShareFlightResult.Success(
                        shareCode = shareCode,
                        flightIdent = personalFlight.flight.flightNumber,
                        origin = personalFlight.flight.originCode,
                        destination = personalFlight.flight.destinationCode,
                        airlineName = personalFlight.flight.airlineName
                    )
                },
                onFailure = { ShareFlightResult.Error(it.message ?: "Failed to create share") }
            )
        }
    }

    fun resetShareResult() {
        _shareResult.value = ShareFlightResult.Idle
    }

    fun resetJoinResult() {
        _joinResult.value = JoinFlightResult.Idle
    }

    fun joinSharedFlight(shareCode: String) {
        viewModelScope.launch {
            _joinResult.value = JoinFlightResult.Loading
            val result = repository.joinSharedFlight(shareCode)
            _joinResult.value = result.fold(
                onSuccess = { JoinFlightResult.Success },
                onFailure = { JoinFlightResult.Error(it.message ?: "Failed to join flight") }
            )
        }
    }

    fun removeSharedFlight(shareCode: String) {
        viewModelScope.launch {
            repository.stopSharedFlight(shareCode)
        }
    }
}

/** Wrapper that pairs a SharedFlightEntity with a synthetic PersonalFlight for FlightCard */
data class SharedPersonalFlight(
    val entity: SharedFlightEntity
) {
    val personalFlight: PersonalFlight by lazy {
        val flight = Flight(
            id = entity.id,
            flightNumber = entity.flightIdent,
            displayFlightNumber = entity.flightIdent,
            airlineName = entity.airline,
            airlineIata = entity.airline?.take(2),
            originCode = entity.origin ?: "???",
            destinationCode = entity.destination ?: "???",
            scheduledDeparture = entity.departureDate,
            estimatedDeparture = entity.departureDate,
            scheduledArrival = entity.arrivalDate,
            estimatedArrival = entity.arrivalDate,
            departureDelay = entity.departureDelay,
            arrivalDelay = entity.arrivalDelay,
            departureGate = entity.departureGate,
            arrivalGate = entity.arrivalGate,
            rawStatus = entity.status
        )
        PersonalFlight(
            localId = entity.id,
            flight = flight
        )
    }
}

sealed class ShareFlightResult {
    data object Idle : ShareFlightResult()
    data object Loading : ShareFlightResult()
    data class Success(
        val shareCode: String,
        val flightIdent: String,
        val origin: String,
        val destination: String,
        val airlineName: String?
    ) : ShareFlightResult() {
        val shareUrl: String get() {
            val params = mutableListOf(
                "flight" to flightIdent,
                "code" to shareCode,
                "origin" to origin,
                "destination" to destination
            )
            airlineName?.let { params.add("airline" to it) }
            val query = params.joinToString("&") { (k, v) ->
                "$k=${java.net.URLEncoder.encode(v, "UTF-8")}"
            }
            return "https://trueskiesapp.com/share/?$query"
        }
        val shareText: String get() = buildString {
            airlineName?.let { appendLine(it) }
            appendLine("$flightIdent: $origin → $destination")
            appendLine()
            appendLine("Track this flight live:")
            append(shareUrl)
        }
    }
    data class Error(val message: String) : ShareFlightResult()
}

sealed class JoinFlightResult {
    data object Idle : JoinFlightResult()
    data object Loading : JoinFlightResult()
    data object Success : JoinFlightResult()
    data class Error(val message: String) : JoinFlightResult()
}

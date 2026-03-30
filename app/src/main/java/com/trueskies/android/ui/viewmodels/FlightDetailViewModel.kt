package com.trueskies.android.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.remote.api.OpenMeteoApi
import com.trueskies.android.data.preferences.UserPreferencesRepository
import com.trueskies.android.data.remote.api.TrueSkiesApi
import com.trueskies.android.data.repository.FlightRepository
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.VisaRequirement
import com.trueskies.android.domain.models.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    private val repository: FlightRepository,
    private val openMeteoApi: OpenMeteoApi,
    private val trueSkiesApi: TrueSkiesApi,
    private val userPrefs: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class DetailUiState(
        val flight: Flight? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val isAddedToPersonal: Boolean = false,
        val seat: String = "",
        val notes: String = "",
        val arrivalWeather: WeatherInfo? = null,
        val weatherLoading: Boolean = false,
        val weatherError: String? = null,
        val visaRequirement: VisaRequirement? = null,
        val visaLoading: Boolean = false,
        val visaError: String? = null
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
                    loadArrivalWeather(flight)
                    loadVisaRequirement(flight)
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

    private fun loadArrivalWeather(flight: Flight) {
        val lat = flight.destinationLat ?: return
        val lon = flight.destinationLon ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(weatherLoading = true, weatherError = null)
            try {
                val response = openMeteoApi.getCurrentWeather(latitude = lat, longitude = lon)
                if (response.isSuccessful) {
                    val body = response.body()
                    val current = body?.current
                    if (current != null) {
                        val weather = WeatherInfo(
                            airportCode = flight.destinationCode,
                            temperature = current.temperature?.toInt() ?: 0,
                            feelsLike = current.feelsLike?.toInt() ?: 0,
                            conditions = WeatherInfo.conditionsFromWmoCode(current.weatherCode),
                            windSpeed = current.windSpeed?.toInt(),
                            windDirection = WeatherInfo.cardinalDirection(current.windDirection),
                            humidity = current.humidity
                        )
                        _uiState.value = _uiState.value.copy(
                            arrivalWeather = weather,
                            weatherLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            weatherLoading = false,
                            weatherError = "No weather data"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        weatherLoading = false,
                        weatherError = "Weather unavailable"
                    )
                }
            } catch (e: Exception) {
                Log.w("FlightDetailVM", "Weather fetch failed", e)
                _uiState.value = _uiState.value.copy(
                    weatherLoading = false,
                    weatherError = "Unable to load"
                )
            }
        }
    }

    private fun loadVisaRequirement(flight: Flight) {
        val destCountry = flight.destinationCountry ?: return

        viewModelScope.launch {
            val passportCountry = userPrefs.citizenshipCountry.first().ifEmpty {
                _uiState.value = _uiState.value.copy(
                    visaLoading = false,
                    visaError = "Set your home country in Settings"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(visaLoading = true, visaError = null)
            try {
                val response = trueSkiesApi.checkVisa(
                    passport = passportCountry,
                    destination = destCountry
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.requirement != null) {
                        _uiState.value = _uiState.value.copy(
                            visaRequirement = VisaRequirement.fromResponse(
                                requirement = body.requirement,
                                visaRequired = body.visaRequired,
                                visaFree = body.visaFree,
                                passportCountry = passportCountry,
                                destinationCountry = destCountry
                            ),
                            visaLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            visaLoading = false,
                            visaError = "No visa data available"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        visaLoading = false,
                        visaError = "Unable to load visa requirements"
                    )
                }
            } catch (e: Exception) {
                Log.w("FlightDetailVM", "Visa check failed", e)
                _uiState.value = _uiState.value.copy(
                    visaLoading = false,
                    visaError = "Unable to load visa requirements"
                )
            }
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

    // ── Share ────────────────────────────────────────────────

    private val _shareResult = MutableStateFlow<ShareFlightResult>(ShareFlightResult.Idle)
    val shareResult: StateFlow<ShareFlightResult> = _shareResult.asStateFlow()

    fun createShare() {
        val flight = _uiState.value.flight ?: return
        viewModelScope.launch {
            _shareResult.value = ShareFlightResult.Loading
            val result = repository.createSharedFlight(
                flight = flight,
                userId = "android-user",
                displayName = null
            )
            _shareResult.value = result.fold(
                onSuccess = { shareCode ->
                    ShareFlightResult.Success(
                        shareCode = shareCode,
                        flightIdent = flight.flightNumber,
                        origin = flight.originCode,
                        destination = flight.destinationCode,
                        airlineName = flight.airlineName
                    )
                },
                onFailure = { ShareFlightResult.Error(it.message ?: "Failed to create share") }
            )
        }
    }

    fun resetShareResult() {
        _shareResult.value = ShareFlightResult.Idle
    }
}

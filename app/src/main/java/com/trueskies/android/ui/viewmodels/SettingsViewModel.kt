package com.trueskies.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    data class SettingsUiState(
        val notificationsEnabled: Boolean = true,
        val departureAlerts: Boolean = true,
        val gateChangeAlerts: Boolean = true,
        val statusChangeAlerts: Boolean = true,
        val useImperialUnits: Boolean = true,
        val use24HourTime: Boolean = true
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.notificationsEnabled,
        prefs.departureAlerts,
        prefs.gateChangeAlerts,
        prefs.statusChangeAlerts,
        prefs.useImperialUnits,
        prefs.use24HourTime
    ) { values ->
        SettingsUiState(
            notificationsEnabled  = values[0] as Boolean,
            departureAlerts       = values[1] as Boolean,
            gateChangeAlerts      = values[2] as Boolean,
            statusChangeAlerts    = values[3] as Boolean,
            useImperialUnits      = values[4] as Boolean,
            use24HourTime         = values[5] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotificationsEnabled(v: Boolean) = viewModelScope.launch { prefs.setNotificationsEnabled(v) }
    fun setDepartureAlerts(v: Boolean)      = viewModelScope.launch { prefs.setDepartureAlerts(v) }
    fun setGateChangeAlerts(v: Boolean)     = viewModelScope.launch { prefs.setGateChangeAlerts(v) }
    fun setStatusChangeAlerts(v: Boolean)   = viewModelScope.launch { prefs.setStatusChangeAlerts(v) }
    fun setUseImperialUnits(v: Boolean)     = viewModelScope.launch { prefs.setUseImperialUnits(v) }
    fun setUse24HourTime(v: Boolean)        = viewModelScope.launch { prefs.setUse24HourTime(v) }
}

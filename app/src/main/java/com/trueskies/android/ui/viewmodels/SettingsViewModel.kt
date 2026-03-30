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
        val use24HourTime: Boolean = true,
        val nervousFlyerEnabled: Boolean = false,
        val shareDiagnostics: Boolean = true,
        val citizenshipCountry: String = ""
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            prefs.notificationsEnabled,
            prefs.departureAlerts,
            prefs.gateChangeAlerts,
            prefs.statusChangeAlerts
        ) { a -> a },
        combine(
            prefs.useImperialUnits,
            prefs.use24HourTime,
            prefs.nervousFlyerEnabled,
            prefs.shareDiagnostics
        ) { b -> b },
        prefs.citizenshipCountry
    ) { a, b, citizenship ->
        SettingsUiState(
            notificationsEnabled  = a[0] as Boolean,
            departureAlerts       = a[1] as Boolean,
            gateChangeAlerts      = a[2] as Boolean,
            statusChangeAlerts    = a[3] as Boolean,
            useImperialUnits      = b[0] as Boolean,
            use24HourTime         = b[1] as Boolean,
            nervousFlyerEnabled   = b[2] as Boolean,
            shareDiagnostics      = b[3] as Boolean,
            citizenshipCountry    = citizenship
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotificationsEnabled(v: Boolean) = viewModelScope.launch { prefs.setNotificationsEnabled(v) }
    fun setDepartureAlerts(v: Boolean)      = viewModelScope.launch { prefs.setDepartureAlerts(v) }
    fun setGateChangeAlerts(v: Boolean)     = viewModelScope.launch { prefs.setGateChangeAlerts(v) }
    fun setStatusChangeAlerts(v: Boolean)   = viewModelScope.launch { prefs.setStatusChangeAlerts(v) }
    fun setUseImperialUnits(v: Boolean)     = viewModelScope.launch { prefs.setUseImperialUnits(v) }
    fun setUse24HourTime(v: Boolean)        = viewModelScope.launch { prefs.setUse24HourTime(v) }
    fun setNervousFlyerEnabled(v: Boolean)  = viewModelScope.launch { prefs.setNervousFlyerEnabled(v) }
    fun setShareDiagnostics(v: Boolean)     = viewModelScope.launch { prefs.setShareDiagnostics(v) }
    fun setCitizenshipCountry(v: String)   = viewModelScope.launch { prefs.setCitizenshipCountry(v) }
}

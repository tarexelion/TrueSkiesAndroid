package com.trueskies.android.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.auth.AuthRepository
import com.trueskies.android.data.auth.AuthState
import com.trueskies.android.data.auth.AuthUser
import com.trueskies.android.data.local.TrueSkiesDatabase
import com.trueskies.android.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val database: TrueSkiesDatabase,
    private val authRepository: AuthRepository
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
        val citizenshipCountry: String = "",
        val reduceVisualEffects: Boolean = false,
        val displayName: String = "",
        val unitsPreference: String = "automatic",
        val timeFormatPreference: String = "automatic",
        // Auth
        val authState: AuthState = AuthState.NotAuthenticated,
        val authError: String? = null,
        val isSigningIn: Boolean = false
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
            prefs.shareDiagnostics,
            prefs.reduceVisualEffects
        ) { b -> b },
        combine(
            prefs.citizenshipCountry,
            prefs.displayName,
            prefs.unitsPreference,
            prefs.timeFormatPreference
        ) { c -> c },
        authRepository.authState
    ) { a, b, c, auth ->
        SettingsUiState(
            notificationsEnabled  = a[0] as Boolean,
            departureAlerts       = a[1] as Boolean,
            gateChangeAlerts      = a[2] as Boolean,
            statusChangeAlerts    = a[3] as Boolean,
            useImperialUnits      = b[0] as Boolean,
            use24HourTime         = b[1] as Boolean,
            nervousFlyerEnabled   = b[2] as Boolean,
            shareDiagnostics      = b[3] as Boolean,
            reduceVisualEffects   = b[4] as Boolean,
            citizenshipCountry    = c[0] as String,
            displayName           = c[1] as String,
            unitsPreference       = c[2] as String,
            timeFormatPreference  = c[3] as String,
            authState             = auth
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _signInState = MutableStateFlow<SignInEvent?>(null)
    val signInEvent: StateFlow<SignInEvent?> = _signInState

    fun setNotificationsEnabled(v: Boolean) = viewModelScope.launch { prefs.setNotificationsEnabled(v) }
    fun setDepartureAlerts(v: Boolean)      = viewModelScope.launch { prefs.setDepartureAlerts(v) }
    fun setGateChangeAlerts(v: Boolean)     = viewModelScope.launch { prefs.setGateChangeAlerts(v) }
    fun setStatusChangeAlerts(v: Boolean)   = viewModelScope.launch { prefs.setStatusChangeAlerts(v) }
    fun setUseImperialUnits(v: Boolean)     = viewModelScope.launch { prefs.setUseImperialUnits(v) }
    fun setUse24HourTime(v: Boolean)        = viewModelScope.launch { prefs.setUse24HourTime(v) }
    fun setNervousFlyerEnabled(v: Boolean)  = viewModelScope.launch { prefs.setNervousFlyerEnabled(v) }
    fun setShareDiagnostics(v: Boolean)     = viewModelScope.launch { prefs.setShareDiagnostics(v) }
    fun setCitizenshipCountry(v: String)    = viewModelScope.launch { prefs.setCitizenshipCountry(v) }
    fun setReduceVisualEffects(v: Boolean)  = viewModelScope.launch { prefs.setReduceVisualEffects(v) }
    fun setDisplayName(v: String)           = viewModelScope.launch { prefs.setDisplayName(v) }
    fun setUnitsPreference(v: String)       = viewModelScope.launch { prefs.setUnitsPreference(v) }
    fun setTimeFormatPreference(v: String)  = viewModelScope.launch { prefs.setTimeFormatPreference(v) }

    /**
     * Triggers Google Sign-In. Needs an Activity context for Credential Manager.
     */
    fun signInWithGoogle(activityContext: Context) = viewModelScope.launch {
        _signInState.value = SignInEvent.Loading
        val result = authRepository.signInWithGoogle(activityContext)
        result.fold(
            onSuccess = { user ->
                // Sync Google display name to local prefs if no custom name set
                if (uiState.value.displayName.isBlank() && !user.displayName.isNullOrBlank()) {
                    prefs.setDisplayName(user.displayName)
                }
                _signInState.value = SignInEvent.Success(user)
            },
            onFailure = { e ->
                _signInState.value = SignInEvent.Error(
                    e.message ?: "Sign-in failed"
                )
            }
        )
    }

    fun signOut() = viewModelScope.launch {
        authRepository.signOut()
        _signInState.value = null
    }

    fun clearSignInEvent() {
        _signInState.value = null
    }

    fun deleteAllData() = viewModelScope.launch {
        authRepository.signOut()
        database.clearAllTables()
        prefs.clearAllData()
    }

    sealed class SignInEvent {
        data object Loading : SignInEvent()
        data class Success(val user: AuthUser) : SignInEvent()
        data class Error(val message: String) : SignInEvent()
    }
}

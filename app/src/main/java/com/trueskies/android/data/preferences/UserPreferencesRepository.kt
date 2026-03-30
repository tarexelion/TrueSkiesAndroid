package com.trueskies.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NOTIFICATIONS_ENABLED    = booleanPreferencesKey("notifications_enabled")
        val DEPARTURE_ALERTS         = booleanPreferencesKey("departure_alerts")
        val GATE_CHANGE_ALERTS       = booleanPreferencesKey("gate_change_alerts")
        val STATUS_CHANGE_ALERTS     = booleanPreferencesKey("status_change_alerts")
        val USE_IMPERIAL_UNITS       = booleanPreferencesKey("use_imperial_units")
        val USE_24_HOUR_TIME         = booleanPreferencesKey("use_24_hour_time")
        val NERVOUS_FLYER_ENABLED    = booleanPreferencesKey("nervous_flyer_enabled")
        val SHARE_DIAGNOSTICS        = booleanPreferencesKey("share_diagnostics")
        val CITIZENSHIP_COUNTRY      = stringPreferencesKey("citizenship_country")
    }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val departureAlerts: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DEPARTURE_ALERTS] ?: true }

    val gateChangeAlerts: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.GATE_CHANGE_ALERTS] ?: true }

    val statusChangeAlerts: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.STATUS_CHANGE_ALERTS] ?: true }

    val useImperialUnits: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.USE_IMPERIAL_UNITS] ?: true }

    val use24HourTime: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.USE_24_HOUR_TIME] ?: true }

    val nervousFlyerEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NERVOUS_FLYER_ENABLED] ?: false }

    val shareDiagnostics: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SHARE_DIAGNOSTICS] ?: true }

    val citizenshipCountry: Flow<String> =
        context.dataStore.data.map { it[Keys.CITIZENSHIP_COUNTRY] ?: "" }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDepartureAlerts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEPARTURE_ALERTS] = enabled }
    }

    suspend fun setGateChangeAlerts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GATE_CHANGE_ALERTS] = enabled }
    }

    suspend fun setStatusChangeAlerts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STATUS_CHANGE_ALERTS] = enabled }
    }

    suspend fun setUseImperialUnits(imperial: Boolean) {
        context.dataStore.edit { it[Keys.USE_IMPERIAL_UNITS] = imperial }
    }

    suspend fun setUse24HourTime(use24h: Boolean) {
        context.dataStore.edit { it[Keys.USE_24_HOUR_TIME] = use24h }
    }

    suspend fun setNervousFlyerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NERVOUS_FLYER_ENABLED] = enabled }
    }

    suspend fun setShareDiagnostics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHARE_DIAGNOSTICS] = enabled }
    }

    suspend fun setCitizenshipCountry(country: String) {
        context.dataStore.edit { it[Keys.CITIZENSHIP_COUNTRY] = country }
    }
}

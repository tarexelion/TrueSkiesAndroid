package com.trueskies.android.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueskies.android.data.auth.AuthRepository
import com.trueskies.android.data.local.TrueSkiesDatabase
import com.trueskies.android.data.local.dao.PersonalFlightDao
import com.trueskies.android.data.local.dao.SharedFlightDao
import com.trueskies.android.data.local.dao.FlightEventDao
import com.trueskies.android.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val database: TrueSkiesDatabase,
    private val authRepository: AuthRepository,
    private val personalFlightDao: PersonalFlightDao,
    private val sharedFlightDao: SharedFlightDao,
    private val flightEventDao: FlightEventDao,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    data class AccountUiState(
        val displayName: String = "",
        val citizenshipCountry: String = "",
        val databaseSizeFormatted: String = "Calculating...",
        val cacheSizeFormatted: String = "Calculating...",
        val preferencesSizeFormatted: String = "Calculating...",
        val totalStorageFormatted: String = "Calculating..."
    )

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        // Observe preferences
        viewModelScope.launch {
            combine(
                prefs.displayName,
                prefs.citizenshipCountry
            ) { name, country ->
                name to country
            }.collect { (name, country) ->
                _uiState.update { it.copy(displayName = name, citizenshipCountry = country) }
            }
        }

        // Calculate storage sizes
        viewModelScope.launch {
            calculateStorageSizes()
        }
    }

    fun setDisplayName(name: String) = viewModelScope.launch {
        prefs.setDisplayName(name)
    }

    fun setCitizenshipCountry(country: String) = viewModelScope.launch {
        prefs.setCitizenshipCountry(country)
    }

    fun clearCache(context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()
        }
        calculateStorageSizes()
    }

    fun deleteAllData() = viewModelScope.launch {
        authRepository.signOut()
        database.clearAllTables()
        prefs.clearAllData()
        withContext(Dispatchers.IO) {
            appContext.cacheDir.deleteRecursively()
            appContext.cacheDir.mkdirs()
        }
        calculateStorageSizes()
    }

    fun exportDataAsJson(context: Context, onFileReady: (File) -> Unit) = viewModelScope.launch {
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        val exportData = withContext(Dispatchers.IO) {
            val flights = personalFlightDao.getAllPersonalFlightsSnapshot()
            val shared = sharedFlightDao.getAllSharedFlightsSnapshot()

            val flightList = flights.map { f ->
                ExportFlight(
                    flightNumber = f.flightNumber,
                    origin = f.originCode,
                    destination = f.destinationCode,
                    airline = f.airlineName,
                    status = f.status,
                    scheduledDeparture = f.scheduledDeparture,
                    scheduledArrival = f.scheduledArrival,
                    seatNumber = f.seatNumber,
                    seatClass = f.seatClass,
                    bookingReference = f.bookingReference,
                    notes = f.notes,
                    addedAt = f.addedAt
                )
            }

            val sharedList = shared.map { s ->
                ExportSharedFlight(
                    flightIdent = s.flightIdent,
                    shareCode = s.shareCode,
                    sharedBy = s.sharedByName,
                    origin = s.origin,
                    destination = s.destination,
                    airline = s.airline,
                    status = s.status,
                    createdAt = s.createdAt
                )
            }

            val preferences = ExportPreferences(
                displayName = _uiState.value.displayName,
                citizenshipCountry = _uiState.value.citizenshipCountry
            )

            ExportData(
                exportedAt = System.currentTimeMillis(),
                personalFlights = flightList,
                sharedFlights = sharedList,
                preferences = preferences
            )
        }

        val jsonString = json.encodeToString(exportData)

        withContext(Dispatchers.IO) {
            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, "trueskies_export.json")
            file.writeText(jsonString)
            withContext(Dispatchers.Main) {
                onFileReady(file)
            }
        }
    }

    private suspend fun calculateStorageSizes() {
        withContext(Dispatchers.IO) {
            // Database size
            val dbFile = appContext.getDatabasePath("trueskies_database")
            val dbSize = if (dbFile.exists()) dbFile.length() else 0L
            val dbWalFile = File(dbFile.path + "-wal")
            val dbShmFile = File(dbFile.path + "-shm")
            val totalDbSize = dbSize +
                (if (dbWalFile.exists()) dbWalFile.length() else 0L) +
                (if (dbShmFile.exists()) dbShmFile.length() else 0L)

            // Cache size
            val cacheSize = getDirSize(appContext.cacheDir)

            // Preferences size
            val prefsDir = File(appContext.filesDir.parent ?: "", "shared_prefs")
            val dataStoreDir = File(appContext.filesDir, "datastore")
            val prefsSize = getDirSize(prefsDir) + getDirSize(dataStoreDir)

            val totalSize = totalDbSize + cacheSize + prefsSize

            _uiState.update {
                it.copy(
                    databaseSizeFormatted = formatFileSize(totalDbSize),
                    cacheSizeFormatted = formatFileSize(cacheSize),
                    preferencesSizeFormatted = formatFileSize(prefsSize),
                    totalStorageFormatted = formatFileSize(totalSize)
                )
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceAtMost(units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return "${DecimalFormat("#,##0.#").format(value)} ${units[digitGroups]}"
    }
}

// ─── Export data models ───

@Serializable
data class ExportData(
    val exportedAt: Long,
    val personalFlights: List<ExportFlight>,
    val sharedFlights: List<ExportSharedFlight>,
    val preferences: ExportPreferences
)

@Serializable
data class ExportFlight(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val airline: String? = null,
    val status: String? = null,
    val scheduledDeparture: String? = null,
    val scheduledArrival: String? = null,
    val seatNumber: String? = null,
    val seatClass: String? = null,
    val bookingReference: String? = null,
    val notes: String? = null,
    val addedAt: Long = 0
)

@Serializable
data class ExportSharedFlight(
    val flightIdent: String,
    val shareCode: String,
    val sharedBy: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val airline: String? = null,
    val status: String? = null,
    val createdAt: Long = 0
)

@Serializable
data class ExportPreferences(
    val displayName: String = "",
    val citizenshipCountry: String = ""
)

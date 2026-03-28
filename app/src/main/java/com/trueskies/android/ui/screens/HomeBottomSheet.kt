package com.trueskies.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.FlightStatus
import com.trueskies.android.ui.components.FlightCard
import com.trueskies.android.ui.components.FlightRow
import com.trueskies.android.ui.components.SearchResultFlightCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FlightsViewModel
import com.trueskies.android.ui.viewmodels.SearchViewModel

/**
 * My Flights bottom sheet content — search bar + personal flights list.
 * Matches iOS PersonalFlightsPanel: collapsed shows search + flights,
 * expanded fills most of the screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyFlightsSheetContent(
    onFlightClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    flightsViewModel: FlightsViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val flightsState by flightsViewModel.uiState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    val maxSheetHeight = if (isImeVisible) {
        (LocalConfiguration.current.screenHeightDp * 0.90f).dp
    } else {
        (LocalConfiguration.current.screenHeightDp * 0.60f).dp
    }

    // Determine if we're in "Add Flight" results mode
    val showAddFlightView = searchState.hasSearched && searchState.query.isNotEmpty()
            && searchState.results.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .background(Color.Transparent)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            focusManager.clearFocus()
                        }
                    }
                }
            }
    ) {
        if (showAddFlightView) {
            // ── "Add Flight" results view (matches iOS FlightSearchResultsView) ──
            AddFlightResultsHeader(
                query = searchState.query,
                resultCount = searchState.results.size,
                onBack = { searchViewModel.updateQuery("") }
            )

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

            // Search by Date row
            SearchByDateRow()

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))

            // Grouped flight results
            val sections = remember(searchState.results) {
                groupFlightsIntoSections(searchState.results)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrueSkiesSpacing.md,
                    end = TrueSkiesSpacing.md,
                    bottom = TrueSkiesSpacing.xxl
                ),
                verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
            ) {
                sections.forEach { section ->
                    // Section header
                    item(key = "header_${section.title}") {
                        FlightSectionHeader(
                            title = section.title,
                            icon = section.icon,
                            color = section.color
                        )
                    }
                    // Section flights
                    items(section.flights, key = { it.id }) { flight ->
                        SearchResultFlightCard(
                            flight = flight,
                            onClick = { onFlightClick(flight.id) }
                        )
                    }
                }
            }
        } else {
            // ── Default view: search bar + personal flights ──
            Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

            // Search bar — mirrors iOS TSSearchField in PersonalFlightsPanel
            OutlinedTextField(
                value = searchState.query,
                onValueChange = { searchViewModel.updateQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrueSkiesSpacing.md),
                placeholder = {
                    Text(
                        "Search to add flight",
                        color = TrueSkiesColors.TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TrueSkiesColors.TextMuted
                    )
                },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.updateQuery("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TrueSkiesColors.TextMuted
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        searchViewModel.search()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrueSkiesColors.AccentBlue,
                    unfocusedBorderColor = TrueSkiesColors.GlassBorder,
                    focusedContainerColor = TrueSkiesColors.SurfaceElevated,
                    unfocusedContainerColor = TrueSkiesColors.SurfaceElevated,
                    cursorColor = TrueSkiesColors.AccentBlue,
                    focusedTextColor = TrueSkiesColors.TextPrimary,
                    unfocusedTextColor = TrueSkiesColors.TextPrimary
                ),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.xl)
            )

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

            // Content: loading → no results → personal flights → empty state
            when {
                searchState.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = TrueSkiesColors.AccentBlue,
                            modifier = Modifier.size(TrueSkiesSpacing.xl)
                        )
                    }
                }

                searchState.hasSearched && searchState.query.isNotEmpty() -> {
                    // No results found
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flights found",
                            style = TrueSkiesTypography.bodyMedium,
                            color = TrueSkiesColors.TextTertiary
                        )
                    }
                }

                flightsState.personalFlights.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = TrueSkiesSpacing.md,
                            end = TrueSkiesSpacing.md,
                            bottom = TrueSkiesSpacing.xxl
                        ),
                        verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
                    ) {
                        items(
                            items = flightsState.personalFlights,
                            key = { it.localId }
                        ) { personalFlight ->
                            FlightCard(
                                personalFlight = personalFlight,
                                onClick = { onFlightClick(personalFlight.flight.id) }
                            )
                        }
                    }
                }

                else -> {
                    // Empty state — matches iOS FlightEmptyStateView
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                modifier = Modifier.size(TrueSkiesSpacing.xxxl),
                                tint = TrueSkiesColors.TextMuted.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
                            Text(
                                text = "Search a flight to start tracking",
                                style = TrueSkiesTypography.bodyMedium,
                                color = TrueSkiesColors.TextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
    }
}

// ─────────────────────────────────────────────────────────────
// "Add Flight" results header — matches iOS FlightSearchResultsView header
// ─────────────────────────────────────────────────────────────

@Composable
private fun AddFlightResultsHeader(
    query: String,
    resultCount: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button (circular)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TrueSkiesColors.SurfaceElevated)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TrueSkiesColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(TrueSkiesSpacing.sm))

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Add Flight",
                style = TrueSkiesTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TrueSkiesColors.TextPrimary
            )
            Text(
                text = "Found $resultCount flight${if (resultCount != 1) "s" else ""}",
                style = TrueSkiesTypography.bodySmall,
                color = TrueSkiesColors.TextMuted
            )
        }

        // Flight number badge (capsule)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
                .background(TrueSkiesColors.SurfaceElevated)
                .padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xxs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Flight,
                    contentDescription = null,
                    tint = TrueSkiesColors.AccentBlue,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(45f)
                )
                Text(
                    text = query.uppercase(),
                    style = TrueSkiesTypography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TrueSkiesColors.TextPrimary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// "Search by Date" row
// ─────────────────────────────────────────────────────────────

@Composable
private fun SearchByDateRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrueSkiesSpacing.md)
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
            .background(TrueSkiesColors.SurfaceElevated)
            .clickable { /* TODO: date picker */ }
            .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = TrueSkiesColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(TrueSkiesSpacing.sm))
        Text(
            text = "Search by Date",
            style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TrueSkiesColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TrueSkiesColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Flight section header (NOW / TODAY / UPCOMING / COMPLETED)
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlightSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TrueSkiesSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = TrueSkiesTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Flight section grouping logic — matches iOS FlightSearchSection
// ─────────────────────────────────────────────────────────────

private data class FlightSection(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val flights: List<Flight>
)

private fun groupFlightsIntoSections(flights: List<Flight>): List<FlightSection> {
    val now = mutableListOf<Flight>()
    val today = mutableListOf<Flight>()
    val upcoming = mutableListOf<Flight>()
    val completed = mutableListOf<Flight>()

    val todayDate = java.time.LocalDate.now()

    for (flight in flights) {
        val status = flight.status
        when {
            // Active flights → NOW
            status.isActive -> now.add(flight)
            // Completed flights → COMPLETED
            status.isCompleted -> completed.add(flight)
            // Scheduled — check date
            else -> {
                val depDate = try {
                    flight.bestDepartureTime?.let {
                        java.time.ZonedDateTime.parse(it).toLocalDate()
                    }
                } catch (e: Exception) { null }

                when {
                    depDate == null -> today.add(flight)
                    depDate == todayDate -> today.add(flight)
                    depDate.isAfter(todayDate) -> upcoming.add(flight)
                    else -> completed.add(flight)
                }
            }
        }
    }

    return buildList {
        if (now.isNotEmpty()) add(
            FlightSection("NOW", Icons.Default.Flight, TrueSkiesColors.StatusOnTime, now)
        )
        if (today.isNotEmpty()) add(
            FlightSection("TODAY", Icons.Default.CalendarToday, TrueSkiesColors.AccentBlue, today)
        )
        if (upcoming.isNotEmpty()) add(
            FlightSection("UPCOMING", Icons.Default.DateRange, TrueSkiesColors.TextSecondary, upcoming)
        )
        if (completed.isNotEmpty()) add(
            FlightSection("COMPLETED", Icons.Default.CheckCircle, TrueSkiesColors.StatusOnTime, completed)
        )
    }
}

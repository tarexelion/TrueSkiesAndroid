package com.trueskies.android.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.ui.components.FlightCard
import com.trueskies.android.ui.components.FlightRow
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FlightsViewModel
import com.trueskies.android.ui.viewmodels.SearchViewModel

/**
 * Bottom sheet tab items — Material 3 NavigationBar inside the sheet.
 */
enum class BottomSheetTab(val title: String, val icon: ImageVector) {
    MY_FLIGHTS("My Flights", Icons.Default.FlightTakeoff),
    FRIENDS("Friends' Flights", Icons.Default.People),
    FLIGHT_LOG("Flight Log", Icons.Default.Public)
}

/**
 * Main bottom sheet content with 3 tabs and a drag handle.
 * Tabs are at the top so they're always visible; content scrolls below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBottomSheetContent(
    onFlightClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(BottomSheetTab.MY_FLIGHTS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TrueSkiesColors.SurfaceSecondary)
    ) {
        // Tab bar at the top — always visible
        NavigationBar(
            containerColor = TrueSkiesColors.SurfaceSecondary,
            contentColor = TrueSkiesColors.TextPrimary,
            tonalElevation = 0.dp
        ) {
            BottomSheetTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    selected = selected,
                    onClick = { selectedTab = tab },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TrueSkiesColors.AccentBlue,
                        selectedTextColor = TrueSkiesColors.AccentBlue,
                        unselectedIconColor = TrueSkiesColors.TabInactive,
                        unselectedTextColor = TrueSkiesColors.TabInactive,
                        indicatorColor = TrueSkiesColors.AccentBlue.copy(alpha = 0.12f)
                    )
                )
            }
        }

        // Tab content below
        when (selectedTab) {
            BottomSheetTab.MY_FLIGHTS -> MyFlightsTabContent(onFlightClick = onFlightClick)
            BottomSheetTab.FRIENDS -> FriendsFlightsTabContent()
            BottomSheetTab.FLIGHT_LOG -> FlightLogTabContent()
        }
    }
}

// ────────────────────────────────────────────────────────
// Tab 1: My Flights  (search bar + personal flights list)
// ────────────────────────────────────────────────────────

@Composable
private fun MyFlightsTabContent(
    onFlightClick: (String) -> Unit,
    flightsViewModel: FlightsViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val flightsState by flightsViewModel.uiState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 500.dp)
            .padding(horizontal = TrueSkiesSpacing.md)
    ) {
        Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

        // Search bar
        OutlinedTextField(
            value = searchState.query,
            onValueChange = { searchViewModel.updateQuery(it) },
            modifier = Modifier.fillMaxWidth(),
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

        // Show search results if searching, else show personal flights
        when {
            // Active search in progress
            searchState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TrueSkiesColors.AccentBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            // Search results available
            searchState.hasSearched && searchState.query.isNotEmpty() -> {
                if (searchState.results.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flights found",
                            style = TrueSkiesTypography.bodyMedium,
                            color = TrueSkiesColors.TextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
                    ) {
                        items(searchState.results, key = { it.id }) { flight ->
                            FlightRow(
                                flight = flight,
                                onClick = { onFlightClick(flight.id) }
                            )
                        }
                    }
                }
            }
            // Personal flights list
            flightsState.personalFlights.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
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
            // Empty state — airplane icon
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
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

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
    }
}

// ────────────────────────────────────────────────────────
// Tab 2: Friends' Flights
// ────────────────────────────────────────────────────────

@Composable
private fun FriendsFlightsTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = TrueSkiesColors.AccentBlue
        )

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

        Text(
            text = "Share & track together",
            style = TrueSkiesTypography.headlineSmall,
            color = TrueSkiesColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.lg))

        // Action buttons row: Scan, Paste, Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
        ) {
            // Scan button
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TrueSkiesColors.TextPrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Scan", style = TrueSkiesTypography.labelLarge)
            }

            // Paste button
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TrueSkiesColors.TextPrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Paste", style = TrueSkiesTypography.labelLarge)
            }

            // Share button (filled / accent)
            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrueSkiesColors.AccentBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Share", style = TrueSkiesTypography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
    }
}

// ────────────────────────────────────────────────────────
// Tab 3: Flight Log
// ────────────────────────────────────────────────────────

@Composable
private fun FlightLogTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm)
    ) {
        // Settings button row at the top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalIconButton(
                onClick = { /* TODO: navigate to settings */ },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = TrueSkiesColors.SurfaceElevated,
                    contentColor = TrueSkiesColors.TextSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Empty state content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TrueSkiesSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.HistoryEdu,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = TrueSkiesColors.TextMuted.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

            Text(
                text = "Flight Log",
                style = TrueSkiesTypography.headlineSmall,
                color = TrueSkiesColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxs))

            Text(
                text = "Your completed flights will appear here",
                style = TrueSkiesTypography.bodyMedium,
                color = TrueSkiesColors.TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
        }
    }
}

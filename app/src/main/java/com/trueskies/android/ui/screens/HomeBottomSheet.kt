package com.trueskies.android.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.ui.components.FlightCard
import com.trueskies.android.ui.components.FlightRow
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .background(TrueSkiesColors.SurfaceSecondary)
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

        // Content: search results → personal flights → empty state
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
                if (searchState.results.isEmpty()) {
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
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = TrueSkiesSpacing.md,
                            end = TrueSkiesSpacing.md,
                            bottom = TrueSkiesSpacing.xxl
                        ),
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
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
                        )
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                flightsViewModel.deleteFlight(personalFlight.localId)
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val bgColor by animateColorAsState(
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                        TrueSkiesColors.StatusCancelled.copy(alpha = 0.85f)
                                    else
                                        TrueSkiesColors.SurfaceElevated,
                                    label = "swipe-bg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(bgColor, RoundedCornerShape(TrueSkiesCornerRadius.lg))
                                        .padding(end = TrueSkiesSpacing.lg),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete flight",
                                        tint = TrueSkiesColors.TextPrimary
                                    )
                                }
                            }
                        ) {
                            FlightCard(
                                personalFlight = personalFlight,
                                onClick = { onFlightClick(personalFlight.flight.id) }
                            )
                        }
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

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
    }
}

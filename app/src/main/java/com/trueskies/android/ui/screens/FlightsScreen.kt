package com.trueskies.android.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.ui.components.FlightCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FlightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightsScreen(
    onFlightClick: (String) -> Unit,
    viewModel: FlightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = TrueSkiesSpacing.huge,
                    start = TrueSkiesSpacing.lg,
                    end = TrueSkiesSpacing.lg,
                    bottom = TrueSkiesSpacing.md
                )
        ) {
            Text(
                text = "My Flights",
                style = TrueSkiesTypography.displayMedium,
                color = TrueSkiesColors.TextPrimary
            )
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshFlights() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.personalFlights.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TrueSkiesColors.AccentBlue)
                }
            } else if (uiState.personalFlights.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(TrueSkiesSpacing.xxl)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AirplanemodeActive,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TrueSkiesColors.TextMuted
                        )
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
                        Text(
                            text = "No Flights Yet",
                            style = TrueSkiesTypography.headlineMedium,
                            color = TrueSkiesColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
                        Text(
                            text = "Search for a flight to start tracking it",
                            style = TrueSkiesTypography.bodyMedium,
                            color = TrueSkiesColors.TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Flight list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = TrueSkiesSpacing.md,
                        vertical = TrueSkiesSpacing.xs
                    ),
                    verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
                ) {
                    items(
                        items = uiState.personalFlights,
                        key = { it.localId }
                    ) { personalFlight ->
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteFlight(personalFlight.localId)
                                        true
                                    } else false
                                }
                            ),
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(TrueSkiesColors.Error.copy(alpha = 0.3f))
                                        .padding(end = TrueSkiesSpacing.lg),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TrueSkiesColors.Error
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            modifier = Modifier.animateContentSize()
                        ) {
                            FlightCard(
                                personalFlight = personalFlight,
                                onClick = { onFlightClick(personalFlight.flight.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

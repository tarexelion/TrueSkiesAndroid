package com.trueskies.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.ui.components.FlightRow
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onFlightClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

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
                    bottom = TrueSkiesSpacing.sm
                )
        ) {
            Text(
                text = "Search Flights",
                style = TrueSkiesTypography.displayMedium,
                color = TrueSkiesColors.TextPrimary
            )
        }

        // Search bar
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.updateQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrueSkiesSpacing.md)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    "Flight number (e.g. UA123)",
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
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateQuery("") }) {
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
                    viewModel.search()
                    focusManager.clearFocus()
                }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrueSkiesColors.AccentBlue,
                unfocusedBorderColor = TrueSkiesColors.GlassBorder,
                focusedContainerColor = TrueSkiesColors.SurfaceSecondary,
                unfocusedContainerColor = TrueSkiesColors.SurfaceSecondary,
                cursorColor = TrueSkiesColors.AccentBlue,
                focusedTextColor = TrueSkiesColors.TextPrimary,
                unfocusedTextColor = TrueSkiesColors.TextPrimary
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(TrueSkiesCornerRadius.md)
        )

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))

        // Results
        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TrueSkiesColors.AccentBlue)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Search failed",
                        style = TrueSkiesTypography.bodyMedium,
                        color = TrueSkiesColors.Error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            uiState.hasSearched && uiState.results.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No flights found",
                            style = TrueSkiesTypography.headlineSmall,
                            color = TrueSkiesColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
                        Text(
                            text = "Try a different flight number or date",
                            style = TrueSkiesTypography.bodyMedium,
                            color = TrueSkiesColors.TextTertiary
                        )
                    }
                }
            }
            uiState.results.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = TrueSkiesSpacing.md,
                        vertical = TrueSkiesSpacing.xs
                    ),
                    verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
                ) {
                    items(
                        items = uiState.results,
                        key = { it.id }
                    ) { flight ->
                        FlightRow(
                            flight = flight,
                            onClick = { onFlightClick(flight.id) }
                        )
                    }
                }
            }
            else -> {
                // Initial state - show search tips
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(TrueSkiesSpacing.xxl)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TrueSkiesColors.TextMuted
                        )
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
                        Text(
                            text = "Search by flight number",
                            style = TrueSkiesTypography.headlineSmall,
                            color = TrueSkiesColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
                        Text(
                            text = "e.g. UA123, BA456, TK1234",
                            style = TrueSkiesTypography.bodyMedium,
                            color = TrueSkiesColors.TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

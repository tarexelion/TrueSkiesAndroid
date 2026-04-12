package com.trueskies.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FlightLogViewModel

/**
 * Flight Log tab — map + draggable bottom sheet.
 * Map shows flight routes for the selected time period.
 * Sheet shows profile header, time range selector, travel summary stats,
 * travel stories, and trip-grouped flights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightLogScreen(
    onSettingsClick: () -> Unit = {},
    onFlightClick: (String) -> Unit = {},
    viewModel: FlightLogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(38.5, 30.0), 5f)
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            FlightLogSheetContent(
                state = state,
                onRangeChange = { viewModel.selectRange(it) },
                onSettingsClick = onSettingsClick,
                onFlightClick = onFlightClick
            )
        },
        sheetPeekHeight = 340.dp,
        sheetShape = RoundedCornerShape(
            topStart = TrueSkiesCornerRadius.xl,
            topEnd = TrueSkiesCornerRadius.xl
        ),
        sheetContainerColor = Color(0xFF1C1C1E).copy(alpha = 0.92f),
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TrueSkiesColors.TextMuted.copy(alpha = 0.4f)
            )
        },
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 20.dp,
        containerColor = Color.Transparent
    ) {
        // Full-screen satellite map showing flight log routes
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.HYBRID,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false
                )
            ) {
                state.mapFlights.forEach { flight ->
                    FlightMapOverlay(flight = flight, onFlightClick = onFlightClick)
                }
            }
        }
    }
}

@Composable
private fun FlightLogSheetContent(
    state: FlightLogUiState,
    onRangeChange: (FlightLogRange) -> Unit,
    onSettingsClick: () -> Unit,
    onFlightClick: (String) -> Unit
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.90f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
    ) {
        // Profile header
        ProfileHeaderSection(
            userName = state.userName,
            onProfileTap = { },
            onSettingsAction = onSettingsClick
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.15f)
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TrueSkiesSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xl)
        ) {
            Spacer(Modifier.height(TrueSkiesSpacing.sm))

            TimeRangeSelector(
                ranges = state.availableRanges,
                selectedRange = state.selectedRange,
                onRangeChange = onRangeChange
            )

            if (state.hasFlights) {
                TravelSummarySection(state = state)
                TravelStoriesSection(state = state)
                TripGroupedFlightsSection(
                    flights = state.recentFlights,
                    onFlightClick = onFlightClick
                )
            } else {
                EmptyTravelState(onAddFlight = { })
            }

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxl))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Profile Header — mirrors iOS ProfileHeaderSection
// ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeaderSection(
    userName: String,
    onProfileTap: () -> Unit,
    onSettingsAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = TrueSkiesSpacing.md,
                vertical = TrueSkiesSpacing.lg
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar — tappable circle with initial
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.2f))
                .clickable(onClick = onProfileTap),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.firstOrNull()?.uppercase() ?: "?",
                style = TrueSkiesTypography.headlineSmall,
                color = TrueSkiesColors.AccentBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(TrueSkiesSpacing.md))

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = TrueSkiesTypography.titleLarge,
                color = TrueSkiesColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Settings gear — 44dp tappable circle (matches iOS)
        FilledTonalIconButton(
            onClick = onSettingsAction,
            modifier = Modifier.size(44.dp),
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
}

// ─────────────────────────────────────────────────────────────
// Time Range Selector — mirrors iOS TimeRangeSelector
// ─────────────────────────────────────────────────────────────

@Composable
private fun TimeRangeSelector(
    ranges: List<FlightLogRange>,
    selectedRange: FlightLogRange,
    onRangeChange: (FlightLogRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
    ) {
        ranges.forEach { range ->
            val isSelected = range == selectedRange
            Surface(
                onClick = { onRangeChange(range) },
                shape = RoundedCornerShape(TrueSkiesCornerRadius.pill),
                color = if (isSelected) TrueSkiesColors.AccentBlue else TrueSkiesColors.GlassBackground,
                shadowElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Text(
                    text = range.displayName.uppercase(),
                    style = TrueSkiesTypography.labelMedium,
                    color = if (isSelected) Color.White else TrueSkiesColors.TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.xs)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Travel Summary — mirrors iOS TravelSummaryCard (stats view)
// ─────────────────────────────────────────────────────────────

@Composable
private fun TravelSummarySection(state: FlightLogUiState) {
    LiquidGlassCard {
        Column(
            modifier = Modifier.padding(TrueSkiesSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
        ) {
            Text(
                text = "Travel Summary",
                style = TrueSkiesTypography.titleMedium,
                color = TrueSkiesColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            // 2x2 stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
            ) {
                StatCard(
                    label = "Flights",
                    value = "${state.totalFlights}",
                    icon = Icons.Default.FlightTakeoff,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Countries",
                    value = "${state.countriesVisited}",
                    icon = Icons.Default.Public,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
            ) {
                StatCard(
                    label = "Distance",
                    value = state.totalDistance,
                    icon = Icons.Default.Timeline,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Flight Time",
                    value = state.totalFlightTime,
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
            }

            // Achievements row placeholder
            if (state.unlockedMilestones > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TrueSkiesCornerRadius.md))
                        .background(TrueSkiesColors.DashboardPurple.copy(alpha = 0.15f))
                        .padding(TrueSkiesSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = TrueSkiesColors.DashboardPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${state.unlockedMilestones} achievements unlocked",
                        style = TrueSkiesTypography.labelMedium,
                        color = TrueSkiesColors.DashboardPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(TrueSkiesCornerRadius.md),
        color = TrueSkiesColors.SurfaceElevated.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(TrueSkiesSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xxs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xxs)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TrueSkiesColors.AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = TrueSkiesTypography.labelSmall,
                    color = TrueSkiesColors.TextMuted
                )
            }
            Text(
                text = value,
                style = TrueSkiesTypography.headlineSmall,
                color = TrueSkiesColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Travel Stories — mirrors iOS TravelStoriesSection
// ─────────────────────────────────────────────────────────────

@Composable
private fun TravelStoriesSection(state: FlightLogUiState) {
    if (state.travelStories.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)) {
        Text(
            text = "Travel Stories",
            style = TrueSkiesTypography.titleMedium,
            color = TrueSkiesColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        state.travelStories.forEach { story ->
            LiquidGlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TrueSkiesSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(TrueSkiesCornerRadius.sm))
                            .background(story.iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            story.icon,
                            contentDescription = null,
                            tint = story.iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = story.title,
                            style = TrueSkiesTypography.titleSmall,
                            color = TrueSkiesColors.TextPrimary
                        )
                        Text(
                            text = story.subtitle,
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Trip-Grouped Flights — mirrors iOS TripGroupedFlightsList
// ─────────────────────────────────────────────────────────────

@Composable
private fun TripGroupedFlightsSection(
    flights: List<FlightLogItem>,
    onFlightClick: (String) -> Unit
) {
    if (flights.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)) {
        Text(
            text = "Your Flights",
            style = TrueSkiesTypography.titleMedium,
            color = TrueSkiesColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        flights.forEach { item ->
            LiquidGlassCard(
                modifier = Modifier.clickable { onFlightClick(item.flightId) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TrueSkiesSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Route info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.flightNumber,
                            style = TrueSkiesTypography.titleMedium,
                            color = TrueSkiesColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${item.origin} → ${item.destination}",
                            style = TrueSkiesTypography.bodyMedium,
                            color = TrueSkiesColors.TextSecondary
                        )
                        Text(
                            text = item.date,
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextTertiary
                        )
                    }
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(TrueSkiesCornerRadius.badge),
                        color = item.statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.statusText,
                            style = TrueSkiesTypography.labelSmall,
                            color = item.statusColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = TrueSkiesSpacing.xs, vertical = TrueSkiesSpacing.xxs)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Empty State — mirrors iOS EmptyTravelState
// ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyTravelState(onAddFlight: () -> Unit) {
    LiquidGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TrueSkiesColors.AccentBlue.copy(alpha = 0.6f)
            )

            Text(
                text = "Track your journeys",
                style = TrueSkiesTypography.headlineSmall,
                color = TrueSkiesColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Add your first flight to start building your travel log with stats, achievements, and stories.",
                style = TrueSkiesTypography.bodyMedium,
                color = TrueSkiesColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            Button(
                onClick = onAddFlight,
                shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrueSkiesColors.AccentBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = TrueSkiesSpacing.xl, vertical = TrueSkiesSpacing.sm)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(TrueSkiesSpacing.xs))
                Text("Add Flight", style = TrueSkiesTypography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Data Models for Flight Log UI
// ─────────────────────────────────────────────────────────────

enum class FlightLogRange(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_YEAR("This Year"),
    THIS_MONTH("This Month"),
    LAST_90_DAYS("90 Days"),
    LAST_30_DAYS("30 Days")
}

data class TravelStory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color
)

data class FlightLogItem(
    val flightId: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val date: String,
    val statusText: String,
    val statusColor: Color
)

data class FlightLogUiState(
    val userName: String = "Traveler",
    val selectedRange: FlightLogRange = FlightLogRange.ALL_TIME,
    val availableRanges: List<FlightLogRange> = listOf(FlightLogRange.ALL_TIME),
    val hasFlights: Boolean = false,
    val totalFlights: Int = 0,
    val countriesVisited: Int = 0,
    val totalDistance: String = "0 km",
    val totalFlightTime: String = "0 h",
    val unlockedMilestones: Int = 0,
    val travelStories: List<TravelStory> = emptyList(),
    val recentFlights: List<FlightLogItem> = emptyList(),
    val mapFlights: List<com.trueskies.android.domain.models.Flight> = emptyList()
)

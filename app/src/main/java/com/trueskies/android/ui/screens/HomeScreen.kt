package com.trueskies.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.*
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.MapViewModel
import com.trueskies.android.ui.viewmodels.SearchViewModel
import com.trueskies.android.util.GreatCircle

/**
 * My Flights tab — full-screen map with a draggable bottom sheet.
 * Matches iOS ContentView: MapKit fills the entire screen, PersonalFlightsPanel
 * slides up from the bottom as a BottomCardView.
 *
 * Map type: HYBRID (satellite + roads) — mirrors iOS default .hybrid style.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onFlightClick: (String) -> Unit,
    mapViewModel: MapViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )
    val isImeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current

    // Sheet should be expanded when keyboard is up, searching, or showing results
    val shouldExpand = isImeVisible
            || searchState.isSearching
            || (searchState.hasSearched && searchState.query.isNotEmpty())

    // Use hasSearched as an additional key so the effect re-fires after search completes
    // (shouldExpand may stay true throughout the keyboard→search→results flow)
    LaunchedEffect(shouldExpand, searchState.hasSearched) {
        if (shouldExpand) {
            scaffoldState.bottomSheetState.expand()
        } else {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    // Default camera: Turkey/Europe region (sensible first-launch default)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(38.5, 30.0), 5f)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            MyFlightsSheetContent(
                onFlightClick = onFlightClick,
                searchViewModel = searchViewModel
            )
        },
        // Peek: drag handle + search bar + enough room for "No flights found" state
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
        // Full-screen satellite map — matches iOS default .hybrid map style
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { focusManager.clearFocus() },
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
                mapState.allFlights.forEach { flight ->
                    FlightMapOverlay(flight = flight, onFlightClick = onFlightClick)
                }
            }

            // Loading indicator — top center below status bar
            if (mapState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = TrueSkiesSpacing.md),
                    color = TrueSkiesColors.AccentBlue,
                    strokeWidth = 2.dp
                )
            }

            // Backend connection warning pill
            if (!mapState.isBackendHealthy) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = TrueSkiesSpacing.md)
                ) {
                    Surface(
                        color = TrueSkiesColors.Warning.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(TrueSkiesCornerRadius.pill)
                    ) {
                        Text(
                            text = "Backend Connection Issue",
                            style = TrueSkiesTypography.labelMedium,
                            color = TrueSkiesColors.TextInverse,
                            modifier = Modifier.padding(
                                horizontal = TrueSkiesSpacing.md,
                                vertical = TrueSkiesSpacing.xxs
                            )
                        )
                    }
                }
            }
        }
    }

    // Reload flights when camera settles
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            if (bounds != null) {
                mapViewModel.loadFlightsInBounds(
                    minLat = bounds.southwest.latitude,
                    maxLat = bounds.northeast.latitude,
                    minLon = bounds.southwest.longitude,
                    maxLon = bounds.northeast.longitude
                )
            }
        }
    }
}

// ── Flight path colors ──
private val FlightPathColor = Color(0xFF00B3FF)          // Electric cyan-blue (iOS traveled path)
private val FlightPathGlowColor = Color(0xFF00B3FF)
private val UpcomingPathColor = Color(0xFF22D3EE)         // Accent cyan
private val AirportMarkerColor = Color(0xFF3B82F6)        // Accent blue

/**
 * Renders the complete flight overlay on the map for a single flight:
 * traveled path (glow polyline), upcoming path (dashed), airplane marker, airport markers.
 * Mirrors iOS MyFlightsMapContent.swift layered rendering.
 */
@Composable
fun FlightMapOverlay(
    flight: Flight,
    onFlightClick: (String) -> Unit
) {
    val hasOrigin = flight.originLat != null && flight.originLon != null
    val hasDestination = flight.destinationLat != null && flight.destinationLon != null
    val hasPosition = flight.hasPosition

    val originLatLng = if (hasOrigin) LatLng(flight.originLat!!, flight.originLon!!) else null
    val destinationLatLng = if (hasDestination) LatLng(flight.destinationLat!!, flight.destinationLon!!) else null
    val currentLatLng = if (hasPosition) LatLng(flight.latitude!!, flight.longitude!!) else null

    // ── 1. Traveled path (from track points or origin → current position) ──
    val traveledPath = remember(flight.id, flight.trackPoints.size, flight.latitude) {
        when {
            flight.trackPoints.size >= 2 -> {
                flight.trackPoints.map { LatLng(it.latitude, it.longitude) }
            }
            hasOrigin && hasPosition -> {
                GreatCircle.sampleArc(originLatLng!!, currentLatLng!!)
            }
            else -> emptyList()
        }
    }

    // ── 2. Upcoming path (current position → destination, or origin → destination) ──
    val upcomingPath = remember(flight.id, flight.latitude, flight.destinationLat) {
        when {
            hasPosition && hasDestination -> {
                GreatCircle.sampleArc(currentLatLng!!, destinationLatLng!!)
            }
            hasOrigin && hasDestination && !hasPosition -> {
                // Pre-departure: show full great-circle route
                GreatCircle.sampleArc(originLatLng!!, destinationLatLng!!)
            }
            else -> emptyList()
        }
    }

    // ── Draw traveled path — outer glow layer ──
    if (traveledPath.size >= 2) {
        Polyline(
            points = traveledPath,
            color = FlightPathGlowColor.copy(alpha = 0.10f),
            width = 24f,
            jointType = JointType.ROUND,
            startCap = RoundCap(),
            endCap = RoundCap()
        )
        // Middle glow
        Polyline(
            points = traveledPath,
            color = FlightPathGlowColor.copy(alpha = 0.25f),
            width = 14f,
            jointType = JointType.ROUND,
            startCap = RoundCap(),
            endCap = RoundCap()
        )
        // Core line
        Polyline(
            points = traveledPath,
            color = FlightPathColor,
            width = 4f,
            jointType = JointType.ROUND,
            startCap = RoundCap(),
            endCap = RoundCap()
        )
    }

    // ── Draw upcoming path — dashed line ──
    if (upcomingPath.size >= 2) {
        Polyline(
            points = upcomingPath,
            color = if (hasPosition) UpcomingPathColor.copy(alpha = 0.5f) else UpcomingPathColor.copy(alpha = 0.3f),
            width = 4f,
            jointType = JointType.ROUND,
            startCap = RoundCap(),
            endCap = RoundCap(),
            pattern = listOf(Dash(24f), Gap(16f))
        )
    }

    // ── 3. Airport markers ──
    if (hasOrigin) {
        MarkerComposable(
            state = MarkerState(position = originLatLng!!),
            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
            zIndex = 0f
        ) {
            AirportMapMarker(code = flight.originCode, isDeparture = true)
        }
    }
    if (hasDestination) {
        MarkerComposable(
            state = MarkerState(position = destinationLatLng!!),
            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
            zIndex = 0f
        ) {
            AirportMapMarker(code = flight.destinationCode, isDeparture = false)
        }
    }

    // ── 4. Airplane marker at current position ──
    if (hasPosition) {
        MarkerComposable(
            state = MarkerState(position = currentLatLng!!),
            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
            zIndex = 1f,
            onClick = {
                onFlightClick(flight.id)
                true
            },
            title = flight.displayFlightNumber,
            snippet = "${flight.originCode} → ${flight.destinationCode}"
        ) {
            AircraftMapMarker(heading = flight.heading?.toFloat() ?: 0f)
        }
    }
}

/**
 * Custom aircraft marker — white airplane icon with shadow,
 * rotated to match flight heading. Mirrors iOS PlaneAvatar3D.
 */
@Composable
private fun AircraftMapMarker(heading: Float) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow background
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(8.dp, CircleShape, ambientColor = FlightPathColor, spotColor = FlightPathColor)
                .background(FlightPathColor.copy(alpha = 0.2f), CircleShape)
        )
        // Airplane icon — rotated by heading (add 45° because the Flight icon points NE by default)
        Icon(
            imageVector = Icons.Filled.Flight,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .rotate(heading + 45f),
            tint = Color.White
        )
    }
}

/**
 * Airport marker pill — shows IATA code with departure/arrival icon.
 * Mirrors iOS airport annotation capsules.
 */
@Composable
private fun AirportMapMarker(code: String, isDeparture: Boolean) {
    Surface(
        color = AirportMarkerColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(TrueSkiesCornerRadius.pill),
        shadowElevation = 4.dp
    ) {
        Text(
            text = code,
            style = TrueSkiesTypography.labelSmall.copy(fontSize = 10.sp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

package com.trueskies.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.MapViewModel

/**
 * My Flights tab — full-screen map with a draggable bottom sheet.
 * Matches iOS ContentView: MapKit fills the entire screen, PersonalFlightsPanel
 * slides up from the bottom as a BottomCardView.
 *
 * Map type: HYBRID (satellite + roads) — mirrors iOS default .hybrid style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFlightClick: (String) -> Unit,
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    // Default camera: Turkey/Europe region (sensible first-launch default)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(38.5, 30.0), 5f)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            MyFlightsSheetContent(onFlightClick = onFlightClick)
        },
        // Peek: drag handle + search bar + ~1 card — mirrors iOS PersonalFlightsPanel collapsed height
        sheetPeekHeight = 260.dp,
        sheetShape = RoundedCornerShape(
            topStart = TrueSkiesCornerRadius.xl,
            topEnd = TrueSkiesCornerRadius.xl
        ),
        sheetContainerColor = TrueSkiesColors.SurfaceSecondary,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TrueSkiesColors.TextMuted.copy(alpha = 0.4f)
            )
        },
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 20.dp,
        containerColor = TrueSkiesColors.SurfacePrimary
    ) {
        // Full-screen satellite map — matches iOS default .hybrid map style
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
                // Flight markers
                mapState.flights.forEach { flight ->
                    if (flight.hasPosition) {
                        val position = LatLng(flight.latitude!!, flight.longitude!!)
                        Marker(
                            state = MarkerState(position = position),
                            title = flight.displayFlightNumber,
                            snippet = "${flight.originCode} → ${flight.destinationCode}",
                            rotation = flight.heading?.toFloat() ?: 0f,
                            onClick = {
                                onFlightClick(flight.id)
                                true
                            }
                        )
                    }
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

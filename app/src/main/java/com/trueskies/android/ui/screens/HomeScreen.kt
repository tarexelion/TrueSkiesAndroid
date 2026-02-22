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
 * Home screen — full-screen map with a draggable Material 3 bottom sheet.
 * The sheet contains the tab navigation (My Flights, Friends' Flights, Flight Log).
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

    // Default camera: Turkey region (matching iOS screenshot)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(38.5, 30.0), 5f)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            HomeBottomSheetContent(
                onFlightClick = onFlightClick
            )
        },
        sheetPeekHeight = 320.dp,
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
        sheetShadowElevation = 16.dp,
        containerColor = TrueSkiesColors.SurfacePrimary
    ) { innerPadding ->
        // Full-screen map as background
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
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

            // Loading indicator
            if (mapState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp),
                    color = TrueSkiesColors.AccentBlue
                )
            }

            // Backend status warning
            if (!mapState.isBackendHealthy) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Surface(
                        color = TrueSkiesColors.Warning.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(TrueSkiesCornerRadius.sm)
                    ) {
                        Text(
                            text = "Backend Connection Issue",
                            style = TrueSkiesTypography.labelMedium,
                            color = TrueSkiesColors.TextInverse,
                            modifier = Modifier.padding(
                                horizontal = TrueSkiesSpacing.md,
                                vertical = TrueSkiesSpacing.xs
                            )
                        )
                    }
                }
            }
        }
    }

    // Load flights when camera stops moving
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

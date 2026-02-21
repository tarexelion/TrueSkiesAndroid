package com.trueskies.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.MapViewModel

@Composable
fun MapScreen(
    onFlightClick: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Default camera: world view
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.0, -30.0), 3f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
    ) {
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
            uiState.flights.forEach { flight ->
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
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                color = TrueSkiesColors.AccentBlue
            )
        }

        // Backend status
        if (!uiState.isBackendHealthy) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .background(
                        TrueSkiesColors.Warning.copy(alpha = 0.9f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(TrueSkiesCornerRadius.sm)
                    )
                    .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.xs)
            ) {
                Text(
                    text = "Backend Connection Issue",
                    style = TrueSkiesTypography.labelMedium,
                    color = TrueSkiesColors.TextInverse
                )
            }
        }
    }

    // Load flights when camera moves
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            if (bounds != null) {
                viewModel.loadFlightsInBounds(
                    minLat = bounds.southwest.latitude,
                    maxLat = bounds.northeast.latitude,
                    minLon = bounds.southwest.longitude,
                    maxLon = bounds.northeast.longitude
                )
            }
        }
    }
}

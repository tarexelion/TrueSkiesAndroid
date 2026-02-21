package com.trueskies.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.domain.models.FlightStatus
import com.trueskies.android.ui.components.FlightStatusBadge
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FlightDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    onBack: () -> Unit,
    viewModel: FlightDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = uiState.flight?.displayFlightNumber ?: "Flight Details",
                    style = TrueSkiesTypography.headlineMedium,
                    color = TrueSkiesColors.TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TrueSkiesColors.AccentBlue
                    )
                }
            },
            actions = {
                // Add to personal flights
                IconButton(onClick = { viewModel.addToPersonalFlights() }) {
                    Icon(
                        imageVector = if (uiState.isAddedToPersonal) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (uiState.isAddedToPersonal) "Added" else "Track Flight",
                        tint = if (uiState.isAddedToPersonal) TrueSkiesColors.Success else TrueSkiesColors.AccentBlue
                    )
                }
                // Refresh
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TrueSkiesColors.AccentBlue
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = TrueSkiesColors.SurfacePrimary
            )
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TrueSkiesColors.AccentBlue)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Failed to load flight",
                            style = TrueSkiesTypography.headlineSmall,
                            color = TrueSkiesColors.Error
                        )
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
                        Text(
                            text = uiState.error ?: "",
                            style = TrueSkiesTypography.bodyMedium,
                            color = TrueSkiesColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = TrueSkiesColors.AccentBlue)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.flight != null -> {
                val flight = uiState.flight!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = TrueSkiesSpacing.md)
                ) {
                    Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

                    // ── Status + Airline ──
                    LiquidGlassCard {
                        Column(modifier = Modifier.padding(TrueSkiesSpacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = flight.displayFlightNumber,
                                        style = AviationTypography.flightNumber,
                                        color = TrueSkiesColors.TextPrimary
                                    )
                                    flight.airlineName?.let {
                                        Text(
                                            text = it,
                                            style = TrueSkiesTypography.bodyMedium,
                                            color = TrueSkiesColors.TextSecondary
                                        )
                                    }
                                }
                                FlightStatusBadge(status = flight.status)
                            }

                            // Aircraft info
                            flight.aircraftType?.let { type ->
                                Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))
                                Text(
                                    text = "Aircraft: $type${flight.aircraftRegistration?.let { " ($it)" } ?: ""}",
                                    style = TrueSkiesTypography.bodySmall,
                                    color = TrueSkiesColors.TextTertiary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))

                    // ── Route Card ──
                    LiquidGlassCard {
                        Column(modifier = Modifier.padding(TrueSkiesSpacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                // Origin
                                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                                    Text(text = "FROM", style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextMuted)
                                    Text(
                                        text = flight.originCode,
                                        style = AviationTypography.airportCode,
                                        color = TrueSkiesColors.TextPrimary
                                    )
                                    flight.originCity?.let {
                                        Text(text = it, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextSecondary)
                                    }
                                    flight.originName?.let {
                                        Text(text = it, style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextTertiary)
                                    }
                                }

                                // Flight icon + progress
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = TrueSkiesSpacing.lg)
                                ) {
                                    Icon(
                                        Icons.Default.Flight,
                                        contentDescription = null,
                                        tint = TrueSkiesColors.AccentBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    flight.progressPercent?.let { pct ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$pct%",
                                            style = TrueSkiesTypography.labelSmall,
                                            color = TrueSkiesColors.AccentBlue
                                        )
                                    }
                                }

                                // Destination
                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                    Text(text = "TO", style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextMuted)
                                    Text(
                                        text = flight.destinationCode,
                                        style = AviationTypography.airportCode,
                                        color = TrueSkiesColors.TextPrimary
                                    )
                                    flight.destinationCity?.let {
                                        Text(text = it, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextSecondary)
                                    }
                                    flight.destinationName?.let {
                                        Text(text = it, style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextTertiary)
                                    }
                                }
                            }

                            // Progress bar
                            flight.progressPercent?.takeIf { it > 0 }?.let { pct ->
                                Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(TrueSkiesColors.SurfaceElevated)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(pct / 100f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(TrueSkiesColors.AccentBlue, TrueSkiesColors.AccentCyan)
                                                )
                                            )
                                    )
                                    // Plane dot
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(pct / 100f)
                                            .fillMaxHeight()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(TrueSkiesColors.FlightPathDotActive)
                                                .align(Alignment.CenterEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))

                    // ── Timing Card ──
                    LiquidGlassCard {
                        Column(modifier = Modifier.padding(TrueSkiesSpacing.md)) {
                            Text(text = "Schedule", style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextSecondary)
                            Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

                            // Departure
                            TimeRow(
                                label = "Departure",
                                scheduled = flight.scheduledDeparture,
                                estimated = flight.estimatedDeparture,
                                actual = flight.actualDeparture,
                                gate = flight.departureGate,
                                terminal = flight.departureTerminal,
                                delay = flight.departureDelay
                            )

                            Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
                            HorizontalDivider(color = TrueSkiesColors.GlassHighlight)
                            Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))

                            // Arrival
                            TimeRow(
                                label = "Arrival",
                                scheduled = flight.scheduledArrival,
                                estimated = flight.estimatedArrival,
                                actual = flight.actualArrival,
                                gate = flight.arrivalGate,
                                terminal = flight.arrivalTerminal,
                                delay = flight.arrivalDelay
                            )
                        }
                    }

                    // ── Position Info ──
                    if (flight.hasPosition) {
                        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))
                        LiquidGlassCard {
                            Column(modifier = Modifier.padding(TrueSkiesSpacing.md)) {
                                Text(text = "Live Position", style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextSecondary)
                                Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    InfoItem("Altitude", "${flight.altitude?.toInt() ?: "—"} ft")
                                    InfoItem("Speed", "${flight.speed?.toInt() ?: "—"} kts")
                                    InfoItem("Heading", "${flight.heading?.toInt() ?: "—"}°")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    scheduled: String?,
    estimated: String?,
    actual: String?,
    gate: String?,
    terminal: String?,
    delay: Int?
) {
    Column {
        Text(text = label, style = TrueSkiesTypography.labelMedium, color = TrueSkiesColors.TextMuted)
        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = "Scheduled", style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextTertiary)
                Text(
                    text = formatTimeShort(scheduled),
                    style = AviationTypography.timeDisplay,
                    color = TrueSkiesColors.TextPrimary
                )
            }
            if (estimated != null || actual != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (actual != null) "Actual" else "Estimated",
                        style = TrueSkiesTypography.labelSmall,
                        color = TrueSkiesColors.TextTertiary
                    )
                    val delayColor = when {
                        delay != null && delay > 15 -> TrueSkiesColors.StatusDelayed
                        delay != null && delay > 0 -> TrueSkiesColors.Warning
                        else -> TrueSkiesColors.StatusOnTime
                    }
                    Text(
                        text = formatTimeShort(actual ?: estimated),
                        style = AviationTypography.timeDisplay,
                        color = delayColor
                    )
                }
            }
        }
        // Gate & Terminal
        Row(modifier = Modifier.padding(top = TrueSkiesSpacing.xxs)) {
            terminal?.let {
                Text("Terminal $it", style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
                Spacer(modifier = Modifier.width(TrueSkiesSpacing.md))
            }
            gate?.let {
                Text("Gate $it", style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
            }
        }
        // Delay
        delay?.takeIf { it > 0 }?.let {
            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxs))
            Text(
                text = "+${it} min delay",
                style = TrueSkiesTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TrueSkiesColors.StatusDelayed
            )
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextMuted)
        Text(
            text = value,
            style = AviationTypography.dataValue,
            color = TrueSkiesColors.TextPrimary
        )
    }
}

private fun formatTimeShort(isoString: String?): String {
    if (isoString == null) return "--:--"
    return try {
        val zdt = java.time.ZonedDateTime.parse(isoString)
        zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        try {
            val s = isoString.substringBefore("Z").substringBefore("+")
            val ldt = java.time.LocalDateTime.parse(s.take(19))
            ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e2: Exception) {
            "--:--"
        }
    }
}

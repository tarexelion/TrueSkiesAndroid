package com.trueskies.android.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.FlightStatus
import com.trueskies.android.ui.components.FlightStatusBadge
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FlightDetailViewModel
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    onBack: () -> Unit,
    viewModel: FlightDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showShareSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .statusBarsPadding()
    ) {
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
                uiState.flight?.let {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TrueSkiesColors.AccentBlue
                        )
                    }
                }
                IconButton(onClick = { viewModel.addToPersonalFlights() }) {
                    Icon(
                        imageVector = if (uiState.isAddedToPersonal) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (uiState.isAddedToPersonal) "Tracking" else "Track Flight",
                        tint = if (uiState.isAddedToPersonal) TrueSkiesColors.Success else TrueSkiesColors.AccentBlue
                    )
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TrueSkiesColors.AccentBlue
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TrueSkiesColors.SurfacePrimary)
        )

        if (showShareSheet && uiState.flight != null) {
            ShareBottomSheet(
                flight = uiState.flight!!,
                onDismiss = { showShareSheet = false }
            )
        }

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TrueSkiesColors.AccentBlue)
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load flight", style = TrueSkiesTypography.headlineSmall, color = TrueSkiesColors.Error)
                        Spacer(Modifier.height(TrueSkiesSpacing.xs))
                        Text(uiState.error ?: "", style = TrueSkiesTypography.bodyMedium, color = TrueSkiesColors.TextTertiary)
                        Spacer(Modifier.height(TrueSkiesSpacing.md))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = TrueSkiesColors.AccentBlue)
                        ) { Text("Retry") }
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
                    Spacer(Modifier.height(TrueSkiesSpacing.sm))

                    // ── Status + Airline Card ──
                    StatusAirlineCard(flight)

                    Spacer(Modifier.height(TrueSkiesSpacing.md))

                    // ── Countdown Banner (< 2 hours to scheduled/boarding departure) ──
                    CountdownBanner(flight)

                    // ── Delay / Inbound Banner ──
                    DelayBanner(flight)

                    // ── Unified Flight Timing Card (route + departure + arrival) ──
                    DetailFlightCard(flight)

                    // ── Live Position ──
                    if (flight.hasPosition) {
                        Spacer(Modifier.height(TrueSkiesSpacing.md))
                        LivePositionCard(flight)
                    }

                    Spacer(Modifier.height(TrueSkiesSpacing.md))

                    // ── Seat Card ──
                    SeatCard(
                        seat = uiState.seat,
                        onUpdateSeat = { viewModel.updateSeat(it) }
                    )

                    Spacer(Modifier.height(TrueSkiesSpacing.sm))

                    // ── Timezone Change Card ──
                    TimezoneCard(flight)

                    Spacer(Modifier.height(TrueSkiesSpacing.sm))

                    // ── Weather Placeholder Card ──
                    WeatherPlaceholderCard(flight)

                    Spacer(Modifier.height(TrueSkiesSpacing.md))

                    // ── Notes Card ──
                    NotesCard(
                        notes = uiState.notes,
                        onUpdateNotes = { viewModel.updateNotes(it) }
                    )

                    Spacer(Modifier.height(TrueSkiesSpacing.sm))

                    // ── Airline Info Card ──
                    AirlineInfoCard(flight)

                    Spacer(Modifier.height(TrueSkiesSpacing.sm))

                    // ── Route History Card ──
                    RouteHistoryCard(flight)

                    Spacer(Modifier.height(TrueSkiesSpacing.sm))

                    // ── Visa Requirement Card ──
                    VisaRequirementCard()

                    // ── Delete Button (only when tracking this flight) ──
                    if (uiState.isAddedToPersonal) {
                        Spacer(Modifier.height(TrueSkiesSpacing.xl))
                        DeleteButton(
                            onDelete = {
                                viewModel.deleteFromPersonalFlights()
                                onBack()
                            }
                        )
                    }

                    Spacer(Modifier.height(TrueSkiesSpacing.xxl))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Status + Airline Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun StatusAirlineCard(flight: Flight) {
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
                        Text(it, style = TrueSkiesTypography.bodyMedium, color = TrueSkiesColors.TextSecondary)
                    }
                    flight.aircraftRegistration?.let {
                        Text(it, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
                    }
                }
                FlightStatusBadge(status = flight.status)
            }
            flight.aircraftType?.let { type ->
                Spacer(Modifier.height(TrueSkiesSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Flight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TrueSkiesColors.TextMuted
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(type, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Countdown Banner  (LED ticker — shown < 2 hours before dep)
// ─────────────────────────────────────────────────────────────

@Composable
private fun CountdownBanner(flight: Flight) {
    val depTime = parseIso(flight.estimatedDeparture ?: flight.scheduledDeparture) ?: return
    val isPreDeparture = flight.status == FlightStatus.SCHEDULED || flight.status == FlightStatus.BOARDING

    var secondsLeft by remember { mutableLongStateOf(secondsUntil(depTime)) }

    LaunchedEffect(depTime) {
        while (true) {
            secondsLeft = secondsUntil(depTime)
            delay(1_000L)
        }
    }

    if (!isPreDeparture || secondsLeft <= 0 || secondsLeft > 7200) return

    val blinkAnim = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by blinkAnim.animateFloat(
        initialValue = 1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "blinkAlpha"
    )

    val title = when {
        flight.status == FlightStatus.BOARDING -> "BOARDING"
        flight.departureGate != null -> "GATE ${flight.departureGate}"
        else -> "DEPARTING"
    }

    Spacer(Modifier.height(TrueSkiesSpacing.sm))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.md))
            .background(Color(0xFF1A1A1E))
            .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blinking green LED dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TrueSkiesColors.StatusOnTime.copy(alpha = blinkAlpha))
            )
            Spacer(Modifier.width(TrueSkiesSpacing.sm))
            Text(
                text = title,
                style = TrueSkiesTypography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = Color(0xFFFFF0CC)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatCountdown(secondsLeft),
                style = TrueSkiesTypography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = Color(0xFFFFF0CC)
            )
        }
    }
    Spacer(Modifier.height(TrueSkiesSpacing.sm))
}

// ─────────────────────────────────────────────────────────────
// Delay / Inbound Banner
// ─────────────────────────────────────────────────────────────

@Composable
private fun DelayBanner(flight: Flight) {
    val delay = flight.departureDelay ?: return
    if (delay <= 0) return
    val isPreDeparture = flight.actualDeparture == null

    if (!isPreDeparture) return

    val blinkAnim = rememberInfiniteTransition(label = "delayBlink")
    val blinkAlpha by blinkAnim.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "delayBlinkAlpha"
    )

    val delayText = formatDelayMinutes(delay)

    Spacer(Modifier.height(TrueSkiesSpacing.xs))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.md))
            .background(TrueSkiesColors.StatusDelayed.copy(alpha = 0.12f))
            .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TrueSkiesColors.StatusDelayed.copy(alpha = blinkAlpha))
            )
            Spacer(Modifier.width(TrueSkiesSpacing.sm))
            Column {
                Text(
                    text = "DELAYED",
                    style = TrueSkiesTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.StatusDelayed
                )
                Text(
                    text = "Inbound aircraft running late · $delayText",
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextSecondary
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "+$delayText",
                style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TrueSkiesColors.StatusDelayed
            )
        }
    }
    Spacer(Modifier.height(TrueSkiesSpacing.sm))
}

// ─────────────────────────────────────────────────────────────
// Unified Flight Card (departure + duration + arrival)
// ─────────────────────────────────────────────────────────────

@Composable
private fun DetailFlightCard(flight: Flight) {
    // Deep retro-panel dark background, matching iOS UnifiedFlightCard style
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF13131C), Color(0xFF1A1A26), Color(0xFF0F0F17))
                )
            )
    ) {
        // Inner border overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
                .background(Color.Transparent)
        )
        Column {
            // Departure
            EndpointSection(
                label = "DEPARTURE",
                time = flight.bestDepartureTime,
                scheduledTime = flight.scheduledDeparture,
                airportCode = flight.originCode,
                city = flight.originCity,
                gate = flight.departureGate,
                terminal = flight.departureTerminal,
                delay = flight.departureDelay,
                isDeparture = true,
                baggageCarousel = null
            )

            // Separator with duration + distance + progress
            DurationSeparator(flight)

            // Arrival
            EndpointSection(
                label = "ARRIVAL",
                time = flight.bestArrivalTime,
                scheduledTime = flight.scheduledArrival,
                airportCode = flight.destinationCode,
                city = flight.destinationCity,
                gate = flight.arrivalGate,
                terminal = flight.arrivalTerminal,
                delay = flight.arrivalDelay,
                isDeparture = false,
                baggageCarousel = flight.arrivalBaggage
            )
        }
    }
}

@Composable
private fun EndpointSection(
    label: String,
    time: String?,
    scheduledTime: String?,
    airportCode: String,
    city: String?,
    gate: String?,
    terminal: String?,
    delay: Int?,
    isDeparture: Boolean,
    baggageCarousel: String?
) {
    // Section label
    val labelAlignment = if (isDeparture) Alignment.Start else Alignment.End
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // ── Label row ──
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isDeparture) Alignment.CenterStart else Alignment.CenterEnd) {
            Text(
                text = label,
                style = TrueSkiesTypography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                ),
                color = TrueSkiesColors.TextMuted.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── Row 1: Split-flap time  +  IATA code  +  city ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDeparture) {
                SplitFlapTime(formatTimeShort(time))
                Spacer(Modifier.width(TrueSkiesSpacing.sm))
                AirportInlineDisplay(code = airportCode, city = city, alignEnd = false)
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
                AirportInlineDisplay(code = airportCode, city = city, alignEnd = true)
                Spacer(Modifier.width(TrueSkiesSpacing.sm))
                SplitFlapTime(formatTimeShort(time))
            }
        }

        Spacer(Modifier.height(TrueSkiesSpacing.xs))

        // ── Row 2: Status  +  Gate ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDeparture) {
                FlightStatusRow(delay = delay, scheduledTime = scheduledTime)
                Spacer(Modifier.weight(1f))
                GateChip(gate = gate, terminal = terminal)
            } else {
                GateChip(gate = gate, terminal = terminal)
                Spacer(Modifier.weight(1f))
                FlightStatusRow(delay = delay, scheduledTime = scheduledTime)
            }
        }

        // ── Row 3 (arrival only): Baggage carousel ──
        if (!isDeparture && baggageCarousel != null) {
            Spacer(Modifier.height(TrueSkiesSpacing.xs))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BaggageChip(carousel = baggageCarousel)
            }
        }
    }
}

@Composable
private fun SplitFlapTime(time: String) {
    // iOS SplitFlapCard: dark rounded box with center-divider line
    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.sm))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1E1E28), Color(0xFF16161E)))
            )
            .border(
                width = 1.dp,
                color = Color(0xFF2E2E3E),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.sm)
            )
            .padding(horizontal = TrueSkiesSpacing.sm, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = time,
                style = TrueSkiesTypography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color(0xFFF0F0FF)
            )
            // Split-flap center divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2A2A3A).copy(alpha = 0.3f),
                                Color(0xFF3A3A50),
                                Color(0xFF2A2A3A).copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }
    }
}

/** Airport IATA code (large) + • city (small) shown inline — matches iOS `airportDisplay` */
@Composable
private fun AirportInlineDisplay(code: String, city: String?, alignEnd: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (alignEnd && city != null) {
            Text(
                text = "$city •",
                style = TrueSkiesTypography.labelSmall,
                color = TrueSkiesColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = code,
            style = TrueSkiesTypography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            ),
            color = Color(0xFFF0F0FF)
        )
        if (!alignEnd && city != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = "• $city",
                style = TrueSkiesTypography.labelSmall,
                color = TrueSkiesColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** ON TIME / DELAYED / EARLY status row — matches iOS `statusDisplay` */
@Composable
private fun FlightStatusRow(delay: Int?, scheduledTime: String?) {
    when {
        delay != null && delay > 0 -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DELAYED",
                    style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.StatusDelayed
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "+${formatDelayMinutes(delay)}",
                    style = TrueSkiesTypography.labelSmall,
                    color = TrueSkiesColors.StatusDelayed
                )
                if (scheduledTime != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatTimeShort(scheduledTime),
                        style = TrueSkiesTypography.labelSmall,
                        color = TrueSkiesColors.TextMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
        delay != null && delay < 0 -> {
            Text(
                text = "EARLY",
                style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TrueSkiesColors.StatusOnTime
            )
        }
        else -> {
            Text(
                text = "ON TIME",
                style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = TrueSkiesColors.StatusOnTime
            )
        }
    }
}

@Composable
private fun GateChip(gate: String?, terminal: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        terminal?.let {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
                    .background(Color(0xFF2A2A3A))
                    .padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xxs)
            ) {
                Text(
                    text = "T$it",
                    style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextSecondary
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        if (gate != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
                    .background(TrueSkiesColors.AccentBlue)
                    .padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xxs)
            ) {
                Text(
                    text = gate,
                    style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        } else if (terminal == null) {
            // Both null: show Gate TBD (matches iOS GateTerminalDisplay)
            Text(
                text = "Gate TBD",
                style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = TrueSkiesColors.TextMuted
            )
        }
    }
}

@Composable
private fun BaggageChip(carousel: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
            .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.15f))
            .padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Luggage,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = TrueSkiesColors.AccentBlue
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Baggage $carousel",
            style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = TrueSkiesColors.TextPrimary
        )
    }
}

@Composable
private fun DurationSeparator(flight: Flight) {
    val duration = flight.routeDuration
    val distance = flight.routeDistance
    val progress = (flight.progressPercent ?: 0) / 100f

    Column {
        // ── Metal-style separator (iOS MetalGridSeparator) ──
        Column {
            // Top highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.04f))
            )
            // Main gradient bar with rivet dots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF252535), Color(0xFF1C1C2A), Color(0xFF141420))
                        )
                    )
            ) {
                // Rivet dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(8) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E2E42))
                        )
                    }
                }
            }
            // Bottom shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        // ── Duration / distance / progress row ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Full-width progress bar: origin dot → plane icon at progress → destination dot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                // Track background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2A2A38))
                )
                // Filled progress
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(TrueSkiesColors.AccentBlue, TrueSkiesColors.AccentCyan)
                                )
                            )
                    )
                }
                // Origin dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(TrueSkiesColors.AccentBlue)
                        .align(Alignment.CenterStart)
                )
                // Destination dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (progress >= 1f) TrueSkiesColors.AccentCyan
                            else Color(0xFF2A2A38)
                        )
                        .border(1.dp, TrueSkiesColors.AccentCyan.copy(alpha = 0.5f), CircleShape)
                        .align(Alignment.CenterEnd)
                )
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // Duration + distance text centred
            val parts = buildList {
                duration?.let { add("⏱ ${formatDuration(it)}") }
                distance?.let { add("↔ ${formatDistance(it)}") }
            }
            if (parts.isNotEmpty()) {
                Text(
                    text = parts.joinToString("   "),
                    style = TrueSkiesTypography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    color = TrueSkiesColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Closing separator ──
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.04f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF141420), Color(0xFF1C1C2A), Color(0xFF252535))
                        )
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Live Position Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun LivePositionCard(flight: Flight) {
    LiquidGlassCard {
        Column(modifier = Modifier.padding(TrueSkiesSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TrueSkiesColors.StatusOnTime)
                )
                Spacer(Modifier.width(TrueSkiesSpacing.xs))
                Text("LIVE POSITION", style = TrueSkiesTypography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ), color = TrueSkiesColors.TextSecondary)
            }
            Spacer(Modifier.height(TrueSkiesSpacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("Altitude", flight.altitude?.let { "${it.toInt()} ft" } ?: "—")
                InfoItem("Speed", flight.speed?.let { "${it.toInt()} kts" } ?: "—")
                InfoItem("Heading", flight.heading?.let { "${it.toInt()}°" } ?: "—")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Timezone Change Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun TimezoneCard(flight: Flight) {
    val originTz = flight.originTimezone ?: return
    val destTz = flight.destinationTimezone ?: return

    val originOffset = java.util.TimeZone.getTimeZone(originTz)?.rawOffset ?: return
    val destOffset = java.util.TimeZone.getTimeZone(destTz)?.rawOffset ?: return
    val hoursDiff = (destOffset - originOffset) / 3_600_000
    if (hoursDiff == 0) return

    LiquidGlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = TrueSkiesColors.AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(TrueSkiesSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Time Zone Change",
                    style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextPrimary
                )
                Text(
                    text = if (hoursDiff > 0)
                        "Destination is ${abs(hoursDiff)}h ahead"
                    else
                        "Destination is ${abs(hoursDiff)}h behind",
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextSecondary
                )
            }
            // Timezone diff badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
                    .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.2f))
                    .padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xxs)
            ) {
                Text(
                    text = "${if (hoursDiff > 0) "+" else ""}${hoursDiff}h",
                    style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.AccentBlue
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Delete Button
// ─────────────────────────────────────────────────────────────

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
            .background(TrueSkiesColors.StatusCancelled.copy(alpha = 0.12f))
            .padding(1.dp)
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
    ) {
        TextButton(
            onClick = { showConfirm = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = TrueSkiesColors.StatusCancelled,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(TrueSkiesSpacing.xs))
            Text(
                text = "Remove Flight",
                style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TrueSkiesColors.StatusCancelled
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Remove Flight", color = TrueSkiesColors.TextPrimary) },
            text = { Text("Stop tracking this flight?", color = TrueSkiesColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) {
                    Text("Remove", color = TrueSkiesColors.StatusCancelled)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = TrueSkiesColors.TextSecondary)
                }
            },
            containerColor = TrueSkiesColors.SurfaceElevated
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Shared subcomponents
// ─────────────────────────────────────────────────────────────

@Composable
private fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextMuted)
        Text(value, style = AviationTypography.dataValue, color = TrueSkiesColors.TextPrimary)
    }
}

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

private fun formatTimeShort(isoString: String?): String {
    if (isoString == null) return "--:--"
    return try {
        ZonedDateTime.parse(isoString).format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        try {
            val s = isoString.substringBefore("Z").substringBefore("+")
            java.time.LocalDateTime.parse(s.take(19))
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e2: Exception) {
            "--:--"
        }
    }
}

private fun parseIso(isoString: String?): ZonedDateTime? {
    if (isoString == null) return null
    return try {
        ZonedDateTime.parse(isoString)
    } catch (e: Exception) {
        null
    }
}

private fun secondsUntil(zdt: ZonedDateTime): Long {
    return java.time.Duration.between(ZonedDateTime.now(), zdt).seconds
}

private fun formatCountdown(seconds: Long): String {
    if (seconds <= 0) return "NOW"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

private fun formatDelayMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else {
        "${minutes}m"
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0 && m > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m"
}

private fun formatDistance(miles: Double): String {
    return "${"%,.0f".format(miles)} mi"
}

// ─────────────────────────────────────────────────────────────
// Seat Card  (iOS: PersonalFlightInfoSection)
// ─────────────────────────────────────────────────────────────

@Composable
private fun SeatCard(seat: String, onUpdateSeat: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yellow person circle (figure.wave.circle.fill equivalent)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFCC00)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Centre text
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (seat.isNotEmpty()) {
                    Text(
                        text = "My Seat",
                        style = TrueSkiesTypography.bodySmall,
                        color = TrueSkiesColors.TextSecondary
                    )
                    Text(
                        text = seat,
                        style = TrueSkiesTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TrueSkiesColors.TextPrimary
                    )
                } else {
                    Text(
                        text = "Tap to select seat",
                        style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TrueSkiesColors.TextSecondary
                    )
                }
            }

            // Seat icon
            Icon(
                Icons.Default.AirlineSeatReclineNormal,
                contentDescription = null,
                tint = TrueSkiesColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showDialog) {
        SeatInputDialog(
            currentSeat = seat,
            onConfirm = { onUpdateSeat(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun SeatInputDialog(
    currentSeat: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentSeat) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Seat Number", color = TrueSkiesColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 6) text = it.uppercase() },
                placeholder = { Text("e.g. 14A", color = TrueSkiesColors.TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrueSkiesColors.AccentBlue,
                    unfocusedBorderColor = TrueSkiesColors.GlassBorder,
                    focusedTextColor = TrueSkiesColors.TextPrimary,
                    unfocusedTextColor = TrueSkiesColors.TextPrimary,
                    cursorColor = TrueSkiesColors.AccentBlue
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text("Save", color = TrueSkiesColors.AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TrueSkiesColors.TextSecondary)
            }
        },
        containerColor = TrueSkiesColors.SurfaceElevated
    )
}

// ─────────────────────────────────────────────────────────────
// Weather Placeholder Card  (iOS: ArrivalWeatherInfoCard)
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeatherPlaceholderCard(flight: Flight) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LED-style housing
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TrueSkiesColors.AccentCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = TrueSkiesColors.AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(TrueSkiesSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Arrival Weather",
                    style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextPrimary
                )
                Text(
                    text = "${flight.destinationCode} · Unable to load",
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
                    .background(TrueSkiesColors.AccentCyan.copy(alpha = 0.15f))
                    .padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xxs)
            ) {
                Text(
                    text = "—",
                    style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.AccentCyan
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Notes Card  (iOS: NotesCard)
// ─────────────────────────────────────────────────────────────

@Composable
private fun NotesCard(notes: String, onUpdateNotes: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextPrimary
                )
            }
            Spacer(Modifier.height(TrueSkiesSpacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDialog = true }
                    .padding(vertical = TrueSkiesSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (notes.isEmpty()) "Tap to add notes..." else notes,
                    style = TrueSkiesTypography.bodySmall,
                    color = if (notes.isEmpty()) TrueSkiesColors.TextMuted else TrueSkiesColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (notes.isEmpty()) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = TrueSkiesColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        NotesInputDialog(
            currentNotes = notes,
            onConfirm = { onUpdateNotes(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun NotesInputDialog(
    currentNotes: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentNotes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flight Notes", color = TrueSkiesColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 500) text = it },
                placeholder = { Text("Add notes about this flight...", color = TrueSkiesColors.TextMuted) },
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrueSkiesColors.AccentBlue,
                    unfocusedBorderColor = TrueSkiesColors.GlassBorder,
                    focusedTextColor = TrueSkiesColors.TextPrimary,
                    unfocusedTextColor = TrueSkiesColors.TextPrimary,
                    cursorColor = TrueSkiesColors.AccentBlue
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text("Save", color = TrueSkiesColors.AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TrueSkiesColors.TextSecondary)
            }
        },
        containerColor = TrueSkiesColors.SurfaceElevated
    )
}

// ─────────────────────────────────────────────────────────────
// Airline Info Card  (iOS: AirlineInfoCard)
// ─────────────────────────────────────────────────────────────

@Composable
private fun AirlineInfoCard(flight: Flight) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md)
        ) {
            Text(
                text = flight.airlineName ?: "Airline Info",
                style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TrueSkiesColors.TextPrimary
            )
            Spacer(Modifier.height(TrueSkiesSpacing.sm))
            CodeRow(label = "ATC Callsign", value = flight.callsign ?: flight.displayFlightNumber)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = TrueSkiesSpacing.xxs),
                color = TrueSkiesColors.GlassHighlight,
                thickness = 0.5.dp
            )
            CodeRow(label = "ICAO", value = flight.airlineIcao ?: "—")
            HorizontalDivider(
                modifier = Modifier.padding(vertical = TrueSkiesSpacing.xxs),
                color = TrueSkiesColors.GlassHighlight,
                thickness = 0.5.dp
            )
            CodeRow(label = "IATA", value = flight.airlineIata ?: "—")
        }
    }
}

@Composable
private fun CodeRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TrueSkiesSpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = TrueSkiesTypography.bodySmall,
            color = TrueSkiesColors.TextSecondary
        )
        Text(
            text = value,
            style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TrueSkiesColors.TextPrimary
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Route History Card  (iOS: RouteHistoryCard — placeholder)
// ─────────────────────────────────────────────────────────────

@Composable
private fun RouteHistoryCard(flight: Flight) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Route History",
                    style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextPrimary
                )
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = TrueSkiesColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(TrueSkiesSpacing.xs))
            Text(
                text = "${flight.originCode} → ${flight.destinationCode}",
                style = TrueSkiesTypography.bodySmall,
                color = TrueSkiesColors.TextSecondary
            )
            Spacer(Modifier.height(TrueSkiesSpacing.md))
            Text(
                text = "No previous flights on this route",
                style = TrueSkiesTypography.bodySmall,
                color = TrueSkiesColors.TextMuted
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Visa Requirement Card  (iOS: VisaRequirementCard — error state)
// ─────────────────────────────────────────────────────────────

@Composable
private fun VisaRequirementCard() {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TrueSkiesColors.TextMuted.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = TrueSkiesColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(TrueSkiesSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Visa Requirement",
                    style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextPrimary
                )
                Text(
                    text = "Unable to load visa requirements",
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextSecondary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Share Bottom Sheet
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareBottomSheet(flight: Flight, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showQrDialog by remember { mutableStateOf(false) }

    val flightUrl = "https://trueskiesapp.com/flight/${flight.id}"
    val shareText = buildString {
        appendLine("✈ ${flight.displayFlightNumber}")
        appendLine("${flight.originCode} → ${flight.destinationCode}")
        flight.airlineName?.let { appendLine(it) }
        appendLine(flightUrl)
    }.trim()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TrueSkiesColors.SurfaceSecondary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = TrueSkiesSpacing.sm, bottom = TrueSkiesSpacing.xs)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
                    .background(TrueSkiesColors.TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrueSkiesSpacing.lg)
                .padding(bottom = TrueSkiesSpacing.xxl)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = TrueSkiesColors.AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(TrueSkiesSpacing.md))
                Column {
                    Text(
                        text = "Share Flight",
                        style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TrueSkiesColors.TextPrimary
                    )
                    Text(
                        text = "${flight.displayFlightNumber}  ·  ${flight.originCode} → ${flight.destinationCode}",
                        style = TrueSkiesTypography.bodySmall,
                        color = TrueSkiesColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xl))

            // QR Code option
            ShareOptionRow(
                icon = Icons.Default.QrCode,
                iconTint = TrueSkiesColors.AccentCyan,
                iconBg = TrueSkiesColors.AccentCyan.copy(alpha = 0.12f),
                title = "QR Code",
                subtitle = "Show a scannable code to share this flight"
            ) {
                showQrDialog = true
            }

            Spacer(Modifier.height(TrueSkiesSpacing.sm))

            // Share URL option
            ShareOptionRow(
                icon = Icons.Default.Link,
                iconTint = TrueSkiesColors.AccentBlue,
                iconBg = TrueSkiesColors.AccentBlue.copy(alpha = 0.12f),
                title = "Share Link",
                subtitle = "Copy or send the flight link"
            ) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, flight.displayFlightNumber)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "Share Flight"))
                onDismiss()
            }
        }
    }

    if (showQrDialog) {
        QrCodeDialog(
            url = flightUrl,
            flightNumber = flight.displayFlightNumber,
            route = "${flight.originCode} → ${flight.destinationCode}",
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
private fun ShareOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
            .background(TrueSkiesColors.SurfaceElevated)
            .clickable { onClick() }
            .padding(TrueSkiesSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TrueSkiesTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TrueSkiesColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = TrueSkiesTypography.bodySmall,
                color = TrueSkiesColors.TextSecondary
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TrueSkiesColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// QR Code Dialog
// ─────────────────────────────────────────────────────────────

@Composable
private fun QrCodeDialog(
    url: String,
    flightNumber: String,
    route: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(url) { generateQrBitmap(url) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(TrueSkiesCornerRadius.xxl))
                .background(TrueSkiesColors.SurfaceSecondary)
                .padding(TrueSkiesSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flight info header
            Text(
                text = flightNumber,
                style = TrueSkiesTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TrueSkiesColors.TextPrimary
            )
            Text(
                text = route,
                style = TrueSkiesTypography.bodyMedium,
                color = TrueSkiesColors.TextSecondary
            )

            Spacer(Modifier.height(TrueSkiesSpacing.xl))

            // QR Code image
            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
                        .background(Color.White)
                        .padding(TrueSkiesSpacing.sm)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code for $flightNumber",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
                        .background(TrueSkiesColors.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Unable to generate QR", color = TrueSkiesColors.TextMuted,
                        style = TrueSkiesTypography.bodySmall, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.lg))

            // URL label
            Text(
                text = url,
                style = TrueSkiesTypography.labelSmall,
                color = TrueSkiesColors.TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(TrueSkiesSpacing.xl))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
            ) {
                // Copy URL
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Flight URL", url))
                    },
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TrueSkiesColors.AccentBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TrueSkiesColors.AccentBlue)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", style = TrueSkiesTypography.bodySmall)
                }
                // Done
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TrueSkiesColors.AccentBlue)
                ) {
                    Text("Done", style = TrueSkiesTypography.bodySmall)
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int = 512): android.graphics.Bitmap? {
    return try {
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

package com.trueskies.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.FlightStatus
import com.trueskies.android.domain.models.PersonalFlight
import com.trueskies.android.ui.theme.*
import com.trueskies.android.util.rememberAirlineLogo
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Personal flight card — matches iOS PersonalFlightCard horizontal layout.
 *
 * Structure:
 *  [LEFT ~96dp: duration/state] | [RIGHT flexible]
 *                                  Row1: Airline circle + flight# + status text
 *                                  Row2: City → City (route)
 *                                  Row3: ✈dep IAD 21:15   ✈arr IST 15:20
 */
@Composable
fun FlightCard(
    personalFlight: PersonalFlight,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val flight = personalFlight.flight
    val status = personalFlight.confirmedStatus

    LiquidGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TrueSkiesSpacing.md, end = TrueSkiesSpacing.xl,
                    top = TrueSkiesSpacing.sm, bottom = TrueSkiesSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── LEFT: Duration / State column (fixed 96dp) ──
            Box(
                modifier = Modifier.width(96.dp),
                contentAlignment = Alignment.Center
            ) {
                LeftStateColumn(flight = flight, status = status)
            }

            // ── RIGHT: Flight details ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xxs)
            ) {
                // Row 1: Airline initials + flight number + status text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AirlineAvatar(
                        iata = personalFlight.logoAirlineCode ?: flight.airlineIata,
                        name = flight.airlineName
                    )
                    Spacer(Modifier.width(TrueSkiesSpacing.xs))
                    Text(
                        text = personalFlight.displayName,
                        style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TrueSkiesColors.TextSecondary,
                        maxLines = 1
                    )
                    if (flight.isCodeshare) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "·",
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextMuted
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = flight.marketingAirlineName ?: "",
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = statusText(flight, status),
                        style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = statusTextColor(flight, status),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Row 2: City → City route
                Text(
                    text = routeDescription(flight),
                    style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Row 3: Dep icon + origin + dep time + arr icon + dest + arr time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
                ) {
                    AirportTimeChip(
                        code = flight.originCode,
                        time = formatTime(flight.bestDepartureTime, flight.originTimezone),
                        isDeparture = true,
                        delay = flight.departureDelay,
                        scheduledTime = flight.scheduledDeparture
                    )
                    AirportTimeChip(
                        code = flight.destinationCode,
                        time = formatTime(flight.bestArrivalTime, flight.destinationTimezone),
                        isDeparture = false,
                        delay = flight.arrivalDelay,
                        scheduledTime = flight.scheduledArrival
                    )
                }

                // Diversion info (iOS)
                if (flight.diverted && flight.divertedToAirportCode != null) {
                    Text(
                        text = "Diverted to ${flight.divertedToAirportName ?: flight.divertedToAirportCode}",
                        style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TrueSkiesColors.StatusDiverted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Left state column ──

@Composable
private fun LeftStateColumn(flight: Flight, status: FlightStatus) {
    when {
        status == FlightStatus.CANCELLED -> {
            Text(
                text = "CANCELLED",
                style = TrueSkiesTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = TrueSkiesColors.StatusCancelled
            )
        }
        status == FlightStatus.BOARDING -> {
            BoardingPulse()
        }
        status == FlightStatus.DIVERTED -> {
            Text(
                text = "DIVERTED",
                style = TrueSkiesTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = TrueSkiesColors.StatusDiverted
            )
        }
        status.isAirborne || status == FlightStatus.DEPARTED || status == FlightStatus.TAXIING_OUT -> {
            // If arrival time has passed, show LANDED instead of IN FLIGHT
            val arrTime = parseIso(flight.bestArrivalTime)
            if (arrTime != null && ZonedDateTime.now().isAfter(arrTime)) {
                LandedDisplay()
            } else {
                InFlightDisplay()
            }
        }
        status == FlightStatus.ARRIVED || status == FlightStatus.LANDED ||
        status == FlightStatus.COMPLETED || status == FlightStatus.TAXIING_IN -> {
            DurationDisplay(
                durationMinutes = flight.routeDuration,
                labelTop = null,
                labelBottom = null,
                color = TrueSkiesColors.TextPrimary
            )
        }
        else -> {
            // Countdown to departure
            val depTime = parseIso(flight.estimatedDeparture ?: flight.scheduledDeparture)
            if (depTime != null) {
                CountdownDisplay(depTime = depTime, isDelayed = (flight.departureDelay ?: 0) > 0)
            } else {
                DurationDisplay(durationMinutes = flight.routeDuration, color = TrueSkiesColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun DurationDisplay(
    durationMinutes: Int?,
    labelTop: String? = null,
    labelBottom: String? = null,
    color: Color
) {
    if (durationMinutes == null) {
        Text("—", style = TrueSkiesTypography.headlineMedium, color = TrueSkiesColors.TextMuted)
        return
    }
    val h = durationMinutes / 60
    val m = durationMinutes % 60
    val large = if (h > 0) "${h}h" else "${m}m"
    val small = if (h > 0 && m > 0) "${m}m" else null

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        labelTop?.let {
            Text(it, style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp), color = color)
        }
        Text(
            text = large,
            style = TrueSkiesTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        small?.let {
            Text(it, style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.Medium), color = color.copy(alpha = 0.7f))
        }
        labelBottom?.let {
            Text(it, style = TrueSkiesTypography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun CountdownDisplay(depTime: ZonedDateTime, isDelayed: Boolean) {
    var secondsLeft by remember { mutableLongStateOf(secondsUntil(depTime)) }
    LaunchedEffect(depTime) {
        while (true) {
            secondsLeft = secondsUntil(depTime)
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val color = if (isDelayed) TrueSkiesColors.StatusDelayed else TrueSkiesColors.TextPrimary
    val labelColor = if (isDelayed) TrueSkiesColors.StatusDelayed else TrueSkiesColors.TextMuted

    if (secondsLeft <= 0) {
        DurationDisplay(durationMinutes = null, color = color)
        return
    }

    val totalMins = secondsLeft / 60
    val h = totalMins / 60
    val m = totalMins % 60
    val large = if (h > 0) "${h}h" else "${m}m"
    val small = if (h > 0 && m > 0) "${m}m" else null

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "DEPARTS IN",
            style = TrueSkiesTypography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                fontSize = 9.sp
            ),
            color = labelColor
        )
        Text(
            text = large,
            style = TrueSkiesTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        small?.let {
            Text(it, style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun BoardingPulse() {
    val pulse = rememberInfiniteTransition(label = "boarding")
    val alpha by pulse.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "boardingAlpha"
    )
    Text(
        text = "BOARDING",
        style = TrueSkiesTypography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            fontSize = 11.sp
        ),
        color = TrueSkiesColors.StatusOnTime.copy(alpha = alpha)
    )
}

@Composable
private fun InFlightDisplay() {
    val pulse = rememberInfiniteTransition(label = "inflight")
    val translateX by pulse.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "planeX"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Flight,
            contentDescription = null,
            tint = TrueSkiesColors.AccentBlue,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { translationX = translateX }
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "IN FLIGHT",
            style = TrueSkiesTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp
            ),
            color = TrueSkiesColors.AccentBlue
        )
    }
}

@Composable
private fun LandedDisplay() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Flight,
            contentDescription = null,
            tint = TrueSkiesColors.StatusOnTime,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { rotationZ = 90f }
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "LANDED",
            style = TrueSkiesTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp
            ),
            color = TrueSkiesColors.StatusOnTime
        )
    }
}

// ── Airline logo / initials avatar ──

@Composable
private fun AirlineAvatar(iata: String?, name: String?) {
    val logoBitmap = rememberAirlineLogo(airlineName = name, airlineCode = iata)

    if (logoBitmap != null) {
        Image(
            bitmap = logoBitmap,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(20.dp)
                .widthIn(max = 64.dp)
        )
    } else {
        // Fallback: initials circle
        val initials = when {
            iata != null -> iata.take(2).uppercase()
            name != null -> name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("").take(2)
            else -> "?"
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = TrueSkiesTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = TrueSkiesColors.AccentBlue
            )
        }
    }
}

// ── Airport + time chip (dep/arr row) ──

@Composable
private fun AirportTimeChip(
    code: String,
    time: String,
    isDeparture: Boolean,
    delay: Int?,
    scheduledTime: String?
) {
    val timeColor = when {
        delay != null && delay > 0 -> TrueSkiesColors.StatusDelayed
        else -> TrueSkiesColors.TextPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "✈",
            fontSize = 12.sp,
            color = if (isDeparture) TrueSkiesColors.StatusOnTime else TrueSkiesColors.AccentCyan
        )
        Text(
            text = code,
            style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = TrueSkiesColors.TextPrimary
        )
        Text(
            text = time,
            style = TrueSkiesTypography.bodySmall,
            color = timeColor
        )
    }
}

// ── Status text (matches iOS displayStatus) ──

private fun statusText(flight: Flight, status: FlightStatus): String {
    return when (status) {
        FlightStatus.ARRIVED, FlightStatus.LANDED, FlightStatus.COMPLETED -> {
            val arrTime = flight.actualArrival ?: flight.bestArrivalTime
            if (arrTime != null) {
                val zdt = parseIso(arrTime)
                if (zdt != null) {
                    val localZdt = if (flight.destinationTimezone != null) {
                        zdt.withZoneSameInstant(java.time.ZoneId.of(flight.destinationTimezone))
                    } else zdt
                    val fmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")
                    "Arrived ${localZdt.format(fmt)}"
                } else "Arrived"
            } else "Arrived"
        }
        FlightStatus.BOARDING -> "Now Boarding"
        FlightStatus.CANCELLED -> "Cancelled"
        FlightStatus.DIVERTED -> {
            if (flight.divertedToAirportCode != null) "Diverted to ${flight.divertedToAirportCode}"
            else "Diverted"
        }
        FlightStatus.SCHEDULED, FlightStatus.FILING -> {
            val delay = flight.departureDelay ?: 0
            if (delay > 0) "Delayed +${formatDelayMins(delay)}" else "On Time"
        }
        FlightStatus.EN_ROUTE, FlightStatus.CRUISE, FlightStatus.APPROACH,
        FlightStatus.CLIMB, FlightStatus.TAKEOFF, FlightStatus.LANDING,
        FlightStatus.TAXIING_IN, FlightStatus.TAXIING_OUT,
        FlightStatus.DEPARTED -> {
            val arrTime = flight.bestArrivalTime
            if (arrTime != null) {
                val zdt = parseIso(arrTime)
                if (zdt != null && ZonedDateTime.now().isAfter(zdt)) {
                    // Arrival time has passed — show as arrived
                    val localZdt = if (flight.destinationTimezone != null) {
                        zdt.withZoneSameInstant(java.time.ZoneId.of(flight.destinationTimezone))
                    } else zdt
                    "Arrived ${localZdt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                } else {
                    val time = formatTime(arrTime, flight.destinationTimezone)
                    "Arrives $time"
                }
            } else "In Flight"
        }
        else -> status.displayName
    }
}

private fun statusTextColor(flight: Flight, status: FlightStatus): Color {
    return when (status) {
        FlightStatus.ARRIVED, FlightStatus.LANDED, FlightStatus.COMPLETED,
        FlightStatus.BOARDING -> TrueSkiesColors.StatusOnTime
        FlightStatus.CANCELLED -> TrueSkiesColors.StatusCancelled
        FlightStatus.DIVERTED -> TrueSkiesColors.StatusDiverted
        FlightStatus.EN_ROUTE, FlightStatus.CRUISE, FlightStatus.APPROACH,
        FlightStatus.CLIMB, FlightStatus.TAKEOFF, FlightStatus.LANDING,
        FlightStatus.DEPARTED, FlightStatus.TAXIING_OUT,
        FlightStatus.TAXIING_IN -> {
            // If arrival time has passed, use arrived color
            val arrTime = parseIso(flight.bestArrivalTime)
            if (arrTime != null && ZonedDateTime.now().isAfter(arrTime)) {
                TrueSkiesColors.StatusOnTime
            } else {
                TrueSkiesColors.StatusActive
            }
        }
        FlightStatus.SCHEDULED, FlightStatus.FILING -> if ((flight.departureDelay ?: 0) > 0)
            TrueSkiesColors.StatusDelayed else TrueSkiesColors.StatusOnTime
        else -> TrueSkiesColors.TextSecondary
    }
}

private fun routeDescription(flight: Flight): String {
    val origin = flight.originCity ?: flight.originCode
    val dest = flight.destinationCity ?: flight.destinationCode
    return "$origin → $dest"
}

// ─────────────────────────────────────────────────────────────
// FlightRow — search result row (compact, legacy)
// ─────────────────────────────────────────────────────────────

@Composable
fun FlightRow(
    flight: Flight,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xxs)
        ) {
            // Row 1: Flight number + Route + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = flight.displayFlightNumber,
                    style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.TextPrimary
                )
                Spacer(Modifier.width(TrueSkiesSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        flight.originCode,
                        style = AviationTypography.airportCodeSmall.copy(fontSize = 14.sp),
                        color = TrueSkiesColors.TextSecondary
                    )
                    Text(" → ", color = TrueSkiesColors.AccentBlue, fontSize = 13.sp)
                    Text(
                        flight.destinationCode,
                        style = AviationTypography.airportCodeSmall.copy(fontSize = 14.sp),
                        color = TrueSkiesColors.TextSecondary
                    )
                }
                Spacer(Modifier.weight(1f))
                FlightStatusBadge(status = flight.status)
            }

            // Row 2: Date · Aircraft · Registration · Duration · Progress
            val details = buildList {
                val dateStr = formatDate(flight.bestDepartureTime)
                if (dateStr.isNotEmpty()) add(dateStr)
                flight.aircraftType?.let { add(it) }
                flight.aircraftRegistration?.let { add(it) }
                flight.routeDuration?.let { mins ->
                    val h = mins / 60; val m = mins % 60
                    add(if (h > 0 && m > 0) "${h}h ${m}m flight" else if (h > 0) "${h}h flight" else "${m}m flight")
                }
                flight.progressPercent?.let { pct ->
                    if (pct > 0 && flight.status.isActive) add("↗ $pct%")
                }
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SearchResultFlightCard — iOS-style Add Flight result card
// ─────────────────────────────────────────────────────────────

/**
 * Matches iOS FlightSearchResultsView card layout:
 * [Status indicator (72dp)] | [Airline + flight# + date]
 *                           | [Route bold]
 *                           | [Status text · Gate]
 */
@Composable
fun SearchResultFlightCard(
    flight: Flight,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val status = flight.status

    LiquidGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TrueSkiesSpacing.md,
                    end = TrueSkiesSpacing.xl,
                    top = TrueSkiesSpacing.md,
                    bottom = TrueSkiesSpacing.md
                ),
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── LEFT: Status indicator (fixed 72dp) ──
            Box(
                modifier = Modifier.width(72.dp),
                contentAlignment = Alignment.Center
            ) {
                SearchStatusIndicator(flight = flight, status = status)
            }

            // ── RIGHT: Flight details ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xxs)
            ) {
                // Row 1: Airline logo + flight number ... date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AirlineAvatar(iata = flight.airlineIata ?: flight.airlineIcao, name = flight.airlineName)
                    Spacer(Modifier.width(TrueSkiesSpacing.xs))
                    Text(
                        text = flight.displayFlightNumber,
                        style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TrueSkiesColors.TextSecondary,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatDate(flight.bestDepartureTime),
                        style = TrueSkiesTypography.bodySmall,
                        color = TrueSkiesColors.TextMuted,
                        maxLines = 1
                    )
                }

                // Row 2: City → City route (bold)
                Text(
                    text = routeDescription(flight),
                    style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TrueSkiesColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Row 3: Status text · Gate
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = searchStatusText(flight, status),
                        style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = searchStatusColor(status),
                        maxLines = 1
                    )
                    val gate = flight.currentRelevantGate
                    if (gate != null) {
                        Text(
                            text = "·",
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextMuted
                        )
                        Text(
                            text = "Gate $gate",
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ── Search status indicator (left side of search result card) ──

@Composable
private fun SearchStatusIndicator(flight: Flight, status: FlightStatus) {
    when {
        // Active / live flights: pulsing dot + short label
        status.isActive -> {
            val pulse = rememberInfiniteTransition(label = "live-pulse")
            val scale by pulse.animateFloat(
                initialValue = 0.8f, targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            val alpha by pulse.animateFloat(
                initialValue = 1f, targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Halo ring
                    Box(
                        modifier = Modifier
                            .size((12 * scale).dp)
                            .clip(CircleShape)
                            .background(TrueSkiesColors.StatusOnTime.copy(alpha = alpha * 0.3f))
                    )
                    // Solid dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TrueSkiesColors.StatusOnTime)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = searchShortStatusLabel(flight, status),
                    style = TrueSkiesTypography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    ),
                    color = TrueSkiesColors.StatusOnTime
                )
            }
        }
        // Cancelled
        status == FlightStatus.CANCELLED -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✕",
                    style = TrueSkiesTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.StatusCancelled
                )
            }
        }
        // Completed / arrived
        status.isCompleted -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✓",
                    style = TrueSkiesTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.StatusOnTime
                )
            }
        }
        // Scheduled — show departure time
        else -> {
            val depTime = formatTime(flight.bestDepartureTime)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = depTime,
                    style = TrueSkiesTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.TextPrimary
                )
            }
        }
    }
}

private fun searchShortStatusLabel(flight: Flight, status: FlightStatus): String {
    return when (status) {
        FlightStatus.BOARDING -> "BOARDING"
        FlightStatus.TAXIING_OUT, FlightStatus.TAXIING_IN -> "TAXI"
        FlightStatus.ARRIVED, FlightStatus.LANDED -> "LANDED"
        else -> if (flight.isLive) "LIVE" else "ACTIVE"
    }
}

private fun searchStatusText(flight: Flight, status: FlightStatus): String {
    return when (status) {
        FlightStatus.EN_ROUTE, FlightStatus.CRUISE, FlightStatus.CLIMB,
        FlightStatus.APPROACH -> "En Route"
        FlightStatus.BOARDING -> "Boarding"
        FlightStatus.DEPARTED, FlightStatus.TAKEOFF, FlightStatus.TAXIING_OUT -> "In Flight"
        FlightStatus.LANDING -> "Landing"
        FlightStatus.TAXIING_IN -> "Taxiing to Gate"
        FlightStatus.ARRIVED, FlightStatus.LANDED, FlightStatus.COMPLETED -> "Arrived"
        FlightStatus.CANCELLED -> "Cancelled"
        FlightStatus.DIVERTED -> "Diverted"
        FlightStatus.SCHEDULED, FlightStatus.FILING -> {
            val delay = flight.departureDelay ?: 0
            if (delay > 0) "Delayed +${formatDelayMins(delay)}" else "On Time"
        }
        else -> status.displayName
    }
}

private fun searchStatusColor(status: FlightStatus): Color {
    return when (status) {
        FlightStatus.EN_ROUTE, FlightStatus.CRUISE, FlightStatus.CLIMB,
        FlightStatus.APPROACH, FlightStatus.DEPARTED, FlightStatus.TAKEOFF,
        FlightStatus.TAXIING_OUT, FlightStatus.LANDING, FlightStatus.BOARDING,
        FlightStatus.TAXIING_IN -> TrueSkiesColors.StatusOnTime
        FlightStatus.ARRIVED, FlightStatus.LANDED, FlightStatus.COMPLETED -> TrueSkiesColors.StatusOnTime
        FlightStatus.CANCELLED -> TrueSkiesColors.StatusCancelled
        FlightStatus.DIVERTED -> TrueSkiesColors.StatusDiverted
        FlightStatus.SCHEDULED, FlightStatus.FILING -> TrueSkiesColors.StatusOnTime
        else -> TrueSkiesColors.TextSecondary
    }
}

private fun formatDate(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val zdt = ZonedDateTime.parse(isoString)
        zdt.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    } catch (e: Exception) {
        try {
            val s = isoString.substringBefore("Z").substringBefore("+")
            java.time.LocalDateTime.parse(s.take(19))
                .format(DateTimeFormatter.ofPattern("EEE, d MMM"))
        } catch (e2: Exception) {
            ""
        }
    }
}

// ─────────────────────────────────────────────────────────────
// FlightStatusBadge
// ─────────────────────────────────────────────────────────────

@Composable
fun FlightStatusBadge(
    status: FlightStatus,
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        FlightStatus.SCHEDULED, FlightStatus.FILING -> TrueSkiesColors.StatusScheduled
        FlightStatus.BOARDING -> TrueSkiesColors.StatusBoarding
        FlightStatus.DEPARTED, FlightStatus.TAXIING_OUT, FlightStatus.TAKEOFF -> TrueSkiesColors.StatusActive
        FlightStatus.EN_ROUTE, FlightStatus.CRUISE, FlightStatus.CLIMB,
        FlightStatus.APPROACH, FlightStatus.LANDING -> TrueSkiesColors.StatusOnTime
        FlightStatus.TAXIING_IN, FlightStatus.ARRIVED, FlightStatus.LANDED -> TrueSkiesColors.StatusLanded
        FlightStatus.COMPLETED -> TrueSkiesColors.StatusLanded
        FlightStatus.CANCELLED -> TrueSkiesColors.StatusCancelled
        FlightStatus.DIVERTED -> TrueSkiesColors.StatusDiverted
        else -> TrueSkiesColors.StatusUnknown
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
            .background(bgColor.copy(alpha = 0.2f))
            .padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xxs)
    ) {
        Text(
            text = status.displayName.uppercase(),
            style = AviationTypography.statusLabel,
            color = bgColor
        )
    }
}

// ── Private helpers ──

private fun formatTime(isoString: String?, timezone: String? = null): String {
    if (isoString == null) return "--:--"
    return try {
        val zdt = ZonedDateTime.parse(isoString)
        val localZdt = if (timezone != null) {
            zdt.withZoneSameInstant(java.time.ZoneId.of(timezone))
        } else {
            zdt
        }
        localZdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        try {
            val s = isoString.substringBefore("Z").substringBefore("+")
            java.time.LocalDateTime.parse(s.take(19)).format(DateTimeFormatter.ofPattern("HH:mm"))
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

private fun secondsUntil(zdt: ZonedDateTime): Long =
    java.time.Duration.between(ZonedDateTime.now(), zdt).seconds

private fun formatDelayMins(minutes: Int): String {
    return if (minutes >= 60) {
        val h = minutes / 60; val m = minutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else "${minutes}m"
}

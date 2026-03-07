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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trueskies.android.domain.models.Flight
import com.trueskies.android.domain.models.FlightStatus
import com.trueskies.android.domain.models.PersonalFlight
import com.trueskies.android.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Personal flight card — matches iOS PersonalFlightCard horizontal layout exactly.
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
                        iata = flight.airlineIata,
                        name = flight.airlineName
                    )
                    Spacer(Modifier.width(TrueSkiesSpacing.xs))
                    Text(
                        text = personalFlight.displayName,
                        style = TrueSkiesTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TrueSkiesColors.TextSecondary,
                        maxLines = 1
                    )
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
                        time = formatTime(flight.bestDepartureTime),
                        isDeparture = true,
                        delay = flight.departureDelay,
                        scheduledTime = flight.scheduledDeparture
                    )
                    AirportTimeChip(
                        code = flight.destinationCode,
                        time = formatTime(flight.bestArrivalTime),
                        isDeparture = false,
                        delay = flight.arrivalDelay,
                        scheduledTime = flight.scheduledArrival
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
        status.isAirborne || status == FlightStatus.DEPARTED || status == FlightStatus.TAXIING_OUT -> {
            InFlightDisplay()
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

// ── Airline initials avatar ──

@Composable
private fun AirlineAvatar(iata: String?, name: String?) {
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

// ── Airport + time chip (dep/arr row) ──

@Composable
private fun AirportTimeChip(
    code: String,
    time: String,
    isDeparture: Boolean,
    delay: Int?,
    scheduledTime: String?
) {
    val icon = if (isDeparture) "✈" else "🛬"
    val timeColor = when {
        delay != null && delay > 0 -> TrueSkiesColors.StatusDelayed
        else -> TrueSkiesColors.TextPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = if (isDeparture) "✈" else "✈",
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

// ── Status text ──

private fun statusText(flight: Flight, status: FlightStatus): String {
    return when (status) {
        FlightStatus.ARRIVED, FlightStatus.LANDED, FlightStatus.COMPLETED -> {
            val arrTime = flight.actualArrival ?: flight.bestArrivalTime
            if (arrTime != null) {
                val zdt = parseIso(arrTime)
                if (zdt != null) {
                    val fmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")
                    "Arrived ${zdt.format(fmt)}"
                } else "Arrived"
            } else "Arrived"
        }
        FlightStatus.BOARDING -> "Now Boarding"
        FlightStatus.CANCELLED -> "Cancelled"
        FlightStatus.DIVERTED -> "Diverted"
        FlightStatus.SCHEDULED -> {
            val delay = flight.departureDelay ?: 0
            if (delay > 0) "Delayed +${formatDelayMins(delay)}" else "On Time"
        }
        FlightStatus.EN_ROUTE, FlightStatus.APPROACH -> {
            val pct = flight.progressPercent
            if (pct != null && pct > 0) "En Route · $pct%" else "En Route"
        }
        FlightStatus.TAXIING_IN -> "Taxiing to Gate"
        FlightStatus.TAXIING_OUT -> "Taxiing Out"
        FlightStatus.DEPARTED -> "Departed"
        else -> status.displayName
    }
}

private fun statusTextColor(flight: Flight, status: FlightStatus): Color {
    return when (status) {
        FlightStatus.ARRIVED, FlightStatus.LANDED, FlightStatus.COMPLETED,
        FlightStatus.BOARDING -> TrueSkiesColors.StatusOnTime
        FlightStatus.CANCELLED -> TrueSkiesColors.StatusCancelled
        FlightStatus.DIVERTED -> TrueSkiesColors.StatusDiverted
        FlightStatus.EN_ROUTE, FlightStatus.APPROACH,
        FlightStatus.DEPARTED, FlightStatus.TAXIING_OUT,
        FlightStatus.TAXIING_IN -> TrueSkiesColors.AccentBlue
        FlightStatus.SCHEDULED -> if ((flight.departureDelay ?: 0) > 0)
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
// FlightRow — search result row (compact)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
        ) {
            // Left: flight number + airline
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flight.displayFlightNumber,
                    style = TrueSkiesTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.TextPrimary
                )
                flight.airlineName?.let {
                    Text(
                        text = it,
                        style = TrueSkiesTypography.bodySmall,
                        color = TrueSkiesColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Centre: origin → dest + time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flight.originCode,
                        style = AviationTypography.airportCodeSmall.copy(fontSize = 15.sp),
                        color = TrueSkiesColors.TextPrimary)
                    Text(" → ", color = TrueSkiesColors.AccentBlue, fontSize = 13.sp)
                    Text(flight.destinationCode,
                        style = AviationTypography.airportCodeSmall.copy(fontSize = 15.sp),
                        color = TrueSkiesColors.TextPrimary)
                }
                Text(
                    text = formatTime(flight.bestDepartureTime),
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextSecondary
                )
            }

            // Right: status badge
            FlightStatusBadge(status = flight.status)
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
        FlightStatus.SCHEDULED -> TrueSkiesColors.StatusScheduled
        FlightStatus.BOARDING -> TrueSkiesColors.StatusBoarding
        FlightStatus.DEPARTED, FlightStatus.TAXIING_OUT -> TrueSkiesColors.StatusActive
        FlightStatus.EN_ROUTE, FlightStatus.APPROACH -> TrueSkiesColors.StatusOnTime
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

private fun formatTime(isoString: String?): String {
    if (isoString == null) return "--:--"
    return try {
        ZonedDateTime.parse(isoString).format(DateTimeFormatter.ofPattern("HH:mm"))
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

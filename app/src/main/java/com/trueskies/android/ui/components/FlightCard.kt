package com.trueskies.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * Personal flight card — ported from iOS PersonalFlightCard.swift.
 * Shows flight number, route, status, and timing in the LiquidGlass card style.
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md)
        ) {
            // Top row: flight number + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = personalFlight.displayName,
                    style = AviationTypography.flightNumber,
                    color = TrueSkiesColors.TextPrimary
                )
                FlightStatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

            // Route: ORIGIN → DESTINATION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Origin
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = flight.originCode,
                        style = AviationTypography.airportCodeSmall,
                        color = TrueSkiesColors.TextPrimary
                    )
                    flight.originCity?.let {
                        Text(
                            text = it,
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Arrow / Progress
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = TrueSkiesSpacing.sm)
                ) {
                    Text(
                        text = if (status.isAirborne) "✈" else "→",
                        fontSize = 20.sp,
                        color = TrueSkiesColors.AccentBlue
                    )
                    flight.progressPercent?.let { pct ->
                        if (pct > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
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
                            }
                        }
                    }
                }

                // Destination
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = flight.destinationCode,
                        style = AviationTypography.airportCodeSmall,
                        color = TrueSkiesColors.TextPrimary
                    )
                    flight.destinationCity?.let {
                        Text(
                            text = it,
                            style = TrueSkiesTypography.bodySmall,
                            color = TrueSkiesColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))

            // Bottom row: times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Departure time
                Column {
                    Text("Departs", style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextMuted)
                    Text(
                        text = formatTime(flight.bestDepartureTime),
                        style = AviationTypography.timeDisplay,
                        color = TrueSkiesColors.TextPrimary
                    )
                }
                // Arrival time
                Column(horizontalAlignment = Alignment.End) {
                    Text("Arrives", style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextMuted)
                    Text(
                        text = formatTime(flight.bestArrivalTime),
                        style = AviationTypography.timeDisplay,
                        color = TrueSkiesColors.TextPrimary
                    )
                }
            }

            // Airline name
            flight.airlineName?.let { airline ->
                Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))
                Text(
                    text = airline,
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextTertiary
                )
            }
        }
    }
}

/**
 * Simpler flight row for search results.
 */
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
                .padding(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flight number
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flight.displayFlightNumber,
                    style = TrueSkiesTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TrueSkiesColors.TextPrimary
                )
                flight.airlineName?.let {
                    Text(
                        text = it,
                        style = TrueSkiesTypography.bodySmall,
                        color = TrueSkiesColors.TextTertiary
                    )
                }
            }

            // Route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = flight.originCode,
                        style = AviationTypography.airportCodeSmall.copy(fontSize = 16.sp),
                        color = TrueSkiesColors.TextPrimary
                    )
                    Text(
                        text = " → ",
                        color = TrueSkiesColors.AccentBlue,
                        fontSize = 14.sp
                    )
                    Text(
                        text = flight.destinationCode,
                        style = AviationTypography.airportCodeSmall.copy(fontSize = 16.sp),
                        color = TrueSkiesColors.TextPrimary
                    )
                }
                Text(
                    text = formatTime(flight.bestDepartureTime),
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextSecondary
                )
            }

            // Status
            FlightStatusBadge(
                status = flight.status,
                modifier = Modifier
            )
        }
    }
}

/**
 * Status badge with color-coded background.
 */
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

// ── Helpers ──

private fun formatTime(isoString: String?): String {
    if (isoString == null) return "--:--"
    return try {
        val zdt = ZonedDateTime.parse(isoString)
        zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        // Try without timezone
        try {
            val ldt = java.time.LocalDateTime.parse(isoString.substringBefore("Z").substringBefore("+").substringBefore("-"))
            ldt.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e2: Exception) {
            isoString.takeLast(5)
        }
    }
}

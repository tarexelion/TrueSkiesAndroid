package com.trueskies.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*

/**
 * Flight Log tab — mirrors iOS FlightLogScreen / FlightLogView.
 * Shows the user's flight history with statistics and a globe visualization placeholder.
 */
@Composable
fun FlightLogScreen(onSettingsClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header row with title + settings button — mirrors iOS FlightLogScreenContent header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TrueSkiesSpacing.lg,
                    end = TrueSkiesSpacing.md,
                    top = TrueSkiesSpacing.md,
                    bottom = TrueSkiesSpacing.sm
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Flight Log",
                style = TrueSkiesTypography.displayMedium,
                color = TrueSkiesColors.TextPrimary
            )

            // Settings button — mirrors iOS gear icon in FlightLogScreenContent
            FilledTonalIconButton(
                onClick = onSettingsClick,
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

        Column(
            modifier = Modifier.padding(horizontal = TrueSkiesSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
        ) {
            // Globe placeholder — mirrors iOS FlightLogGlobeView section
            GlobePlaceholder()

            // Stats summary cards
            StatsSummarySection()

            // Add first flight CTA — mirrors iOS FlightEmptyStateView
            AddFlightCard()

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxl))
        }
    }
}

@Composable
private fun GlobePlaceholder() {
    LiquidGlassCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Globe icon placeholder — will be replaced by actual globe/map visualization
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = TrueSkiesColors.AccentBlue.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))
                Text(
                    text = "Your flight routes will appear here",
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatsSummarySection() {
    // 2×2 stats grid — mirrors iOS AggregateStatisticsView
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
    ) {
        StatCard(
            label = "Flights",
            value = "0",
            icon = Icons.Default.FlightTakeoff,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Countries",
            value = "0",
            icon = Icons.Default.Public,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(TrueSkiesSpacing.sm))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
    ) {
        StatCard(
            label = "Distance",
            value = "0 km",
            icon = Icons.Default.Timeline,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Hours",
            value = "0 h",
            icon = Icons.Default.Schedule,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(TrueSkiesSpacing.md),
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
                color = TrueSkiesColors.TextPrimary
            )
        }
    }
}

@Composable
private fun AddFlightCard() {
    LiquidGlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
        ) {
            // Plus icon in accent circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = TrueSkiesColors.AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Log a Flight",
                    style = TrueSkiesTypography.titleLarge,
                    color = TrueSkiesColors.TextPrimary
                )
                Text(
                    text = "Add past or upcoming flights to build your log",
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextTertiary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TrueSkiesColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

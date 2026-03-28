package com.trueskies.android.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FriendsViewModel

/**
 * Friends tab — mirrors iOS FriendsFlightsView.
 * Shows shared flights from friends with invite/scan/paste actions.
 * Three states: empty, loading, populated.
 */
@Composable
fun FriendsScreen(
    onFlightClick: (String) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // QR scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val code = result.contents
        if (code != null) {
            val shareCode = extractShareCode(code)
            if (shareCode != null) {
                viewModel.joinSharedFlight(shareCode)
            } else {
                Toast.makeText(context, "Invalid TrueSkies QR code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .statusBarsPadding()
    ) {
        // Large navigation title — mirrors iOS .navigationBarTitleDisplayMode(.large)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TrueSkiesSpacing.lg,
                    end = TrueSkiesSpacing.lg,
                    top = TrueSkiesSpacing.md,
                    bottom = TrueSkiesSpacing.sm
                )
        ) {
            Text(
                text = "Friends",
                style = TrueSkiesTypography.displayMedium,
                color = TrueSkiesColors.TextPrimary
            )
        }

        when {
            state.isLoading -> FriendsLoadingState()
            state.sharedFlights.isNotEmpty() -> FriendsPopulatedState(
                sharedFlights = state.sharedFlights,
                onFlightClick = onFlightClick,
                onShareTap = { shareMyFlight(context) },
                onScanQR = { launchQRScanner(scanLauncher) },
                onPasteLink = { pasteShareLink(context, viewModel) },
                onRemoveFlight = { viewModel.removeSharedFlight(it) }
            )
            else -> FriendsEmptyState(
                onShareTap = { shareMyFlight(context) },
                onScanQR = { launchQRScanner(scanLauncher) },
                onPasteLink = { pasteShareLink(context, viewModel) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Empty State — mirrors iOS FriendsEmptyStateView
// ─────────────────────────────────────────────────────────────

@Composable
private fun FriendsEmptyState(
    onShareTap: () -> Unit,
    onScanQR: () -> Unit,
    onPasteLink: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TrueSkiesSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = TrueSkiesColors.AccentBlue.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))

        Text(
            text = "Share & Track Together",
            style = TrueSkiesTypography.headlineSmall,
            color = TrueSkiesColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))

        Text(
            text = "Share your flight with friends or join theirs. You'll see each other on the map in real time.",
            style = TrueSkiesTypography.bodyMedium,
            color = TrueSkiesColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xl))

        // Primary action — Share your flights
        Button(
            onClick = onShareTap,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrueSkiesColors.AccentBlue,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(vertical = TrueSkiesSpacing.md)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(TrueSkiesSpacing.xs))
            Text("Share your flights", style = TrueSkiesTypography.labelLarge, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.md))

        // Secondary actions row — Scan QR + Paste Link
        LiquidGlassCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TrueSkiesSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
            ) {
                // Scan QR
                OutlinedButton(
                    onClick = onScanQR,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TrueSkiesColors.TextPrimary
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text("Scan QR", style = TrueSkiesTypography.labelMedium)
                    }
                }

                // Paste link
                OutlinedButton(
                    onClick = onPasteLink,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TrueSkiesColors.TextPrimary
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text("Paste Link", style = TrueSkiesTypography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxl))

        // Feature preview section — mirrors iOS FriendsFeatureRow
        Column(verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)) {
            FriendsFeatureRow(
                icon = Icons.Default.Visibility,
                title = "Real-time tracking",
                description = "See friends' flights live on the map"
            )
            FriendsFeatureRow(
                icon = Icons.Default.Notifications,
                title = "Flight updates",
                description = "Get notified about delays and gate changes"
            )
            FriendsFeatureRow(
                icon = Icons.Default.QrCode,
                title = "Easy sharing",
                description = "Share via QR code or link"
            )
        }
    }
}

@Composable
private fun FriendsFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(TrueSkiesCornerRadius.sm))
                .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TrueSkiesColors.AccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(title, style = TrueSkiesTypography.titleSmall, color = TrueSkiesColors.TextPrimary)
            Text(description, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Loading State — mirrors iOS FriendsLoadingStateView
// ─────────────────────────────────────────────────────────────

@Composable
private fun FriendsLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TrueSkiesSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
    ) {
        // Shimmer skeleton cards
        repeat(3) { index ->
            LiquidGlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TrueSkiesSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
                ) {
                    // Avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TrueSkiesColors.SurfaceElevated)
                    )
                    // Text placeholders
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(TrueSkiesCornerRadius.xxs))
                                .background(TrueSkiesColors.SurfaceElevated)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(TrueSkiesCornerRadius.xxs))
                                .background(TrueSkiesColors.SurfaceElevated.copy(alpha = 0.6f))
                        )
                    }
                    // Status placeholder
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(TrueSkiesCornerRadius.xxs))
                            .background(TrueSkiesColors.SurfaceElevated.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Populated State — mirrors iOS FriendsPopulatedStateView
// ─────────────────────────────────────────────────────────────

@Composable
private fun FriendsPopulatedState(
    sharedFlights: List<SharedFlightUiItem>,
    onFlightClick: (String) -> Unit,
    onShareTap: () -> Unit,
    onScanQR: () -> Unit,
    onPasteLink: () -> Unit,
    onRemoveFlight: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
    ) {
        // Action buttons row at top
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
            ) {
                ActionChip(
                    label = "Share",
                    icon = Icons.Default.Share,
                    onClick = onShareTap,
                    modifier = Modifier.weight(1f)
                )
                ActionChip(
                    label = "Scan",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = onScanQR,
                    modifier = Modifier.weight(1f)
                )
                ActionChip(
                    label = "Paste",
                    icon = Icons.Default.ContentPaste,
                    onClick = onPasteLink,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Shared flight cards
        items(sharedFlights, key = { it.id }) { flight ->
            SharedFlightCard(
                flight = flight,
                onClick = { onFlightClick(flight.flightId) }
            )
        }

        // Empty sub-state when friends exist but no active flights
        if (sharedFlights.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TrueSkiesSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = TrueSkiesColors.TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(TrueSkiesSpacing.sm))
                    Text(
                        "No active flights",
                        style = TrueSkiesTypography.titleMedium,
                        color = TrueSkiesColors.TextSecondary
                    )
                    Text(
                        "Friends' flights will appear here when they share",
                        style = TrueSkiesTypography.bodySmall,
                        color = TrueSkiesColors.TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(TrueSkiesCornerRadius.pill),
        color = TrueSkiesColors.GlassBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrueSkiesSpacing.sm, vertical = TrueSkiesSpacing.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = TrueSkiesColors.AccentBlue)
            Spacer(Modifier.width(TrueSkiesSpacing.xxs))
            Text(label, style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextPrimary)
        }
    }
}

@Composable
private fun SharedFlightCard(
    flight: SharedFlightUiItem,
    onClick: () -> Unit
) {
    LiquidGlassCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.md)
        ) {
            // Friend avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TrueSkiesColors.AccentCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = flight.friendName.firstOrNull()?.uppercase() ?: "?",
                    style = TrueSkiesTypography.titleMedium,
                    color = TrueSkiesColors.AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            // Flight info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flight.friendName,
                    style = TrueSkiesTypography.titleSmall,
                    color = TrueSkiesColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${flight.flightNumber}  ${flight.origin} → ${flight.destination}",
                    style = TrueSkiesTypography.bodyMedium,
                    color = TrueSkiesColors.TextSecondary
                )
            }

            // Status badge
            Surface(
                shape = RoundedCornerShape(TrueSkiesCornerRadius.badge),
                color = flight.statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = flight.statusText,
                    style = TrueSkiesTypography.labelSmall,
                    color = flight.statusColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = TrueSkiesSpacing.xs, vertical = TrueSkiesSpacing.xxs)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helper functions
// ─────────────────────────────────────────────────────────────

private fun shareMyFlight(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Track my flight on TrueSkies")
        putExtra(
            Intent.EXTRA_TEXT,
            "Track my flight in real time on TrueSkies!\nhttps://trueskiesapp.com/share"
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}

private fun launchQRScanner(scanLauncher: androidx.activity.result.ActivityResultLauncher<ScanOptions>) {
    scanLauncher.launch(
        ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan a TrueSkies flight QR code")
            setBeepEnabled(false)
            setOrientationLocked(false)
        }
    )
}

private fun pasteShareLink(context: Context, viewModel: FriendsViewModel) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
    val shareCode = extractShareCode(text)
    if (shareCode != null) {
        viewModel.joinSharedFlight(shareCode)
    } else {
        Toast.makeText(context, "No valid flight link in clipboard", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extracts a share code from a TrueSkies share URL or raw code.
 * Handles: "https://trueskiesapp.com/share/ABC123", "/share/ABC123", or bare "ABC123".
 */
private fun extractShareCode(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    val urlMatch = Regex("""trueskiesapp\.com/share/([A-Za-z0-9]+)|/share/([A-Za-z0-9]+)""").find(trimmed)
    if (urlMatch != null) return urlMatch.groupValues[1].ifEmpty { urlMatch.groupValues[2] }
    // Try bare flight code
    if (Regex("""^[A-Za-z0-9]{4,12}$""").matches(trimmed)) return trimmed
    return null
}

// ─────────────────────────────────────────────────────────────
// UI Data models
// ─────────────────────────────────────────────────────────────

data class SharedFlightUiItem(
    val id: String,
    val flightId: String,
    val friendName: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val statusText: String,
    val statusColor: Color
)

data class FriendsUiState(
    val isLoading: Boolean = false,
    val sharedFlights: List<SharedFlightUiItem> = emptyList(),
    val error: String? = null
)

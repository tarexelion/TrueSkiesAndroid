package com.trueskies.android.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.trueskies.android.ui.theme.*

/**
 * Friends tab — mirrors iOS FriendsFlightsView.
 * Shows shared flights from friends with invite/scan/paste actions.
 */
@Composable
fun FriendsScreen(onFlightClick: (String) -> Unit = {}) {
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

        // Empty state — mirrors iOS FriendsEmptyStateView
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TrueSkiesSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
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

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxl))

            // Action buttons row — mirrors iOS FriendsActionButtons
            val context = LocalContext.current

            // QR scanner launcher
            val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
                val code = result.contents
                if (code != null) {
                    val flightId = extractFlightId(code)
                    if (flightId != null) {
                        onFlightClick(flightId)
                    } else {
                        Toast.makeText(context, "Invalid TrueSkies QR code", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
            ) {
                // Scan QR
                OutlinedButton(
                    onClick = {
                        scanLauncher.launch(
                            ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt("Scan a TrueSkies flight QR code")
                                setBeepEnabled(false)
                                setOrientationLocked(false)
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TrueSkiesColors.TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text("Scan", style = TrueSkiesTypography.labelMedium)
                    }
                }

                // Paste link
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        val flightId = extractFlightId(text)
                        if (flightId != null) {
                            onFlightClick(flightId)
                        } else {
                            Toast.makeText(context, "No valid flight link in clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TrueSkiesColors.TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text("Paste", style = TrueSkiesTypography.labelMedium)
                    }
                }

                // Share my flight (filled/accent)
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Track my flight on TrueSkies")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Track my flight in real time on TrueSkies!\nhttps://trueskiesapp.com/share"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(TrueSkiesCornerRadius.xl),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrueSkiesColors.AccentBlue,
                        contentColor = Color.White
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text("Share", style = TrueSkiesTypography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * Extracts a flight ID from a TrueSkies share URL or raw flight code.
 * Handles: "https://trueskies.app/flight/TK123", "flight/TK123", or bare "TK123".
 */
private fun extractFlightId(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    // Try URL patterns: /flight/{id}
    val urlMatch = Regex("""trueskiesapp\.com/flight/([A-Za-z0-9]+)|/flight/([A-Za-z0-9]+)""").find(trimmed)
    if (urlMatch != null) return urlMatch.groupValues[1].ifEmpty { urlMatch.groupValues[2] }
    // Try bare flight code (e.g. TK1234, AA100)
    if (Regex("""^[A-Z]{2,3}\d{1,4}$""").matches(trimmed.uppercase())) return trimmed.uppercase()
    return null
}

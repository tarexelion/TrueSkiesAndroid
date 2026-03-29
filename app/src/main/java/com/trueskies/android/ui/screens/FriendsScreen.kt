package com.trueskies.android.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.trueskies.android.domain.models.PersonalFlight
import com.trueskies.android.ui.components.FlightCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.FriendsViewModel
import com.trueskies.android.ui.viewmodels.JoinFlightResult
import com.trueskies.android.ui.viewmodels.ShareFlightResult
import com.trueskies.android.ui.viewmodels.SharedPersonalFlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onFlightClick: (String) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showFlightSelector by remember { mutableStateOf(false) }

    val personalFlights by viewModel.personalFlights.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    val joinResult by viewModel.joinResult.collectAsState()
    val sharedFlights by viewModel.sharedFlightsAsPersonal.collectAsState()

    // Handle share result — open share sheet when backend returns a share code
    LaunchedEffect(shareResult) {
        when (val result = shareResult) {
            is ShareFlightResult.Success -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Track ${result.flightIdent} on TrueSkies")
                    putExtra(Intent.EXTRA_TEXT, result.shareText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                viewModel.resetShareResult()
            }
            is ShareFlightResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                viewModel.resetShareResult()
            }
            else -> {}
        }
    }

    // Handle join result
    LaunchedEffect(joinResult) {
        when (val result = joinResult) {
            is JoinFlightResult.Success -> {
                Toast.makeText(context, "Flight added!", Toast.LENGTH_SHORT).show()
                viewModel.resetJoinResult()
            }
            is JoinFlightResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                viewModel.resetJoinResult()
            }
            else -> {}
        }
    }

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

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(38.5, 30.0), 5f)
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            FriendsSheetContent(
                sharedFlights = sharedFlights,
                onScanQR = {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Scan a TrueSkies flight QR code")
                            setBeepEnabled(false)
                            setOrientationLocked(false)
                        }
                    )
                },
                onPasteLink = { pasteShareLink(context, viewModel) },
                onShare = {
                    if (personalFlights.isEmpty()) {
                        Toast.makeText(context, "Add a flight first to share it", Toast.LENGTH_SHORT).show()
                    } else {
                        showFlightSelector = true
                    }
                },
                isSharing = shareResult is ShareFlightResult.Loading,
                onFlightClick = onFlightClick,
                onDeleteFlight = { viewModel.removeSharedFlight(it.entity.shareCode) }
            )
        },
        sheetPeekHeight = 200.dp,
        sheetShape = RoundedCornerShape(
            topStart = TrueSkiesCornerRadius.xl,
            topEnd = TrueSkiesCornerRadius.xl
        ),
        sheetContainerColor = Color(0xFF1C1C1E).copy(alpha = 0.92f),
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TrueSkiesColors.TextMuted.copy(alpha = 0.4f)
            )
        },
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 20.dp,
        containerColor = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.HYBRID,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false
                )
            )
        }
    }

    // Flight selector dialog
    if (showFlightSelector) {
        FlightSelectorDialog(
            flights = personalFlights,
            onFlightSelected = { personalFlight ->
                showFlightSelector = false
                viewModel.createShareForFlight(personalFlight)
            },
            onDismiss = { showFlightSelector = false }
        )
    }
}

@Composable
private fun FlightSelectorDialog(
    flights: List<PersonalFlight>,
    onFlightSelected: (PersonalFlight) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrueSkiesColors.SurfacePrimary,
        titleContentColor = TrueSkiesColors.TextPrimary,
        title = {
            Text(
                text = "Select a flight to share",
                style = TrueSkiesTypography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xs)
            ) {
                items(flights, key = { it.localId }) { pf ->
                    FlightSelectorRow(
                        personalFlight = pf,
                        onClick = { onFlightSelected(pf) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TrueSkiesColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun FlightSelectorRow(
    personalFlight: PersonalFlight,
    onClick: () -> Unit
) {
    val flight = personalFlight.flight
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(TrueSkiesCornerRadius.md),
        colors = CardDefaults.cardColors(containerColor = TrueSkiesColors.SurfaceElevated)
    ) {
        Row(
            modifier = Modifier.padding(TrueSkiesSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = null,
                tint = TrueSkiesColors.AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flight.displayFlightNumber,
                    style = TrueSkiesTypography.bodyMedium,
                    color = TrueSkiesColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${flight.originCode} → ${flight.destinationCode}",
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TrueSkiesColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsSheetContent(
    sharedFlights: List<SharedPersonalFlight>,
    onScanQR: () -> Unit,
    onPasteLink: () -> Unit,
    onShare: () -> Unit,
    isSharing: Boolean = false,
    onFlightClick: (String) -> Unit = {},
    onDeleteFlight: (SharedPersonalFlight) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrueSkiesSpacing.lg, vertical = TrueSkiesSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = TrueSkiesColors.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(TrueSkiesSpacing.xs))
            Text(
                text = if (sharedFlights.isNotEmpty())
                    "${sharedFlights.size} flight${if (sharedFlights.size > 1) "s" else ""} shared"
                else "Share & track together",
                style = TrueSkiesTypography.titleMedium,
                color = TrueSkiesColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(TrueSkiesSpacing.lg))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
        ) {
            // Scan button
            Button(
                onClick = onScanQR,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrueSkiesColors.SurfaceElevated,
                    contentColor = TrueSkiesColors.TextPrimary
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(TrueSkiesSpacing.xxs))
                Text("Scan", style = TrueSkiesTypography.labelLarge)
            }

            // Paste button
            Button(
                onClick = onPasteLink,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrueSkiesColors.SurfaceElevated,
                    contentColor = TrueSkiesColors.TextPrimary
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(TrueSkiesSpacing.xxs))
                Text("Paste", style = TrueSkiesTypography.labelLarge)
            }

            // Share button (primary/blue)
            Button(
                onClick = onShare,
                enabled = !isSharing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(TrueSkiesCornerRadius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrueSkiesColors.AccentBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (isSharing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(TrueSkiesSpacing.xxs))
                Text("Share", style = TrueSkiesTypography.labelLarge)
            }
        }

        // Shared flights list — using the same FlightCard as My Flights
        if (sharedFlights.isNotEmpty()) {
            Spacer(Modifier.height(TrueSkiesSpacing.lg))

            sharedFlights.forEach { item ->
                key(item.entity.id) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onDeleteFlight(item)
                            }
                            // Always return false so the card snaps back;
                            // the actual removal happens via the Flow update
                            false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> TrueSkiesColors.StatusCancelled
                                    else -> Color.Transparent
                                },
                                label = "swipeBg"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.lg))
                                    .background(color)
                                    .padding(horizontal = TrueSkiesSpacing.xl),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.White
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    ) {
                        FlightCard(
                            personalFlight = item.personalFlight,
                            onClick = { onFlightClick(item.entity.id) }
                        )
                    }

                    Spacer(Modifier.height(TrueSkiesSpacing.xs))
                }
            }
        }

        Spacer(Modifier.height(TrueSkiesSpacing.md))
    }
}

// ─────────────────────────────────────────────────────────────
// Helper functions
// ─────────────────────────────────────────────────────────────

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

private fun extractShareCode(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    // Query param format: ?code=ABC123 or &code=ABC123
    val queryMatch = Regex("""[?&]code=([A-Za-z0-9]+)""").find(trimmed)
    if (queryMatch != null) return queryMatch.groupValues[1]
    // Legacy path format: /share/ABC123
    val pathMatch = Regex("""trueskiesapp\.com/share/([A-Za-z0-9]+)""").find(trimmed)
    if (pathMatch != null) return pathMatch.groupValues[1]
    // Raw share code
    if (Regex("""^[A-Za-z0-9]{4,12}$""").matches(trimmed)) return trimmed
    return null
}

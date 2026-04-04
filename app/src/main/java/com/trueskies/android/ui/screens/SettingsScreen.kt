package com.trueskies.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.data.auth.AuthState
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.SettingsViewModel

private val FirstClassGold = Color(0xFFD4A843)
private val FirstClassGoldBg = Color(0xFF3D3520)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToAccountSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }
    val versionCode = try {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    } catch (e: Exception) { 1L }

    val signInEvent by viewModel.signInEvent.collectAsStateWithLifecycle()
    val isSignedIn = state.authState is AuthState.Authenticated
    val authUser = (state.authState as? AuthState.Authenticated)?.user

    val displayName = when {
        state.displayName.isNotBlank() -> state.displayName
        authUser?.displayName != null -> authUser.displayName
        else -> "Traveler"
    }

    LaunchedEffect(signInEvent) {
        when (val event = signInEvent) {
            is SettingsViewModel.SignInEvent.Success -> {
                Toast.makeText(context, "Signed in as ${event.user.email}", Toast.LENGTH_SHORT).show()
                viewModel.clearSignInEvent()
            }
            is SettingsViewModel.SignInEvent.Error -> {
                Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                viewModel.clearSignInEvent()
            }
            else -> {}
        }
    }

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showUnitsPicker by remember { mutableStateOf(false) }
    var showTimeFormatPicker by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text("Settings", style = TrueSkiesTypography.headlineMedium, color = TrueSkiesColors.TextPrimary)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TrueSkiesColors.AccentBlue)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TrueSkiesColors.SurfacePrimary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TrueSkiesSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
        ) {
            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── ACCOUNT ──
            SettingsSectionHeader("Account")
            LiquidGlassCard {
                Column {
                    // Profile row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(TrueSkiesColors.AccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                displayName.first().uppercase(),
                                style = TrueSkiesTypography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(displayName, style = TrueSkiesTypography.titleLarge, color = TrueSkiesColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(TrueSkiesSpacing.xs))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(TrueSkiesCornerRadius.xs))
                                        .background(TrueSkiesColors.TextMuted.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ECONOMY", style = TrueSkiesTypography.labelSmall, color = TrueSkiesColors.TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                        IconButton(onClick = onNavigateToAccountSettings) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = TrueSkiesColors.TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }

                    SettingsDivider()

                    // Free Plan card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUpgradeDialog = true }
                            .padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(TrueSkiesColors.TextMuted.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TrueSkiesColors.TextMuted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Free Plan", style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Limited to 3 flights per day", style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
                        }
                        Text("Upgrade", style = TrueSkiesTypography.titleSmall, color = TrueSkiesColors.AccentBlue, fontWeight = FontWeight.SemiBold)
                    }

                    SettingsDivider()

                    // Sign in with Google / Signed in state
                    if (isSignedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showSignOutConfirm = true }.padding(TrueSkiesSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(TrueSkiesColors.StatusOnTime.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = TrueSkiesColors.StatusOnTime, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(TrueSkiesSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Signed in with Google", style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
                                Text(authUser?.email ?: "", style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
                            }
                            Text("Sign Out", style = TrueSkiesTypography.labelMedium, color = TrueSkiesColors.Error)
                        }
                    } else {
                        val isLoading = signInEvent is SettingsViewModel.SignInEvent.Loading
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !isLoading) {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) viewModel.signInWithGoogle(activity)
                                    else Toast.makeText(context, "Unable to start sign-in", Toast.LENGTH_SHORT).show()
                                }
                                .padding(TrueSkiesSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(TrueSkiesColors.AccentBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TrueSkiesColors.AccentBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(TrueSkiesSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sign in with Google", style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
                                Text("Back up your flights and preferences", style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
                            }
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TrueSkiesColors.AccentBlue, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TrueSkiesColors.TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── COMFORT & WELLNESS ──
            SettingsSectionHeader("Comfort & Wellness")
            LiquidGlassCard {
                Column {
                    FirstClassRow(icon = Icons.Default.HeartBroken, iconTint = Color(0xFF8B5CF6), title = "Nervous Flyer Mode", subtitle = "Calming features for anxious travelers", onClick = { showUpgradeDialog = true })
                    SettingsDivider()
                    FirstClassRow(icon = Icons.Default.Pets, iconTint = TrueSkiesColors.AccentCyan, title = "Travelling with Pets", subtitle = "Travel guidance for your pets", onClick = { showUpgradeDialog = true })
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── PREFERENCES ──
            SettingsSectionHeader("Preferences")
            LiquidGlassCard {
                Column {
                    val unitsLabel = when (state.unitsPreference) { "imperial" -> "Imperial"; "metric" -> "Metric"; else -> "Automatic" }
                    DropdownRow(
                        icon = Icons.Default.Straighten, iconTint = TrueSkiesColors.AccentBlue, title = "Units",
                        subtitle = when (state.unitsPreference) { "imperial" -> "Miles, feet, Fahrenheit"; "metric" -> "Kilometres, metres, Celsius"; else -> "Based on device region" },
                        selectedValue = unitsLabel, onClick = { showUnitsPicker = true }
                    )
                    SettingsDivider()
                    val timeLabel = when (state.timeFormatPreference) { "12h" -> "12-Hour"; "24h" -> "24-Hour"; else -> "Automatic" }
                    DropdownRow(
                        icon = Icons.Default.Schedule, iconTint = TrueSkiesColors.AccentBlue, title = "Time Format",
                        subtitle = when (state.timeFormatPreference) { "12h" -> "e.g. 2:30 PM"; "24h" -> "e.g. 14:30"; else -> "Based on device setting" },
                        selectedValue = timeLabel, onClick = { showTimeFormatPicker = true }
                    )
                    SettingsDivider()
                    DropdownRow(
                        icon = Icons.Default.Language, iconTint = TrueSkiesColors.AccentBlue, title = "Language",
                        subtitle = "Based on device setting", selectedValue = "Automatic",
                        onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon = Icons.Default.AutoAwesome, iconTint = TrueSkiesColors.AccentBlue,
                        title = "Reduce Visual Effects", subtitle = "Simplifies blur and shadow effects for smoother scrolling",
                        checked = state.reduceVisualEffects, onCheckedChange = { viewModel.setReduceVisualEffects(it) }
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── NOTIFICATIONS ──
            SettingsSectionHeader("Notifications")
            LiquidGlassCard {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications, iconTint = TrueSkiesColors.AccentBlue,
                        title = "Push Notifications", subtitle = if (state.notificationsEnabled) "Enabled" else "Disabled",
                        checked = state.notificationsEnabled, onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    SettingsDivider()
                    SettingsLinkRow(
                        icon = Icons.Default.Tune, iconTint = TrueSkiesColors.AccentBlue,
                        title = "All Notification Settings", subtitle = "Configure which alerts you receive",
                        onClick = onNavigateToNotificationSettings
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── DATA & PRIVACY ──
            SettingsSectionHeader("Data & Privacy")
            LiquidGlassCard {
                Column {
                    SettingsLinkRow(
                        icon = Icons.Default.Person, iconTint = TrueSkiesColors.AccentBlue,
                        title = "Account Settings", subtitle = "Name, country, storage, export data",
                        onClick = onNavigateToAccountSettings
                    )
                    SettingsDivider()
                    SettingsLinkRow(
                        icon = Icons.Default.BarChart, iconTint = TrueSkiesColors.AccentBlue,
                        title = "Data Usage",
                        onClick = {
                            context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            })
                        }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon = Icons.Default.BugReport, iconTint = TrueSkiesColors.TextSecondary,
                        title = "Share Diagnostics", subtitle = "Help improve True Skies by sharing anonymous usage data",
                        checked = state.shareDiagnostics, onCheckedChange = { viewModel.setShareDiagnostics(it) }
                    )
                    SettingsDivider()
                    ExternalLinkRow(icon = Icons.Default.PrivacyTip, iconTint = TrueSkiesColors.AccentBlue, title = "Privacy Policy",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskiesapp.com/privacy"))) })
                    SettingsDivider()
                    // Delete All Data
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToAccountSettings() }.padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(TrueSkiesColors.Error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = TrueSkiesColors.Error, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Text("Delete All Data", style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.Error, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TrueSkiesColors.TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── ABOUT ──
            SettingsSectionHeader("About")
            LiquidGlassCard {
                Column {
                    ExternalLinkRow(icon = Icons.Default.Email, iconTint = TrueSkiesColors.AccentBlue, title = "Email Support",
                        onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:info@trueskiesapp.com")
                            putExtra(Intent.EXTRA_SUBJECT, "TrueSkies Android Support")
                        }) })
                    SettingsDivider()
                    ExternalLinkRow(icon = Icons.Default.BugReport, iconTint = TrueSkiesColors.Error, title = "Report a Bug",
                        onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:info@trueskiesapp.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Bug Report - TrueSkies Android v$versionName")
                            putExtra(Intent.EXTRA_TEXT,
                                "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                                "Android: ${android.os.Build.VERSION.RELEASE}\n" +
                                "App Version: $versionName ($versionCode)\n\n" +
                                "Description of the bug:\n\n")
                        }) })
                    SettingsDivider()
                    ExternalLinkRow(icon = Icons.Default.Warning, iconTint = TrueSkiesColors.Warning, title = "Report Data Issue",
                        onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:info@trueskiesapp.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Data Issue - TrueSkies Android v$versionName")
                        }) })
                    SettingsDivider()
                    ExternalLinkRow(icon = Icons.Default.Description, iconTint = TrueSkiesColors.AccentBlue, title = "Terms of Service",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskiesapp.com/terms"))) })
                    SettingsDivider()
                    // Version
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(TrueSkiesSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(TrueSkiesColors.TextMuted.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TrueSkiesColors.TextMuted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(TrueSkiesSpacing.md))
                        Text("Version", style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text("$versionName ($versionCode)", style = TrueSkiesTypography.bodyMedium, color = TrueSkiesColors.TextTertiary)
                    }
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── DATA SOURCES ──
            SettingsSectionHeader("Data Sources")
            LiquidGlassCard {
                Column {
                    ExternalLinkRow(icon = Icons.Default.Cloud, iconTint = TrueSkiesColors.AccentBlue, title = "Weather",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openweathermap.org"))) })
                    SettingsDivider()
                    ExternalLinkRow(icon = Icons.Default.Flight, iconTint = TrueSkiesColors.AccentBlue, title = "FlightAware",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://flightaware.com"))) })
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xxl))
        }
    }

    // ─── Dialogs ───

    if (showUpgradeDialog) {
        UpgradeToFirstClassDialog(onDismiss = { showUpgradeDialog = false })
    }

    if (showUnitsPicker) {
        OptionPickerDialog(
            title = "Units",
            options = listOf("automatic" to "Automatic", "imperial" to "Imperial", "metric" to "Metric"),
            selectedKey = state.unitsPreference,
            onSelect = { viewModel.setUnitsPreference(it); showUnitsPicker = false },
            onDismiss = { showUnitsPicker = false }
        )
    }

    if (showTimeFormatPicker) {
        OptionPickerDialog(
            title = "Time Format",
            options = listOf("automatic" to "Automatic", "12h" to "12-Hour", "24h" to "24-Hour"),
            selectedKey = state.timeFormatPreference,
            onSelect = { viewModel.setTimeFormatPreference(it); showTimeFormatPicker = false },
            onDismiss = { showTimeFormatPicker = false }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = TrueSkiesColors.SurfaceSecondary,
            title = { Text("Sign Out?", color = TrueSkiesColors.TextPrimary) },
            text = { Text("You will no longer be able to back up your flights and preferences.", color = TrueSkiesColors.TextSecondary) },
            confirmButton = {
                Button(onClick = { viewModel.signOut(); showSignOutConfirm = false; Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = TrueSkiesColors.Error, contentColor = Color.White)) { Text("Sign Out") }
            },
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel", color = TrueSkiesColors.TextMuted) } }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────

@Composable
private fun UpgradeToFirstClassDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(TrueSkiesCornerRadius.xl)).background(TrueSkiesColors.SurfaceSecondary).padding(TrueSkiesSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("\uD83D\uDC51", fontSize = 48.sp)
            Spacer(Modifier.height(TrueSkiesSpacing.md))
            Text("Upgrade to First Class", style = TrueSkiesTypography.headlineSmall, color = FirstClassGold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(TrueSkiesSpacing.sm))
            Text("Unlock premium features for the ultimate travel experience", style = TrueSkiesTypography.bodyMedium, color = TrueSkiesColors.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(TrueSkiesSpacing.xl))
            listOf("Unlimited flight tracking", "Nervous Flyer Mode", "Travelling with Pets guidance", "Priority data refresh", "Ad-free experience").forEach { feature ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = FirstClassGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(TrueSkiesSpacing.sm))
                    Text(feature, style = TrueSkiesTypography.bodyMedium, color = TrueSkiesColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(TrueSkiesSpacing.xl))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = FirstClassGold, contentColor = Color.Black), shape = RoundedCornerShape(TrueSkiesCornerRadius.md)) {
                Text("Coming Soon", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(Modifier.height(TrueSkiesSpacing.xs))
            TextButton(onClick = onDismiss) { Text("Maybe Later", color = TrueSkiesColors.TextMuted) }
        }
    }
}

@Composable
private fun OptionPickerDialog(title: String, options: List<Pair<String, String>>, selectedKey: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(TrueSkiesCornerRadius.xl)).background(TrueSkiesColors.SurfaceSecondary).padding(vertical = TrueSkiesSpacing.lg)
        ) {
            Text(title, style = TrueSkiesTypography.headlineSmall, color = TrueSkiesColors.TextPrimary, modifier = Modifier.padding(start = TrueSkiesSpacing.xl, end = TrueSkiesSpacing.xl, bottom = TrueSkiesSpacing.md))
            options.forEach { (key, label) ->
                val isSelected = key == selectedKey
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(key) }
                        .background(if (isSelected) TrueSkiesColors.AccentBlue.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(horizontal = TrueSkiesSpacing.xl, vertical = TrueSkiesSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = TrueSkiesTypography.bodyLarge, color = if (isSelected) TrueSkiesColors.AccentBlue else TrueSkiesColors.TextPrimary, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = TrueSkiesColors.AccentBlue, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(TrueSkiesSpacing.sm))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Notification Settings Screen
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().background(TrueSkiesColors.SurfacePrimary).statusBarsPadding()) {
        TopAppBar(
            title = { Text("Notification Settings", style = TrueSkiesTypography.headlineMedium, color = TrueSkiesColors.TextPrimary) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TrueSkiesColors.AccentBlue) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TrueSkiesColors.SurfacePrimary)
        )
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = TrueSkiesSpacing.md), verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)) {
            Spacer(Modifier.height(TrueSkiesSpacing.xs))
            SettingsSectionHeader("Flight Alerts")
            LiquidGlassCard {
                Column {
                    SettingsToggleRow(icon = Icons.Default.FlightTakeoff, iconTint = TrueSkiesColors.StatusOnTime, title = "Departure Alerts", subtitle = "30 min and 2 hour reminders before departure", checked = state.departureAlerts, onCheckedChange = { viewModel.setDepartureAlerts(it) })
                    SettingsDivider()
                    SettingsToggleRow(icon = Icons.Default.MeetingRoom, iconTint = TrueSkiesColors.AccentCyan, title = "Gate Change Alerts", subtitle = "Notify when your gate changes", checked = state.gateChangeAlerts, onCheckedChange = { viewModel.setGateChangeAlerts(it) })
                    SettingsDivider()
                    SettingsToggleRow(icon = Icons.Default.Info, iconTint = TrueSkiesColors.Warning, title = "Status Change Alerts", subtitle = "Delays, diversions, and cancellations", checked = state.statusChangeAlerts, onCheckedChange = { viewModel.setStatusChangeAlerts(it) })
                }
            }
            Spacer(Modifier.height(TrueSkiesSpacing.xs))
            SettingsSectionHeader("System")
            LiquidGlassCard {
                Column {
                    val context = LocalContext.current
                    SettingsLinkRow(icon = Icons.Default.PhoneAndroid, iconTint = TrueSkiesColors.AccentBlue, title = "System Notification Settings", subtitle = "Manage channels in Android settings",
                        onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName) }) })
                }
            }
            Spacer(Modifier.height(TrueSkiesSpacing.xxl))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Reusable row components
// ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(text = title.uppercase(), style = TrueSkiesTypography.labelMedium, color = TrueSkiesColors.TextMuted, modifier = Modifier.padding(start = TrueSkiesSpacing.xs, bottom = TrueSkiesSpacing.xxs))
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 60.dp), color = TrueSkiesColors.GlassHighlight, thickness = 0.5.dp)
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, iconTint: Color, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = TrueSkiesSpacing.md, end = TrueSkiesSpacing.md, top = TrueSkiesSpacing.sm, bottom = TrueSkiesSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
            Text(subtitle, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TrueSkiesColors.StatusOnTime, uncheckedThumbColor = TrueSkiesColors.TextMuted, uncheckedTrackColor = TrueSkiesColors.SurfaceElevated))
    }
}

@Composable
private fun SettingsLinkRow(icon: ImageVector, iconTint: Color, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(TrueSkiesSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
            subtitle?.let { Text(it, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary) }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TrueSkiesColors.TextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ExternalLinkRow(icon: ImageVector, iconTint: Color, title: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(TrueSkiesSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TrueSkiesColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DropdownRow(icon: ImageVector, iconTint: Color, title: String, subtitle: String, selectedValue: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(TrueSkiesSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
            Text(subtitle, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
        }
        Text(selectedValue, style = TrueSkiesTypography.bodyMedium, color = TrueSkiesColors.AccentBlue)
    }
}

@Composable
private fun FirstClassRow(icon: ImageVector, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(TrueSkiesSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
        }
        Spacer(Modifier.width(TrueSkiesSpacing.xs))
        Box(modifier = Modifier.clip(RoundedCornerShape(TrueSkiesCornerRadius.sm)).background(FirstClassGoldBg).border(0.5.dp, FirstClassGold.copy(alpha = 0.4f), RoundedCornerShape(TrueSkiesCornerRadius.sm)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDC51", fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text("First Class", style = TrueSkiesTypography.labelSmall, color = FirstClassGold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
    }
}

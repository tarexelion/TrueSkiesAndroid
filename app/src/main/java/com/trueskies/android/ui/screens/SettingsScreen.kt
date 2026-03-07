package com.trueskies.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*
import com.trueskies.android.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TrueSkiesColors.AccentBlue
                    )
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

            // ── Notifications ──
            SettingsSectionHeader("Notifications")
            LiquidGlassCard {
                Column {
                    ToggleRow(
                        icon = Icons.Default.Notifications,
                        iconTint = TrueSkiesColors.AccentBlue,
                        title = "Enable Notifications",
                        subtitle = "Receive flight alerts",
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    if (state.notificationsEnabled) {
                        SettingsDivider()
                        ToggleRow(
                            icon = Icons.Default.FlightTakeoff,
                            iconTint = TrueSkiesColors.StatusOnTime,
                            title = "Departure Alerts",
                            subtitle = "30 min and 2 hour reminders",
                            checked = state.departureAlerts,
                            onCheckedChange = { viewModel.setDepartureAlerts(it) },
                            indent = true
                        )
                        SettingsDivider()
                        ToggleRow(
                            icon = Icons.Default.MeetingRoom,
                            iconTint = TrueSkiesColors.AccentCyan,
                            title = "Gate Change Alerts",
                            subtitle = "Notify when gate changes",
                            checked = state.gateChangeAlerts,
                            onCheckedChange = { viewModel.setGateChangeAlerts(it) },
                            indent = true
                        )
                        SettingsDivider()
                        ToggleRow(
                            icon = Icons.Default.Info,
                            iconTint = TrueSkiesColors.Warning,
                            title = "Status Change Alerts",
                            subtitle = "Delays, diversions, cancellations",
                            checked = state.statusChangeAlerts,
                            onCheckedChange = { viewModel.setStatusChangeAlerts(it) },
                            indent = true
                        )
                    }
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── Display ──
            SettingsSectionHeader("Display")
            LiquidGlassCard {
                Column {
                    ToggleRow(
                        icon = Icons.Default.Speed,
                        iconTint = TrueSkiesColors.AccentBlue,
                        title = "Imperial Units",
                        subtitle = if (state.useImperialUnits) "Miles, feet, knots" else "Kilometres, metres",
                        checked = state.useImperialUnits,
                        onCheckedChange = { viewModel.setUseImperialUnits(it) }
                    )
                    SettingsDivider()
                    ToggleRow(
                        icon = Icons.Default.Schedule,
                        iconTint = TrueSkiesColors.AccentBlue,
                        title = "24-Hour Time",
                        subtitle = if (state.use24HourTime) "e.g. 14:30" else "e.g. 2:30 PM",
                        checked = state.use24HourTime,
                        onCheckedChange = { viewModel.setUse24HourTime(it) }
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── Privacy ──
            SettingsSectionHeader("Privacy")
            LiquidGlassCard {
                Column {
                    LinkRow(
                        icon = Icons.Default.PrivacyTip,
                        iconTint = TrueSkiesColors.AccentBlue,
                        title = "Privacy Policy",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskies.app/privacy"))
                            )
                        }
                    )
                    SettingsDivider()
                    LinkRow(
                        icon = Icons.Default.Gavel,
                        iconTint = TrueSkiesColors.AccentBlue,
                        title = "Terms of Service",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskies.app/terms"))
                            )
                        }
                    )
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
    Text(
        text = title.uppercase(),
        style = TrueSkiesTypography.labelMedium,
        color = TrueSkiesColors.TextMuted,
        modifier = Modifier.padding(start = TrueSkiesSpacing.xs, bottom = TrueSkiesSpacing.xxs)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = TrueSkiesColors.GlassHighlight,
        thickness = 0.5.dp
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (indent) TrueSkiesSpacing.xl else TrueSkiesSpacing.md,
                end = TrueSkiesSpacing.md,
                top = TrueSkiesSpacing.sm,
                bottom = TrueSkiesSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(TrueSkiesCornerRadius.sm))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
            Text(subtitle, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TrueSkiesColors.AccentBlue,
                uncheckedThumbColor = TrueSkiesColors.TextMuted,
                uncheckedTrackColor = TrueSkiesColors.SurfaceElevated
            )
        )
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TrueSkiesSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(TrueSkiesCornerRadius.sm))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Text(
            title,
            style = TrueSkiesTypography.titleMedium,
            color = TrueSkiesColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = TrueSkiesColors.AccentBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

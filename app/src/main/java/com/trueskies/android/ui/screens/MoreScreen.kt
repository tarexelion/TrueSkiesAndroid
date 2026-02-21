package com.trueskies.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*

@Composable
fun MoreScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = TrueSkiesSpacing.huge,
                    start = TrueSkiesSpacing.lg,
                    end = TrueSkiesSpacing.lg,
                    bottom = TrueSkiesSpacing.md
                )
        ) {
            Text(
                text = "More",
                style = TrueSkiesTypography.displayMedium,
                color = TrueSkiesColors.TextPrimary
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = TrueSkiesSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.sm)
        ) {
            // Account Section
            SectionHeader("Account")
            LiquidGlassCard {
                Column {
                    MoreMenuItem(
                        icon = Icons.Default.Person,
                        title = "Profile",
                        subtitle = "Manage your account"
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Configure alerts"
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.Star,
                        title = "Subscription",
                        subtitle = "Premium features"
                    )
                }
            }

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))

            // Features Section
            SectionHeader("Features")
            LiquidGlassCard {
                Column {
                    MoreMenuItem(
                        icon = Icons.Default.BarChart,
                        title = "Flight Stats",
                        subtitle = "Your flying statistics"
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.People,
                        title = "Friends",
                        subtitle = "Track friends' flights"
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.History,
                        title = "Flight Log",
                        subtitle = "Past flights history"
                    )
                }
            }

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xs))

            // App Section
            SectionHeader("App")
            LiquidGlassCard {
                Column {
                    MoreMenuItem(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        subtitle = "App preferences"
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Help & Support",
                        subtitle = "FAQ and contact"
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Version 1.0.0"
                    )
                }
            }

            Spacer(modifier = Modifier.height(TrueSkiesSpacing.xxl))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = TrueSkiesTypography.labelMedium,
        color = TrueSkiesColors.TextMuted,
        modifier = Modifier.padding(
            start = TrueSkiesSpacing.xs,
            bottom = TrueSkiesSpacing.xxs
        )
    )
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
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
                .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TrueSkiesColors.AccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TrueSkiesTypography.titleMedium,
                color = TrueSkiesColors.TextPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = TrueSkiesTypography.bodySmall,
                    color = TrueSkiesColors.TextTertiary
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TrueSkiesColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = TrueSkiesColors.GlassHighlight,
        thickness = 0.5.dp
    )
}

package com.trueskies.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Large navigation title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = TrueSkiesSpacing.md,
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

            // ── Flight Management ──
            SectionHeader("Flight Management")
            LiquidGlassCard {
                Column {
                    MoreMenuItem(
                        icon = Icons.Default.BarChart,
                        iconTint = TrueSkiesColors.AccentBlue,
                        title = "Flight Stats",
                        subtitle = "Your flying statistics",
                        onClick = { onNavigate("flight_log") }
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.People,
                        iconTint = Color(0xFF8B5CF6),
                        title = "Friends",
                        subtitle = "Track friends' flights",
                        onClick = { onNavigate("friends") }
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.History,
                        iconTint = TrueSkiesColors.AccentCyan,
                        title = "Flight Log",
                        subtitle = "Your past flights",
                        onClick = { onNavigate("flight_log") }
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            // ── App ──
            SectionHeader("App")
            LiquidGlassCard {
                Column {
                    MoreMenuItem(
                        icon = Icons.Default.Settings,
                        iconTint = TrueSkiesColors.TextSecondary,
                        title = "Settings",
                        subtitle = "Notifications, units, display",
                        onClick = { onNavigate("settings") }
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = Color(0xFF8B5CF6),
                        title = "Help & Support",
                        subtitle = "FAQ and contact",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskies.app/support"))
                            )
                        },
                        trailingIcon = Icons.Default.OpenInNew
                    )
                    MenuDivider()
                    MoreMenuItem(
                        icon = Icons.Default.Info,
                        iconTint = TrueSkiesColors.AccentBlue,
                        title = "About",
                        subtitle = "Version $versionName",
                        onClick = { onNavigate("about") }
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xxl))
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
    iconTint: Color = TrueSkiesColors.AccentBlue,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    trailingIcon: ImageVector = Icons.Default.ChevronRight
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
            subtitle?.let {
                Text(it, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
            }
        }
        Icon(
            imageVector = trailingIcon,
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

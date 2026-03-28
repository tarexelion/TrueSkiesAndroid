package com.trueskies.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trueskies.android.R
import com.trueskies.android.ui.components.LiquidGlassCard
import com.trueskies.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) { null }
    val versionName = packageInfo?.versionName ?: "1.0.0"
    val versionCode = packageInfo?.longVersionCode ?: 1L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueSkiesColors.SurfacePrimary)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text("About", style = TrueSkiesTypography.headlineMedium, color = TrueSkiesColors.TextPrimary)
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(TrueSkiesSpacing.xl))

            // App icon hero
            Image(
                painter = painterResource(id = R.drawable.trueskies_logo),
                contentDescription = "TrueSkies",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.xl))
            )

            Spacer(Modifier.height(TrueSkiesSpacing.md))

            Text(
                "TrueSkies",
                style = TrueSkiesTypography.headlineLarge,
                color = TrueSkiesColors.TextPrimary
            )
            Text(
                "Version $versionName ($versionCode)",
                style = TrueSkiesTypography.bodyMedium,
                color = TrueSkiesColors.TextSecondary
            )

            Spacer(Modifier.height(TrueSkiesSpacing.xs))

            Text(
                "Real-time flight tracking for every journey.",
                style = TrueSkiesTypography.bodyMedium,
                color = TrueSkiesColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = TrueSkiesSpacing.xl)
            )

            Spacer(Modifier.height(TrueSkiesSpacing.xl))

            // Links card
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AboutLinkRow(
                        icon = Icons.Default.Language,
                        title = "Website",
                        subtitle = "trueskies.app",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskies.app"))
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        color = TrueSkiesColors.GlassHighlight,
                        thickness = 0.5.dp
                    )
                    AboutLinkRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskies.app/privacy"))
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        color = TrueSkiesColors.GlassHighlight,
                        thickness = 0.5.dp
                    )
                    AboutLinkRow(
                        icon = Icons.Default.Gavel,
                        title = "Terms of Service",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://trueskies.app/terms"))
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        color = TrueSkiesColors.GlassHighlight,
                        thickness = 0.5.dp
                    )
                    AboutLinkRow(
                        icon = Icons.Default.Star,
                        title = "Rate on Play Store",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=${context.packageName}")
                                ).also {
                                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(TrueSkiesSpacing.xl))

            // Credits
            Text(
                "Made with ❤️ for travellers worldwide.",
                style = TrueSkiesTypography.bodySmall,
                color = TrueSkiesColors.TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(TrueSkiesSpacing.xs))
            Text(
                "Flight data powered by FlightAware AeroAPI.",
                style = TrueSkiesTypography.labelSmall,
                color = TrueSkiesColors.TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(TrueSkiesSpacing.xxl))
        }
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
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
                .background(TrueSkiesColors.AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TrueSkiesColors.AccentBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(TrueSkiesSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TrueSkiesTypography.titleMedium, color = TrueSkiesColors.TextPrimary)
            subtitle?.let {
                Text(it, style = TrueSkiesTypography.bodySmall, color = TrueSkiesColors.TextTertiary)
            }
        }
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

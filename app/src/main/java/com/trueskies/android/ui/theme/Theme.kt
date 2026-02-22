package com.trueskies.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * TrueSkies Material 3 theme — dark-mode-first design.
 * Edge-to-edge enabled for the map-centric layout.
 */

private val TrueSkiesDarkColorScheme = darkColorScheme(
    primary = TrueSkiesColors.AccentBlue,
    onPrimary = TrueSkiesColors.TextPrimary,
    primaryContainer = TrueSkiesColors.PrimaryNavyLight,
    onPrimaryContainer = TrueSkiesColors.TextPrimary,
    secondary = TrueSkiesColors.AccentIndigo,
    onSecondary = TrueSkiesColors.TextPrimary,
    secondaryContainer = TrueSkiesColors.SurfaceElevated,
    onSecondaryContainer = TrueSkiesColors.TextPrimary,
    tertiary = TrueSkiesColors.AccentCyan,
    onTertiary = TrueSkiesColors.TextInverse,
    background = TrueSkiesColors.SurfacePrimary,
    onBackground = TrueSkiesColors.TextPrimary,
    surface = TrueSkiesColors.SurfacePrimary,
    onSurface = TrueSkiesColors.TextPrimary,
    surfaceVariant = TrueSkiesColors.SurfaceSecondary,
    onSurfaceVariant = TrueSkiesColors.TextSecondary,
    outline = TrueSkiesColors.GlassBorder,
    outlineVariant = TrueSkiesColors.GlassHighlight,
    error = TrueSkiesColors.Error,
    onError = TrueSkiesColors.TextPrimary,
)

@Composable
fun TrueSkiesTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = TrueSkiesDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: transparent bars so the map shows behind them
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = TrueSkiesColors.SurfacePrimary.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TrueSkiesTypography,
        content = content
    )
}

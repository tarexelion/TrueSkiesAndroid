package com.trueskies.android.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, corner radius, elevation, and animation tokens
 * — ported from iOS LiquidGlass.Spacing / CornerRadius / Shadow.
 */
object TrueSkiesSpacing {
    val xxxs = 2.dp
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 48.dp
    val massive = 64.dp
}

object TrueSkiesCornerRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val pill = 100.dp
}

object TrueSkiesElevation {
    val none = 0.dp
    val xs = 1.dp
    val sm = 2.dp
    val md = 4.dp
    val lg = 8.dp
    val xl = 12.dp
    val xxl = 16.dp
}

/** Standard animation durations (ms) matching iOS LiquidGlass.Animation */
object TrueSkiesAnimation {
    const val INSTANT = 100
    const val FAST = 200
    const val NORMAL = 300
    const val SLOW = 500
    const val VERY_SLOW = 800
}

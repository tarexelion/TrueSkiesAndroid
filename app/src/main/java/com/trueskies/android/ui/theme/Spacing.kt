package com.trueskies.android.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, corner radius, elevation, card dimensions, icon sizing, and animation tokens
 * — ported from iOS TrueSkiesDesignSystem.swift.
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

/** Semantic spacing for cards and sections — matches iOS cardInternal, sectionSpacing etc. */
object TrueSkiesCardSpacing {
    val cardInternal = 12.dp
    val cardPadding = 24.dp
    val sectionSpacing = 32.dp
    val elementSpacing = 12.dp
    val itemSpacing = 8.dp
    val cardSpacing = 18.dp          // iOS space between cards
    val statusIndicator = 6.dp
    val timelineSection = 20.dp
}

object TrueSkiesCornerRadius {
    val xxs = 2.dp                   // iOS xs
    val xs = 4.dp                    // iOS sm
    val sm = 8.dp                    // iOS md
    val md = 12.dp                   // iOS lg
    val lg = 16.dp                   // iOS xl
    val xl = 20.dp                   // iOS xxl
    val xxl = 24.dp
    val xxxl = 28.dp                 // iOS large cards & sections
    val heroCard = 34.dp             // iOS hero card
    val card = 18.dp                 // iOS modern card radius
    val panel = 20.dp                // iOS panel radius
    val badge = 6.dp                 // iOS standard badge
    val badgeSmall = 3.dp            // iOS tiny badge
    val chip = 10.dp                 // iOS compact tag
    val button = 14.dp               // iOS button corner radius
    val pillButton = 24.dp           // iOS pill button
    val pill = 100.dp
}

/** Card dimensions — matches iOS card styling */
object TrueSkiesCardDimensions {
    val cornerRadius = 18.dp
    val spacing = 18.dp
    val buttonHeight = 48.dp         // iOS enhanced touch target
}

/** Icon sizing — matches iOS standard/small/large */
object TrueSkiesIconSize {
    val small = 18.dp
    val standard = 22.dp
    val large = 26.dp
}

/** Status and indicator dimensions — matches iOS */
object TrueSkiesIndicator {
    val statusSize = 10.dp
    val progressBarHeight = 5.dp
    val gateDisplaySize = 52.dp
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

/**
 * Shadow levels — matches iOS card shadow system.
 * Use with Modifier.shadow() elevation parameter.
 */
object TrueSkiesShadow {
    // Card shadows (iOS: radius, opacity%, Y offset)
    val cardRadius = 10.dp           // opacity 10%, Y 3
    val cardElevated = 16.dp         // opacity 12%, Y 8
    val subtle = 5.dp                // opacity 6%, Y 2

    // Flight log card shadows
    val heroRadius = 24.dp           // opacity 35%, Y 12
    val secondaryRadius = 16.dp      // opacity 25%, Y 8
    val dataRadius = 10.dp           // opacity 18%, Y 5
}

/** Animation durations (ms) — matches iOS TrueSkiesDesignSystem.Animation */
object TrueSkiesAnimation {
    // Basic durations
    const val INSTANT = 100
    const val FAST = 200
    const val NORMAL = 300
    const val SLOW = 500
    const val VERY_SLOW = 800

    // Spring animation durations (response time in ms)
    const val SPRING_INSTANT = 150
    const val SPRING_QUICK = 220
    const val SPRING_STANDARD = 300
    const val SPRING_GENTLE = 350
    const val SPRING_SMOOTH = 350
    const val SPRING_FLUID = 450

    // Micro-interactions
    const val MICRO_INTERACTION = 100
    const val BUTTON_PRESS = 120
    const val LIST_ITEM = 240
    const val CARD_ENTRY = 350
    const val CARD_SWIPE = 300

    // Aviation-specific
    const val AIRCRAFT_MOVE = 520
    const val MAP_TRANSITION = 700
    const val ROUTE_DRAWING = 1050

    // Modal & transitions
    const val MODAL_PRESENT = 420
    const val MODAL_DISMISS = 350
    const val CARD_FLIP = 480
    const val SLIDE_TRANSITION = 350
}

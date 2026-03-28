package com.trueskies.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * TrueSkies color palette — ported from iOS TrueSkiesDesignSystem.swift + TrueSkiesColorScheme.swift.
 * Dark-first design with aviation-inspired palette.
 */
object TrueSkiesColors {

    // ── Primary Brand ──
    val PrimaryNavy = Color(0xFF0A1628)
    val PrimaryNavyLight = Color(0xFF1A2A4A)
    val AccentBlue = Color(0xFF007AFF)           // iOS primary accent rgb(0,122,255)
    val AccentBlueLight = Color(0xFF40A6FF)      // iOS secondary accent dark rgb(64,166,255)
    val AccentIndigo = Color(0xFF6366F1)
    val AccentCyan = Color(0xFF22D3EE)
    val AccentMuted = Color(0xFF89B0EE)          // iOS muted accent rgb(137,176,238)

    // ── Surface / Background (iOS dark mode — neutral charcoal, not navy) ──
    val SurfacePrimary = Color(0xFF0C0C14)        // iOS systemBackground dark ~rgb(12,12,20)
    val SurfaceSecondary = Color(0xFF161620)       // iOS secondarySystemBackground dark
    val SurfaceCard = Color(0xFF161620)
    val SurfaceElevated = Color(0xFF1C1C28)        // iOS tertiarySystemBackground dark
    val SurfaceOverlay = Color(0x99161620)          // 60% opacity

    // ── Glass Effect (iOS ultraThinMaterial) ──
    val GlassBackground = Color(0x14FFFFFF)      // 8% white (iOS dark mode)
    val GlassBorder = Color(0x1AFFFFFF)           // 10% white (iOS separator)
    val GlassHighlight = Color(0x1AFFFFFF)        // 10% white
    val GlassOverlayLight = Color(0x0DFFFFFF)     // 5% white
    val GlassSecondary = Color(0x0DFFFFFF)        // 5% white
    val GlassTertiary = Color(0x08FFFFFF)         // 3% white

    // ── Text ──
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextTertiary = Color(0xFF94A3B8)
    val TextInverse = Color(0xFF0F172A)
    val TextMuted = Color(0xFF64748B)

    // ── Status Colors (iOS TrueSkiesColorScheme) ──
    val StatusOnTime = Color(0xFF1A8C40)         // iOS dark green rgb(26,140,64)
    val StatusActive = Color(0xFF4080E6)          // iOS in-flight blue rgb(64,128,230)
    val StatusDelayed = Color(0xFFFFB333)         // iOS bright orange rgb(255,179,51)
    val StatusCancelled = Color(0xFFFF6666)       // iOS coral-red rgb(255,102,102)
    val StatusDiverted = Color(0xFFFF6666)        // iOS red rgb(255,102,102)
    val StatusLanded = Color(0xFF1A8C40)          // iOS arrived = on-time green
    val StatusScheduled = Color(0xFF94A3B8)
    val StatusBoarding = Color(0xFF10B981)
    val StatusTaxiing = Color(0xFF8B5CF6)
    val StatusUnknown = Color(0xFF64748B)
    val StatusGate = Color(0xFFFFFF00)            // iOS gate yellow

    // ── Semantic / Action ──
    val Success = Color(0xFF34C759)              // iOS positive action rgb(52,199,89)
    val Warning = Color(0xFFFFB333)              // iOS warning orange
    val Error = Color(0xFFFF3B30)                // iOS negative action rgb(255,59,48)
    val Info = Color(0xFF73AEFF)                 // iOS info sky blue rgb(115,174,255)

    // ── Tab / Navigation ──
    val TabActive = AccentBlue
    val TabInactive = Color(0xFF64748B)

    // ── Gradients ──
    val GradientDarkStart = Color(0xFF08080D)    // iOS dark bg start rgb(8,8,13)
    val GradientDarkMid = Color(0xFF101018)      // iOS dark bg mid
    val GradientDarkEnd = Color(0xFF181822)       // iOS dark bg end
    val GradientStartPrimary = Color(0xFF0C0C14)
    val GradientEndPrimary = Color(0xFF161620)
    val GradientStartAccent = Color(0xFF3B82F6)
    val GradientEndAccent = Color(0xFF6366F1)
    val GradientHeroStart = Color(0xFF24293B)    // iOS hero card start rgb(36,41,59)
    val GradientHeroEnd = Color(0xFF171C2E)      // iOS hero card end rgb(23,28,46)

    // ── Flight Path / Map ──
    val FlightPathActive = AccentBlue
    val FlightPathCompleted = Color(0xFF64748B)
    val FlightPathDotActive = Color(0xFFFFFFFF)
    val AircraftActive = Color.White
    val AircraftSelected = AccentBlue
    val AirportMarker = AccentBlue

    // ── Airline Logo Placeholder ──
    val AirlineLogoBg = Color(0xFF293548)
    val AirlineLogoText = Color(0xFFE2E8F0)

    // ── Data Dashboard (iOS FlightLog) ──
    val DashboardCharcoal = Color(0xFF171C21)
    val DashboardCyan = Color(0xFF59A6FF)
    val DashboardCyanGlow = Color(0xFF78BFFF)
    val DashboardPurple = Color(0xFFA370F7)
    val DashboardText = Color(0xFFE6EDF2)
    val DashboardMuted = Color(0xFF7D858F)
    val DashboardGreen = Color(0xFF40BA4F)

    // ── Card Tints (iOS FlightLog cards) ──
    val AircraftTint = Color(0x242E8CC7)         // 14% opacity
    val TimelineTint = Color(0x29BF8C26)         // 16% opacity
    val StatsTint = Color(0x14737A85)            // 8% opacity

    // ── Boarding Pass Theme ──
    val BoardingBlue = Color(0xFF1F3B5E)
    val BoardingTeal = Color(0xFF2E6B80)
    val BoardingSky = Color(0xFF87CFEB)
    val BoardingGray = Color(0xFF4A5469)

    // ── Breathing Mode (Nervous Flyer) ──
    val BreathingDeep = Color(0xFF0F0D1F)
    val BreathingMid = Color(0xFF1A142E)
    val BreathingSoft = Color(0xFF241F3D)
    val BreathingTextPrimary = Color(0xFFF2EDE6)
    val BreathingPrepare = Color(0xFFA694CC)     // lavender
    val BreathingInhale = Color(0xFFD98C99)      // rose
    val BreathingHold = Color(0xFFE6B373)        // amber
    val BreathingExhale = Color(0xFF8CB89E)      // sage
    val BreathingComplete = Color(0xFF80B3D9)    // celestial blue

    // ── Retro Panel ──
    val RetroPanelPrimary = Color(0xFFF0EBDE)
    val RetroPanelSecondary = Color(0xFFE8E3D6)
    val RetroLedAmber = Color(0xFFFFC70D)
}

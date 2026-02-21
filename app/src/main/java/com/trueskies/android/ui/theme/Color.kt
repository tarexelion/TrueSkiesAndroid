package com.trueskies.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * TrueSkies color palette — ported from iOS LiquidGlass.swift.
 * Dark-first design with aviation-inspired palette.
 */
object TrueSkiesColors {

    // ── Primary Brand ──
    val PrimaryNavy = Color(0xFF0A1628)
    val PrimaryNavyLight = Color(0xFF1A2A4A)
    val AccentBlue = Color(0xFF3B82F6)
    val AccentBlueLight = Color(0xFF60A5FA)
    val AccentIndigo = Color(0xFF6366F1)
    val AccentCyan = Color(0xFF22D3EE)

    // ── Surface / Background ──
    val SurfacePrimary = Color(0xFF0F172A)
    val SurfaceSecondary = Color(0xFF1E293B)
    val SurfaceCard = Color(0xFF1E293B)
    val SurfaceElevated = Color(0xFF293548)
    val SurfaceOverlay = Color(0x991E293B) // 60% opacity

    // ── Glass Effect ──
    val GlassBackground = Color(0x33FFFFFF)      // 20% white
    val GlassBorder = Color(0x26FFFFFF)           // 15% white
    val GlassHighlight = Color(0x1AFFFFFF)        // 10% white
    val GlassOverlayLight = Color(0x0DFFFFFF)     // 5% white

    // ── Text ──
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextTertiary = Color(0xFF94A3B8)
    val TextInverse = Color(0xFF0F172A)
    val TextMuted = Color(0xFF64748B)

    // ── Status Colors ──
    val StatusOnTime = Color(0xFF22C55E)
    val StatusActive = Color(0xFF3B82F6)
    val StatusDelayed = Color(0xFFF59E0B)
    val StatusCancelled = Color(0xFFEF4444)
    val StatusDiverted = Color(0xFFA855F7)
    val StatusLanded = Color(0xFF06B6D4)
    val StatusScheduled = Color(0xFF94A3B8)
    val StatusBoarding = Color(0xFF10B981)
    val StatusTaxiing = Color(0xFF8B5CF6)
    val StatusUnknown = Color(0xFF64748B)

    // ── Semantic ──
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)

    // ── Tab / Navigation ──
    val TabActive = AccentBlue
    val TabInactive = Color(0xFF64748B)

    // ── Gradients ──
    val GradientStartPrimary = Color(0xFF0F172A)
    val GradientEndPrimary = Color(0xFF1E293B)
    val GradientStartAccent = Color(0xFF3B82F6)
    val GradientEndAccent = Color(0xFF6366F1)
    val GradientStartSuccess = Color(0xFF22C55E)
    val GradientEndSuccess = Color(0xFF059669)
    val GradientStartWarning = Color(0xFFF59E0B)
    val GradientEndWarning = Color(0xFFD97706)

    // ── Flight Path ──
    val FlightPathActive = AccentBlue
    val FlightPathCompleted = Color(0xFF64748B)
    val FlightPathDotActive = Color(0xFFFFFFFF)

    // ── Airline Logo Placeholder ──
    val AirlineLogoBg = Color(0xFF293548)
    val AirlineLogoText = Color(0xFFE2E8F0)
}

package com.trueskies.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.trueskies.android.ui.theme.*

/**
 * LiquidGlass-style card — ported from iOS liquidGlassBackground() modifier.
 * Creates a translucent card with gradient background and subtle shadow.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(TrueSkiesCornerRadius.lg)

    Column(
        modifier = modifier
            .shadow(
                elevation = TrueSkiesElevation.md,
                shape = shape,
                ambientColor = TrueSkiesColors.PrimaryNavy,
                spotColor = TrueSkiesColors.AccentBlue.copy(alpha = 0.1f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TrueSkiesColors.SurfaceElevated,
                        TrueSkiesColors.SurfaceElevated.copy(alpha = 0.92f)
                    )
                )
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        content = content
    )
}

package com.trueskies.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trueskies.android.ui.theme.*

/**
 * LiquidGlass-style card — ported from iOS liquidGlassBackground() modifier.
 * Creates a translucent card mimicking iOS .ultraThinMaterial with subtle glass border.
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
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.09f),  // iOS ultraThinMaterial ~8-10%
                        Color.White.copy(alpha = 0.05f)   // fades to ~5%
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),  // top edge highlight
                        Color.White.copy(alpha = 0.05f)   // bottom edge subtle
                    )
                ),
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        content = content
    )
}

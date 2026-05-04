package com.nuvio.app.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object OmnioSurfaceTokens {
    val pagePadding = 16.dp
    val sectionSpacing = 16.dp
    val cardShape = RoundedCornerShape(24.dp)
    val chipShape = RoundedCornerShape(999.dp)
    val buttonShape = RoundedCornerShape(18.dp)
    val heroShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    val navShape = RoundedCornerShape(28.dp)
}

@Composable
fun omnioHeroScrimBrush(): Brush {
    val background = MaterialTheme.colorScheme.background
    return Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.16f),
            background.copy(alpha = 0.08f),
            background.copy(alpha = 0.38f),
            background.copy(alpha = 0.92f),
        ),
    )
}

@Composable
fun omnioBottomFadeBrush(): Brush {
    val background = MaterialTheme.colorScheme.background
    return Brush.verticalGradient(
        colors = listOf(
            background.copy(alpha = 0f),
            background.copy(alpha = 0.9f),
            background,
        ),
    )
}

@Composable
fun omnioBackdropWashBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.3f),
    ),
)

@Composable
fun omnioCardOverlayBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.1f),
        Color.Black.copy(alpha = 0.72f),
    ),
)

@Composable
fun omnioGlassSurfaceColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)

@Composable
fun omnioHairlineColor(): Color = Color.White.copy(alpha = 0.08f)

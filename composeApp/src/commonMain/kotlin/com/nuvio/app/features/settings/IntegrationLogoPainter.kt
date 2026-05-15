package com.nuvio.app.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

internal enum class IntegrationLogo {
    Tmdb,
    Trakt,
    MdbList,
    Emby,
}

@Composable
internal expect fun integrationLogoPainter(logo: IntegrationLogo): Painter

/**
 * Material-icon stand-in for the Emby logo until a brand asset is added.
 * Both Android and iOS actuals route [IntegrationLogo.Emby] through this helper
 * so the icon stays consistent across platforms.
 */
@Composable
internal fun embyFallbackPainter(): Painter = rememberVectorPainter(Icons.Filled.Dns)

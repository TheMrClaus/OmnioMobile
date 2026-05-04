package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.OmnioSurfaceTokens
import com.nuvio.app.core.ui.omnioBackdropWashBrush
import com.nuvio.app.core.ui.omnioBottomFadeBrush
import com.nuvio.app.core.ui.omnioHeroScrimBrush
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.formatMetaReleaseLineForDetails
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_AUDIENCE
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_IMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TOMATOES
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun DetailHero(
    meta: MetaDetails,
    isTablet: Boolean = false,
    scrollOffset: Int = 0,
    contentMaxWidth: Dp = 560.dp,
    onHeightChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        val heroHeight = detailHeroHeight(maxWidth, isTablet)
        val pills = detailHeroPills(meta)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .onSizeChanged { onHeightChanged(it.height) }
                .graphicsLayer {
                    clip = true
                    shape = OmnioSurfaceTokens.heroShape
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomStart,
            ) {
                val imageUrl = meta.background ?: meta.poster
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = meta.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = scrollOffset * 0.5f
                                scaleX = 1.08f
                                scaleY = 1.08f
                            },
                        alignment = if (isTablet) Alignment.TopCenter else Alignment.Center,
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(omnioBackdropWashBrush()),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(omnioHeroScrimBrush()),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTablet) 180.dp else 220.dp)
                        .align(Alignment.BottomCenter)
                        .background(omnioBottomFadeBrush()),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (isTablet) 0.72f else 1f)
                        .widthIn(max = contentMaxWidth)
                        .padding(horizontal = if (isTablet) 32.dp else 18.dp)
                        .padding(bottom = if (isTablet) 28.dp else 22.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (meta.logo != null) {
                        AsyncImage(
                            model = meta.logo,
                            contentDescription = stringResource(Res.string.detail_logo_content_description, meta.name),
                            modifier = Modifier
                                .fillMaxWidth(if (isTablet) 0.58f else 0.72f)
                                .widthIn(max = if (isTablet) 420.dp else 300.dp)
                                .height(if (isTablet) 78.dp else 84.dp),
                            alignment = Alignment.CenterStart,
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = meta.name,
                            style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (pills.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            pills.forEachIndexed { index, pill ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                DetailHeroPillChip(pill = pill)
                            }
                        }
                    }

                    if (meta.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = meta.genres.take(3).joinToString(" \u2022 "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeroPillChip(
    pill: DetailHeroPill,
) {
    val background = if (pill.isHighlighted) {
        Color.White
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }
    val contentColor = if (pill.isHighlighted) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = Modifier
            .background(background, OmnioSurfaceTokens.chipShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = pill.text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

internal data class DetailHeroPill(
    val text: String,
    val isHighlighted: Boolean,
)

internal fun detailHeroPills(meta: MetaDetails): List<DetailHeroPill> {
    val pills = mutableListOf<DetailHeroPill>()

    detailMatchPercent(meta)?.let { percent ->
        pills += DetailHeroPill(text = "$percent%", isHighlighted = true)
    }
    formatMetaReleaseLineForDetails(meta)?.let { releaseLine ->
        pills += DetailHeroPill(text = releaseLine, isHighlighted = false)
    }
    meta.ageRating
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { ageRating ->
            pills += DetailHeroPill(text = ageRating, isHighlighted = false)
        }

    return pills
}

private fun detailMatchPercent(meta: MetaDetails): Int? {
    val ratingsBySource = meta.externalRatings.associateBy { it.source }
    val preferredScore = ratingsBySource[PROVIDER_AUDIENCE]?.value
        ?: ratingsBySource[PROVIDER_TOMATOES]?.value
        ?: ratingsBySource[PROVIDER_TMDB]?.value
        ?: ratingsBySource[PROVIDER_IMDB]?.value?.times(10)
        ?: meta.imdbRating?.toDoubleOrNull()?.times(10)

    return preferredScore
        ?.roundToInt()
        ?.coerceIn(0, 100)
}

private fun detailHeroHeight(maxWidth: Dp, isTablet: Boolean): Dp =
    if (!isTablet) {
        (maxWidth * 1.33f).coerceIn(420.dp, 760.dp)
    } else {
        (maxWidth * 0.42f).coerceIn(300.dp, 420.dp)
    }

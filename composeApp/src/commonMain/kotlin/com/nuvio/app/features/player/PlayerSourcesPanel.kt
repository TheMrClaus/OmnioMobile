package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.OmnioSurfaceTokens
import com.nuvio.app.core.ui.omnioGlassSurfaceColor
import com.nuvio.app.core.ui.omnioHairlineColor
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.features.streams.AddonStreamGroup
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamsUiState
import kotlin.math.round
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal data class PlayerPanelFilterOption(
    val addonId: String,
    val addonName: String,
    val isLoading: Boolean,
    val hasError: Boolean,
)

internal fun playerPanelFilterOptions(groups: List<AddonStreamGroup>): List<PlayerPanelFilterOption> =
    groups
        .groupBy { it.addonId }
        .values
        .map { matchingGroups ->
            val firstGroup = matchingGroups.first()
            PlayerPanelFilterOption(
                addonId = firstGroup.addonId,
                addonName = firstGroup.addonName,
                isLoading = matchingGroups.any { it.isLoading },
                hasError = matchingGroups.any { it.error != null },
            )
        }

@Composable
fun PlayerSourcesPanel(
    visible: Boolean,
    streamsUiState: StreamsUiState,
    currentStreamUrl: String?,
    currentStreamName: String?,
    onFilterSelected: (String?) -> Unit,
    onStreamSelected: (StreamItem) -> Unit,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                )
                .background(colorScheme.scrim.copy(alpha = 0.52f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(300)) { it / 3 } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(250)) { it / 3 } + fadeOut(tween(250)),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(0.92f)
                        .heightIn(max = 600.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(omnioGlassSurfaceColor())
                        .border(1.dp, omnioHairlineColor(), RoundedCornerShape(24.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.compose_player_panel_sources),
                                color = colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PanelChipButton(
                                    label = stringResource(Res.string.compose_action_reload),
                                    icon = Icons.Rounded.Refresh,
                                    onClick = onReload,
                                )
                                PanelChipButton(
                                    label = stringResource(Res.string.action_close),
                                    onClick = onDismiss,
                                )
                            }
                        }

                        // Addon filter chips
                        val filterOptions = remember(streamsUiState.groups) {
                            playerPanelFilterOptions(streamsUiState.groups)
                        }
                        if (filterOptions.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AddonFilterChip(
                                    label = stringResource(Res.string.collections_tab_all),
                                    isSelected = streamsUiState.selectedFilter == null,
                                    onClick = { onFilterSelected(null) },
                                )
                                filterOptions.forEach { option ->
                                    AddonFilterChip(
                                        label = option.addonName,
                                        isSelected = streamsUiState.selectedFilter == option.addonId,
                                        isLoading = option.isLoading,
                                        hasError = option.hasError,
                                        onClick = { onFilterSelected(option.addonId) },
                                    )
                                }
                            }
                        }

                        // Content
                        when {
                            streamsUiState.isAnyLoading && streamsUiState.allStreams.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = colorScheme.primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }

                            streamsUiState.allStreams.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.compose_player_no_streams_found),
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                    )
                                }
                            }

                            else -> {
                                val streams = streamsUiState.filteredGroups.flatMap { it.streams }
                                LazyColumn(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                                ) {
                                    itemsIndexed(
                                        items = streams,
                                        key = { index, stream -> "${stream.addonId}::${index}::${stream.url ?: stream.infoHash ?: stream.name}" },
                                    ) { _, stream ->
                                        val isCurrent = isCurrentStream(
                                            stream = stream,
                                            currentUrl = currentStreamUrl,
                                            currentName = currentStreamName,
                                        )
                                        SourceStreamRow(
                                            stream = stream,
                                            isCurrent = isCurrent,
                                            onClick = { onStreamSelected(stream) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceStreamRow(
    stream: StreamItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(cardShape)
            .background(
                if (isCurrent) colorScheme.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
            )
            .border(
                width = 1.dp,
                color = if (isCurrent) colorScheme.primary.copy(alpha = 0.45f) else omnioHairlineColor(),
                shape = cardShape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stream.streamLabel,
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp,
                        letterSpacing = 0.1.sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(OmnioSurfaceTokens.chipShape)
                            .background(colorScheme.primary.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.compose_player_playing),
                            color = colorScheme.onBackground,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            val subtitle = stream.streamSubtitle
            if (!subtitle.isNullOrBlank() && subtitle != stream.streamLabel) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayerStreamFileSizeBadge(stream = stream)
                Text(
                    text = stream.addonName,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlayerStreamFileSizeBadge(stream: StreamItem) {
    val bytes = stream.behaviorHints.videoSize ?: return
    val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val sizeLabel = if (gib >= 1.0) {
        val roundedGiB = round(gib * 10.0) / 10.0
        "$roundedGiB ${localizedByteUnit("GB")}"
    } else {
        val mib = bytes.toDouble() / (1024.0 * 1024.0)
        "${round(mib).toInt()} ${localizedByteUnit("MB")}"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, omnioHairlineColor(), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(Res.string.streams_size, sizeLabel),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
internal fun AddonFilterChip(
    label: String,
    isSelected: Boolean,
    isLoading: Boolean = false,
    hasError: Boolean = false,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isSelected -> colorScheme.primary.copy(alpha = 0.18f)
                    else -> omnioGlassSurfaceColor()
                },
            )
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                } else {
                    Modifier.border(1.dp, omnioHairlineColor(), RoundedCornerShape(20.dp))
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = colorScheme.primary,
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = label,
                color = when {
                    hasError -> colorScheme.error
                    isSelected -> colorScheme.onBackground
                    else -> colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
internal fun PanelChipButton(
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(omnioGlassSurfaceColor())
            .border(1.dp, omnioHairlineColor(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

private fun isCurrentStream(
    stream: StreamItem,
    currentUrl: String?,
    currentName: String?,
): Boolean {
    if (currentUrl != null && stream.directPlaybackUrl == currentUrl) return true
    if (currentName != null && stream.streamLabel.equals(currentName, ignoreCase = true) &&
        stream.directPlaybackUrl == currentUrl
    ) return true
    return false
}

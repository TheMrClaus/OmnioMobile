package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nuvio.app.core.ui.SettingsMultiChoiceDialog
import com.nuvio.app.core.ui.SettingsPickerOption
import com.nuvio.app.core.ui.SettingsSingleChoiceDialog
import com.nuvio.app.features.streams.prefs.StreamPrefAudioTag
import com.nuvio.app.features.streams.prefs.StreamPrefCodec
import com.nuvio.app.features.streams.prefs.StreamPrefEncode
import com.nuvio.app.features.streams.prefs.StreamPrefMinQuality
import com.nuvio.app.features.streams.prefs.StreamPrefVisualTag
import com.nuvio.app.features.streams.prefs.StreamPreferences
import com.nuvio.app.features.streams.prefs.StreamPreferencesRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_stream_prefs
import nuvio.composeapp.generated.resources.settings_stream_prefs_subtitle
import nuvio.composeapp.generated.resources.stream_prefs_audio_excluded_title
import nuvio.composeapp.generated.resources.stream_prefs_audio_required_title
import nuvio.composeapp.generated.resources.stream_prefs_cached_subtitle
import nuvio.composeapp.generated.resources.stream_prefs_cached_title
import nuvio.composeapp.generated.resources.stream_prefs_codec_excluded_title
import nuvio.composeapp.generated.resources.stream_prefs_codec_required_title
import nuvio.composeapp.generated.resources.stream_prefs_enable_subtitle
import nuvio.composeapp.generated.resources.stream_prefs_enable_title
import nuvio.composeapp.generated.resources.stream_prefs_encode_excluded_title
import nuvio.composeapp.generated.resources.stream_prefs_encode_required_title
import nuvio.composeapp.generated.resources.stream_prefs_hdr_excluded_title
import nuvio.composeapp.generated.resources.stream_prefs_hdr_required_title
import nuvio.composeapp.generated.resources.stream_prefs_language_excluded_title
import nuvio.composeapp.generated.resources.stream_prefs_language_required_title
import nuvio.composeapp.generated.resources.stream_prefs_min_quality_title
import nuvio.composeapp.generated.resources.stream_prefs_preload_subtitle
import nuvio.composeapp.generated.resources.stream_prefs_preload_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.streamPreferencesSettingsContent(
    isTablet: Boolean,
    prefs: StreamPreferences,
    enabled: Boolean = prefs.enabled,
) {
    item {
        StreamPreferencesBody(isTablet = isTablet, prefs = prefs, enabled = enabled)
    }
}

@Composable
private fun StreamPreferencesBody(
    isTablet: Boolean,
    prefs: StreamPreferences,
    enabled: Boolean,
) {
    val up: ((StreamPreferences) -> StreamPreferences) -> Unit = { xform ->
        StreamPreferencesRepository.update(xform)
    }

    var showMinQualityDialog by remember { mutableStateOf(false) }
    var showRequiredCodecDialog by remember { mutableStateOf(false) }
    var showExcludedCodecDialog by remember { mutableStateOf(false) }
    var showRequiredVisualDialog by remember { mutableStateOf(false) }
    var showExcludedVisualDialog by remember { mutableStateOf(false) }
    var showRequiredAudioDialog by remember { mutableStateOf(false) }
    var showExcludedAudioDialog by remember { mutableStateOf(false) }
    var showRequiredEncodeDialog by remember { mutableStateOf(false) }
    var showExcludedEncodeDialog by remember { mutableStateOf(false) }

    SettingsSection(
        title = stringResource(Res.string.settings_stream_prefs),
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            SettingsSwitchRow(
                title = stringResource(Res.string.stream_prefs_enable_title),
                description = stringResource(Res.string.stream_prefs_enable_subtitle),
                checked = prefs.enabled,
                isTablet = isTablet,
                onCheckedChange = { checked -> up { it.copy(enabled = checked) } },
            )
        }

        SettingsGroup(isTablet = isTablet) {
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_min_quality_title),
                description = "",
                isTablet = isTablet,
                onClick = { showMinQualityDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_codec_required_title),
                description = "",
                isTablet = isTablet,
                onClick = { showRequiredCodecDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_codec_excluded_title),
                description = "",
                isTablet = isTablet,
                onClick = { showExcludedCodecDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_hdr_required_title),
                description = "",
                isTablet = isTablet,
                onClick = { showRequiredVisualDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_hdr_excluded_title),
                description = "",
                isTablet = isTablet,
                onClick = { showExcludedVisualDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_audio_required_title),
                description = "",
                isTablet = isTablet,
                onClick = { showRequiredAudioDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_audio_excluded_title),
                description = "",
                isTablet = isTablet,
                onClick = { showExcludedAudioDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_encode_required_title),
                description = "",
                isTablet = isTablet,
                onClick = { showRequiredEncodeDialog = true },
            )
            SettingsNavigationRow(
                title = stringResource(Res.string.stream_prefs_encode_excluded_title),
                description = "",
                isTablet = isTablet,
                onClick = { showExcludedEncodeDialog = true },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.stream_prefs_cached_title),
                description = stringResource(Res.string.stream_prefs_cached_subtitle),
                checked = prefs.requireCached,
                enabled = enabled,
                isTablet = isTablet,
                onCheckedChange = { checked -> up { it.copy(requireCached = checked) } },
            )
        }
    }

    if (showMinQualityDialog) {
        SettingsSingleChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_min_quality_title),
            options = StreamPrefMinQuality.entries.map { q ->
                SettingsPickerOption(value = q, title = if (q == StreamPrefMinQuality.NONE) "Any" else q.name)
            },
            selected = prefs.minResolution,
            onSelect = { q -> up { it.copy(minResolution = q) }; showMinQualityDialog = false },
            onDismiss = { showMinQualityDialog = false },
        )
    }

    if (showRequiredCodecDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_codec_required_title),
            options = StreamPrefCodec.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.requiredCodecs,
            onSave = { sel -> up { it.copy(requiredCodecs = sel) }; showRequiredCodecDialog = false },
            onDismiss = { showRequiredCodecDialog = false },
        )
    }

    if (showExcludedCodecDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_codec_excluded_title),
            options = StreamPrefCodec.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.excludedCodecs,
            onSave = { sel -> up { it.copy(excludedCodecs = sel) }; showExcludedCodecDialog = false },
            onDismiss = { showExcludedCodecDialog = false },
        )
    }

    if (showRequiredVisualDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_hdr_required_title),
            options = StreamPrefVisualTag.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.requiredVisualTags,
            onSave = { sel -> up { it.copy(requiredVisualTags = sel) }; showRequiredVisualDialog = false },
            onDismiss = { showRequiredVisualDialog = false },
        )
    }

    if (showExcludedVisualDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_hdr_excluded_title),
            options = StreamPrefVisualTag.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.excludedVisualTags,
            onSave = { sel -> up { it.copy(excludedVisualTags = sel) }; showExcludedVisualDialog = false },
            onDismiss = { showExcludedVisualDialog = false },
        )
    }

    if (showRequiredAudioDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_audio_required_title),
            options = StreamPrefAudioTag.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.requiredAudioTags,
            onSave = { sel -> up { it.copy(requiredAudioTags = sel) }; showRequiredAudioDialog = false },
            onDismiss = { showRequiredAudioDialog = false },
        )
    }

    if (showExcludedAudioDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_audio_excluded_title),
            options = StreamPrefAudioTag.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.excludedAudioTags,
            onSave = { sel -> up { it.copy(excludedAudioTags = sel) }; showExcludedAudioDialog = false },
            onDismiss = { showExcludedAudioDialog = false },
        )
    }

    if (showRequiredEncodeDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_encode_required_title),
            options = StreamPrefEncode.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.requiredEncodes,
            onSave = { sel -> up { it.copy(requiredEncodes = sel) }; showRequiredEncodeDialog = false },
            onDismiss = { showRequiredEncodeDialog = false },
        )
    }

    if (showExcludedEncodeDialog) {
        SettingsMultiChoiceDialog(
            visible = true,
            title = stringResource(Res.string.stream_prefs_encode_excluded_title),
            options = StreamPrefEncode.entries.map { SettingsPickerOption(value = it, title = it.label) },
            selected = prefs.excludedEncodes,
            onSave = { sel -> up { it.copy(excludedEncodes = sel) }; showExcludedEncodeDialog = false },
            onDismiss = { showExcludedEncodeDialog = false },
        )
    }
}

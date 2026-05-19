package com.nuvio.app.features.sourcecloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.settings.SettingsGroup
import com.nuvio.app.features.settings.SettingsGroupDivider
import com.nuvio.app.features.settings.SettingsSection
import com.nuvio.app.features.settings.SettingsSwitchRow
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_source_cloud_advanced_open
import nuvio.composeapp.generated.resources.settings_source_cloud_advanced_regenerate
import nuvio.composeapp.generated.resources.settings_source_cloud_advanced_section
import nuvio.composeapp.generated.resources.settings_source_cloud_advanced_session_default_message
import nuvio.composeapp.generated.resources.settings_source_cloud_advanced_subtitle
import nuvio.composeapp.generated.resources.settings_source_cloud_advanced_title
import nuvio.composeapp.generated.resources.settings_source_cloud_backend_missing
import nuvio.composeapp.generated.resources.settings_source_cloud_config_section
import nuvio.composeapp.generated.resources.settings_source_cloud_config_status_title
import nuvio.composeapp.generated.resources.settings_source_cloud_config_status_unknown
import nuvio.composeapp.generated.resources.settings_source_cloud_enable_description
import nuvio.composeapp.generated.resources.settings_source_cloud_enable_title
import nuvio.composeapp.generated.resources.settings_source_cloud_intro
import nuvio.composeapp.generated.resources.settings_source_cloud_no_services
import nuvio.composeapp.generated.resources.settings_source_cloud_reset
import nuvio.composeapp.generated.resources.settings_source_cloud_reset_subtitle
import nuvio.composeapp.generated.resources.settings_source_cloud_section
import nuvio.composeapp.generated.resources.settings_source_cloud_service_connect
import nuvio.composeapp.generated.resources.settings_source_cloud_service_connected
import nuvio.composeapp.generated.resources.settings_source_cloud_service_disconnect
import nuvio.composeapp.generated.resources.settings_source_cloud_service_disconnected
import nuvio.composeapp.generated.resources.settings_source_cloud_services_section
import org.jetbrains.compose.resources.stringResource

/**
 * Settings page for Omnio Source Cloud.
 *
 * Mirrors the OmnioTV `SourceCloudSettingsContent` panel: a master enable
 * toggle, an info section while no services are connected, per-service
 * connect rows, the advanced-config session launcher, and a reset action.
 * Phone & tablet adapt via [isTablet] and the rows reuse OmnioMobile's
 * shared SettingsGroup primitives so the look stays consistent with the
 * rest of the settings UI.
 */
internal fun LazyListScope.sourceCloudSettingsContent(
    isTablet: Boolean,
    uiState: SourceCloudUiState,
) {
    val status = uiState.status
    val config = status?.config
    val services = status?.services.orEmpty()
    val backendConfigured = status?.baseUrlConfigured ?: false
    val hasConnectedService = services.any { it.connected }

    item {
        SettingsGroup(isTablet = isTablet) {
            SourceCloudIntro(isTablet = isTablet)
        }
    }

    if (!backendConfigured) {
        item {
            SettingsGroup(isTablet = isTablet) {
                SourceCloudInfoText(
                    isTablet = isTablet,
                    message = stringResource(Res.string.settings_source_cloud_backend_missing),
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_source_cloud_section),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_source_cloud_enable_title),
                    description = stringResource(Res.string.settings_source_cloud_enable_description),
                    checked = uiState.enabled,
                    enabled = !uiState.isLoading,
                    isTablet = isTablet,
                    onCheckedChange = SourceCloudRepository::setEnabled,
                )
                if (status != null && !hasConnectedService) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SourceCloudInfoText(
                        isTablet = isTablet,
                        message = stringResource(Res.string.settings_source_cloud_no_services),
                    )
                }
                uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    SettingsGroupDivider(isTablet = isTablet)
                    SourceCloudInfoText(
                        isTablet = isTablet,
                        message = message,
                        tone = SourceCloudInfoTone.Error,
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_source_cloud_services_section),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SourceCloudService.entries.forEachIndexed { index, service ->
                    if (index > 0) {
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    val serviceStatus = services.firstOrNull { it.service == service }
                    val connected = serviceStatus?.connected
                        ?: (service.key in uiState.connectedServiceKeys)
                    SourceCloudServiceRow(
                        isTablet = isTablet,
                        title = serviceStatus?.label?.takeIf { it.isNotBlank() } ?: service.displayName,
                        subtitle = if (connected) {
                            stringResource(Res.string.settings_source_cloud_service_connected)
                        } else {
                            stringResource(Res.string.settings_source_cloud_service_disconnected)
                        },
                        connected = connected,
                        enabled = !uiState.isLoading && !uiState.isConnectServiceSubmitting,
                        onToggle = { newConnected ->
                            if (newConnected) {
                                SourceCloudRepository.beginConnectService(service)
                            } else {
                                SourceCloudRepository.disconnectService(service)
                            }
                        },
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_source_cloud_config_section),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SourceCloudConfigCard(
                    isTablet = isTablet,
                    label = config?.label,
                    message = config?.message,
                )
                if (config?.canReset == true) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SourceCloudActionRow(
                        isTablet = isTablet,
                        title = stringResource(Res.string.settings_source_cloud_reset),
                        subtitle = stringResource(Res.string.settings_source_cloud_reset_subtitle),
                        enabled = !uiState.isLoading,
                        isLoading = uiState.isLoading,
                        onClick = SourceCloudRepository::resetConfig,
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = "API keys",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SourceCloudApiKeysCard(
                    isTablet = isTablet,
                    uiState = uiState,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_source_cloud_advanced_section),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SourceCloudAdvancedCard(
                    isTablet = isTablet,
                    advancedAvailable = config?.advancedConfigAvailable == true,
                    isLoading = uiState.isAdvancedConfigLoading,
                    session = uiState.advancedConfigSession,
                )
            }
        }
    }
}

@Composable
private fun SourceCloudIntro(isTablet: Boolean) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_source_cloud_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SourceCloudInfoText(
    isTablet: Boolean,
    message: String,
    tone: SourceCloudInfoTone = SourceCloudInfoTone.Neutral,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 14.dp else 12.dp

    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        style = MaterialTheme.typography.bodyMedium,
        color = when (tone) {
            SourceCloudInfoTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            SourceCloudInfoTone.Error -> MaterialTheme.colorScheme.error
        },
    )
}

@Composable
private fun SourceCloudServiceRow(
    isTablet: Boolean,
    title: String,
    subtitle: String,
    connected: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (connected) {
            TextButton(
                onClick = { onToggle(false) },
                enabled = enabled,
            ) {
                Text(stringResource(Res.string.settings_source_cloud_service_disconnect))
            }
        } else {
            Button(
                onClick = { onToggle(true) },
                enabled = enabled,
            ) {
                Text(stringResource(Res.string.settings_source_cloud_service_connect))
            }
        }
    }
}

@Composable
private fun SourceCloudConfigCard(
    isTablet: Boolean,
    label: String?,
    message: String?,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val subtitle = listOfNotNull(
        label?.takeIf { it.isNotBlank() },
        message?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank {
        stringResource(Res.string.settings_source_cloud_config_status_unknown)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_source_cloud_config_status_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SourceCloudActionRow(
    isTablet: Boolean,
    title: String,
    subtitle: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(title)
            }
        }
    }
}

@Composable
private fun SourceCloudAdvancedCard(
    isTablet: Boolean,
    advancedAvailable: Boolean,
    isLoading: Boolean,
    session: SourceCloudAdvancedConfigSession?,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_source_cloud_advanced_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(Res.string.settings_source_cloud_advanced_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        session?.let { resolved ->
            Text(
                text = resolved.message?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.settings_source_cloud_advanced_session_default_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            resolved.configurePassword?.takeIf { it.isNotBlank() }?.let { pwd ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Paste this password when AIOStreams asks:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = pwd,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { clipboardManager.setText(AnnotatedString(pwd)) },
                        ) {
                            Text("Copy")
                        }
                    }
                }
            }
            resolved.directConfigureUrl?.takeIf { it.isNotBlank() }?.let { directUrl ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Bookmark this in a password manager to open from any browser:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = directUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                        )
                        TextButton(
                            onClick = { clipboardManager.setText(AnnotatedString(directUrl)) },
                        ) {
                            Text("Copy")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    if (session != null) {
                        runCatching { uriHandler.openUri(session.url) }
                            .onFailure { /* swallow — user can copy manually */ }
                    } else {
                        SourceCloudRepository.requestAdvancedConfigSession(autoOpen = true)
                    }
                },
                enabled = !isLoading && (session != null || advancedAvailable),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(stringResource(Res.string.settings_source_cloud_advanced_open))
                }
            }
            if (session != null) {
                TextButton(
                    onClick = SourceCloudRepository::requestAdvancedConfigSession,
                    enabled = !isLoading,
                ) {
                    Text(stringResource(Res.string.settings_source_cloud_advanced_regenerate))
                }
            }
        }
    }
}

private enum class SourceCloudInfoTone {
    Neutral,
    Error,
}

private data class ApiKeysFormState(
    val tmdbApiKey: String,
    val tmdbAccessToken: String,
    val tvdbApiKey: String,
    val rpdbApiKey: String,
    val animeToshoEnabled: Boolean,
    val debridioApiKey: String,
)

private fun SourceCloudConfigSummary.toForm(): ApiKeysFormState = ApiKeysFormState(
    tmdbApiKey = tmdbApiKey,
    tmdbAccessToken = tmdbAccessToken,
    tvdbApiKey = tvdbApiKey,
    rpdbApiKey = rpdbApiKey.ifEmpty { "t0-free-rpdb" },
    animeToshoEnabled = animeToshoEnabled,
    debridioApiKey = debridioApiKey,
)

@Composable
private fun SourceCloudApiKeysCard(
    isTablet: Boolean,
    uiState: SourceCloudUiState,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        if (uiState.configSummary == null && !uiState.isConfigSummaryLoading) {
            SourceCloudRepository.refreshConfigSummary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val summary = uiState.configSummary
        if (summary == null) {
            if (uiState.isConfigSummaryLoading) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Loading API keys...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = uiState.configSummaryError
                        ?: "Connect a service first to manage API keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.configSummaryError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { SourceCloudRepository.refreshConfigSummary() },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) { Text("Retry") }
            }
            return@Column
        }

        var form by remember(summary) { mutableStateOf(summary.toForm()) }
        val baseline = remember(summary) { summary.toForm() }
        val dirty = form != baseline

        val tmdbKeyError = if (form.tmdbApiKey.isNotEmpty() && form.tmdbApiKey.length != 32) {
            "Should be exactly 32 characters"
        } else null
        val tmdbTokenError = if (form.tmdbAccessToken.isNotEmpty() && form.tmdbAccessToken.length < 200) {
            "Should be 200+ characters"
        } else null
        val hasErrors = tmdbKeyError != null || tmdbTokenError != null

        ApiKeyField(
            label = "TMDB API Key",
            value = form.tmdbApiKey,
            onChange = { form = form.copy(tmdbApiKey = it) },
            errorText = tmdbKeyError,
            helpUrl = "https://www.themoviedb.org/settings/api",
            helpLabel = "Get your TMDB key/token →",
            uriHandler = uriHandler,
        )
        ApiKeyField(
            label = "TMDB Read Access Token",
            value = form.tmdbAccessToken,
            onChange = { form = form.copy(tmdbAccessToken = it) },
            errorText = tmdbTokenError,
        )
        ApiKeyField(
            label = "TVDB API Key",
            value = form.tvdbApiKey,
            onChange = { form = form.copy(tvdbApiKey = it) },
            helpUrl = "https://www.thetvdb.com/api-information",
            helpLabel = "Get your TVDB key →",
            uriHandler = uriHandler,
        )
        ApiKeyField(
            label = "RPDB API Key",
            value = form.rpdbApiKey,
            onChange = { form = form.copy(rpdbApiKey = it) },
            placeholder = "t0-free-rpdb",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("AnimeTosho", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = "Free anime source addon; no API key required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = form.animeToshoEnabled,
                onCheckedChange = { form = form.copy(animeToshoEnabled = it) },
            )
        }
        ApiKeyField(
            label = "Debridio API Key",
            value = form.debridioApiKey,
            onChange = { form = form.copy(debridioApiKey = it) },
            helpUrl = "https://debridio.com",
            helpLabel = "Sign up for Debridio →",
            uriHandler = uriHandler,
        )

        uiState.configSummaryError?.takeIf { it.isNotBlank() }?.let { msg ->
            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = {
                    SourceCloudRepository.saveConfigSummary(
                        SourceCloudConfigSummary(
                            tmdbApiKey = form.tmdbApiKey,
                            tmdbAccessToken = form.tmdbAccessToken,
                            tvdbApiKey = form.tvdbApiKey,
                            rpdbApiKey = form.rpdbApiKey,
                            animeToshoEnabled = form.animeToshoEnabled,
                            debridioApiKey = form.debridioApiKey,
                            provisioned = summary.provisioned,
                        ),
                    )
                },
                enabled = dirty && !hasErrors && !uiState.isConfigSummarySaving,
            ) {
                if (uiState.isConfigSummarySaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    errorText: String? = null,
    placeholder: String? = null,
    helpUrl: String? = null,
    helpLabel: String? = null,
    uriHandler: androidx.compose.ui.platform.UriHandler? = null,
) {
    var revealed by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (revealed) "Hide" else "Reveal",
                    )
                }
            },
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        if (helpUrl != null && helpLabel != null && uriHandler != null) {
            TextButton(
                onClick = { runCatching { uriHandler.openUri(helpUrl) } },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                Text(helpLabel, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SourceCloudConnectServiceDialog(uiState: SourceCloudUiState) {
    val service = uiState.connectServiceTarget ?: return
    val uriHandler = LocalUriHandler.current
    var apiKey by remember(service) { mutableStateOf("") }

    val apiKeyHelpUrl: String? = when (service) {
        SourceCloudService.REAL_DEBRID -> "https://real-debrid.com/apitoken"
        SourceCloudService.TORBOX -> "https://torbox.app/settings"
    }
    val signUpUrl: String? = when (service) {
        SourceCloudService.TORBOX -> "https://torbox.app/subscription?referral=ef446ad8-b935-4c71-aa8f-fb62813c7a23"
        else -> null
    }

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isConnectServiceSubmitting) {
                SourceCloudRepository.cancelConnectService()
            }
        },
        title = {
            Text(text = "Connect ${service.displayName}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Paste your ${service.displayName} API key. It's stored encrypted in Omnio Source Cloud and used to provision your private AIOStreams config.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    singleLine = true,
                    enabled = !uiState.isConnectServiceSubmitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                apiKeyHelpUrl?.let { url ->
                    TextButton(
                        onClick = { runCatching { uriHandler.openUri(url) } },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Text("Get your ${service.displayName} API key →")
                    }
                }
                signUpUrl?.let { url ->
                    Text(
                        text = "Don't have a ${service.displayName} account?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { runCatching { uriHandler.openUri(url) } },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Text("Sign up (+15 days free via referral) →")
                    }
                }
                uiState.connectServiceError?.takeIf { it.isNotBlank() }?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { SourceCloudRepository.submitConnectService(service, apiKey) },
                enabled = !uiState.isConnectServiceSubmitting && apiKey.isNotBlank(),
            ) {
                if (uiState.isConnectServiceSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { SourceCloudRepository.cancelConnectService() },
                enabled = !uiState.isConnectServiceSubmitting,
            ) {
                Text("Cancel")
            }
        },
    )
}

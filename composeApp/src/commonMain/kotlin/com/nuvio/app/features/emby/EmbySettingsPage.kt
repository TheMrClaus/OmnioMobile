package com.nuvio.app.features.emby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.settings.SettingsGroup
import com.nuvio.app.features.settings.SettingsSection
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_emby_authentication
import nuvio.composeapp.generated.resources.settings_emby_connect
import nuvio.composeapp.generated.resources.settings_emby_connected_as
import nuvio.composeapp.generated.resources.settings_emby_copy_from_main
import nuvio.composeapp.generated.resources.settings_emby_disconnect
import nuvio.composeapp.generated.resources.settings_emby_intro_description
import nuvio.composeapp.generated.resources.settings_emby_password_label
import nuvio.composeapp.generated.resources.settings_emby_server_url_hint
import nuvio.composeapp.generated.resources.settings_emby_server_url_label
import nuvio.composeapp.generated.resources.settings_emby_sign_in_description
import nuvio.composeapp.generated.resources.settings_emby_username_label
import org.jetbrains.compose.resources.stringResource

/**
 * Settings page for Emby account connection.
 *
 * State is hoisted from [SettingsScreen] (mirroring TraktSettingsPage). Actions go
 * straight to [EmbyAuthRepository] which performs the network calls and updates
 * [EmbyAuthRepository.uiState] asynchronously.
 */
internal fun LazyListScope.embySettingsContent(
    isTablet: Boolean,
    uiState: EmbyAuthUiState,
) {
    item {
        SettingsGroup(isTablet = isTablet) {
            EmbyIntro(isTablet = isTablet)
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_emby_authentication),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                EmbyConnectionCard(
                    isTablet = isTablet,
                    uiState = uiState,
                )
            }
        }
    }
}

@Composable
private fun EmbyIntro(isTablet: Boolean) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_emby_intro_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmbyConnectionCard(
    isTablet: Boolean,
    uiState: EmbyAuthUiState,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.isConfigured) {
            ConnectedView(uiState = uiState)
        } else {
            DisconnectedView(uiState = uiState)
        }

        uiState.statusMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.isStatusSuccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ConnectedView(uiState: EmbyAuthUiState) {
    Text(
        text = stringResource(
            Res.string.settings_emby_connected_as,
            uiState.username,
            uiState.serverName.ifBlank { uiState.serverUrl },
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
    )
    if (uiState.serverUrl.isNotBlank()) {
        Text(
            text = uiState.serverUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Button(
        onClick = EmbyAuthRepository::onDisconnectRequested,
        enabled = !uiState.isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSurface,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(stringResource(Res.string.settings_emby_disconnect))
        }
    }
}

@Composable
private fun DisconnectedView(uiState: EmbyAuthUiState) {
    var serverUrl by rememberSaveable(uiState.updatedAtMs) {
        mutableStateOf(uiState.serverUrl)
    }
    var username by rememberSaveable(uiState.updatedAtMs) {
        mutableStateOf(uiState.username)
    }
    var password by rememberSaveable { mutableStateOf("") }

    Text(
        text = stringResource(Res.string.settings_emby_sign_in_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
    )

    OutlinedTextField(
        value = serverUrl,
        onValueChange = { serverUrl = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !uiState.isLoading,
        label = { Text(stringResource(Res.string.settings_emby_server_url_label)) },
        placeholder = { Text(stringResource(Res.string.settings_emby_server_url_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        colors = fieldColors,
    )

    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !uiState.isLoading,
        label = { Text(stringResource(Res.string.settings_emby_username_label)) },
        colors = fieldColors,
    )

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !uiState.isLoading,
        label = { Text(stringResource(Res.string.settings_emby_password_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        colors = fieldColors,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                EmbyAuthRepository.onSignInRequested(
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                )
            },
            enabled = !uiState.isLoading &&
                serverUrl.isNotBlank() &&
                username.isNotBlank(),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(stringResource(Res.string.settings_emby_connect))
            }
        }

        if (uiState.canCopyFromMain) {
            TextButton(
                onClick = EmbyAuthRepository::onCopyFromMainRequested,
                enabled = !uiState.isLoading,
            ) {
                Text(stringResource(Res.string.settings_emby_copy_from_main))
            }
        }
    }
}

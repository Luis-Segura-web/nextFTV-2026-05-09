package com.stream.nextftv.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stream.nextftv.R
import com.stream.nextftv.presentation.player.PlaybackCacheMode
import com.stream.nextftv.presentation.theme.dimens
import com.stream.nextftv.presentation.theme.spacing

@Composable
internal fun SettingsSyncIntervalItem(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    val options = listOf(6, 12, 24, 48)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Update,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(MaterialTheme.dimens.iconMedium)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_sync_interval),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_sync_interval_subtitle, currentInterval),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { hours ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_hours_unit, hours)) },
                        onClick = {
                            onIntervalChange(hours)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsPlaybackCacheItem(
    title: String,
    subtitle: String,
    selectedMode: PlaybackCacheMode,
    options: List<PlaybackCacheMode>,
    onModeSelected: (PlaybackCacheMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(MaterialTheme.dimens.iconMedium)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            Text(
                text = selectedMode.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.displayName()) },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun PlaybackCacheMode.displayName(): String = when (this) {
    PlaybackCacheMode.DISABLED -> "Sin cache"
    PlaybackCacheMode.PROXY -> "Proxy cache"
    PlaybackCacheMode.EXO -> "Exo cache"
}

@Composable
internal fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small)
        )
        Surface(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
internal fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(MaterialTheme.dimens.iconMedium)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(MaterialTheme.dimens.iconMedium)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

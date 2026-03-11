package com.stream.iptvrevolut.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stream.iptvrevolut.R
import com.stream.iptvrevolut.presentation.player.PlayerEngine
import com.stream.iptvrevolut.presentation.theme.dimens
import com.stream.iptvrevolut.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenParentalControl: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val activeProfile = viewModel.activeProfile

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings_title), 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Sección de Cuenta
            SettingsSection(title = stringResource(R.string.settings_section_account)) {
                SettingsItem(
                    title = stringResource(R.string.settings_active_profile),
                    subtitle = activeProfile?.name ?: stringResource(R.string.settings_unknown),
                    icon = Icons.Default.Person,
                    onClick = {}
                )
                SettingsItem(
                    title = stringResource(R.string.settings_server),
                    subtitle = activeProfile?.url ?: "---",
                    icon = Icons.Default.Dns,
                    onClick = {}
                )
                SettingsItem(
                    title = stringResource(R.string.settings_logout),
                    subtitle = stringResource(R.string.settings_logout_subtitle),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.onLogout()
                        onLogout()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Sección de Reproducción
            SettingsSection(title = stringResource(R.string.settings_section_playback)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_pip),
                    subtitle = stringResource(R.string.settings_pip_subtitle),
                    icon = Icons.Default.PictureInPicture,
                    checked = viewModel.isPipEnabled,
                    onCheckedChange = { viewModel.onPipEnabledChange(it) }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_background_playback),
                    subtitle = stringResource(R.string.settings_background_playback_subtitle),
                    icon = Icons.Default.Headset,
                    checked = viewModel.backgroundPlaybackEnabled,
                    onCheckedChange = { viewModel.onBackgroundPlaybackEnabledChange(it) }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_autoplay),
                    subtitle = stringResource(R.string.settings_autoplay_subtitle),
                    icon = Icons.Default.Autorenew,
                    checked = viewModel.autoPlayNext,
                    onCheckedChange = { viewModel.autoPlayNext = it }
                )
                SettingsPlayerEngineItem(
                    currentEngine = viewModel.preferredPlayerEngine,
                    onEngineChange = { viewModel.onPreferredPlayerEngineChange(it) }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Sección de Mantenimiento
            SettingsSection(title = "Mantenimiento") {
                var showClearRecentsDialog by remember { mutableStateOf(false) }
                var showClearCacheDialog by remember { mutableStateOf(false) }

                SettingsItem(
                    title = "Limpiar Recientes",
                    subtitle = "Borrar el historial de canales y películas",
                    icon = Icons.Default.History,
                    onClick = { showClearRecentsDialog = true }
                )
                
                val context = LocalContext.current
                SettingsItem(
                    title = "Limpiar Caché",
                    subtitle = "Eliminar imágenes almacenadas",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { showClearCacheDialog = true }
                )

                if (showClearRecentsDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearRecentsDialog = false },
                        title = { Text("¿Limpiar Recientes?") },
                        text = { Text("Se borrará el historial de reproducción de todos los módulos.") },
                        confirmButton = {
                            TextButton(onClick = { 
                                viewModel.onClearRecents()
                                showClearRecentsDialog = false
                            }) { Text("LIMPIAR", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearRecentsDialog = false }) { Text("CANCELAR") }
                        }
                    )
                }

                if (showClearCacheDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearCacheDialog = false },
                        title = { Text("¿Limpiar Caché?") },
                        text = { Text("Se eliminarán las imágenes de pósters y logos descargadas.") },
                        confirmButton = {
                            TextButton(onClick = { 
                                viewModel.onClearCache(context)
                                showClearCacheDialog = false
                            }) { Text("LIMPIAR", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearCacheDialog = false }) { Text("CANCELAR") }
                        }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Sección de General
            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                SettingsSwitchItem(
                    title = "Integración con TMDB",
                    subtitle = "Obtener pósters y detalles enriquecidos",
                    icon = Icons.Default.Info,
                    checked = viewModel.isTmdbEnabled,
                    onCheckedChange = { viewModel.onTmdbEnabledChange(it) }
                )
                SettingsSyncIntervalItem(
                    currentInterval = viewModel.syncInterval,
                    onIntervalChange = { viewModel.onSyncIntervalChange(it) }
                )
                SettingsItem(
                    title = stringResource(R.string.settings_parental),
                    subtitle = "Acceso protegido por NIP de 4 dígitos",
                    icon = Icons.Default.Lock,
                    onClick = onOpenParentalControl
                )
                SettingsItem(
                    title = stringResource(R.string.settings_app_info),
                    subtitle = stringResource(R.string.settings_app_version),
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }
            
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
        }
    }
}

@Composable
fun SettingsSyncIntervalItem(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    val options = listOf(6, 12, 24, 48)
    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }

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
fun SettingsPlayerEngineItem(
    currentEngine: PlayerEngine,
    onEngineChange: (PlayerEngine) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val subtitle = when (currentEngine) {
        PlayerEngine.MEDIA3 -> "Media3 (integrado)"
        PlayerEngine.MPV -> "MPV (embebido)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.OndemandVideo,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(MaterialTheme.dimens.iconMedium)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Reproductor preferido",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Media3 (integrado)") },
                    onClick = {
                        onEngineChange(PlayerEngine.MEDIA3)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("MPV (embebido)") },
                    onClick = {
                        onEngineChange(PlayerEngine.MPV)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small)
        )
        content()
    }
}

@Composable
fun SettingsItem(
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
fun SettingsSwitchItem(
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

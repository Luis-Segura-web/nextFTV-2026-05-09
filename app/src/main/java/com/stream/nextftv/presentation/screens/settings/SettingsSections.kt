package com.stream.nextftv.presentation.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.stream.nextftv.R
import com.stream.nextftv.presentation.player.PlaybackCacheMode
import com.stream.nextftv.presentation.player.PlaybackContentType
import com.stream.nextftv.presentation.theme.spacing
import java.util.Locale

@Composable
internal fun SettingsContent(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onOpenParentalControl: () -> Unit,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
    val activeProfile = viewModel.activeProfile
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showAccountInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsSection(title = stringResource(R.string.settings_section_account)) {
            SettingsItem(
                title = stringResource(R.string.settings_active_profile),
                subtitle = activeProfile?.name ?: stringResource(R.string.settings_unknown),
                icon = Icons.Default.Person,
                onClick = { showAccountInfoDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.settings_server),
                subtitle = activeProfile?.url ?: "---",
                icon = Icons.Default.Dns,
                onClick = { showAccountInfoDialog = true }
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

        SettingsDivider()

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
                onCheckedChange = { viewModel.onAutoPlayNextChange(it) }
            )
            SettingsPlaybackCacheItem(
                title = "Cache para peliculas",
                subtitle = "Proxy cache es la opcion recomendada para VOD y datos moviles",
                selectedMode = viewModel.moviesCacheMode,
                options = listOf(PlaybackCacheMode.DISABLED, PlaybackCacheMode.PROXY, PlaybackCacheMode.EXO),
                onModeSelected = { viewModel.onPlaybackCacheModeChange(PlaybackContentType.MOVIE, it) }
            )
            SettingsPlaybackCacheItem(
                title = "Cache para series",
                subtitle = "Proxy cache es la opcion recomendada para episodios y reanudacion",
                selectedMode = viewModel.seriesCacheMode,
                options = listOf(PlaybackCacheMode.DISABLED, PlaybackCacheMode.PROXY, PlaybackCacheMode.EXO),
                onModeSelected = { viewModel.onPlaybackCacheModeChange(PlaybackContentType.SERIES, it) }
            )
        }

        SettingsDivider()

        SettingsMaintenanceSection(viewModel = viewModel)

        SettingsDivider()

        SettingsSection(title = stringResource(R.string.settings_section_general)) {
            SettingsSwitchItem(
                title = "Integracion con TMDB",
                subtitle = "Obtener posters y detalles enriquecidos",
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
                subtitle = "Acceso protegido por NIP de 4 digitos",
                icon = Icons.Default.Lock,
                onClick = onOpenParentalControl
            )
            SettingsItem(
                title = stringResource(R.string.settings_app_info),
                subtitle = stringResource(R.string.settings_app_version),
                icon = Icons.Default.Info,
                onClick = { showAppInfoDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
    }

    if (showAppInfoDialog) {
        AppInfoDialog(
            viewModel = viewModel,
            onDismiss = { showAppInfoDialog = false }
        )
    }

    if (showAccountInfoDialog) {
        AccountInfoDialog(
            viewModel = viewModel,
            onDismiss = { showAccountInfoDialog = false }
        )
    }
}

@Composable
private fun SettingsMaintenanceSection(viewModel: SettingsViewModel) {
    var showClearRecentsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SettingsSection(title = "Mantenimiento") {
        SettingsItem(
            title = "Limpiar Recientes",
            subtitle = "Borrar el historial de canales y peliculas",
            icon = Icons.Default.History,
            onClick = { showClearRecentsDialog = true }
        )
        SettingsItem(
            title = "Limpiar Cache",
            subtitle = "Eliminar imagenes, detalles y cache de reproduccion",
            icon = Icons.Default.DeleteSweep,
            onClick = { showClearCacheDialog = true }
        )
    }

    if (showClearRecentsDialog) {
        AlertDialog(
            onDismissRequest = { showClearRecentsDialog = false },
            title = { Text("¿Limpiar Recientes?") },
            text = { Text("Se borrara el historial de reproduccion de todos los modulos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onClearRecents()
                        showClearRecentsDialog = false
                    }
                ) { Text("LIMPIAR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearRecentsDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("¿Limpiar Cache?") },
            text = { Text("Se eliminaran imagenes, detalles guardados y cache temporal de reproduccion.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onClearCache(context)
                        showClearCacheDialog = false
                    }
                ) { Text("LIMPIAR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("CANCELAR") }
            }
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}

@Composable
private fun AppInfoDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeProfile = viewModel.activeProfile
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName.orEmpty()
    val versionCode = packageInfo.longVersionCode
    val appInfoText = remember(
        activeProfile,
        viewModel.isPipEnabled,
        viewModel.backgroundPlaybackEnabled,
        viewModel.autoPlayNext,
        viewModel.isTmdbEnabled,
        viewModel.syncInterval,
        viewModel.moviesCacheMode,
        viewModel.seriesCacheMode
    ) {
        buildString {
            appendLine("Aplicacion")
            appendLine("Nombre: ${context.getString(R.string.app_name)}")
            appendLine("Version: $versionName")
            appendLine("Build: $versionCode")
            appendLine("Paquete: ${context.packageName}")
            appendLine()
            appendLine("Perfil")
            appendLine("Activo: ${activeProfile?.name ?: "Ninguno"}")
            appendLine("Servidor: ${activeProfile?.url ?: "---"}")
            appendLine("Zona horaria servidor: ${activeProfile?.serverTimezone ?: "---"}")
            appendLine()
            appendLine("Reproduccion")
            appendLine("PiP: ${enabledLabel(viewModel.isPipEnabled)}")
            appendLine("Segundo plano: ${enabledLabel(viewModel.backgroundPlaybackEnabled)}")
            appendLine("Autoplay siguiente: ${enabledLabel(viewModel.autoPlayNext)}")
            appendLine("TMDB: ${enabledLabel(viewModel.isTmdbEnabled)}")
            appendLine("Sync cada: ${viewModel.syncInterval}h")
            appendLine("Cache Peliculas: ${viewModel.moviesCacheMode.displayName()}")
            appendLine("Cache Series: ${viewModel.seriesCacheMode.displayName()}")
            appendLine()
            appendLine("Dispositivo")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Marca: ${Build.MANUFACTURER}")
            appendLine("Modelo: ${Build.MODEL}")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "---"}")
            appendLine("Espacio libre app: ${formatBytes(context.filesDir.usableSpace)}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Info") },
        text = {
            Text(
                text = appInfoText,
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("app_info", appInfoText))
                }
            ) {
                Text("COPIAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR")
            }
        }
    )
}

@Composable
private fun AccountInfoDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val activeProfile = viewModel.activeProfile
    val accountInfoText = remember(activeProfile) {
        buildString {
            appendLine("Cuenta")
            appendLine("Perfil: ${activeProfile?.name ?: "Ninguno"}")
            appendLine("Usuario: ${activeProfile?.username ?: "---"}")
            appendLine("Estado: ${activeProfile?.accountStatus ?: "---"}")
            appendLine("Vencimiento: ${activeProfile?.expirationDate ?: "Ilimitada"}")
            appendLine(
                "Conexiones: ${
                    when {
                        activeProfile?.activeConnections != null && activeProfile.maxConnections != null ->
                            "${activeProfile.activeConnections}/${activeProfile.maxConnections}"
                        activeProfile?.maxConnections != null -> "0/${activeProfile.maxConnections}"
                        else -> "---"
                    }
                }"
            )
            appendLine()
            appendLine("Servidor")
            appendLine("URL: ${activeProfile?.url ?: "---"}")
            appendLine("Origen: ${activeProfile?.sourceType ?: "---"}")
            appendLine("Zona horaria: ${activeProfile?.serverTimezone ?: "---"}")
            appendLine(
                "Formatos permitidos: ${
                    activeProfile?.allowedOutputFormats
                        ?.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ")
                        ?: "---"
                }"
            )
            appendLine("EPG: ${activeProfile?.epgUrl ?: "---"}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informacion de la cuenta") },
        text = {
            Column {
                Text(
                    text = accountInfoText,
                    style = MaterialTheme.typography.bodySmall
                )
                viewModel.accountRefreshMessage?.let { message ->
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.refreshAccountInfo() },
                enabled = !viewModel.isRefreshingAccount && activeProfile != null
            ) {
                Text(if (viewModel.isRefreshingAccount) "ACTUALIZANDO..." else "ACTUALIZAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR")
            }
        }
    )
}

private fun enabledLabel(value: Boolean): String = if (value) "Activado" else "Desactivado"

private fun PlaybackCacheMode.displayName(): String = when (this) {
    PlaybackCacheMode.DISABLED -> "Sin cache"
    PlaybackCacheMode.PROXY -> "Proxy cache"
    PlaybackCacheMode.EXO -> "Exo cache"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L ->
        String.format(Locale.US, "%.2f GB", bytes.toDouble() / (1024L * 1024L * 1024L))
    bytes >= 1024L * 1024L ->
        String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024L * 1024L))
    bytes >= 1024L ->
        "${bytes / 1024L} KB"
    else -> "$bytes B"
}

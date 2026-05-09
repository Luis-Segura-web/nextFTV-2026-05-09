package com.stream.nextftv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stream.nextftv.R
import com.stream.nextftv.presentation.screens.home.components.DownloadModuleCard
import com.stream.nextftv.presentation.screens.home.components.PremiumModuleCard
import com.stream.nextftv.presentation.theme.SuccessGreen
import com.stream.nextftv.presentation.theme.SuccessGreenLight
import com.stream.nextftv.presentation.theme.spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(
    activeProfileName: String?,
    isGlobalSyncing: Boolean,
    onSyncAll: () -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.home_app_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                activeProfileName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSyncAll,
                enabled = !isGlobalSyncing
            ) {
                if (isGlobalSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = stringResource(R.string.home_sync_all)
                    )
                }
            }
            IconButton(
                onClick = onOpenSettings,
                enabled = !isGlobalSyncing
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.home_settings)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
internal fun HomeBottomBar(
    expirationDate: String?,
    accountStatus: String?,
    isAccountActive: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Vencimiento:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = expirationDate ?: "Ilimitada",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAccountActive) accountStatusColor() else Color.Red,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Estado:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                val statusText = accountStatus ?: "ACTIVO"
                Text(
                    text = statusText.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAccountActive) accountStatusColor() else Color.Red,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
internal fun HomeContent(
    backgroundBrush: androidx.compose.ui.graphics.Brush?,
    isAccountActive: Boolean,
    isGlobalSyncing: Boolean,
    counts: Map<String, Int>,
    syncStates: Map<String, SyncState>,
    syncProgress: Map<String, Int>,
    syncMessages: Map<String, String?>,
    lastSync: Map<String, String>,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onSyncModule: (String) -> Unit,
    onNavigateToSection: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush) else Modifier
            )
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Box(modifier = Modifier.weight(0.5f)) {
                val isLiveSyncing = syncStates["live"] == SyncState.SYNCING
                val liveEnabled = isAccountActive && !isGlobalSyncing && !isLiveSyncing && (counts["live"] ?: 0) > 0

                PremiumModuleCard(
                    title = stringResource(R.string.module_live_title),
                    count = counts["live"],
                    lastSync = lastSync["live"] ?: stringResource(R.string.module_sync_never),
                    icon = Icons.Default.LiveTv,
                    accentColor = Color(0xFFE57373),
                    syncState = syncStates["live"] ?: SyncState.IDLE,
                    syncProgress = syncProgress["live"] ?: 0,
                    syncMessage = syncMessages["live"],
                    enabled = liveEnabled,
                    onSyncClick = { if (!isGlobalSyncing) onSyncModule("live") },
                    onCardClick = { onNavigateToSection("live") },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                val isMoviesSyncing = syncStates["movies"] == SyncState.SYNCING
                val moviesEnabled = isAccountActive && !isGlobalSyncing && !isMoviesSyncing && (counts["movies"] ?: 0) > 0

                PremiumModuleCard(
                    title = stringResource(R.string.module_movies_title),
                    count = counts["movies"],
                    lastSync = lastSync["movies"] ?: stringResource(R.string.module_sync_never),
                    icon = Icons.Default.Movie,
                    accentColor = Color(0xFF64B5F6),
                    syncState = syncStates["movies"] ?: SyncState.IDLE,
                    syncProgress = syncProgress["movies"] ?: 0,
                    syncMessage = syncMessages["movies"],
                    enabled = moviesEnabled,
                    onSyncClick = { if (!isGlobalSyncing) onSyncModule("movies") },
                    onCardClick = { onNavigateToSection("movies") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                val isSeriesSyncing = syncStates["series"] == SyncState.SYNCING
                val seriesEnabled = isAccountActive && !isGlobalSyncing && !isSeriesSyncing && (counts["series"] ?: 0) > 0

                PremiumModuleCard(
                    title = stringResource(R.string.module_series_title),
                    count = counts["series"],
                    lastSync = lastSync["series"] ?: stringResource(R.string.module_sync_never),
                    icon = Icons.Default.Tv,
                    accentColor = Color(0xFF81C784),
                    syncState = syncStates["series"] ?: SyncState.IDLE,
                    syncProgress = syncProgress["series"] ?: 0,
                    syncMessage = syncMessages["series"],
                    enabled = seriesEnabled,
                    onSyncClick = { if (!isGlobalSyncing) onSyncModule("series") },
                    onCardClick = { onNavigateToSection("series") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Column(
                modifier = Modifier.weight(0.2f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DownloadModuleCard(
                        title = "Búsqueda por Artistas",
                        count = null,
                        icon = Icons.Default.Person,
                        accentColor = Color(0xFFBA68C8),
                        onCardClick = { onNavigateToSection("artist_search") },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    DownloadModuleCard(
                        title = "Películas y Series Descargadas",
                        count = null,
                        icon = Icons.Default.Download,
                        accentColor = Color(0xFFFFD54F),
                        onCardClick = { onNavigateToSection("downloads") },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun accountStatusColor(): Color {
    return if (androidx.compose.foundation.isSystemInDarkTheme()) SuccessGreenLight else SuccessGreen
}

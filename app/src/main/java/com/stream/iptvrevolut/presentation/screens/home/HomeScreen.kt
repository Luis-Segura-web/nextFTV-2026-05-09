package com.stream.iptvrevolut.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stream.iptvrevolut.R
import com.stream.iptvrevolut.presentation.screens.home.components.DownloadModuleCard
import com.stream.iptvrevolut.presentation.screens.home.components.PremiumModuleCard
import com.stream.iptvrevolut.presentation.theme.SuccessGreen
import com.stream.iptvrevolut.presentation.theme.SuccessGreenLight
import com.stream.iptvrevolut.presentation.theme.dimens
import com.stream.iptvrevolut.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSection: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val activeProfile = viewModel.activeProfile
    val isGlobalSyncing = viewModel.isGlobalSyncing
    val counts = viewModel.counts

    val isAccountActive = activeProfile?.accountStatus?.equals("Active", ignoreCase = true) == true

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_app_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        activeProfile?.name?.let {
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
                        onClick = { viewModel.syncAll() },
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
                        onClick = onNavigateToSettings,
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
        },
        bottomBar = {
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
                            text = activeProfile?.expirationDate ?: "Ilimitada",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAccountActive) (if (isSystemInDarkTheme()) SuccessGreenLight else SuccessGreen) else Color.Red,
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
                        val statusText = activeProfile?.accountStatus ?: "ACTIVO"
                        Text(
                            text = statusText.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAccountActive) (if (isSystemInDarkTheme()) SuccessGreenLight else SuccessGreen) else Color.Red,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                // SECCIÓN 1: TV EN VIVO (Bloqueo si está sincronizando CUALQUIER cosa o si la cuenta no es activa)
                Box(modifier = Modifier.weight(0.5f)) {
                    val isLiveSyncing = viewModel.syncStates["live"] == SyncState.SYNCING
                    val liveEnabled = isAccountActive && !isGlobalSyncing && !isLiveSyncing && (counts["live"] ?: 0) > 0
                    
                    PremiumModuleCard(
                        title = stringResource(R.string.module_live_title),
                        count = counts["live"],
                        lastSync = viewModel.lastSync["live"] ?: stringResource(R.string.module_sync_never),
                        icon = Icons.Default.LiveTv,
                        accentColor = Color(0xFFE57373),
                        syncState = viewModel.syncStates["live"] ?: SyncState.IDLE,
                        enabled = liveEnabled,
                        onSyncClick = { if (!isGlobalSyncing) viewModel.syncModule("live") },
                        onCardClick = { onNavigateToSection("live") },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // SECCIÓN 2: PELÍCULAS Y SERIES
                Row(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    val isMoviesSyncing = viewModel.syncStates["movies"] == SyncState.SYNCING
                    val moviesEnabled = isAccountActive && !isGlobalSyncing && !isMoviesSyncing && (counts["movies"] ?: 0) > 0
                    
                    PremiumModuleCard(
                        title = stringResource(R.string.module_movies_title),
                        count = counts["movies"],
                        lastSync = viewModel.lastSync["movies"] ?: stringResource(R.string.module_sync_never),
                        icon = Icons.Default.Movie,
                        accentColor = Color(0xFF64B5F6),
                        syncState = viewModel.syncStates["movies"] ?: SyncState.IDLE,
                        enabled = moviesEnabled,
                        onSyncClick = { if (!isGlobalSyncing) viewModel.syncModule("movies") },
                        onCardClick = { onNavigateToSection("movies") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    val isSeriesSyncing = viewModel.syncStates["series"] == SyncState.SYNCING
                    val seriesEnabled = isAccountActive && !isGlobalSyncing && !isSeriesSyncing && (counts["series"] ?: 0) > 0
                    
                    PremiumModuleCard(
                        title = stringResource(R.string.module_series_title),
                        count = counts["series"],
                        lastSync = viewModel.lastSync["series"] ?: stringResource(R.string.module_sync_never),
                        icon = Icons.Default.Tv,
                        accentColor = Color(0xFF81C784),
                        syncState = viewModel.syncStates["series"] ?: SyncState.IDLE,
                        enabled = seriesEnabled,
                        onSyncClick = { if (!isGlobalSyncing) viewModel.syncModule("series") },
                        onCardClick = { onNavigateToSection("series") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // SECCIÓN 3: DESCARGAS (Siempre habilitada si la cuenta es activa, ya que es contenido local)
                Box(modifier = Modifier.weight(0.1f)) {
                    DownloadModuleCard(
                        title = stringResource(R.string.module_downloads_title),
                        count = counts["downloads"],
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

package com.stream.nextftv.presentation.screens.downloads

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stream.nextftv.presentation.theme.StreamingBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayMovie: (Int) -> Unit,
    onPlayEpisode: (Int, String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val movieDownloads by viewModel.movieDownloads.collectAsState()
    val seriesDownloads by viewModel.seriesDownloads.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Colores Cinematic Glass según el tema
    val backgroundColor = if (isDark) Color(0xFF0A0E14) else Color(0xFFF5F7FA)
    val cardBackground = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val textColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    viewModel.downloadToDelete?.let { download ->
        DownloadDeleteDialog(
            download = download,
            isDark = isDark,
            textColor = textColor,
            onDismiss = { viewModel.downloadToDelete = null },
            onConfirm = { viewModel.removeDownload(download) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "BIBLIOTECA OFFLINE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = textColor
                    )
                )
            }
        ) { innerPadding ->
            DownloadsContent(
                movieDownloads = movieDownloads,
                seriesDownloads = seriesDownloads,
                selectedTab = viewModel.selectedTab,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                textColor = textColor,
                innerPadding = innerPadding,
                onSelectTab = { viewModel.selectedTab = it },
                onPlayMovie = onPlayMovie,
                onPlayEpisode = onPlayEpisode,
                onDelete = { viewModel.downloadToDelete = it },
                onPause = viewModel::pauseDownload,
                onResume = viewModel::resumeDownload
            )
        }
    }
}

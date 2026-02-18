package com.stream.iptvrevolut.presentation.screens.downloads

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity
import com.stream.iptvrevolut.presentation.theme.spacing

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

    // Diálogo de Confirmación Estilizado
    viewModel.downloadToDelete?.let { download ->
        AlertDialog(
            onDismissRequest = { viewModel.downloadToDelete = null },
            containerColor = if (isDark) Color(0xFF1C1F26) else Color.White,
            title = { Text("¿Eliminar descarga?", color = textColor, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Se borrará '${download.title}' permanentemente.", color = textColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.removeDownload(download) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.downloadToDelete = null }) { 
                    Text("Cancelar", color = if (isDark) Color.Gray else Color.Black) 
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "BIBLIOTECA OFFLINE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = textColor
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            // TabRow Cinematic
            PrimaryTabRow(
                selectedTabIndex = viewModel.selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(viewModel.selectedTab),
                        width = 40.dp,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = viewModel.selectedTab == 0,
                    onClick = { viewModel.selectedTab = 0 },
                    text = { Text("PELÍCULAS", fontWeight = if (viewModel.selectedTab == 0) FontWeight.Black else FontWeight.Normal) }
                )
                Tab(
                    selected = viewModel.selectedTab == 1,
                    onClick = { viewModel.selectedTab = 1 },
                    text = { Text("SERIES", fontWeight = if (viewModel.selectedTab == 1) FontWeight.Black else FontWeight.Normal) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.selectedTab == 0) {
                MoviesDownloadList(movieDownloads, cardBackground, cardBorder, textColor, onPlayMovie, { viewModel.downloadToDelete = it }, viewModel::pauseDownload, viewModel::resumeDownload)
            } else {
                SeriesDownloadList(seriesDownloads, cardBackground, cardBorder, textColor, onPlayEpisode, { viewModel.downloadToDelete = it }, viewModel::pauseDownload, viewModel::resumeDownload)
            }
        }
    }
}

@Composable
fun MovieDownloadItem(
    download: DownloadEntity, 
    cardBg: Color, 
    cardBorder: Color, 
    textColor: Color,
    onPlay: () -> Unit, 
    onDelete: () -> Unit, 
    onPause: (String) -> Unit, 
    onResume: (DownloadEntity) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 4.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Póster con bordes suavizados
            AsyncImage(
                model = download.posterUrl, 
                contentDescription = null, 
                modifier = Modifier.width(70.dp).aspectRatio(0.7f).clip(RoundedCornerShape(14.dp)), 
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title, 
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold, 
                    maxLines = 4, 
                    overflow = TextOverflow.Ellipsis, 
                    lineHeight = 18.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when(download.status) {
                        "completed" -> Icons.Default.CheckCircle
                        "paused" -> Icons.Default.PauseCircleFilled
                        "error" -> Icons.Default.Error
                        else -> Icons.Default.Downloading
                    }
                    val statusColor = when(download.status) {
                        "completed" -> Color(0xFF4CAF50)
                        "paused" -> Color.Gray
                        "error" -> Color.Red
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Icon(statusIcon, null, modifier = Modifier.size(14.dp), tint = statusColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    val speedText = if (download.status == "downloading") " • ${formatSpeed(download.downloadSpeed)}" else ""
                    Text(
                        text = if (download.status == "completed") "Listo para ver" else "${(download.progress * 100).toInt()}%$speedText",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
                
                if (download.status != "completed") {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { download.progress }, 
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), 
                        color = if (download.status == "error") Color.Red else MaterialTheme.colorScheme.primary,
                        trackColor = textColor.copy(alpha = 0.1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            DownloadControls(download, onPlay, onDelete, onPause, onResume, textColor)
        }
    }
}

@Composable
fun SeriesGroupItem(
    seriesId: Int, 
    episodes: List<DownloadEntity>, 
    cardBg: Color, 
    cardBorder: Color, 
    textColor: Color,
    onPlayEpisode: (Int, String) -> Unit, 
    onDeleteEpisode: (DownloadEntity) -> Unit, 
    onPause: (String) -> Unit, 
    onResume: (DownloadEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val firstEpisode = episodes.first()
    val sortedEpisodes = remember(episodes) {
        episodes.sortedWith(compareBy<DownloadEntity> { com.stream.iptvrevolut.data.utils.StringUtils.naturalSort(it.title) })
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier.clickable { expanded = !expanded }.padding(12.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = firstEpisode.posterUrl, 
                    contentDescription = null, 
                    modifier = Modifier.width(70.dp).aspectRatio(0.7f).clip(RoundedCornerShape(14.dp)), 
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = firstEpisode.parentName ?: "Serie", 
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold, 
                        maxLines = 2, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${episodes.size} episodios guardados", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = textColor.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.7f)
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = cardBorder)
                    sortedEpisodes.forEach { episode ->
                        EpisodeDownloadRow(episode, seriesId, textColor, onPlayEpisode, onDeleteEpisode, onPause, onResume)
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeDownloadRow(
    episode: DownloadEntity, 
    seriesId: Int, 
    textColor: Color,
    onPlay: (Int, String) -> Unit, 
    onDelete: (DownloadEntity) -> Unit, 
    onPause: (String) -> Unit, 
    onResume: (DownloadEntity) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title, 
                style = MaterialTheme.typography.bodyMedium, 
                color = textColor,
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis
            )
            if (episode.status != "completed") {
                val speed = if (episode.status == "downloading") " • ${formatSpeed(episode.downloadSpeed)}" else ""
                Text(
                    text = "${(episode.progress * 100).toInt()}%$speed", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { episode.progress }, 
                    modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).clip(CircleShape), 
                    color = if (episode.status == "error") Color.Red else MaterialTheme.colorScheme.primary
                )
            }
        }
        DownloadControls(episode, { onPlay(seriesId, episode.streamId.toString()) }, { onDelete(episode) }, onPause, onResume, textColor, iconSize = 20.dp)
    }
}

@Composable
fun DownloadControls(
    download: DownloadEntity, 
    onPlay: () -> Unit, 
    onDelete: () -> Unit, 
    onPause: (String) -> Unit, 
    onResume: (DownloadEntity) -> Unit,
    textColor: Color,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    Row {
        when (download.status) {
            "completed" -> IconButton(onClick = onPlay) { 
                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize)) 
            }
            "downloading", "queued", "retrying" -> IconButton(onClick = { onPause(download.id) }) { 
                Icon(Icons.Default.Pause, null, tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(iconSize)) 
            }
            "paused", "error" -> IconButton(onClick = { onResume(download) }) { 
                Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize)) 
            }
        }
        IconButton(onClick = onDelete) { 
            Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(iconSize)) 
        }
    }
}

@Composable
fun MoviesDownloadList(downloads: List<DownloadEntity>, cardBg: Color, cardBorder: Color, textColor: Color, onPlay: (Int) -> Unit, onDelete: (DownloadEntity) -> Unit, onPause: (String) -> Unit, onResume: (DownloadEntity) -> Unit) {
    if (downloads.isEmpty()) EmptyState("Sin películas descargadas")
    else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(downloads, key = { it.id }) { MovieDownloadItem(it, cardBg, cardBorder, textColor, { onPlay(it.streamId) }, { onDelete(it) }, onPause, onResume) }
    }
}

@Composable
fun SeriesDownloadList(downloads: List<DownloadEntity>, cardBg: Color, cardBorder: Color, textColor: Color, onPlayEpisode: (Int, String) -> Unit, onDelete: (DownloadEntity) -> Unit, onPause: (String) -> Unit, onResume: (DownloadEntity) -> Unit) {
    val grouped = remember(downloads) { downloads.groupBy { it.parentId ?: 0 } }
    if (grouped.isEmpty()) EmptyState("Sin series descargadas")
    else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        grouped.forEach { (seriesId, episodes) -> item(key = seriesId) { SeriesGroupItem(seriesId, episodes, cardBg, cardBorder, textColor, onPlayEpisode, onDelete, onPause, onResume) } }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec.toDouble() / (1024 * 1024))
        bytesPerSec >= 1024 -> String.format("%d KB/s", bytesPerSec / 1024)
        else -> "$bytesPerSec B/s"
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
    }
}

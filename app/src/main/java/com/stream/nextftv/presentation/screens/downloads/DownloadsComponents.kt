package com.stream.nextftv.presentation.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import java.io.File

@Composable
internal fun MovieDownloadItem(
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
                    val statusIcon = downloadStatusIcon(download.status)
                    val statusColor = downloadStatusColor(download.status)

                    Icon(statusIcon, null, modifier = Modifier.size(14.dp), tint = statusColor)
                    Spacer(modifier = Modifier.width(6.dp))

                    val speedText = if (download.status == "downloading") {
                        " • ${formatSpeed(download.downloadSpeed)}"
                    } else {
                        ""
                    }
                    Text(
                        text = downloadStatusSummary(download, speedText),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor.copy(alpha = 0.95f)
                    )
                }

                if (download.status == "completed") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildCompletedFileMeta(download),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f)
                    )
                }

                if (download.status != "completed") {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = downloadStatusColor(download.status),
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
internal fun SeriesGroupItem(
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
        episodes.sortedWith(compareBy<DownloadEntity> {
            com.stream.nextftv.data.utils.StringUtils.naturalSort(it.title)
        })
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
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
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
                        EpisodeDownloadRow(
                            episode = episode,
                            seriesId = seriesId,
                            textColor = textColor,
                            onPlay = onPlayEpisode,
                            onDelete = onDeleteEpisode,
                            onPause = onPause,
                            onResume = onResume
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeDownloadRow(
    episode: DownloadEntity,
    seriesId: Int,
    textColor: Color,
    onPlay: (Int, String) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onPause: (String) -> Unit,
    onResume: (DownloadEntity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.status == "completed") {
                Text(
                    text = buildCompletedFileMeta(episode),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f)
                )
            } else {
                val speed = if (episode.status == "downloading") " • ${formatSpeed(episode.downloadSpeed)}" else ""
                Text(
                    text = downloadStatusSummary(episode, speed),
                    style = MaterialTheme.typography.labelSmall,
                    color = downloadStatusColor(episode.status)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { episode.progress },
                    modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).clip(CircleShape),
                    color = downloadStatusColor(episode.status)
                )
            }
        }
        DownloadControls(
            download = episode,
            onPlay = { onPlay(seriesId, episode.streamId.toString()) },
            onDelete = { onDelete(episode) },
            onPause = onPause,
            onResume = onResume,
            textColor = textColor,
            iconSize = 20.dp
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes <= 0L -> "Tamano desconocido"
    bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes.toDouble() / (1024L * 1024L * 1024L))
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes.toDouble() / (1024L * 1024L))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

private fun buildCompletedFileMeta(download: DownloadEntity): String {
    val sizeText = formatFileSize(download.downloadedSize.takeIf { it > 0L } ?: download.totalSize)
    val formatText = download.filePath.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.uppercase()
    return if (formatText != null) "$sizeText • $formatText" else sizeText
}

@Composable
private fun DownloadControls(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (DownloadEntity) -> Unit,
    textColor: Color,
    iconSize: Dp = 24.dp
) {
    val canPlayCompleted = remember(download.filePath, download.status) {
        download.status == "completed" &&
            File(download.filePath).exists() &&
            File(download.filePath).length() > 0L
    }

    Row {
        when (download.status) {
            "completed" -> {
                if (canPlayCompleted) {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
                    }
                }
            }
            "downloading", "queued", "retrying" -> {
                IconButton(onClick = { onPause(download.id) }) {
                    Icon(Icons.Default.Pause, null, tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(iconSize))
                }
            }
            "paused", "error" -> {
                IconButton(onClick = { onResume(download) }) {
                    Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
                }
            }
            "cancelled" -> Unit
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
        }
    }
}

private fun downloadStatusIcon(status: String): ImageVector = when (status) {
    "completed" -> Icons.Default.CheckCircle
    "paused" -> Icons.Default.PauseCircleFilled
    "error" -> Icons.Default.Error
    "retrying" -> Icons.Default.Refresh
    "queued" -> Icons.Default.HourglassTop
    "cancelled" -> Icons.Default.DeleteOutline
    else -> Icons.Default.Downloading
}

@Composable
private fun downloadStatusColor(status: String): Color = when (status) {
    "completed" -> Color(0xFF43A047)
    "paused" -> MaterialTheme.colorScheme.secondary
    "error" -> MaterialTheme.colorScheme.error
    "retrying" -> Color(0xFFE67E22)
    "queued" -> MaterialTheme.colorScheme.tertiary
    "cancelled" -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.primary
}

private fun downloadStatusSummary(download: DownloadEntity, speedText: String = ""): String = when (download.status) {
    "completed" -> "Listo para ver"
    "paused" -> "${(download.progress * 100).toInt()}% • En pausa"
    "retrying" -> "${(download.progress * 100).toInt()}% • Reintentando"
    "queued" -> "${(download.progress * 100).toInt()}% • En cola"
    "error" -> "${(download.progress * 100).toInt()}% • Error"
    "cancelled" -> "Cancelada"
    else -> "${(download.progress * 100).toInt()}%$speedText"
}

@Composable
internal fun MoviesDownloadList(
    downloads: List<DownloadEntity>,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    onPlay: (Int) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onPause: (String) -> Unit,
    onResume: (DownloadEntity) -> Unit
) {
    if (downloads.isEmpty()) {
        EmptyState("Sin peliculas descargadas")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(downloads, key = { it.id }) {
                MovieDownloadItem(it, cardBg, cardBorder, textColor, { onPlay(it.streamId) }, { onDelete(it) }, onPause, onResume)
            }
        }
    }
}

@Composable
internal fun SeriesDownloadList(
    downloads: List<DownloadEntity>,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    onPlayEpisode: (Int, String) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onPause: (String) -> Unit,
    onResume: (DownloadEntity) -> Unit
) {
    val grouped = remember(downloads) { downloads.groupBy { it.parentId ?: 0 } }
    if (grouped.isEmpty()) {
        EmptyState("Sin series descargadas")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            grouped.forEach { (seriesId, episodes) ->
                item(key = seriesId) {
                    SeriesGroupItem(seriesId, episodes, cardBg, cardBorder, textColor, onPlayEpisode, onDelete, onPause, onResume)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec.toDouble() / (1024 * 1024))
        bytesPerSec >= 1024 -> String.format("%d KB/s", bytesPerSec / 1024)
        else -> "$bytesPerSec B/s"
    }
}

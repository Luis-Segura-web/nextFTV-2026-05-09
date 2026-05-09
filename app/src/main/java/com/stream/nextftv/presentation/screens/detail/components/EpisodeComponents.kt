package com.stream.nextftv.presentation.screens.detail.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.data.remote.SeriesEpisodeDto
import com.stream.nextftv.presentation.screens.seriesdetail.buildEpisodeDisplayTitle
import com.stream.nextftv.presentation.theme.shimmerLoading
import com.stream.nextftv.presentation.theme.spacing

@Composable
fun EpisodeItem(
    episode: SeriesEpisodeDto,
    isPlaying: Boolean,
    progressMs: Long = 0L,
    videoDurationMs: Long = 0L,
    download: DownloadEntity?,
    seriesName: String? = null,
    preferredTitle: String? = null,
    fallbackUrl: String? = null,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val resolvedDurationMs = remember(videoDurationMs) { videoDurationMs.coerceAtLeast(0L) }
    val progressFraction = remember(progressMs, resolvedDurationMs) {
        when {
            progressMs <= 0L -> 0f
            resolvedDurationMs > 0L -> (progressMs.toFloat() / resolvedDurationMs.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
    }
    val hasProgress = progressMs > 0L
    val isCompleted = resolvedDurationMs > 0L && progressFraction >= 0.98f
    val episodeInfo = episode.info
    val episodeDisplayTitle = remember(
        episode.id,
        episode.title,
        episode.season,
        episode.episodeNum,
        seriesName,
        preferredTitle
    ) {
        buildEpisodeDisplayTitle(
            episode = episode,
            seriesName = seriesName,
            preferredTitle = preferredTitle
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.small,
        border = if (isPlaying) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isEpThumbLoading by remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(16 / 9f)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerLoading(isEpThumbLoading)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val episodeImage = episodeInfo?.movieImage
                val imageUrl = if (episodeImage.isNullOrBlank()) fallbackUrl else episodeImage

                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { isEpThumbLoading = false },
                    onError = { isEpThumbLoading = false }
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                if (hasProgress) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(
                                    if (isCompleted) Color(0xFF2ECC71) else Color(0xFF00C2FF)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episodeDisplayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                val progressAndDurationText = when {
                    isCompleted && resolvedDurationMs > 0L ->
                        "Completado · ${formatElapsed(resolvedDurationMs)}"
                    progressMs > 0L && resolvedDurationMs > 0L ->
                        "${formatElapsed(progressMs)}/${formatElapsed(resolvedDurationMs)}"
                    progressMs > 0L -> formatElapsed(progressMs)
                    resolvedDurationMs > 0L -> formatElapsed(resolvedDurationMs)
                    else -> null
                }

                progressAndDurationText?.let { playbackText ->
                    Text(
                        text = playbackText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f)
                    )
                }
            }

            CircularDownloadButton(
                download = download,
                onClick = onDownload,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

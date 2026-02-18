package com.stream.iptvrevolut.presentation.components.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity

@Composable
fun CircularDownloadButton(
    download: DownloadEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(54.dp)
) {
    Surface(
        onClick = if (download?.status == "completed") ({}) else onClick,
        modifier = modifier,
        color = when (download?.status) {
            "completed" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            "error" -> Color.Red.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (download?.status) {
                "downloading", "retrying" -> {
                    val displayProgress = download.progress.coerceAtLeast(0.01f)
                    CircularProgressIndicator(
                        progress = { displayProgress },
                        modifier = Modifier.fillMaxSize(0.8f),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${(download.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                "queued" -> {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(0.8f),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        strokeWidth = 2.dp
                    )
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "En espera",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                "completed" -> {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
                "paused" -> {
                    Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                "error" -> {
                    Icon(Icons.Default.Error, null, tint = Color.Red)
                }
                else -> {
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

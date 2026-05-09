package com.stream.nextftv.presentation.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stream.nextftv.presentation.screens.home.SyncState
import com.stream.nextftv.presentation.theme.spacing

@Composable
fun PremiumModuleCard(
    title: String,
    count: Int?,
    lastSync: String,
    icon: ImageVector,
    accentColor: Color,
    syncState: SyncState,
    syncProgress: Int = 0,
    syncMessage: String? = null,
    enabled: Boolean = true,
    onSyncClick: (() -> Unit)? = null,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualEnabled = enabled || syncState != SyncState.IDLE
    val cardColor = if (visualEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }
    val contentAlpha = if (visualEnabled) 1f else 0.58f
    val progressValue = syncProgress.coerceIn(0, 100)
    val progressFraction = when (syncState) {
        SyncState.SUCCESS -> 0f
        SyncState.ERROR -> progressValue / 100f
        SyncState.SYNCING -> progressValue / 100f
        SyncState.IDLE -> 0f
    }
    val footerText = when (syncState) {
        SyncState.SYNCING -> syncMessage ?: "Sincrosando..."
        SyncState.SUCCESS -> "Sinc: $lastSync"
        SyncState.ERROR -> syncMessage ?: "Error. Reintentar"
        SyncState.IDLE -> "Sinc: $lastSync"
    }
    val footerColor = when (syncState) {
        SyncState.SYNCING -> accentColor.copy(alpha = 0.94f)
        SyncState.SUCCESS -> accentColor.copy(alpha = if (enabled) 0.9f else 0.5f)
        SyncState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.94f)
        SyncState.IDLE -> accentColor.copy(alpha = if (enabled) 0.9f else 0.5f)
    }
    val pendingBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0x70000000),
            Color(0xA6000000)
        )
    )
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = if (visualEnabled) 0.14f else 0.06f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (visualEnabled) 0.34f else 0.18f)
        )
    )
    val statusBorderColor = when (syncState) {
        SyncState.SYNCING -> accentColor.copy(alpha = 0.7f)
        SyncState.SUCCESS -> accentColor.copy(alpha = if (visualEnabled) 0.34f else 0.18f)
        SyncState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        SyncState.IDLE -> accentColor.copy(alpha = if (visualEnabled) 0.34f else 0.18f)
    }
    val indicatorTextColor = when (syncState) {
        SyncState.SYNCING -> MaterialTheme.colorScheme.onSurface
        else -> accentColor
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 8.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.2.dp, statusBorderColor)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardHeight = maxHeight
            val iconContainerSize = cardHeight * 0.34f
            val iconSize = cardHeight * 0.18f
            val titleFontSize = (cardHeight.value * 0.11f).sp
            val remainingFraction = (1f - progressFraction).coerceIn(0f, 1f)
            if ((syncState == SyncState.SYNCING || syncState == SyncState.ERROR) && remainingFraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(remainingFraction)
                        .background(pendingBrush)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor.copy(alpha = if (visualEnabled) 0.92f else 0.38f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 42.dp)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
                    modifier = Modifier.size(iconContainerSize)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (syncState == SyncState.SYNCING) {
                            Box(
                                modifier = Modifier.size(iconContainerSize * 0.68f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { progressFraction.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxSize(),
                                    color = accentColor,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = "$progressValue%",
                                    color = indicatorTextColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = (iconContainerSize.value * 0.13f).sp,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = titleFontSize * 1.15f,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }

            if (count != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    color = accentColor, // Fondo sólido del color de acento
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (onSyncClick != null) {
                val syncButtonEnabled = enabled && syncState != SyncState.SYNCING
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(34.dp)
                        .clickable(enabled = syncButtonEnabled) { onSyncClick() },
                    color = footerColor
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = footerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

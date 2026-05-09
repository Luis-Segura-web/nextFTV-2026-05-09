package com.stream.nextftv.presentation.screens.downloads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stream.nextftv.data.local.entity.download.DownloadEntity

@Composable
internal fun DownloadDeleteDialog(
    download: DownloadEntity,
    isDark: Boolean,
    textColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1F26) else Color.White,
        title = { Text("¿Eliminar descarga?", color = textColor, fontWeight = FontWeight.ExtraBold) },
        text = { Text("Se borrara '${download.title}' permanentemente.", color = textColor.copy(alpha = 0.7f)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) { Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = if (isDark) Color.Gray else Color.Black)
            }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    )
}

@Composable
internal fun DownloadsContent(
    movieDownloads: List<DownloadEntity>,
    seriesDownloads: List<DownloadEntity>,
    selectedTab: Int,
    cardBackground: Color,
    cardBorder: Color,
    textColor: Color,
    innerPadding: PaddingValues,
    onSelectTab: (Int) -> Unit,
    onPlayMovie: (Int) -> Unit,
    onPlayEpisode: (Int, String) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onPause: (String) -> Unit,
    onResume: (DownloadEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        DownloadsTabRow(
            selectedTab = selectedTab,
            onSelectTab = onSelectTab
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            MoviesDownloadList(
                downloads = movieDownloads,
                cardBg = cardBackground,
                cardBorder = cardBorder,
                textColor = textColor,
                onPlay = onPlayMovie,
                onDelete = onDelete,
                onPause = onPause,
                onResume = onResume
            )
        } else {
            SeriesDownloadList(
                downloads = seriesDownloads,
                cardBg = cardBackground,
                cardBorder = cardBorder,
                textColor = textColor,
                onPlayEpisode = onPlayEpisode,
                onDelete = onDelete,
                onPause = onPause,
                onResume = onResume
            )
        }
    }
}

@Composable
private fun DownloadsTabRow(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
        )
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTab),
                    width = 40.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                text = { Text("PELICULAS", fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                text = { Text("SERIES", fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Normal) }
            )
        }
    }
}

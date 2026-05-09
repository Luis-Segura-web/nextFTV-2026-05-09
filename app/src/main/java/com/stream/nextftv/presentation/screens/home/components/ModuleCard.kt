package com.stream.nextftv.presentation.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stream.nextftv.R
import com.stream.nextftv.presentation.screens.home.SyncState
import com.stream.nextftv.presentation.theme.dimens
import com.stream.nextftv.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    title: String,
    subtitle: String,
    lastSync: String,
    icon: ImageVector,
    accentColor: Color,
    syncState: SyncState,
    onSyncClick: (() -> Unit)? = null, // Ahora es opcional
    onCardClick: () -> Unit,
    isGlobalSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.moduleCardHeight),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.dimens.cardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(MaterialTheme.dimens.moduleCardIconSize + 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(MaterialTheme.dimens.moduleCardIconSize)
                    )
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (onSyncClick != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                    Text(
                        text = stringResource(R.string.module_last_sync, lastSync),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f)
                    )
                }
            }

            if (onSyncClick != null) {
                if (syncState == SyncState.SYNCING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                } else if (!isGlobalSyncing) {
                    IconButton(onClick = onSyncClick) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(R.string.home_sync_all),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
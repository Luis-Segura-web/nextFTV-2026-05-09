package com.stream.nextftv.presentation.screens.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.stream.nextftv.data.local.entity.live.LiveStreamEntity
import com.stream.nextftv.presentation.theme.dimens
import com.stream.nextftv.presentation.theme.spacing

@Composable
internal fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    Surface(
        modifier = Modifier
            .height(MaterialTheme.dimens.categoryChipHeight)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
internal fun ChannelItem(
    stream: LiveStreamEntity,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val itemBackground = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBackground, MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.dimens.channelItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = stream.streamIcon,
            contentDescription = null,
            modifier = Modifier
                .size(MaterialTheme.dimens.channelLogoSize)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

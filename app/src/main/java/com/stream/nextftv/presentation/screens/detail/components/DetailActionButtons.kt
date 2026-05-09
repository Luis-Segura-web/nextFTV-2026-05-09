package com.stream.nextftv.presentation.screens.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailPrimaryActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    supportingText: String? = null,
    progressFraction: Float? = null,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerBrush = Brush.linearGradient(
        colors = listOf(
            colorScheme.primary.copy(alpha = 0.98f),
            colorScheme.primary.copy(alpha = 0.92f),
            colorScheme.tertiary.copy(alpha = 0.82f)
        )
    )
    val titleColor = colorScheme.onPrimary
    val supportingColor = colorScheme.onPrimary.copy(alpha = 0.96f)

    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(containerBrush)
                .defaultMinSize(minHeight = 54.dp)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.16f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = titleColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = titleColor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    if (!supportingText.isNullOrBlank()) {
                        Text(
                            text = supportingText,
                            color = supportingColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                    }
                }
            }

            progressFraction
                ?.takeIf { it > 0f }
                ?.let { fraction ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .background(Color.White.copy(alpha = 0.96f))
                        )
                    }
                }
        }
    }
}

@Composable
fun DetailIconActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String?,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (active) {
        colorScheme.errorContainer.copy(alpha = 0.96f)
    } else {
        colorScheme.surfaceVariant.copy(alpha = 0.96f)
    }
    val iconTint = if (active) {
        colorScheme.onErrorContainer
    } else {
        colorScheme.onSurface.copy(alpha = 0.94f)
    }
    val borderColor = if (active) {
        colorScheme.error.copy(alpha = 0.34f)
    } else {
        colorScheme.outline.copy(alpha = 0.26f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

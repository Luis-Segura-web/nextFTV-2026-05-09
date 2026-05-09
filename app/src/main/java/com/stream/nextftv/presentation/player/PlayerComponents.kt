package com.stream.nextftv.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String? = null,
    tint: Color = Color.White,
    containerColor: Color = Color.White.copy(alpha = 0.1f),
    borderColor: Color = Color.White.copy(alpha = 0.2f)
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTimeBar(
    position: Long,
    duration: Long,
    bufferPosition: Long,
    onSeek: (Long) -> Unit,
    onDragging: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Notificar cambio de estado de arrastre
    LaunchedEffect(isDragging || isPressed) {
        onDragging(isDragging || isPressed)
    }

    val currentProgress = if (isDragging) dragProgress else {
        if (duration > 0) position.toFloat() / duration else 0f
    }
    
    val bufferProgress = if (duration > 0) bufferPosition.toFloat() / duration else 0f
    val displayPosition = if (isDragging) (dragProgress * duration).toLong() else position
    
    val activeColor = Color.Red
    val bufferColor = Color.White.copy(alpha = 0.3f)
    val trackColor = Color.White.copy(alpha = 0.15f)

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. TIEMPOS EN LOS EXTREMOS (AHORA SIEMPRE ARRIBA)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                style = if (isSmall) MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp) else MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = formatTime(duration),
                style = if (isSmall) MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp) else MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(if (isSmall) 44.dp else 56.dp)) {
            // 2. INDICADOR DE TIEMPO FLOTANTE (Más separado del seek)
            if (isDragging || isPressed) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = (currentProgress * 280).coerceIn(0f, 280f).dp,
                            y = (-32).dp
                        ),
                    color = Color.Black.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = formatTime(displayPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. EL SLIDER PERSONALIZADO
            Slider(
                value = currentProgress,
                onValueChange = { 
                    isDragging = true
                    dragProgress = it
                },
                onValueChangeFinished = {
                    onSeek((dragProgress * duration).toLong())
                    isDragging = false
                },
                interactionSource = interactionSource,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                track = { sliderState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isDragging || isPressed) 4.dp else 2.dp)
                            .background(trackColor, CircleShape),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Línea de Buffer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bufferProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(bufferColor, CircleShape)
                        )
                        // Línea de Progreso Activo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(currentProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(activeColor, CircleShape)
                        )
                    }
                },
                thumb = {
                    // SIEMPRE VISIBLE
                    Box(
                        modifier = Modifier
                            .size(if (isDragging || isPressed) 16.dp else 12.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White, CircleShape)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlowTimeBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    PremiumTimeBar(
        position = position,
        duration = duration,
        bufferPosition = 0, // Fallback
        onSeek = onSeek,
        modifier = modifier,
        isSmall = isSmall
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

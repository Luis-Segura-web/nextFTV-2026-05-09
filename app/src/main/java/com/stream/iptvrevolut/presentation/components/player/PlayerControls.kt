package com.stream.iptvrevolut.presentation.components.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Tracks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    isLive: Boolean,
    isSeries: Boolean = false,
    isLocked: Boolean = false,
    title: String,
    subtitle: String? = null,
    position: Long,
    duration: Long,
    bufferedPosition: Long = 0,
    isSmall: Boolean,
    availableTracks: Tracks = Tracks.EMPTY,
    currentSpeed: Float = 1.0f,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onFullScreen: () -> Unit,
    onClose: () -> Unit,
    onAspect: () -> Unit = {},
    onLock: () -> Unit = {},
    onReload: () -> Unit = {},
    onMenuStateChange: (Boolean) -> Unit = {},
    onTrackSelected: (Tracks.Group, Int) -> Unit = { _, _ -> },
    onSpeedSelected: (Float) -> Unit = {},
    onPip: () -> Unit = {},
    onInteraction: () -> Unit = {}, // Nuevo callback para resetear timer
    onDragging: (Boolean) -> Unit = {}, // Nuevo callback para estado de arrastre
    modifier: Modifier = Modifier
) {
    var showAudioMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    // Estado para el progreso de desbloqueo (0.0 a 1.0)
    var unlockProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showAudioMenu, showSpeedMenu) {
        onMenuStateChange(showAudioMenu || showSpeedMenu)
    }

    val secondaryBtnBg = Color.Black.copy(alpha = 0.5f)
    val secondaryBtnBorder = Color.White.copy(alpha = 0.1f)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (isLocked) {
            // --- MODO BLOQUEADO (CENTRAL CON PROGRESO) ---
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onInteraction() // No ocultar mientras se presiona
                                        val startTime = System.currentTimeMillis()
                                        unlockProgress = 0.01f
                                        
                                        // Bucle de progreso
                                        val job = scope.launch {
                                            while (unlockProgress < 1f) {
                                                delay(30)
                                                val elapsed = System.currentTimeMillis() - startTime
                                                unlockProgress = (elapsed.toFloat() / 3000f).coerceAtMost(1f)
                                            }
                                            // Desbloqueo exitoso
                                            onLock()
                                            unlockProgress = 0f
                                        }
                                        
                                        tryAwaitRelease()
                                        job.cancel()
                                        unlockProgress = 0f
                                        onInteraction() // Reiniciar timer al soltar
                                    }
                                )
                            }
                    ) {
                        // Anillo de progreso de desbloqueo
                        if (unlockProgress > 0f) {
                            CircularProgressIndicator(
                                progress = { unlockProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                        
                        // Botón de candado
                        Surface(
                            shape = CircleShape,
                            color = if (unlockProgress > 0f) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (unlockProgress > 0f) "Mantén presionado..." else "Mantén para desbloquear",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // --- MODO NORMAL (Todos los controles) ---
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            ) {
                // BARRA SUPERIOR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(if (isSmall) 8.dp else 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = if (isSmall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = if (isSmall) 16.sp else 24.sp
                        )
                        if (!subtitle.isNullOrBlank() && !isSmall) {
                            Text(
                                text = subtitle, 
                                color = Color.White.copy(alpha = 0.7f), 
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            GlassActionButton(icon = Icons.Default.Refresh, onClick = { onReload(); onInteraction() }, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        GlassActionButton(icon = Icons.Default.Close, onClick = onClose, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                    }
                }

                // ZONA CENTRAL
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isSmall) 16.dp else 48.dp)
                ) {
                    if (!isLive) {
                        if (isSeries && onPrevious != null) {
                            GlassActionButton(icon = Icons.Rounded.SkipPrevious, onClick = { onPrevious(); onInteraction() }, size = if (isSmall) 40.dp else 56.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                        }
                        GlassActionButton(icon = Icons.Rounded.Replay10, onClick = { onSeekBack(); onInteraction() }, size = if (isSmall) 40.dp else 56.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                    } else if (onPrevious != null) {
                        GlassActionButton(icon = Icons.Rounded.SkipPrevious, onClick = { onPrevious(); onInteraction() }, size = if (isSmall) 40.dp else 56.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                    }

                    GlassActionButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        onClick = { onPlayPause(); onInteraction() },
                        size = if (isSmall) 56.dp else 82.dp,
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        borderColor = Color.White.copy(alpha = 0.3f)
                    )

                    if (!isLive) {
                        GlassActionButton(icon = Icons.Rounded.Forward10, onClick = { onSeekForward(); onInteraction() }, size = if (isSmall) 40.dp else 56.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                        if (isSeries && onNext != null) {
                            GlassActionButton(icon = Icons.Rounded.SkipNext, onClick = { onNext(); onInteraction() }, size = if (isSmall) 40.dp else 56.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                        }
                    } else if (onNext != null) {
                        GlassActionButton(icon = Icons.Rounded.SkipNext, onClick = { onNext(); onInteraction() }, size = if (isSmall) 40.dp else 56.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                    }
                }

                                // BARRA INFERIOR (Reorganizada: Tiempos -> Barra -> Botones)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = if (isSmall) 12.dp else 24.dp)
                                        .padding(bottom = if (isSmall) 8.dp else 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(if (isSmall) 2.dp else 8.dp)
                                ) {
                                    // 1. BARRA DE PROGRESO (Incluye tiempos arriba)
                                    if (!isLive) {
                                                                PremiumTimeBar(
                                                                    position = position,
                                                                    duration = duration,
                                                                    bufferPosition = bufferedPosition,
                                                                    onSeek = { onSeek(it); onInteraction() },
                                                                    onDragging = onDragging,
                                                                    isSmall = isSmall
                                                                )                                    } else {
                                        Column {
                                            LinearProgressIndicator(
                                                progress = { 1f },
                                                modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                                                color = Color.Red
                                            )
                                        }
                                    }
                
                                    // 2. BOTONES DE ACCIÓN
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            GlassActionButton(icon = Icons.Default.LockOpen, onClick = { onLock(); onInteraction() }, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                                            GlassActionButton(icon = Icons.Default.AspectRatio, onClick = { onAspect(); onInteraction() }, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                                        }
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(if (isSmall) 4.dp else 8.dp)) {
                                            Box {
                                                GlassActionButton(icon = Icons.Default.Audiotrack, onClick = { showAudioMenu = true; onInteraction() }, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                                                DropdownMenu(expanded = showAudioMenu, onDismissRequest = { showAudioMenu = false; onInteraction() }, modifier = Modifier.background(Color.Black.copy(alpha = 0.9f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
                                                    val audioGroups = availableTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                                                    if (audioGroups.isNotEmpty()) {
                                                        Text("AUDIO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(12.dp))
                                                        audioGroups.forEach { group -> for (i in 0 until group.length) {
                                                            DropdownMenuItem(text = { Text(group.getTrackFormat(i).language?.uppercase() ?: "Audio ${i+1}", color = Color.White) }, onClick = { onTrackSelected(group, i); showAudioMenu = false; onInteraction() }, leadingIcon = { if (group.isTrackSelected(i)) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) })
                                                        }}
                                                    }
                                                    val textGroups = availableTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                                                    if (textGroups.isNotEmpty()) {
                                                        Text("SUBTÍTULOS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(12.dp))
                                                        DropdownMenuItem(text = { Text("Desactivados", color = Color.White) }, onClick = { showAudioMenu = false; onInteraction() })
                                                        textGroups.forEach { group -> for (i in 0 until group.length) {
                                                            DropdownMenuItem(text = { Text(group.getTrackFormat(i).language?.uppercase() ?: "Sub ${i+1}", color = Color.White) }, onClick = { onTrackSelected(group, i); showAudioMenu = false; onInteraction() }, leadingIcon = { if (group.isTrackSelected(i)) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) })
                                                        }}
                                                    }
                                                }
                                            }
                
                                            Box {
                                                GlassActionButton(icon = Icons.Default.Speed, onClick = { showSpeedMenu = true; onInteraction() }, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                                                DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false; onInteraction() }, modifier = Modifier.background(Color.Black.copy(alpha = 0.9f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
                                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                                        DropdownMenuItem(text = { Text(if (speed == 1.0f) "Normal" else "${speed}x", color = Color.White) }, onClick = { onSpeedSelected(speed); showSpeedMenu = false; onInteraction() }, leadingIcon = { if (speed == currentSpeed) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) })
                                                    }
                                                }
                                            }
                
                                            GlassActionButton(icon = Icons.Default.PictureInPicture, onClick = { onPip(); onInteraction() }, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                                            GlassActionButton(icon = if (isSmall) Icons.Default.Fullscreen else Icons.Default.FullscreenExit, onClick = { onFullScreen(); onInteraction() }, size = if (isSmall) 32.dp else 40.dp, containerColor = secondaryBtnBg, borderColor = secondaryBtnBorder)
                                        }
                                    }
                                }            }
        }
    }
}

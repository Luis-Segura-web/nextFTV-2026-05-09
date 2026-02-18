package com.stream.iptvrevolut.presentation.screens.livetv.components

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.stream.iptvrevolut.MainActivity
import com.stream.iptvrevolut.presentation.components.player.PlayerControls
import com.stream.iptvrevolut.utils.findActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    title: String = "Reproduciendo...",
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    onLoading: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
    onFullScreenClick: () -> Unit = {},
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onClose: () -> Unit = {},
    isLive: Boolean = false,
    isSmall: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() as? MainActivity }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var isInPipMode by remember { mutableStateOf(false) }
    var isAnyMenuOpen by remember { mutableStateOf(false) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var availableTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBuffer (Reducido para evitar saturar el pipeline en Xiaomi)
                30000, // maxBuffer
                1000,  // bufferForPlayback (Arranque más rápido)
                2000   // bufferForPlaybackAfterRebuffer
            )
            .setBackBuffer(0, false) // No guardar frames pasados para liberar memoria
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val exoPlayer = remember {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        
        // Forzar header 'Connection: close' para que el servidor libere la sesión inmediatamente
        val defaultRequestProperties = mapOf("Connection" to "close")
        
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(defaultRequestProperties)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory)))
            .build().apply { 
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
    }

    var timerJob by remember { mutableStateOf<Job?>(null) }
    fun resetControlsTimer(forceShow: Boolean = true) {
        timerJob?.cancel()
        if (forceShow) isControlsVisible = true
        
        // El timer solo corre si no está bloqueado, no hay menú, está reproduciendo y NO se está arrastrando el seek
        if (!isAnyMenuOpen && isPlaying && !isDraggingSeek) {
            timerJob = scope.launch {
                delay(5000)
                isControlsVisible = false
            }
        }
    }

    LaunchedEffect(isPlaying, isLocked, isAnyMenuOpen, isDraggingSeek) { 
        // Si se está arrastrando, cancelar cualquier timer de ocultación
        if (isDraggingSeek) {
            timerJob?.cancel()
            isControlsVisible = true
        } else if (isControlsVisible) {
            resetControlsTimer(forceShow = false)
        } else if (!isPlaying && !isBuffering) {
            resetControlsTimer(forceShow = true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isInPipMode && !(activity?.isInPictureInPictureMode ?: false)) {
                    isInPipMode = false
                    resetControlsTimer()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(url) {
        // Limpieza agresiva antes de cambiar a un nuevo URL
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        
        // Delay de seguridad para que el servidor registre el cierre de conexión
        delay(300)
        
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            position = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPosition = exoPlayer.bufferedPosition
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { 
                isBuffering = state == Player.STATE_BUFFERING
                onLoading(isBuffering) 
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onTracksChanged(tracks: Tracks) { availableTracks = tracks }
        }
        exoPlayer.addListener(listener)
        onDispose { 
            exoPlayer.removeListener(listener)
            exoPlayer.setVideoSurface(null)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release() 
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(onTap = { 
                    if (isLocked) {
                        // En modo bloqueo permitimos ver el candado aunque bufferee
                        timerJob?.cancel()
                        isControlsVisible = true
                        timerJob = scope.launch { delay(5000); isControlsVisible = false }
                    } else {
                        if (isControlsVisible && !isAnyMenuOpen) isControlsVisible = false 
                        else {
                            // Mostrar manualmente
                            isControlsVisible = true
                            if (!isAnyMenuOpen && isPlaying) {
                                timerJob?.cancel()
                                timerJob = scope.launch { delay(5000); isControlsVisible = false }
                            }
                        }
                    }
                })
            }
    ) {
        AndroidView(
            factory = { PlayerView(it).apply { player = exoPlayer; useController = false; this.resizeMode = resizeMode; keepScreenOn = true } },
            modifier = Modifier.fillMaxSize(),
            update = { 
                it.resizeMode = resizeMode 
                if (isFullScreen) {
                    it.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                } else {
                    it.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        )

        val isSeries = title.contains("S\\d+E\\d+".toRegex())
        val displayTitle = if (isSeries) title.split(" - ").firstOrNull() ?: title else title
        val displaySubtitle = if (isSeries) title.split(" - ").getOrNull(1) else null

        if (!isInPipMode) {
            PlayerControls(
                isVisible = isControlsVisible,
                isPlaying = isPlaying,
                isLive = isLive,
                isSeries = isSeries,
                isLocked = isLocked,
                title = displayTitle,
                subtitle = displaySubtitle,
                position = position,
                duration = duration,
                bufferedPosition = bufferedPosition,
                isSmall = isSmall || !isFullScreen,
                availableTracks = availableTracks,
                currentSpeed = currentSpeed,
                onPlayPause = { resetControlsTimer(); if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onSeekBack = { resetControlsTimer(); exoPlayer.seekBack() },
                onSeekForward = { resetControlsTimer(); exoPlayer.seekForward() },
                onSeek = { resetControlsTimer(); exoPlayer.seekTo(it) },
                onNext = { resetControlsTimer(); onNext?.invoke() },
                onPrevious = { resetControlsTimer(); onPrevious?.invoke() },
                onFullScreen = { resetControlsTimer(); onFullScreenClick() },
                onClose = onClose,
                onReload = { 
                    resetControlsTimer()
                    val m = MediaItem.fromUri(url)
                    exoPlayer.setMediaItem(m)
                    exoPlayer.prepare()
                    exoPlayer.play()
                },
                onLock = { isLocked = !isLocked; resetControlsTimer() },
                onAspect = {
                    resetControlsTimer()
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                onMenuStateChange = { isOpen -> 
                    isAnyMenuOpen = isOpen
                    if (!isOpen) resetControlsTimer()
                },
                onTrackSelected = { group, index ->
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index)).build()
                },
                onSpeedSelected = { speed ->
                    currentSpeed = speed
                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                },
                onPip = { isInPipMode = true; activity?.enterPipMode() },
                onInteraction = { resetControlsTimer() },
                onDragging = { isDraggingSeek = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isBuffering) {
            val indicatorSize = if (isSmall || !isFullScreen) 56.dp else 82.dp
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(indicatorSize), 
                    color = Color.Red, 
                    strokeWidth = 4.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}

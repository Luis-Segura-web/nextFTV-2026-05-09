package com.stream.iptvrevolut.presentation.screens.livetv.components

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
import android.view.View
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.R as Media3UiR
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.stream.iptvrevolut.MainActivity
import com.stream.iptvrevolut.presentation.components.player.PlayerControls
import com.stream.iptvrevolut.presentation.player.GlobalPlaybackManager
import com.stream.iptvrevolut.presentation.player.PipModeState
import com.stream.iptvrevolut.presentation.player.PipPreferences
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
    useOriginalMedia3Controller: Boolean = false,
    isFullScreen: Boolean = false,
    onLoading: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
    onFullScreenClick: () -> Unit = {},
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onClose: () -> Unit = {},
    onPipRequested: (() -> Unit)? = null,
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
    val isInSystemPipMode = PipModeState.isInPipMode
    val isPipEnabled = PipPreferences.isEnabled
    var isAnyMenuOpen by remember { mutableStateOf(false) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var availableTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    val exoPlayer = remember { GlobalPlaybackManager.getPlayer(context) }

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
                resetControlsTimer()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isInSystemPipMode) {
        val pv = playerViewRef ?: return@LaunchedEffect
        if (isInSystemPipMode) {
            pv.hideController()
        } else if (useOriginalMedia3Controller) {
            pv.controllerShowTimeoutMs = 3000
            pv.showController()
        }
    }

    LaunchedEffect(url, title, isLive) {
        GlobalPlaybackManager.ensurePlaying(
            context = context,
            url = url,
            title = title,
            isLive = isLive
        )
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
        }
    }

    DisposableEffect(onNext, onPrevious) {
        GlobalPlaybackManager.setNavigationCallbacks(
            onNext = onNext,
            onPrevious = onPrevious
        )
        onDispose {
            GlobalPlaybackManager.clearNavigationCallbacks()
        }
    }

    val playerContainerModifier = if (useOriginalMedia3Controller) {
        modifier.background(Color.Black)
    } else {
        modifier
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(onTap = {
                    if (isLocked) {
                        timerJob?.cancel()
                        isControlsVisible = true
                        timerJob = scope.launch { delay(5000); isControlsVisible = false }
                    } else {
                        if (isControlsVisible && !isAnyMenuOpen) isControlsVisible = false
                        else {
                            isControlsVisible = true
                            if (!isAnyMenuOpen && isPlaying) {
                                timerJob?.cancel()
                                timerJob = scope.launch { delay(5000); isControlsVisible = false }
                            }
                        }
                    }
                })
            }
    }

    Box(modifier = playerContainerModifier) {
        AndroidView(
                factory = {
                PlayerView(it).apply {
                    playerViewRef = this
                    player = exoPlayer
                    useController = useOriginalMedia3Controller && !isInSystemPipMode
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setShowNextButton(onNext != null)
                    setShowPreviousButton(onPrevious != null)
                    setShowFastForwardButton(!isLive)
                    setShowRewindButton(!isLive)
                    setShowSubtitleButton(true)
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                    controllerShowTimeoutMs = 5000
                    this.resizeMode = resizeMode
                    keepScreenOn = true
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            findViewById<android.view.View>(com.stream.iptvrevolut.R.id.exo_top_controls)?.visibility =
                                visibility
                            configureCustomPrevNextButtons(
                                playerView = this,
                                onNext = onNext,
                                onPrevious = onPrevious
                            )
                            findViewById<View>(com.stream.iptvrevolut.R.id.exo_pip)?.visibility =
                                if (isPipEnabled) View.VISIBLE else View.GONE
                            findViewById<View>(com.stream.iptvrevolut.R.id.exo_minimal_pip)?.visibility =
                                if (isPipEnabled) View.VISIBLE else View.GONE
                            applyInlineProgressLayout(this, isInline = !isFullScreen)
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { 
                playerViewRef = it
                it.useController = useOriginalMedia3Controller && !isInSystemPipMode
                it.setShowNextButton(onNext != null)
                it.setShowPreviousButton(onPrevious != null)
                it.setShowFastForwardButton(!isLive)
                it.setShowRewindButton(!isLive)
                it.findViewById<android.view.View>(Media3UiR.id.exo_fullscreen)?.setOnClickListener {
                    onFullScreenClick()
                }
                it.findViewById<android.view.View>(Media3UiR.id.exo_minimal_fullscreen)?.setOnClickListener {
                    onFullScreenClick()
                }
                configureCustomPrevNextButtons(
                    playerView = it,
                    onNext = onNext,
                    onPrevious = onPrevious
                )
                it.findViewById<View>(com.stream.iptvrevolut.R.id.exo_pip)?.visibility =
                    if (isPipEnabled) View.VISIBLE else View.GONE
                it.findViewById<View>(com.stream.iptvrevolut.R.id.exo_minimal_pip)?.visibility =
                    if (isPipEnabled) View.VISIBLE else View.GONE
                it.findViewById<android.view.View>(com.stream.iptvrevolut.R.id.exo_pip)?.setOnClickListener {
                    if (isPipEnabled) {
                        playerViewRef?.hideController()
                        (onPipRequested ?: { activity?.enterPipMode() }).invoke()
                    }
                }
                it.findViewById<android.view.View>(com.stream.iptvrevolut.R.id.exo_minimal_pip)?.setOnClickListener {
                    if (isPipEnabled) {
                        playerViewRef?.hideController()
                        (onPipRequested ?: { activity?.enterPipMode() }).invoke()
                    }
                }
                it.findViewById<android.view.View>(com.stream.iptvrevolut.R.id.exo_close)?.setOnClickListener {
                    GlobalPlaybackManager.stopAndClear()
                    onClose()
                }
                // Keep fullscreen always visible in inline by reducing optional controls.
                it.findViewById<View>(Media3UiR.id.exo_vr)?.visibility = View.GONE
                it.findViewById<View>(Media3UiR.id.exo_shuffle)?.visibility = View.GONE
                it.findViewById<View>(Media3UiR.id.exo_repeat_toggle)?.visibility = View.GONE
                it.findViewById<View>(Media3UiR.id.exo_overflow_show)?.visibility = View.GONE
                it.findViewById<View>(Media3UiR.id.exo_overflow_hide)?.visibility = View.GONE
                it.findViewById<View>(Media3UiR.id.exo_fullscreen)?.visibility = View.VISIBLE
                applyInlineProgressLayout(it, isInline = !isFullScreen)
                configureTimeBar(
                    playerView = it,
                    duration = exoPlayer.duration,
                    position = exoPlayer.currentPosition,
                    bufferedPosition = exoPlayer.bufferedPosition
                )
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

        if (!isInSystemPipMode && !useOriginalMedia3Controller) {
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
                pipEnabled = isPipEnabled,
                onPlayPause = { resetControlsTimer(); if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onSeekBack = { resetControlsTimer(); exoPlayer.seekBack() },
                onSeekForward = { resetControlsTimer(); exoPlayer.seekForward() },
                onSeek = { resetControlsTimer(); exoPlayer.seekTo(it) },
                onNext = { resetControlsTimer(); onNext?.invoke() },
                onPrevious = { resetControlsTimer(); onPrevious?.invoke() },
                onFullScreen = { resetControlsTimer(); onFullScreenClick() },
                onClose = {
                    GlobalPlaybackManager.stopAndClear()
                    onClose()
                },
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
                onPip = { 
                    if (isPipEnabled) {
                        playerViewRef?.hideController()
                        (onPipRequested ?: { activity?.enterPipMode() }).invoke()
                    }
                },
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

private fun applyInlineProgressLayout(playerView: PlayerView, isInline: Boolean) {
    val density = playerView.resources.displayMetrics.density

    val targetBottomBarHeight = if (isInline) {
        (40f * density).toInt()
    } else {
        playerView.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_bottom_bar_height)
    }

    val targetBottomBarMarginTop = if (isInline) {
        (0f * density).toInt()
    } else {
        playerView.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_bottom_bar_margin_top)
    }

    val targetHeight = if (isInline) {
        (38f * density).toInt()
    } else {
        playerView.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_progress_layout_height)
    }

    val targetBottomMargin = if (isInline) {
        (42f * density).toInt()
    } else {
        playerView.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_progress_margin_bottom)
    }

    playerView.findViewById<View>(Media3UiR.id.exo_bottom_bar)?.let { bottomBar ->
        val params = bottomBar.layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return@let
        if (params.height != targetBottomBarHeight || params.topMargin != targetBottomBarMarginTop) {
            params.height = targetBottomBarHeight
            params.topMargin = targetBottomBarMarginTop
            bottomBar.layoutParams = params
            bottomBar.requestLayout()
        }
    }

    playerView.findViewById<View>(Media3UiR.id.exo_progress_placeholder)?.let { placeholder ->
        val params = placeholder.layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return@let
        if (params.height != targetHeight || params.bottomMargin != targetBottomMargin) {
            params.height = targetHeight
            params.bottomMargin = targetBottomMargin
            placeholder.layoutParams = params
            placeholder.requestLayout()
        }
    }

    playerView.findViewById<View>(Media3UiR.id.exo_progress)?.let { progressBar ->
        val params = progressBar.layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return@let
        if (params.height != targetHeight || params.bottomMargin != targetBottomMargin) {
            params.height = targetHeight
            params.bottomMargin = targetBottomMargin
            progressBar.layoutParams = params
            progressBar.requestLayout()
        }
    }
}

private fun configureTimeBar(
    playerView: PlayerView,
    duration: Long,
    position: Long,
    bufferedPosition: Long
) {
    (playerView.findViewById<View>(Media3UiR.id.exo_progress) as? DefaultTimeBar)?.let { timeBar ->
        val played = android.graphics.Color.parseColor("#E53935")
        val buffered = android.graphics.Color.parseColor("#CCFFFFFF")
        val unplayed = android.graphics.Color.parseColor("#33FFFFFF")
        val scrubber = android.graphics.Color.parseColor("#E53935")
        timeBar.setPlayedColor(played)
        timeBar.setBufferedColor(buffered)
        timeBar.setUnplayedColor(unplayed)
        timeBar.setScrubberColor(scrubber)

        val safeDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
        timeBar.setDuration(safeDuration)
        timeBar.setPosition(position.coerceAtLeast(0L))
        timeBar.setBufferedPosition(bufferedPosition.coerceAtLeast(0L))
    }
}

private fun configureCustomPrevNextButtons(
    playerView: PlayerView,
    onNext: (() -> Unit)?,
    onPrevious: (() -> Unit)?
) {
    playerView.findViewById<View>(Media3UiR.id.exo_prev)?.let { prev ->
        prev.isEnabled = onPrevious != null
        prev.isClickable = onPrevious != null
        prev.alpha = if (onPrevious != null) 1f else 0.4f
        prev.setOnClickListener { onPrevious?.invoke() }
    }

    playerView.findViewById<View>(Media3UiR.id.exo_next)?.let { next ->
        next.isEnabled = onNext != null
        next.isClickable = onNext != null
        next.alpha = if (onNext != null) 1f else 0.4f
        next.setOnClickListener { onNext?.invoke() }
    }
}

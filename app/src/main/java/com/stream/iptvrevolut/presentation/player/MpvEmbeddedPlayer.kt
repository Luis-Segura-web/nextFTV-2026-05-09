package com.stream.iptvrevolut.presentation.player

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.view.animation.LinearInterpolator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.C
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import androidx.media3.ui.R as Media3UiR
import com.stream.iptvrevolut.R
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import java.util.Locale
import com.stream.iptvrevolut.utils.findActivity

private const val MPV_SEEK_BACK_INCREMENT_MS = C.DEFAULT_SEEK_BACK_INCREMENT_MS
private const val MPV_SEEK_FORWARD_INCREMENT_MS = C.DEFAULT_SEEK_FORWARD_INCREMENT_MS
private const val MPV_CONTROLS_ANIM_DURATION_MS = 250L
private const val MPV_CONTROLS_HIDE_PROGRESS_DELAY_MS = 2000L
private val MPV_CONTROLS_LINEAR_INTERPOLATOR = LinearInterpolator()

private enum class MpvControlsUxState {
    ALL_VISIBLE,
    ONLY_PROGRESS_VISIBLE,
    NONE_VISIBLE
}

@Composable
fun MpvEmbeddedPlayer(
    url: String,
    title: String,
    isLive: Boolean,
    isFullScreen: Boolean = false,
    resumePositionMs: Long = 0L,
    pipEnabled: Boolean,
    isInSystemPipMode: Boolean,
    onFullScreenClick: () -> Unit,
    onClose: () -> Unit,
    onPipRequested: (() -> Unit)? = null,
    onError: (String) -> Unit,
    onLoading: (Boolean) -> Unit,
    onProgress: (Long) -> Unit,
    onProgressSnapshot: (Long, Long) -> Unit,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    isSmall: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val scope = rememberCoroutineScope()
    val mpvView = remember(url) { EmbeddedMpvSurfaceView(context) }

    var isInitialized by remember(url) { mutableStateOf(false) }
    var isBuffering by remember(url) { mutableStateOf(true) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var lastPosMs by remember(url) { mutableLongStateOf(0L) }
    var bufferedPosMs by remember(url) { mutableLongStateOf(0L) }
    var scrubPositionMs by remember(url) { mutableLongStateOf(0L) }
    var lastDurMs by remember(url) { mutableLongStateOf(0L) }
    var hasStartedPlayback by remember(url) { mutableStateOf(false) }
    var subtitleTracks by remember { mutableStateOf<List<MpvTrackItem>>(emptyList()) }
    var audioTracks by remember { mutableStateOf<List<MpvTrackItem>>(emptyList()) }
    var showTrackMenuDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var controlsInteractionTick by remember { mutableLongStateOf(0L) }
    var controlsUxState by remember { mutableStateOf(MpvControlsUxState.ALL_VISIBLE) }
    var appliedControlsUxState by remember { mutableStateOf<MpvControlsUxState?>(null) }
    var initialSeekDone by remember(url) { mutableStateOf(false) }
    var showUnlockOverlay by remember { mutableStateOf(false) }
    var isUnlockPressing by remember { mutableStateOf(false) }
    var unlockHoldProgress by remember { mutableFloatStateOf(0f) }
    var unlockOverlayJob by remember { mutableStateOf<Job?>(null) }

    fun showUnlockOverlayTemporarily() {
        if (!isLocked) return
        unlockOverlayJob?.cancel()
        showUnlockOverlay = true
        if (!isUnlockPressing) {
            unlockOverlayJob = scope.launch {
                delay(5000)
                if (!isUnlockPressing) {
                    showUnlockOverlay = false
                }
            }
        }
    }

    fun onPlayerLocked() {
        isControlsVisible = false
        controlsUxState = MpvControlsUxState.NONE_VISIBLE
        isUnlockPressing = false
        unlockHoldProgress = 0f
        showUnlockOverlayTemporarily()
    }

    fun onPlayerUnlocked() {
        unlockOverlayJob?.cancel()
        showUnlockOverlay = false
        isUnlockPressing = false
        unlockHoldProgress = 0f
        isControlsVisible = true
        controlsUxState = MpvControlsUxState.ALL_VISIBLE
        controlsInteractionTick++
    }

    fun showControlsAndResetTimer() {
        isControlsVisible = true
        controlsUxState = MpvControlsUxState.ALL_VISIBLE
        controlsInteractionTick++
    }

    fun isMpvReady(): Boolean = isInitialized
    fun getMpvDoubleOrDefault(property: String, default: Double): Double {
        if (!isMpvReady()) return default
        return runCatching { MPVLib.getPropertyDouble(property) ?: default }.getOrDefault(default)
    }
    fun getMpvBooleanOrDefault(property: String, default: Boolean): Boolean {
        if (!isMpvReady()) return default
        return runCatching { MPVLib.getPropertyBoolean(property) ?: default }.getOrDefault(default)
    }
    fun getMpvBufferDurationSec(): Double {
        if (!isMpvReady()) return 0.0
        val primary = runCatching { MPVLib.getPropertyDouble("demuxer-cache-duration") }.getOrNull()
        if (primary != null && primary.isFinite() && primary >= 0.0) return primary
        val legacy = runCatching { MPVLib.getPropertyDouble("cache-duration") }.getOrNull()
        if (legacy != null && legacy.isFinite() && legacy >= 0.0) return legacy
        return 0.0
    }

    val isAnyMenuOpen = showTrackMenuDialog || showSubtitleDialog || showAudioDialog || showSpeedDialog

    DisposableEffect(mpvView) {
        runCatching {
            mpvView.keepScreenOn = true
            mpvView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            mpvView.initialize(context.filesDir.path, context.cacheDir.path)
            mpvView.playFile(url)
            isInitialized = true
            isBuffering = true
            onLoading(true)
        }.onFailure { error ->
            onError("No se pudo inicializar MPV embebido: ${error.message ?: "error desconocido"}")
        }

        onDispose {
            val wasInitialized = isInitialized
            isInitialized = false
            showTrackMenuDialog = false
            showSubtitleDialog = false
            showAudioDialog = false
            showSpeedDialog = false
            runCatching {
                mpvView.keepScreenOn = false
                if (wasInitialized) {
                    onProgressSnapshot(lastPosMs, lastDurMs)
                }
                mpvView.destroy()
            }
        }
    }

    LaunchedEffect(isInitialized, isLive, url, initialSeekDone, resumePositionMs) {
        if (!isInitialized) return@LaunchedEffect
        runCatching { MPVLib.setPropertyBoolean("pause", false) }
        while (true) {
            val paused = getMpvBooleanOrDefault("pause", false)
            val pausedForCache = getMpvBooleanOrDefault("paused-for-cache", false)
            lastPosMs = mpvSecondsToMs(getMpvDoubleOrDefault("time-pos/full", 0.0))
            lastDurMs = mpvSecondsToMs(getMpvDoubleOrDefault("duration/full", 0.0))
            val cacheAheadMs = (getMpvBufferDurationSec() * 1000.0).roundToLong().coerceAtLeast(0L)
            val durationMs = normalizeDurationMs(lastDurMs)
            bufferedPosMs = if (durationMs > 0L) {
                (lastPosMs + cacheAheadMs).coerceIn(lastPosMs, durationMs)
            } else {
                (lastPosMs + cacheAheadMs).coerceAtLeast(lastPosMs)
            }

            isPlaying = !paused

            // Keep spinner visible until playback has effectively started, then mirror rebuffering.
            if (!hasStartedPlayback) {
                val startedByPosition = lastPosMs > 0L
                val startedByState = !paused && !pausedForCache && isLive
                if (startedByPosition || startedByState) {
                    hasStartedPlayback = true
                }
            }
            isBuffering = if (!hasStartedPlayback) true else pausedForCache
            onLoading(isBuffering)

            val shouldEmitProgress = isLive || initialSeekDone || resumePositionMs <= 0L
            if (shouldEmitProgress) {
                onProgress(lastPosMs)
                onProgressSnapshot(lastPosMs, lastDurMs)
            }
            delay(1000)
        }
    }

    LaunchedEffect(isInitialized, isLive, resumePositionMs, url) {
        if (!isInitialized || isLive || initialSeekDone) return@LaunchedEffect
        val safeResumeMs = resumePositionMs.coerceAtLeast(0L)
        if (safeResumeMs <= 0L) {
            initialSeekDone = true
            return@LaunchedEffect
        }
        var attempts = 0
        while (attempts < 20 && !getMpvBooleanOrDefault("seekable", false)) {
            delay(250)
            attempts++
        }
        val targetSec = safeResumeMs.toDouble() / 1000.0
        runCatching {
            MPVLib.command(
                arrayOf(
                    "seek",
                    String.format(Locale.US, "%.3f", targetSec),
                    "absolute",
                    "exact"
                )
            )
        }
        initialSeekDone = true
    }

    LaunchedEffect(
        isControlsVisible,
        isPlaying,
        isLocked,
        isDraggingSeek,
        isAnyMenuOpen,
        controlsInteractionTick
    ) {
        if (isControlsVisible && isPlaying && !isLocked && !isDraggingSeek && !isAnyMenuOpen) {
            delay(5000)
            if (!isControlsVisible || isLocked || isDraggingSeek || isAnyMenuOpen || !isPlaying) return@LaunchedEffect
            controlsUxState = MpvControlsUxState.ONLY_PROGRESS_VISIBLE
            delay(MPV_CONTROLS_HIDE_PROGRESS_DELAY_MS)
            if (!isControlsVisible || isLocked || isDraggingSeek || isAnyMenuOpen || !isPlaying) return@LaunchedEffect
            controlsUxState = MpvControlsUxState.NONE_VISIBLE
            isControlsVisible = false
        }
    }

    SideEffect {
        val decor = activity?.window?.decorView ?: return@SideEffect
        if (isFullScreen && !isInSystemPipMode) {
            decor.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    DisposableEffect(isFullScreen) {
        onDispose {
            if (isFullScreen) {
                activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = {
                        if (isLocked) {
                            showUnlockOverlayTemporarily()
                        } else {
                            if (!isControlsVisible) {
                                showControlsAndResetTimer()
                            } else {
                                isControlsVisible = false
                                controlsUxState = MpvControlsUxState.NONE_VISIBLE
                            }
                        }
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mpvView }
        )

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.widget.FrameLayout(ctx).apply {
                    keepScreenOn = true
                    LayoutInflater.from(ctx).inflate(R.layout.exo_player_control_view, this, true)
                    ensureSeekBarView()
                    findViewById<View>(Media3UiR.id.exo_controls_background)?.apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    findViewById<DefaultTimeBar>(Media3UiR.id.exo_progress)?.apply {
                        // MPV parity: allow seeking by dragging the same progress bar used by Media3.
                        addListener(object : TimeBar.OnScrubListener {
                            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                                if (isLive) return
                                isDraggingSeek = true
                                scrubPositionMs = position.coerceAtLeast(0L)
                                showControlsAndResetTimer()
                            }

                            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                                if (isLive) return
                                scrubPositionMs = position.coerceAtLeast(0L)
                            }

                            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                                if (isLive) return
                                isDraggingSeek = false
                                scrubPositionMs = position.coerceAtLeast(0L)
                                if (canceled) {
                                    showControlsAndResetTimer()
                                    return
                                }
                                if (!isMpvReady()) {
                                    showControlsAndResetTimer()
                                    return
                                }
                                val boundedDurationMs = normalizeDurationMs(lastDurMs)
                                val safePositionMs = if (boundedDurationMs > 0L) {
                                    position.coerceIn(0L, boundedDurationMs)
                                } else {
                                    position.coerceAtLeast(0L)
                                }
                                scrubPositionMs = safePositionMs
                                lastPosMs = safePositionMs
                                seekMpvToPositionMs(safePositionMs)
                                showControlsAndResetTimer()
                            }
                        })
                    }
                }
            },
            update = { controls ->
                val targetUxState = if (isLocked || isInSystemPipMode) {
                    MpvControlsUxState.NONE_VISIBLE
                } else {
                    controlsUxState
                }
                applyControlsUxStateWithAnimation(
                    controls = controls,
                    previousState = appliedControlsUxState,
                    targetState = targetUxState
                )
                appliedControlsUxState = targetUxState
                val controlsVisibility = if (targetUxState == MpvControlsUxState.ALL_VISIBLE) View.VISIBLE else View.GONE
                configureTopTitleContent(
                    controls = controls,
                    sourceTitle = title,
                    isLive = isLive,
                    isFullScreen = isFullScreen,
                    controllerVisibility = controlsVisibility,
                    isLocked = isLocked
                )

                val displayedPosition = if (isDraggingSeek) scrubPositionMs else lastPosMs
                controls.findViewById<TextView>(Media3UiR.id.exo_position)?.text = formatMs(displayedPosition)
                controls.findViewById<TextView>(Media3UiR.id.exo_duration)?.text = formatMs(lastDurMs)

                controls.findViewById<DefaultTimeBar>(Media3UiR.id.exo_progress)?.let { timeBar ->
                    configureTimeBar(
                        timeBar = timeBar,
                        duration = lastDurMs,
                        position = displayedPosition,
                        bufferedPosition = bufferedPosMs,
                        isLive = isLive
                    )
                }
                applyInlineProgressLayout(controls = controls, isInline = !isFullScreen)

                controls.findViewById<ImageButton>(Media3UiR.id.exo_play_pause)?.apply {
                    setImageResource(
                        if (isPlaying) Media3UiR.drawable.exo_styled_controls_pause
                        else Media3UiR.drawable.exo_styled_controls_play
                    )
                    setOnClickListener {
                        if (!isMpvReady()) return@setOnClickListener
                        val paused = getMpvBooleanOrDefault("pause", false)
                        runCatching { MPVLib.setPropertyBoolean("pause", !paused) }
                        showControlsAndResetTimer()
                    }
                }

                controls.findViewById<ImageButton>(Media3UiR.id.exo_rew)?.apply {
                    this.visibility = if (isLive) View.GONE else View.VISIBLE
                    setOnClickListener {
                        if (!isMpvReady()) return@setOnClickListener
                        val targetPositionMs = (lastPosMs - MPV_SEEK_BACK_INCREMENT_MS).coerceAtLeast(0L)
                        scrubPositionMs = targetPositionMs
                        lastPosMs = targetPositionMs
                        seekMpvToPositionMs(targetPositionMs)
                        showControlsAndResetTimer()
                    }
                }

                controls.findViewById<ImageButton>(Media3UiR.id.exo_ffwd)?.apply {
                    this.visibility = if (isLive) View.GONE else View.VISIBLE
                    setOnClickListener {
                        if (!isMpvReady()) return@setOnClickListener
                        val boundedDurationMs = normalizeDurationMs(lastDurMs)
                        val rawTargetPositionMs = lastPosMs + MPV_SEEK_FORWARD_INCREMENT_MS
                        val targetPositionMs = if (boundedDurationMs > 0L) {
                            rawTargetPositionMs.coerceAtMost(boundedDurationMs)
                        } else {
                            rawTargetPositionMs.coerceAtLeast(0L)
                        }
                        scrubPositionMs = targetPositionMs
                        lastPosMs = targetPositionMs
                        seekMpvToPositionMs(targetPositionMs)
                        showControlsAndResetTimer()
                    }
                }

                controls.findViewById<ImageButton>(Media3UiR.id.exo_prev)?.apply {
                    isEnabled = onPrevious != null
                    isClickable = onPrevious != null
                    alpha = if (onPrevious != null) 1f else 0.4f
                    setOnClickListener {
                        if (!isMpvReady()) return@setOnClickListener
                        onPrevious?.invoke() ?: runCatching { MPVLib.command(arrayOf("playlist-prev")) }
                        showControlsAndResetTimer()
                    }
                }

                controls.findViewById<ImageButton>(Media3UiR.id.exo_next)?.apply {
                    isEnabled = onNext != null
                    isClickable = onNext != null
                    alpha = if (onNext != null) 1f else 0.4f
                    setOnClickListener {
                        if (!isMpvReady()) return@setOnClickListener
                        onNext?.invoke() ?: runCatching { MPVLib.command(arrayOf("playlist-next")) }
                        showControlsAndResetTimer()
                    }
                }

                controls.findViewById<ImageButton>(Media3UiR.id.exo_fullscreen)?.setOnClickListener {
                    onFullScreenClick()
                    showControlsAndResetTimer()
                }
                controls.findViewById<ImageButton>(Media3UiR.id.exo_minimal_fullscreen)?.setOnClickListener {
                    onFullScreenClick()
                    showControlsAndResetTimer()
                }
                val fullscreenIcon = if (isFullScreen) {
                    Media3UiR.drawable.exo_styled_controls_fullscreen_exit
                } else {
                    Media3UiR.drawable.exo_styled_controls_fullscreen_enter
                }
                controls.findViewById<ImageButton>(Media3UiR.id.exo_fullscreen)?.setImageResource(fullscreenIcon)
                controls.findViewById<ImageButton>(Media3UiR.id.exo_minimal_fullscreen)?.setImageResource(fullscreenIcon)

                controls.findViewById<View>(R.id.exo_pip)?.visibility =
                    if (pipEnabled && !isInSystemPipMode && !isLocked) controlsVisibility else View.GONE
                controls.findViewById<View>(R.id.exo_minimal_pip)?.visibility = View.GONE
                controls.findViewById<View>(R.id.exo_pip)?.setOnClickListener {
                    if (pipEnabled && !isInSystemPipMode) {
                        isControlsVisible = false
                        controlsUxState = MpvControlsUxState.NONE_VISIBLE
                        onPipRequested?.invoke()
                    }
                }
                controls.findViewById<View>(Media3UiR.id.exo_vr)?.visibility = View.GONE
                controls.findViewById<View>(Media3UiR.id.exo_shuffle)?.visibility = View.GONE
                controls.findViewById<View>(Media3UiR.id.exo_repeat_toggle)?.visibility = View.GONE
                controls.findViewById<View>(Media3UiR.id.exo_overflow_show)?.visibility = View.GONE
                controls.findViewById<View>(Media3UiR.id.exo_overflow_hide)?.visibility = View.GONE

                controls.findViewById<ImageButton>(Media3UiR.id.exo_subtitle)?.apply {
                    val hasSubtitleTracks = isMpvReady() && hasTrackType("sub")
                    val subtitleEnabled = hasSubtitleTracks && !isLocked
                    val subtitleSelected = isMpvReady() && getMpvTrackSelectionId("sid") > 0
                    this.visibility = View.VISIBLE
                    isEnabled = subtitleEnabled
                    isClickable = subtitleEnabled
                    alpha = if (subtitleEnabled) 1f else 0.3f
                    setImageResource(
                        if (subtitleSelected) Media3UiR.drawable.exo_styled_controls_subtitle_on
                        else Media3UiR.drawable.exo_styled_controls_subtitle_off
                    )
                    setOnClickListener {
                        if (!subtitleEnabled) return@setOnClickListener
                        showControlsAndResetTimer()
                        subtitleTracks = loadTrackItems(
                            context = context,
                            type = "sub",
                            property = "sid",
                            includeOff = true,
                            includeAuto = false
                        )
                        if (subtitleTracks.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.player_track_no_subtitles), Toast.LENGTH_SHORT).show()
                        } else {
                            showSubtitleDialog = true
                        }
                    }
                }

                controls.findViewById<ImageButton>(Media3UiR.id.exo_settings)?.apply {
                    val settingsEnabled = !isLocked && isMpvReady()
                    this.visibility = View.VISIBLE
                    isEnabled = settingsEnabled
                    isClickable = settingsEnabled
                    alpha = if (settingsEnabled) 1f else 0.3f
                    setOnClickListener {
                        if (!settingsEnabled) return@setOnClickListener
                        showControlsAndResetTimer()
                        audioTracks = loadTrackItems(
                            context = context,
                            type = "audio",
                            property = "aid",
                            includeOff = false,
                            includeAuto = true
                        )
                        showTrackMenuDialog = true
                    }
                }

                controls.findViewById<ImageButton>(R.id.exo_lock)?.apply {
                    setImageResource(if (isLocked) R.drawable.ic_player_unlock else R.drawable.ic_player_lock)
                    setOnClickListener {
                        isLocked = !isLocked
                        if (isLocked) onPlayerLocked() else onPlayerUnlocked()
                        showControlsAndResetTimer()
                    }
                }

                controls.findViewById<ImageButton>(R.id.exo_close)?.setOnClickListener {
                    onProgressSnapshot(lastPosMs, lastDurMs)
                    onClose()
                }
            }
        )

        if (showSubtitleDialog) {
            TrackSelectionDialog(
                title = stringResource(R.string.player_track_subtitles),
                tracks = subtitleTracks,
                onDismiss = {
                    showSubtitleDialog = false
                    showControlsAndResetTimer()
                },
                onTrackSelected = { selectedId ->
                    if (isMpvReady()) {
                        applyTrackSelection(property = "sid", trackId = selectedId)
                    }
                    showSubtitleDialog = false
                    showControlsAndResetTimer()
                }
            )
        }

        if (showAudioDialog) {
            TrackSelectionDialog(
                title = stringResource(R.string.player_track_audio),
                tracks = audioTracks,
                onDismiss = {
                    showAudioDialog = false
                    showControlsAndResetTimer()
                },
                onTrackSelected = { selectedId ->
                    if (isMpvReady()) {
                        applyTrackSelection(property = "aid", trackId = selectedId)
                    }
                    showAudioDialog = false
                    showControlsAndResetTimer()
                }
            )
        }

        if (showSpeedDialog) {
            val speedOptions = listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)
            val currentSpeed = getMpvDoubleOrDefault("speed", 1.0)
            TrackSelectionDialog(
                title = stringResource(R.string.player_playback_speed),
                tracks = speedOptions.map { speed ->
                    MpvTrackItem(
                        id = speedOptions.indexOf(speed),
                        label = String.format(Locale.US, "%.2fx", speed),
                        selected = kotlin.math.abs(currentSpeed - speed) < 0.01
                    )
                },
                onDismiss = {
                    showSpeedDialog = false
                    showControlsAndResetTimer()
                },
                onTrackSelected = { selectedIndex ->
                    val selectedSpeed = speedOptions.getOrNull(selectedIndex) ?: return@TrackSelectionDialog
                    if (isMpvReady()) {
                        runCatching { MPVLib.setPropertyDouble("speed", selectedSpeed) }
                    }
                    showSpeedDialog = false
                    showControlsAndResetTimer()
                }
            )
        }

        if (showTrackMenuDialog) {
            val currentSpeed = getMpvDoubleOrDefault("speed", 1.0)
            val audioSubText = buildAudioSelectionSubText(
                context = context,
                tracks = audioTracks,
                currentMode = getMpvTrackSelectionMode("aid")
            )
            TrackMenuDialog(
                hasAudio = audioTracks.isNotEmpty(),
                speedLabel = String.format(Locale.US, "%.2fx", currentSpeed),
                audioSubText = audioSubText,
                onDismiss = {
                    showTrackMenuDialog = false
                    showControlsAndResetTimer()
                },
                onOpenSpeed = {
                    showTrackMenuDialog = false
                    showSpeedDialog = true
                },
                onOpenAudio = {
                    showTrackMenuDialog = false
                    if (audioTracks.isEmpty()) return@TrackMenuDialog
                    showAudioDialog = true
                }
            )
        }

        if (isBuffering) {
            val indicatorSize = if (isSmall) 56.dp else 82.dp
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(indicatorSize),
                    color = Color.Red,
                    strokeWidth = 4.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }

        if (isLocked && !isInSystemPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        detectTapGestures(
                            onTap = { showUnlockOverlayTemporarily() },
                            onPress = {
                                if (!isLocked) return@detectTapGestures
                                unlockOverlayJob?.cancel()
                                isUnlockPressing = true
                                showUnlockOverlay = true
                                unlockHoldProgress = 0f
                                var unlocked = false

                                coroutineScope {
                                    val holdJob = launch {
                                        val requiredMs = 3000L
                                        val start = System.currentTimeMillis()
                                        while (isActive) {
                                            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0L)
                                            unlockHoldProgress = (elapsed.toFloat() / requiredMs.toFloat()).coerceIn(0f, 1f)
                                            if (elapsed >= requiredMs) {
                                                unlocked = true
                                                isLocked = false
                                                onPlayerUnlocked()
                                                break
                                            }
                                            delay(16)
                                        }
                                    }

                                    val released = tryAwaitRelease()
                                    if (!unlocked || !released) {
                                        holdJob.cancel()
                                        isUnlockPressing = false
                                        unlockHoldProgress = 0f
                                        showUnlockOverlayTemporarily()
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (showUnlockOverlay) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { unlockHoldProgress },
                                    modifier = Modifier.size(52.dp),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                    strokeWidth = 3.dp
                                )
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Mantén presionado 3s para desbloquear",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun applyControlsUxStateWithAnimation(
    controls: View,
    previousState: MpvControlsUxState?,
    targetState: MpvControlsUxState
) {
    val resources = controls.resources
    val translationYForProgressBar =
        resources.getDimension(Media3UiR.dimen.exo_styled_bottom_bar_height) -
            resources.getDimension(Media3UiR.dimen.exo_styled_progress_bar_height)
    val translationYForNoBars = resources.getDimension(Media3UiR.dimen.exo_styled_bottom_bar_height)

    val controlsBackground = controls.findViewById<View>(Media3UiR.id.exo_controls_background)
    val topControls = controls.findViewById<View>(R.id.exo_top_controls)
    val topTitleContainer = controls.findViewById<View>(R.id.exo_top_title_container)
    val centerControls = controls.findViewById<View>(Media3UiR.id.exo_center_controls)
    val bottomBar = controls.findViewById<View>(Media3UiR.id.exo_bottom_bar)
    val progress = controls.findViewById<View>(Media3UiR.id.exo_progress)
    val progressPlaceholder = controls.findViewById<View>(Media3UiR.id.exo_progress_placeholder)

    controlsBackground?.setBackgroundColor(android.graphics.Color.parseColor("#66000000"))
    controls.findViewById<View>(Media3UiR.id.exo_minimal_controls)?.visibility = View.GONE

    val fromState = previousState ?: targetState
    if (fromState == targetState) {
        // Only hard-initialize once. For recurrent recompositions, don't force values,
        // otherwise the bar can snap instead of animating.
        if (previousState == null) {
            when (targetState) {
                MpvControlsUxState.ALL_VISIBLE -> {
                    setChromeVisible(controlsBackground, topControls, topTitleContainer, centerControls, true)
                    setProgressLayoutVisible(bottomBar, progress, progressPlaceholder, true)
                    bottomBar?.translationY = 0f
                    progress?.translationY = 0f
                    progressPlaceholder?.translationY = 0f
                }
                MpvControlsUxState.ONLY_PROGRESS_VISIBLE -> {
                    setChromeVisible(controlsBackground, topControls, topTitleContainer, centerControls, false)
                    setProgressLayoutVisible(bottomBar, progress, progressPlaceholder, true)
                    bottomBar?.translationY = translationYForProgressBar
                    progress?.translationY = translationYForProgressBar
                    progressPlaceholder?.translationY = translationYForProgressBar
                }
                MpvControlsUxState.NONE_VISIBLE -> {
                    setChromeVisible(controlsBackground, topControls, topTitleContainer, centerControls, false)
                    setProgressLayoutVisible(bottomBar, progress, progressPlaceholder, false)
                }
            }
        }
        return
    }

    when {
        fromState == MpvControlsUxState.ALL_VISIBLE && targetState == MpvControlsUxState.ONLY_PROGRESS_VISIBLE -> {
            animateChrome(controlsBackground, topControls, topTitleContainer, centerControls, false, progress)
            animateProgressLayout(bottomBar, progress, progressPlaceholder, 0f, translationYForProgressBar, true)
        }
        fromState == MpvControlsUxState.ONLY_PROGRESS_VISIBLE && targetState == MpvControlsUxState.NONE_VISIBLE -> {
            animateProgressLayout(
                bottomBar,
                progress,
                progressPlaceholder,
                translationYForProgressBar,
                translationYForNoBars,
                false
            )
        }
        fromState == MpvControlsUxState.ALL_VISIBLE && targetState == MpvControlsUxState.NONE_VISIBLE -> {
            animateChrome(controlsBackground, topControls, topTitleContainer, centerControls, false, progress)
            animateProgressLayout(bottomBar, progress, progressPlaceholder, 0f, translationYForNoBars, false)
        }
        fromState == MpvControlsUxState.NONE_VISIBLE && targetState == MpvControlsUxState.ALL_VISIBLE -> {
            animateChrome(controlsBackground, topControls, topTitleContainer, centerControls, true, progress)
            animateProgressLayout(bottomBar, progress, progressPlaceholder, translationYForNoBars, 0f, true)
        }
        fromState == MpvControlsUxState.ONLY_PROGRESS_VISIBLE && targetState == MpvControlsUxState.ALL_VISIBLE -> {
            animateChrome(controlsBackground, topControls, topTitleContainer, centerControls, true, progress)
            animateProgressLayout(bottomBar, progress, progressPlaceholder, translationYForProgressBar, 0f, true)
        }
        fromState == MpvControlsUxState.NONE_VISIBLE && targetState == MpvControlsUxState.ONLY_PROGRESS_VISIBLE -> {
            animateProgressLayout(
                bottomBar,
                progress,
                progressPlaceholder,
                translationYForNoBars,
                translationYForProgressBar,
                true
            )
        }
    }
}

private fun animateChrome(
    controlsBackground: View?,
    topControls: View?,
    topTitleContainer: View?,
    centerControls: View?,
    show: Boolean,
    progress: View?
) {
    if (progress is DefaultTimeBar) {
        if (show) {
            progress.showScrubber(MPV_CONTROLS_ANIM_DURATION_MS)
        } else {
            progress.hideScrubber(MPV_CONTROLS_ANIM_DURATION_MS)
        }
    }
    listOfNotNull(controlsBackground, topControls, topTitleContainer, centerControls).forEach { view ->
        view.animate().cancel()
        if (show) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(MPV_CONTROLS_ANIM_DURATION_MS)
                .setInterpolator(MPV_CONTROLS_LINEAR_INTERPOLATOR)
                .start()
        } else {
            view.visibility = View.VISIBLE
            view.alpha = 1f
            view.animate()
                .alpha(0f)
                .setDuration(MPV_CONTROLS_ANIM_DURATION_MS)
                .setInterpolator(MPV_CONTROLS_LINEAR_INTERPOLATOR)
                .withEndAction { view.visibility = View.INVISIBLE }
                .start()
        }
    }
}

private fun animateProgressLayout(
    bottomBar: View?,
    progress: View?,
    progressPlaceholder: View?,
    fromY: Float,
    toY: Float,
    keepVisibleAtEnd: Boolean
) {
    listOfNotNull(bottomBar, progress, progressPlaceholder).forEach { view ->
        view.animate().cancel()
        view.visibility = View.VISIBLE
        val startY = if (kotlin.math.abs(view.translationY - fromY) < 0.5f) fromY else view.translationY
        view.translationY = startY
        view.animate()
            .translationY(toY)
            .setDuration(MPV_CONTROLS_ANIM_DURATION_MS)
            .setInterpolator(MPV_CONTROLS_LINEAR_INTERPOLATOR)
            .withEndAction {
                if (!keepVisibleAtEnd) view.visibility = View.INVISIBLE
            }
            .start()
    }
}

private fun setChromeVisible(
    controlsBackground: View?,
    topControls: View?,
    topTitleContainer: View?,
    centerControls: View?,
    visible: Boolean
) {
    val visibility = if (visible) View.VISIBLE else View.INVISIBLE
    listOfNotNull(controlsBackground, topControls, topTitleContainer, centerControls).forEach { view ->
        view.animate().cancel()
        view.visibility = visibility
        view.alpha = if (visible) 1f else 0f
    }
}

private fun setProgressLayoutVisible(
    bottomBar: View?,
    progress: View?,
    progressPlaceholder: View?,
    visible: Boolean
) {
    val visibility = if (visible) View.VISIBLE else View.INVISIBLE
    listOfNotNull(bottomBar, progress, progressPlaceholder).forEach { view ->
        view.animate().cancel()
        view.visibility = visibility
        if (!visible) {
            view.translationY = 0f
        }
    }
}

private fun configureTopTitleContent(
    controls: View,
    sourceTitle: String,
    isLive: Boolean,
    isFullScreen: Boolean,
    controllerVisibility: Int,
    isLocked: Boolean
) {
    val container = controls.findViewById<View>(R.id.exo_top_title_container) ?: return
    val primary = controls.findViewById<TextView>(R.id.exo_top_title_primary) ?: return
    val secondary = controls.findViewById<TextView>(R.id.exo_top_title_secondary) ?: return

    val seriesCodeRegex = Regex("\\bS\\d{2}E\\d{2}\\b")
    val seriesCode = seriesCodeRegex.find(sourceTitle)?.value
    val isSeries = seriesCode != null

    val primaryText: String
    val secondaryText: String?
    val shouldShow = when {
        isLive -> {
            primaryText = sourceTitle
            secondaryText = null
            true
        }
        isSeries -> {
            val parts = sourceTitle.split(" - ")
            val episodeTitle = parts.lastOrNull().orEmpty().ifBlank { sourceTitle }
            primaryText = episodeTitle
            secondaryText = seriesCode
            true
        }
        else -> {
            primaryText = sourceTitle
            secondaryText = null
            isFullScreen
        }
    }

    primary.text = primaryText
    secondary.text = secondaryText.orEmpty()
    secondary.visibility = if (secondaryText.isNullOrBlank()) View.GONE else View.VISIBLE

    val primarySp = if (isFullScreen) 20f else 14f
    val secondarySp = if (isFullScreen) 15f else 11f
    primary.setTextSize(TypedValue.COMPLEX_UNIT_SP, primarySp)
    secondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondarySp)

    container.visibility =
        if (!isLocked && shouldShow && controllerVisibility == View.VISIBLE) View.VISIBLE else View.GONE
}

private fun applyInlineProgressLayout(controls: View, isInline: Boolean) {
    val density = controls.resources.displayMetrics.density

    val targetBottomBarHeight = if (isInline) {
        (40f * density).toInt()
    } else {
        controls.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_bottom_bar_height)
    }

    val targetBottomBarMarginTop = if (isInline) {
        0
    } else {
        controls.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_bottom_bar_margin_top)
    }

    val targetHeight = if (isInline) {
        (38f * density).toInt()
    } else {
        controls.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_progress_layout_height)
    }

    val targetBottomMargin = if (isInline) {
        (42f * density).toInt()
    } else {
        controls.resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_progress_margin_bottom)
    }

    controls.findViewById<View>(Media3UiR.id.exo_bottom_bar)?.let { bottomBar ->
        val params = bottomBar.layoutParams as? ViewGroup.MarginLayoutParams ?: return@let
        if (params.height != targetBottomBarHeight || params.topMargin != targetBottomBarMarginTop) {
            params.height = targetBottomBarHeight
            params.topMargin = targetBottomBarMarginTop
            bottomBar.layoutParams = params
            bottomBar.requestLayout()
        }
    }

    controls.findViewById<View>(Media3UiR.id.exo_progress_placeholder)?.let { placeholder ->
        val params = placeholder.layoutParams as? ViewGroup.MarginLayoutParams ?: return@let
        if (params.height != targetHeight || params.bottomMargin != targetBottomMargin) {
            params.height = targetHeight
            params.bottomMargin = targetBottomMargin
            placeholder.layoutParams = params
            placeholder.requestLayout()
        }
    }

    controls.findViewById<View>(Media3UiR.id.exo_progress)?.let { progressBar ->
        val params = progressBar.layoutParams as? ViewGroup.MarginLayoutParams ?: return@let
        if (params.height != targetHeight || params.bottomMargin != targetBottomMargin) {
            params.height = targetHeight
            params.bottomMargin = targetBottomMargin
            progressBar.layoutParams = params
            progressBar.requestLayout()
        }
    }
}

private fun configureTimeBar(
    timeBar: DefaultTimeBar,
    duration: Long,
    position: Long,
    bufferedPosition: Long,
    isLive: Boolean
) {
    val played = android.graphics.Color.parseColor("#E53935")
    val buffered = android.graphics.Color.parseColor("#CCFFFFFF")
    val unplayed = android.graphics.Color.parseColor("#33FFFFFF")
    val scrubber = android.graphics.Color.parseColor("#E53935")
    timeBar.setPlayedColor(played)
    timeBar.setBufferedColor(buffered)
    timeBar.setUnplayedColor(unplayed)
    timeBar.setScrubberColor(scrubber)

    timeBar.setDuration(normalizeDurationMs(duration))
    timeBar.setPosition(position.coerceAtLeast(0L))
    timeBar.setBufferedPosition(bufferedPosition.coerceAtLeast(0L))
    timeBar.isEnabled = !isLive
}

private fun FrameLayout.ensureSeekBarView() {
    if (findViewById<View>(Media3UiR.id.exo_progress) != null) return

    val placeholder = findViewById<View>(Media3UiR.id.exo_progress_placeholder)
    val timeBar = DefaultTimeBar(context).apply {
        id = Media3UiR.id.exo_progress
        val placeholderParams = placeholder?.layoutParams as? FrameLayout.LayoutParams
        layoutParams = if (placeholderParams != null) {
            FrameLayout.LayoutParams(placeholderParams).apply {
                width = placeholderParams.width
                height = placeholderParams.height
                gravity = placeholderParams.gravity
                bottomMargin = placeholderParams.bottomMargin
                marginStart = placeholderParams.marginStart
                marginEnd = placeholderParams.marginEnd
            }
        } else {
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM
            }
        }
    }

    val insertIndex = if (placeholder != null) indexOfChild(placeholder) + 1 else childCount
    addView(timeBar, insertIndex)
}

private fun normalizeDurationMs(durationMs: Long): Long {
    return if (durationMs > 0L && durationMs != C.TIME_UNSET) durationMs else 0L
}

private fun seekMpvToPositionMs(positionMs: Long) {
    val targetSec = positionMs.coerceAtLeast(0L).toDouble() / 1000.0
    MPVLib.command(
        arrayOf(
            "seek",
            String.format(Locale.US, "%.3f", targetSec),
            "absolute",
            "exact"
        )
    )
}

private class EmbeddedMpvSurfaceView(context: Context) : BaseMPVView(context, null) {
    override fun initOptions() {
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString("hwdec", "mediacodec-copy")
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        MPVLib.setOptionString("audio-buffer", "1.0")
        MPVLib.setOptionString("audio-set-media-role", "yes")
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-on-disk", "yes")
        MPVLib.setOptionString("cache-secs", "120")
        // Keep demuxing ahead for longer windows so pause can continue prebuffering more data.
        MPVLib.setOptionString("demuxer-readahead-secs", "120")
        MPVLib.setOptionString("demuxer-max-bytes", "150MiB")
        MPVLib.setOptionString("demuxer-max-back-bytes", "50MiB")
        MPVLib.setOptionString("cache-pause", "yes")
        MPVLib.setOptionString("cache-pause-initial", "yes")
        MPVLib.setOptionString("cache-pause-wait", "5")
        MPVLib.setOptionString("input-default-bindings", "yes")
        MPVLib.setOptionString("ytdl", "no")
        MPVLib.setOptionString(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
        )
    }

    override fun postInitOptions() {
        MPVLib.setOptionString("save-position-on-quit", "no")
    }

    override fun observeProperties() = Unit
}

private fun mpvSecondsToMs(value: Double?): Long {
    if (value == null || value.isNaN() || value.isInfinite() || value < 0) return 0L
    return (value * 1000.0).roundToLong().coerceAtLeast(0L)
}

private fun getMpvTrackSelectionId(property: String): Int {
    val raw = runCatching { MPVLib.getPropertyString(property) }.getOrNull()?.trim().orEmpty()
    if (raw.isEmpty()) return -1
    val normalized = raw.lowercase(Locale.US)
    if (normalized == "no" || normalized == "auto") return -1
    return raw.toIntOrNull() ?: runCatching { MPVLib.getPropertyInt(property) ?: -1 }.getOrDefault(-1)
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private data class MpvTrackItem(
    val id: Int,
    val label: String,
    val selected: Boolean
)

private fun loadTrackItems(
    context: Context,
    type: String,
    property: String,
    includeOff: Boolean,
    includeAuto: Boolean
): List<MpvTrackItem> {
    val items = mutableListOf<MpvTrackItem>()
    val currentId = getMpvTrackSelectionId(property)
    val mode = getMpvTrackSelectionMode(property)
    if (includeAuto) {
        items += MpvTrackItem(
            id = -2,
            label = context.getString(Media3UiR.string.exo_track_selection_auto),
            selected = mode == TrackSelectionMode.AUTO
        )
    }
    if (includeOff) {
        items += MpvTrackItem(
            id = -1,
            label = context.getString(R.string.player_track_off),
            selected = mode == TrackSelectionMode.OFF || (mode == TrackSelectionMode.SPECIFIC && currentId < 0)
        )
    }

    val count = MPVLib.getPropertyInt("track-list/count") ?: 0
    for (i in 0 until count) {
        val trackType = MPVLib.getPropertyString("track-list/$i/type") ?: continue
        if (trackType != type) continue
        // Media3 hides forced subtitle tracks in the user-selectable subtitle list.
        if (type == "sub" && getTrackListBoolean(i, "forced")) continue
        val trackId = MPVLib.getPropertyInt("track-list/$i/id") ?: continue
        val lang = MPVLib.getPropertyString("track-list/$i/lang")
        val title = MPVLib.getPropertyString("track-list/$i/title")
        val label = buildMpvTrackNameLikeMedia3(
            context = context,
            trackId = trackId,
            type = trackType,
            trackIndex = i,
            lang = lang,
            title = title
        )
        items += MpvTrackItem(
            id = trackId,
            label = label,
            selected = currentId == trackId
        )
    }
    return items
}

private fun applyTrackSelection(property: String, trackId: Int) {
    if (trackId == -2) {
        MPVLib.setPropertyString(property, "auto")
    } else if (trackId < 0) {
        MPVLib.setPropertyString(property, "no")
    } else {
        MPVLib.setPropertyInt(property, trackId)
    }
}

private enum class TrackSelectionMode {
    OFF,
    AUTO,
    SPECIFIC
}

private fun getMpvTrackSelectionMode(property: String): TrackSelectionMode {
    val rawOption = runCatching { MPVLib.getPropertyString("options/$property") }.getOrNull()?.trim().orEmpty()
    return when (rawOption.lowercase(Locale.US)) {
        "no" -> TrackSelectionMode.OFF
        "auto", "" -> TrackSelectionMode.AUTO
        else -> TrackSelectionMode.SPECIFIC
    }
}

private fun getTrackListBoolean(trackIndex: Int, key: String): Boolean {
    val path = "track-list/$trackIndex/$key"
    val raw = runCatching { MPVLib.getPropertyString(path) }.getOrNull()?.trim().orEmpty()
    if (raw.equals("yes", ignoreCase = true) || raw == "1" || raw.equals("true", ignoreCase = true)) return true
    if (raw.equals("no", ignoreCase = true) || raw == "0" || raw.equals("false", ignoreCase = true)) return false
    val intFallback = runCatching { MPVLib.getPropertyInt(path) }.getOrNull()
    return intFallback == 1
}

private fun buildAudioSelectionSubText(
    context: Context,
    tracks: List<MpvTrackItem>,
    currentMode: TrackSelectionMode
): String {
    if (tracks.isEmpty()) return context.getString(Media3UiR.string.exo_track_selection_none)
    if (currentMode == TrackSelectionMode.AUTO) return context.getString(Media3UiR.string.exo_track_selection_auto)
    val selected = tracks.firstOrNull { it.selected }
    return selected?.label ?: context.getString(Media3UiR.string.exo_track_selection_auto)
}

private fun buildMpvTrackNameLikeMedia3(
    context: Context,
    trackId: Int,
    type: String,
    trackIndex: Int,
    lang: String?,
    title: String?
): String {
    val cleanTitle = title?.trim().orEmpty()
    val languageDisplayName = toDisplayLanguageName(lang)
    val primary = when {
        cleanTitle.isNotEmpty() -> cleanTitle
        languageDisplayName.isNotEmpty() -> languageDisplayName
        else -> context.getString(R.string.player_track_label_fallback, trackId)
    }

    if (type != "audio") return primary

    val channels = resolveAudioChannelsText(context, trackIndex)
    val bitrate = resolveAudioBitrateText(context, trackIndex)
    return joinWithMedia3Separator(context, listOf(primary, channels, bitrate))
}

private fun toDisplayLanguageName(lang: String?): String {
    val tag = normalizeLanguageTag(lang?.trim().orEmpty())
    if (tag.isEmpty() || tag.equals("und", ignoreCase = true)) return ""
    val locale = runCatching { Locale.forLanguageTag(tag) }.getOrNull() ?: return ""
    val displayLocale = Locale.getDefault()
    val displayName = runCatching { locale.getDisplayName(displayLocale) }.getOrNull()?.trim().orEmpty()
    if (displayName.isEmpty()) return ""
    val firstCodePointEnd = runCatching { displayName.offsetByCodePoints(0, 1) }.getOrNull() ?: return displayName
    return displayName.substring(0, firstCodePointEnd).uppercase(displayLocale) + displayName.substring(firstCodePointEnd)
}

private fun normalizeLanguageTag(rawTag: String): String {
    if (rawTag.isBlank()) return ""
    val normalized = rawTag.replace('_', '-')
    val lower = normalized.lowercase(Locale.US)
    return when (lower) {
        "spa" -> "es"
        "eng" -> "en"
        "por" -> "pt"
        "fra", "fre" -> "fr"
        "deu", "ger" -> "de"
        "ita" -> "it"
        else -> normalized
    }
}

private fun resolveAudioChannelsText(context: Context, trackIndex: Int): String {
    val channelCountCandidates = listOf(
        "audio-channels",
        "demux-channel-count",
        "channel-count"
    )
    val channelCount = channelCountCandidates
        .asSequence()
        .mapNotNull { key -> runCatching { MPVLib.getPropertyInt("track-list/$trackIndex/$key") }.getOrNull() }
        .firstOrNull { it > 0 } ?: return ""

    return when (channelCount) {
        1 -> context.getString(Media3UiR.string.exo_track_mono)
        2 -> context.getString(Media3UiR.string.exo_track_stereo)
        6, 7 -> context.getString(Media3UiR.string.exo_track_surround_5_point_1)
        8 -> context.getString(Media3UiR.string.exo_track_surround_7_point_1)
        else -> context.getString(Media3UiR.string.exo_track_surround)
    }
}

private fun resolveAudioBitrateText(context: Context, trackIndex: Int): String {
    val bitrateCandidates = listOf(
        "demux-bitrate",
        "hls-bitrate",
        "bitrate"
    )
    val bitrate = bitrateCandidates
        .asSequence()
        .mapNotNull { key ->
            runCatching { MPVLib.getPropertyDouble("track-list/$trackIndex/$key") }.getOrNull()
                ?: runCatching { MPVLib.getPropertyInt("track-list/$trackIndex/$key")?.toDouble() }.getOrNull()
        }
        .firstOrNull { it.isFinite() && it > 0.0 } ?: return ""

    return context.getString(Media3UiR.string.exo_track_bitrate, (bitrate / 1_000_000.0f).toFloat())
}

private fun joinWithMedia3Separator(context: Context, parts: List<String>): String {
    var result = ""
    parts.filter { it.isNotBlank() }.forEach { part ->
        result = if (result.isEmpty()) part else context.getString(Media3UiR.string.exo_item_list, result, part)
    }
    return result
}

private fun hasTrackType(type: String): Boolean {
    val count = MPVLib.getPropertyInt("track-list/count") ?: return false
    for (i in 0 until count) {
        val trackType = MPVLib.getPropertyString("track-list/$i/type") ?: continue
        if (trackType == type) return true
    }
    return false
}

@Composable
private fun TrackMenuDialog(
    hasAudio: Boolean,
    speedLabel: String,
    audioSubText: String,
    onDismiss: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenAudio: () -> Unit,
) {
    Media3SettingsPopup(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Media3SettingsRow(
                iconRes = Media3UiR.drawable.exo_ic_speed,
                title = stringResource(R.string.player_playback_speed),
                subtitle = speedLabel,
                onClick = onOpenSpeed
            )
            Media3SettingsRow(
                iconRes = Media3UiR.drawable.exo_ic_audiotrack,
                title = stringResource(R.string.player_track_audio),
                subtitle = audioSubText,
                onClick = onOpenAudio
            )
        }
    }
}

@Composable
private fun Media3SettingsPopup(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resources = context.resources
    val horizontalOffsetPx = resources.getDimensionPixelSize(Media3UiR.dimen.exo_settings_offset)
    val bottomBarHeightPx = resources.getDimensionPixelSize(Media3UiR.dimen.exo_styled_bottom_bar_height)
    val verticalOffsetPx = bottomBarHeightPx + horizontalOffsetPx
    val density = context.resources.displayMetrics.density
    val horizontalOffsetDp = (horizontalOffsetPx / density).dp
    val verticalOffsetDp = (verticalOffsetPx / density).dp
    val minPopupWidthPx = resources.getDimensionPixelSize(Media3UiR.dimen.exo_setting_width)
    val popupWidthDp = (minPopupWidthPx / density).dp

    Popup(
        alignment = Alignment.BottomEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier
                .padding(end = horizontalOffsetDp, bottom = verticalOffsetDp)
                .widthIn(min = popupWidthDp, max = popupWidthDp)
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            color = Color.Black.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun TrackSelectionDialog(
    title: String,
    tracks: List<MpvTrackItem>,
    onDismiss: () -> Unit,
    onTrackSelected: (Int) -> Unit
) {
    Media3SettingsPopup(onDismiss = onDismiss) {
        Column {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tracks.forEach { track ->
                    Media3SubSettingRow(
                        label = track.label,
                        selected = track.selected,
                        onClick = { onTrackSelected(track.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Media3SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.padding(start = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Media3SubSettingRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = Media3UiR.drawable.exo_ic_check),
            contentDescription = null,
            tint = Color.White.copy(alpha = if (selected) 1f else 0f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 10.dp)
                .fillMaxWidth()
        )
    }
}

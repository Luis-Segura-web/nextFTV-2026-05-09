package com.stream.nextftv.presentation.player

import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.ImageView
import androidx.media3.common.PlaybackException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.asDrawable
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.shuyu.gsyvideoplayer.cache.CacheFactory
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.listener.GSYVideoProgressListener
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.model.VideoOptionModel
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import com.shuyu.gsyvideoplayer.player.IPlayerInitSuccessListener
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import com.stream.nextftv.data.utils.PlaybackCacheLocator
import com.stream.nextftv.utils.findActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.IjkTimedText

private const val GSY_PLAYER_TAG = "GsyVideoPlayer"
private const val EARLY_COMPLETION_THRESHOLD = 0.98f
private const val EARLY_COMPLETION_TOLERANCE_MS = 15_000L
private val knownIjkFallbackUrls = linkedSetOf<String>()

private fun shouldFallbackToIjk(errorCode: Int?): Boolean {
    return when (errorCode) {
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> true
        else -> false
    }
}

private fun extractPlaybackErrorCode(objects: Array<out Any?>): Int? {
    return objects.getOrNull(2) as? Int
}

@Composable
fun GsyVideoPlayer(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    isLive: Boolean = false,
    resumePositionMs: Long = 0L,
    thumbUrl: String? = null,
    contentType: PlaybackContentType = if (isLive) PlaybackContentType.LIVE else PlaybackContentType.MOVIE,
    isInSystemPipMode: Boolean = false,
    onEnterFullscreen: () -> Unit = {},
    onQuitFullscreen: () -> Unit = {},
    onClose: () -> Unit = {},
    onPipRequested: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    shouldKeepFullscreenOnAutoComplete: () -> Boolean = { false },
    onLoading: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
    onProgress: (Long) -> Unit = {},
    onProgressSnapshot: (Long, Long) -> Unit = { _, _ -> }
    ,
    onPlaybackCompleted: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isLiveContent = contentType == PlaybackContentType.LIVE
    val isPipEnabled = PipPreferences.isEnabled
    val isBackgroundPlaybackEnabled = BackgroundPlaybackPreferences.isEnabled
    val supportsPip = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
    val effectiveOnPipRequested = if (isPipEnabled && supportsPip) onPipRequested else null
    val playTag = PlaybackCacheLocator.playTagFor(contentType)
    val playPosition = when (contentType) {
        PlaybackContentType.LIVE -> 0
        PlaybackContentType.MOVIE -> 0
        PlaybackContentType.SERIES -> url.hashCode()
    }
    val maxRetryAttempts = when (contentType) {
        PlaybackContentType.LIVE -> 3
        PlaybackContentType.MOVIE -> 2
        PlaybackContentType.SERIES -> 2
    }
    val retryDelayMs = when (contentType) {
        PlaybackContentType.LIVE -> 1200L
        PlaybackContentType.MOVIE -> 1800L
        PlaybackContentType.SERIES -> 1800L
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val selectedEngine = PlaybackEnginePreferences.engineFor(contentType)
    var activeEngine by remember(url, selectedEngine) {
        mutableStateOf(
            if (selectedEngine == PlaybackEngine.EXO && knownIjkFallbackUrls.contains(url)) {
                PlaybackEngine.IJK
            } else {
                selectedEngine
            }
        )
    }
    val selectedCacheMode = PlaybackCachePreferences.cacheModeFor(contentType)
    val usesIjkEngine = activeEngine == PlaybackEngine.IJK
    val isLocalPlayback = remember(url) {
        url.startsWith("/") || url.startsWith("file://")
    }
    var codecFallbackAttempted by remember(url, selectedEngine) {
        mutableStateOf(selectedEngine == PlaybackEngine.IJK)
    }
    val effectiveCacheMode = when {
        isLocalPlayback -> PlaybackCacheMode.DISABLED
        selectedCacheMode == PlaybackCacheMode.EXO && activeEngine != PlaybackEngine.EXO -> PlaybackCacheMode.DISABLED
        else -> selectedCacheMode
    }
    val cacheEnabled = effectiveCacheMode != PlaybackCacheMode.DISABLED
    var isBuffering by remember(url) { mutableStateOf(true) }
    var retryGeneration by remember(url) { mutableIntStateOf(0) }
    var retryAttempts by remember(url) { mutableIntStateOf(0) }
    var retryJob by remember(url) { mutableStateOf<Job?>(null) }
    var positionSamplerJob by remember(url) { mutableStateOf<Job?>(null) }
    var videoFallbackMonitorJob by remember(url) { mutableStateOf<Job?>(null) }
    var playerRef by remember(url) { mutableStateOf<IptvGsyPlayerView?>(null) }
    var orientationUtils by remember(url) { mutableStateOf<OrientationUtils?>(null) }
    var retryResumePositionMs by remember(url) { mutableStateOf(resumePositionMs.coerceAtLeast(0L)) }
    var lastObservedDurationMs by remember(url) { mutableStateOf(0L) }
    var isRecreatingPlayer by remember(url) { mutableStateOf(false) }
    var pendingFullscreenRestore by remember(url, isFullScreen) { mutableStateOf(isFullScreen) }
    var suppressQuitFullscreenCallback by remember(url) { mutableStateOf(false) }
    val currentPlayerRef by rememberUpdatedState(playerRef)

    fun currentPlaybackPlayer(fallback: IptvGsyPlayerView): IptvGsyPlayerView {
        val basePlayer = playerRef ?: fallback
        return basePlayer.currentPlayer as? IptvGsyPlayerView
            ?: basePlayer.fullWindowPlayer as? IptvGsyPlayerView
            ?: basePlayer
    }

    fun requestPlaybackEngineSwitch(fallback: IptvGsyPlayerView, streamUrl: String) {
        val currentPlayer = currentPlaybackPlayer(fallback)
        val nextEngine = if (activeEngine == PlaybackEngine.EXO) PlaybackEngine.IJK else PlaybackEngine.EXO
        val resumePosition = if (isLiveContent) {
            0L
        } else {
            currentPlayer.currentPositionWhenPlaying.coerceAtLeast(0L)
        }
        val currentDuration = currentPlayer.duration.coerceAtLeast(0L)

        retryResumePositionMs = resumePosition
        if (currentDuration > 0L) {
            lastObservedDurationMs = currentDuration
        }
        onProgress(resumePosition)
        onProgressSnapshot(resumePosition, currentDuration)
        positionSamplerJob?.cancel()
        videoFallbackMonitorJob?.cancel()
        retryJob?.cancel()
        retryAttempts = 0
        codecFallbackAttempted = nextEngine == PlaybackEngine.IJK
        if (nextEngine == PlaybackEngine.EXO) {
            knownIjkFallbackUrls.remove(streamUrl)
        }
        PlaybackEnginePreferences.update(context, contentType, nextEngine)
        currentPlayer.release()
        playerRef = null
        isBuffering = true
        onLoading(true)
        activeEngine = nextEngine
        retryGeneration += 1
    }

    DisposableEffect(lifecycleOwner, url, isBackgroundPlaybackEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            val player = currentPlayerRef ?: return@LifecycleEventObserver
            val isInPictureInPictureMode = player.context.findActivity()?.isInPictureInPictureMode == true
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isInPictureInPictureMode && !isBackgroundPlaybackEnabled) {
                        player.onVideoPause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Do not force-resume if the user paused playback manually while the app was backgrounded.
                    if (player.currentState != GSYVideoPlayer.CURRENT_STATE_PAUSE) {
                        player.onVideoResume(false)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> player.release()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            retryJob?.cancel()
            positionSamplerJob?.cancel()
            videoFallbackMonitorJob?.cancel()
            PlayerFullscreenState.isFullscreen = false
            GSYVideoManager.instance().setPlayerInitSuccessListener(null)
            orientationUtils?.releaseListener()
            orientationUtils = null
            val playerToRelease =
                currentPlayerRef?.currentPlayer as? IptvGsyPlayerView ?: currentPlayerRef
            playerToRelease?.release()
            if (playerRef === playerToRelease) {
                playerRef = null
            }
            GlobalPlaybackManager.clearSession(url)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(playerRef, isFullScreen, pendingFullscreenRestore) {
        val player = playerRef
        if (
            player != null &&
            isFullScreen &&
            pendingFullscreenRestore &&
            !player.isIfCurrentIsFullscreen
        ) {
            player.post {
                val currentPlayer = playerRef ?: return@post
                if (!isFullScreen || !pendingFullscreenRestore || currentPlayer.isIfCurrentIsFullscreen) {
                    return@post
                }
                pendingFullscreenRestore = false
                currentPlayer.onFullscreenRequested?.invoke()
                    ?: currentPlayer.startWindowFullscreen(
                        currentPlayer.context.findActivity() ?: currentPlayer.context,
                        true,
                        true
                    )
            }
        } else if (!isFullScreen) {
            pendingFullscreenRestore = false
        }
        onDispose { }
    }

    key(url, retryGeneration, activeEngine, effectiveCacheMode) {
        if (!isRecreatingPlayer) {
            AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                PlayerFactory.setPlayManager(
                    if (activeEngine == PlaybackEngine.EXO) PatchedExo2PlayerManager::class.java
                    else IjkPlayerManager::class.java
                )
                if (cacheEnabled) {
                    CacheFactory.setCacheManager(
                        when (effectiveCacheMode) {
                            PlaybackCacheMode.EXO -> ExoPlayerCacheManager::class.java
                            PlaybackCacheMode.PROXY -> ProxyCacheManager::class.java
                            PlaybackCacheMode.DISABLED -> null
                        }
                    )
                }

                IptvGsyPlayerView(context).apply {
                    val hostPlayer = this
                    val streamUrl = url
                    val activity = context.findActivity()
                    val cacheDirectory = PlaybackCacheLocator.cacheDirectory(context, playTag)
                    val requestHeaders = hashMapOf(
                        "allowCrossProtocolRedirects" to "true",
                        "User-Agent" to "GSY"
                    )
                    val thumbView = thumbUrl?.takeIf { it.isNotBlank() && !isLiveContent }?.let { imageUrl ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            SingletonImageLoader.get(context).enqueue(
                                ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .target(
                                        onStart = { image -> setImageDrawable(image?.asDrawable(resources)) },
                                        onError = { image -> setImageDrawable(image?.asDrawable(resources)) },
                                        onSuccess = { image -> setImageDrawable(image.asDrawable(resources)) }
                                    )
                                    .build()
                            )
                        }
                    }

                    playerRef = this
                    isBuffering = true
                    onLoading(true)
                    onCloseRequested = {
                        val currentPlayer = playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: hostPlayer
                        val finalPosition = currentPlayer.currentPositionWhenPlaying.coerceAtLeast(0L)
                        val finalDuration = currentPlayer.duration.coerceAtLeast(0L)
                        Log.w(
                            GSY_PLAYER_TAG,
                            "player_close_requested url=$streamUrl engine=$activeEngine position=$finalPosition duration=$finalDuration"
                        )
                        onProgressSnapshot(finalPosition, finalDuration)
                        retryJob?.cancel()
                        GlobalPlaybackManager.stopAndClear()
                        PlayerFullscreenState.isFullscreen = false
                        onClose()
                    }
                    this.onPipRequested = effectiveOnPipRequested
                    playbackEngine = activeEngine
                    onPlaybackEngineToggleRequested = {
                        requestPlaybackEngineSwitch(hostPlayer, streamUrl)
                    }
                    onFullscreenRequested = {
                        val fullscreenActivity = activity ?: context
                        startWindowFullscreen(fullscreenActivity, true, true)
                    }
                    this.isInSystemPipMode = isInSystemPipMode
                    playbackContentType = contentType
                    configureBaseUi()
                    orientationUtils?.releaseListener()
                    orientationUtils = if (activity != null) {
                        OrientationUtils(activity, hostPlayer).apply {
                            setEnable(false)
                            setRotateWithSystem(false)
                        }
                    } else {
                        null
                    }

                    GSYVideoManager.instance().setPlayerInitSuccessListener(
                        object : IPlayerInitSuccessListener {
                            override fun onPlayerInitSuccess(player: IMediaPlayer?, model: com.shuyu.gsyvideoplayer.model.GSYModel?) {
                                player ?: return
                                player.setOnTimedTextListener { _, text: IjkTimedText? ->
                                    val currentPlayer = playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: hostPlayer
                                    currentPlayer.updateSubtitleText(text?.text)
                                }
                            }
                        }
                    )

                    if (usesIjkEngine) {
                        GSYVideoManager.instance().setOptionModelList(
                            listOf(
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                                    "subtitle",
                                    1
                                ),
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                                    "min-frames",
                                    25
                                ),
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                                    "probesize",
                                    5_000_000
                                ),
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                                    "analyzemaxduration",
                                    15_000_000
                                ),
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                                    "find_stream_info",
                                    1
                                ),
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                                    "enable-accurate-seek",
                                    if (isLiveContent) 0 else 1
                                ),
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_CODEC,
                                    "ac",
                                    2
                                ),
                                VideoOptionModel(
                                    IjkMediaPlayer.OPT_CATEGORY_CODEC,
                                    "request_channel_layout",
                                    3
                                )
                            )
                        )
                    } else {
                        GSYVideoManager.instance().setOptionModelList(null)
                    }

                    val callback = object : GSYSampleCallBack() {
                        fun currentPlaybackView(): IptvGsyPlayerView {
                            return playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: hostPlayer
                        }

                        fun canEvaluateVisibleVideoFallback(currentPlayer: IptvGsyPlayerView): Boolean {
                            val activity = currentPlayer.context.findActivity()
                            val isScreenInteractive =
                                currentPlayer.context.getSystemService(PowerManager::class.java)
                                    ?.isInteractive
                                    ?: true
                            return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                                !isInSystemPipMode &&
                                activity?.isInPictureInPictureMode != true &&
                                currentPlayer.windowVisibility == android.view.View.VISIBLE &&
                                isScreenInteractive
                        }

                        fun triggerCodecFallback(reason: String, errorCode: Int?) {
                            if (codecFallbackAttempted || activeEngine == PlaybackEngine.IJK) return

                            val playerToRelease = playerRef ?: currentPlaybackView()
                            positionSamplerJob?.cancel()
                            videoFallbackMonitorJob?.cancel()
                            retryJob?.cancel()
                            retryAttempts = 0
                            retryResumePositionMs = maxOf(
                                retryResumePositionMs.coerceAtLeast(0L),
                                playerToRelease.currentPositionWhenPlaying.coerceAtLeast(0L)
                            )
                            playerRef = null
                            isBuffering = true
                            onLoading(true)
                            Log.w(
                                GSY_PLAYER_TAG,
                                "codec_fallback_to_ijk url=$streamUrl reason=$reason errorCode=$errorCode position=$retryResumePositionMs cache=$effectiveCacheMode"
                            )
                            playerToRelease.release()
                            codecFallbackAttempted = true
                            knownIjkFallbackUrls += streamUrl
                            isRecreatingPlayer = true
                            scope.launch {
                                delay(16)
                                activeEngine = PlaybackEngine.IJK
                                retryGeneration += 1
                                isRecreatingPlayer = false
                            }
                        }

                        fun startPositionSampler() {
                            positionSamplerJob?.cancel()
                            positionSamplerJob = scope.launch {
                                while (true) {
                                    val currentPlayer =
                                        playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: hostPlayer
                                    val sampledPosition = currentPlayer.currentPositionWhenPlaying.coerceAtLeast(0L)
                                    val sampledDuration = currentPlayer.duration.coerceAtLeast(0L)
                                    if (sampledPosition > retryResumePositionMs) {
                                        retryResumePositionMs = sampledPosition
                                    }
                                    if (sampledDuration > lastObservedDurationMs) {
                                        lastObservedDurationMs = sampledDuration
                                    }
                                    delay(500)
                                }
                            }
                        }

                        fun startVisibleVideoFallbackMonitor() {
                            videoFallbackMonitorJob?.cancel()
                            if (
                                activeEngine != PlaybackEngine.EXO ||
                                codecFallbackAttempted ||
                                isLiveContent
                            ) {
                                return
                            }
                            videoFallbackMonitorJob = scope.launch {
                                delay(4000)
                                val currentPlayer = currentPlaybackView()
                                val currentPosition = currentPlayer.currentPositionWhenPlaying.coerceAtLeast(0L)
                                if (
                                    activeEngine == PlaybackEngine.EXO &&
                                    !codecFallbackAttempted &&
                                    !isLiveContent &&
                                    currentPosition >= 2500L &&
                                    currentPlayer.hasExoVideoTrack() &&
                                    currentPlayer.shouldAllowVisibleVideoFallback() &&
                                    !currentPlayer.hasVideoRenderingStarted &&
                                    canEvaluateVisibleVideoFallback(currentPlayer)
                                ) {
                                    triggerCodecFallback(
                                        reason = "visible_audio_only_timeout",
                                        errorCode = null
                                    )
                                }
                            }
                        }

                        fun scheduleRetry() {
                            positionSamplerJob?.cancel()
                            videoFallbackMonitorJob?.cancel()
                            if (retryAttempts < maxRetryAttempts) {
                                retryAttempts += 1
                                isBuffering = true
                                onLoading(true)
                                retryJob?.cancel()
                                retryJob = scope.launch {
                                    delay(retryDelayMs)
                                    playerRef?.release()
                                    playerRef = null
                                    retryGeneration += 1
                                }
                            } else {
                                retryJob?.cancel()
                                isBuffering = false
                                onLoading(false)
                                (playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: hostPlayer).updateSubtitleText(null)
                                onError("GSY no pudo reproducir el stream")
                            }
                        }

                        fun isPrematureCompletion(currentPosition: Long, duration: Long): Boolean {
                            if (isLiveContent) return false
                            if (duration <= 0L) return false
                            val threshold = (duration * EARLY_COMPLETION_THRESHOLD).toLong()
                            val missingMs = (duration - currentPosition).coerceAtLeast(0L)
                            return currentPosition < threshold && missingMs > EARLY_COMPLETION_TOLERANCE_MS
                        }

                        override fun onStartPrepared(url: String?, vararg objects: Any?) {
                            GlobalPlaybackManager.registerSession(
                                url = streamUrl,
                                title = title,
                                isLive = isLiveContent,
                                isPlaying = true,
                                onPlayPause = {
                                    val currentPlayer = playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef
                                    when (currentPlayer?.currentState) {
                                        GSYVideoPlayer.CURRENT_STATE_PLAYING,
                                        GSYVideoPlayer.CURRENT_STATE_PLAYING_BUFFERING_START -> currentPlayer.onVideoPause()
                                        GSYVideoPlayer.CURRENT_STATE_PAUSE -> currentPlayer.onVideoResume(false)
                                        else -> currentPlayer?.startAfterPrepared()
                                    }
                                },
                                onStop = {
                                    val currentPlayer = playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef
                                    currentPlayer?.release()
                                },
                                onNext = onNext,
                                onPrevious = onPrevious
                            )
                            PipActionsController.requestRefresh()
                        }

                        override fun onPrepared(url: String?, vararg objects: Any?) {
                            retryJob?.cancel()
                            retryAttempts = 0
                            isBuffering = false
                            onLoading(false)
                            val currentPlayer = currentPlaybackView()
                            currentPlayer.resetVideoRenderingState()
                            currentPlayer.logExoPlaybackCompatibility("onPrepared")
                            if (
                                activeEngine == PlaybackEngine.EXO &&
                                !codecFallbackAttempted &&
                                currentPlayer.hasUnsupportedUnselectedExoVideoTrack()
                            ) {
                                triggerCodecFallback(
                                    reason = "exo_unselected_unknown_video_track",
                                    errorCode = null
                                )
                                return
                            }
                            startPositionSampler()
                            startVisibleVideoFallbackMonitor()
                            orientationUtils?.isEnable = hostPlayer.isRotateWithSystem
                            (playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef)?.configureBaseUi()
                            GlobalPlaybackManager.updatePlaybackState(streamUrl, true)
                            PipActionsController.requestRefresh()
                        }

                        override fun onAutoComplete(url: String?, vararg objects: Any?) {
                            val basePlayer = playerRef ?: hostPlayer
                            val fullscreenPlayer = basePlayer.fullWindowPlayer as? IptvGsyPlayerView
                            val currentPlaybackPlayer =
                                (basePlayer.currentPlayer as? IptvGsyPlayerView ?: basePlayer)
                            val fallbackPlayer = fullscreenPlayer ?: currentPlaybackPlayer
                            val currentPosition = retryResumePositionMs
                                .coerceAtLeast(fallbackPlayer.currentPositionWhenPlaying.coerceAtLeast(0L))
                            val duration = lastObservedDurationMs
                                .coerceAtLeast(fallbackPlayer.duration.coerceAtLeast(0L))
                            Log.w(
                                GSY_PLAYER_TAG,
                                "playback_auto_complete url=$streamUrl engine=$activeEngine position=$currentPosition duration=$duration cache=$effectiveCacheMode"
                            )
                            if (isPrematureCompletion(currentPosition, duration)) {
                                retryResumePositionMs = currentPosition
                                Log.w(
                                    GSY_PLAYER_TAG,
                                    "premature_completion url=$streamUrl position=$currentPosition duration=$duration local=$isLocalPlayback cache=$effectiveCacheMode retry=$retryAttempts/$maxRetryAttempts"
                                )
                                GlobalPlaybackManager.updatePlaybackState(streamUrl, false)
                                PipActionsController.requestRefresh()
                                scheduleRetry()
                                return
                            }
                            isBuffering = false
                            onLoading(false)
                            positionSamplerJob?.cancel()
                            videoFallbackMonitorJob?.cancel()
                            fallbackPlayer.updateSubtitleText(null)
                            suppressQuitFullscreenCallback = shouldKeepFullscreenOnAutoComplete()
                            if (suppressQuitFullscreenCallback) {
                                Log.d(
                                    GSY_PLAYER_TAG,
                                    "suppress_fullscreen_exit_on_autonext fullscreenPlayer=${fullscreenPlayer != null} currentIsFullscreen=${fallbackPlayer.isIfCurrentIsFullscreen}"
                                )
                                fallbackPlayer.suppressNextFullscreenOrientationReset()
                                fullscreenPlayer
                                    ?.takeIf { it !== fallbackPlayer }
                                    ?.suppressNextFullscreenOrientationReset()
                            }
                            onPlaybackCompleted(currentPosition, duration)
                            GlobalPlaybackManager.clearSession(streamUrl)
                            PipActionsController.requestRefresh()
                        }

                        override fun onPlayError(url: String?, vararg objects: Any?) {
                            val errorCode = extractPlaybackErrorCode(objects)
                            val currentPlayer = playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: hostPlayer
                            Log.w(
                                GSY_PLAYER_TAG,
                                "playback_error url=$streamUrl engine=$activeEngine errorCode=$errorCode position=${currentPlayer.currentPositionWhenPlaying.coerceAtLeast(0L)} duration=${currentPlayer.duration.coerceAtLeast(0L)} cache=$effectiveCacheMode"
                            )
                            val canFallbackToIjk =
                                activeEngine == PlaybackEngine.EXO &&
                                    !codecFallbackAttempted &&
                                    shouldFallbackToIjk(errorCode)
                            if (canFallbackToIjk) {
                                triggerCodecFallback(
                                    reason = "exo_codec_error",
                                    errorCode = errorCode
                                )
                                return
                            }
                            positionSamplerJob?.cancel()
                            videoFallbackMonitorJob?.cancel()
                            GlobalPlaybackManager.clearSession(streamUrl)
                            PipActionsController.requestRefresh()
                            retryResumePositionMs =
                                (playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: hostPlayer)
                                    .currentPositionWhenPlaying
                                    .coerceAtLeast(0L)
                            scheduleRetry()
                        }

                        override fun onClickStartIcon(url: String?, vararg objects: Any?) {
                            GlobalPlaybackManager.updatePlaybackState(streamUrl, true)
                            PipActionsController.requestRefresh()
                        }

                        override fun onClickStartThumb(url: String?, vararg objects: Any?) {
                            GlobalPlaybackManager.updatePlaybackState(streamUrl, true)
                            PipActionsController.requestRefresh()
                        }

                        override fun onClickResume(url: String?, vararg objects: Any?) {
                            GlobalPlaybackManager.updatePlaybackState(streamUrl, true)
                            PipActionsController.requestRefresh()
                        }

                        override fun onClickResumeFullscreen(url: String?, vararg objects: Any?) {
                            GlobalPlaybackManager.updatePlaybackState(streamUrl, true)
                            PipActionsController.requestRefresh()
                        }

                        override fun onClickStop(url: String?, vararg objects: Any?) {
                            GlobalPlaybackManager.updatePlaybackState(streamUrl, false)
                            PipActionsController.requestRefresh()
                        }

                        override fun onClickStopFullscreen(url: String?, vararg objects: Any?) {
                            GlobalPlaybackManager.updatePlaybackState(streamUrl, false)
                            PipActionsController.requestRefresh()
                        }

                        override fun onEnterFullscreen(url: String?, vararg objects: Any?) {
                            PlayerFullscreenState.isFullscreen = true
                            (playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef)?.configureBaseUi()
                            onEnterFullscreen()
                        }

                        override fun onQuitFullscreen(url: String?, vararg objects: Any?) {
                            if (suppressQuitFullscreenCallback) {
                                suppressQuitFullscreenCallback = false
                                pendingFullscreenRestore = true
                                (playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef)?.configureBaseUi()
                                return
                            }
                            PlayerFullscreenState.isFullscreen = false
                            (playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef)?.configureBaseUi()
                            onQuitFullscreen()
                        }

                        override fun onEnterSmallWidget(url: String?, vararg objects: Any?) {
                            onProgressSnapshot(
                                hostPlayer.currentPositionWhenPlaying,
                                hostPlayer.duration
                            )
                        }
                    }

                    val progressListener = GSYVideoProgressListener { _, _, currentPosition, duration ->
                        retryResumePositionMs = currentPosition.coerceAtLeast(0L)
                        lastObservedDurationMs = duration.coerceAtLeast(0L)
                        onProgress(currentPosition)
                        onProgressSnapshot(currentPosition, duration)
                    }

                    GSYVideoOptionBuilder()
                        .setUrl(url)
                        .setThumbImageView(thumbView)
                        .setVideoTitle(title)
                        .setMapHeadData(requestHeaders)
                        .setPlayTag(playTag)
                        .setPlayPosition(playPosition)
                        .setCacheWithPlay(cacheEnabled)
                        .setCachePath(cacheDirectory)
                        .setAutoFullWithSize(true)
                        .setNeedOrientationUtils(false)
                        .setRotateViewAuto(false)
                        .setRotateWithSystem(false)
                        .setLockLand(true)
                        .setNeedLockFull(true)
                        .setShowFullAnimation(false)
                        .setIsTouchWiget(!isLiveContent)
                        .setIsTouchWigetFull(!isLiveContent)
                        .setDismissControlTime(5000)
                        .setShowDragProgressTextOnSeekBar(!isLiveContent)
                        .setShowPauseCover(!isLiveContent)
                        .setReleaseWhenLossAudio(!isBackgroundPlaybackEnabled)
                        .setSurfaceErrorPlay(true)
                        .setNeedShowWifiTip(false)
                        .setThumbPlay(false)
                        .setSeekRatio(1f)
                        .setStartAfterPrepared(true)
                        .setSeekOnStart(if (isLiveContent) 0L else retryResumePositionMs.coerceAtLeast(0L))
                        .setVideoAllCallBack(callback)
                        .setGSYVideoProgressListener(progressListener)
                        .build(this)

                    startPlayLogic()
                }
            },
                update = { player ->
                    playerRef = player
                    val playersToUpdate = buildList {
                        add(player)
                        val activePlayer = player.currentPlayer as? IptvGsyPlayerView
                        if (activePlayer != null && activePlayer !== player) {
                            add(activePlayer)
                        }
                    }
                    val closeHandler = {
                        val currentPlayer = playerRef?.currentPlayer as? IptvGsyPlayerView ?: playerRef ?: player
                        val finalPosition = currentPlayer.currentPositionWhenPlaying.coerceAtLeast(0L)
                        val finalDuration = currentPlayer.duration.coerceAtLeast(0L)
                        Log.w(
                            GSY_PLAYER_TAG,
                            "player_close_requested url=$url engine=$activeEngine position=$finalPosition duration=$finalDuration"
                        )
                        onProgressSnapshot(finalPosition, finalDuration)
                        retryJob?.cancel()
                        GlobalPlaybackManager.stopAndClear()
                        PlayerFullscreenState.isFullscreen = false
                        onClose()
                    }
                    playersToUpdate.forEach { targetPlayer ->
                        targetPlayer.titleTextView.text = title
                        targetPlayer.onCloseRequested = closeHandler
                        targetPlayer.onPipRequested = effectiveOnPipRequested
                        targetPlayer.playbackEngine = activeEngine
                        targetPlayer.onPlaybackEngineToggleRequested = {
                            requestPlaybackEngineSwitch(player, url)
                        }
                        targetPlayer.isInSystemPipMode = isInSystemPipMode
                        targetPlayer.playbackContentType = contentType
                        targetPlayer.configureBaseUi()
                    }
                }
            )
        }
    }
}

package com.stream.nextftv.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VideoPlayer(
    url: String,
    title: String = "Reproduciendo...",
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    resumePositionMs: Long = 0L,
    thumbUrl: String? = null,
    onLoading: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
    onEnterFullscreen: () -> Unit = {},
    onQuitFullscreen: () -> Unit = {},
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    shouldKeepFullscreenOnAutoComplete: () -> Boolean = { false },
    onClose: () -> Unit = {},
    onPipRequested: (() -> Unit)? = null,
    onProgress: (Long) -> Unit = {},
    onProgressSnapshot: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackCompleted: (Long, Long) -> Unit = { _, _ -> },
    isLive: Boolean = false,
    contentType: PlaybackContentType = if (isLive) PlaybackContentType.LIVE else PlaybackContentType.MOVIE,
    isSmall: Boolean = false
) {
    GsyVideoPlayer(
        url = url,
        title = title,
        isFullScreen = isFullScreen,
        isLive = isLive,
        resumePositionMs = resumePositionMs,
        thumbUrl = thumbUrl,
        contentType = contentType,
        isInSystemPipMode = isSmall,
        onEnterFullscreen = onEnterFullscreen,
        onQuitFullscreen = onQuitFullscreen,
        onClose = onClose,
        onPipRequested = onPipRequested,
        onNext = onNext,
        onPrevious = onPrevious,
        shouldKeepFullscreenOnAutoComplete = shouldKeepFullscreenOnAutoComplete,
        onError = onError,
        onLoading = onLoading,
        onProgress = onProgress,
        onProgressSnapshot = onProgressSnapshot,
        onPlaybackCompleted = onPlaybackCompleted,
        modifier = modifier
    )
}

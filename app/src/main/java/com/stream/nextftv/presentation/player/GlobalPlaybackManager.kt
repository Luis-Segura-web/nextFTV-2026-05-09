package com.stream.nextftv.presentation.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class MiniPlayerSession(
    val url: String,
    val title: String,
    val isLive: Boolean
)

private data class PlaybackSession(
    val url: String,
    val title: String,
    val isLive: Boolean,
    val isPlaying: Boolean
)

object GlobalPlaybackManager {
    private var currentSession: PlaybackSession? = null
    private var onPlayPauseRequested: (() -> Unit)? = null
    private var onStopRequested: (() -> Unit)? = null
    private var onNextRequested: (() -> Unit)? = null
    private var onPreviousRequested: (() -> Unit)? = null

    var miniSession: MiniPlayerSession? by mutableStateOf(null)
        private set

    fun registerSession(
        url: String,
        title: String,
        isLive: Boolean,
        isPlaying: Boolean,
        onPlayPause: (() -> Unit)?,
        onStop: (() -> Unit)?,
        onNext: (() -> Unit)?,
        onPrevious: (() -> Unit)?
    ) {
        currentSession = PlaybackSession(
            url = url,
            title = title,
            isLive = isLive,
            isPlaying = isPlaying
        )
        onPlayPauseRequested = onPlayPause
        onStopRequested = onStop
        onNextRequested = onNext
        onPreviousRequested = onPrevious
        BackgroundPlaybackController.requestRefresh()
        PipActionsController.requestRefresh()
    }

    fun updateSessionMetadata(url: String, title: String, isLive: Boolean) {
        val session = currentSession ?: return
        currentSession = session.copy(
            url = url,
            title = title,
            isLive = isLive
        )
        BackgroundPlaybackController.requestRefresh()
        PipActionsController.requestRefresh()
    }

    fun updatePlaybackState(url: String, isPlaying: Boolean) {
        val session = currentSession ?: return
        if (session.url != url) return
        currentSession = session.copy(isPlaying = isPlaying)
        BackgroundPlaybackController.requestRefresh()
        PipActionsController.requestRefresh()
    }

    fun getCurrentTitle(): String = currentSession?.title ?: "Reproduccion activa"

    fun isCurrentLive(): Boolean = currentSession?.isLive == true

    fun showMiniPlayer(url: String, title: String, isLive: Boolean) {
        miniSession = MiniPlayerSession(url = url, title = title, isLive = isLive)
    }

    fun hideMiniPlayer() {
        miniSession = null
    }

    fun hasActivePlayback(): Boolean = currentSession != null && onStopRequested != null

    fun isPlaying(): Boolean = currentSession?.isPlaying == true

    fun togglePlayPause() {
        currentSession = currentSession?.copy(isPlaying = currentSession?.isPlaying != true)
        BackgroundPlaybackController.requestRefresh()
        PipActionsController.requestRefresh()
        onPlayPauseRequested?.invoke()
    }

    fun hasNextAction(): Boolean = onNextRequested != null

    fun hasPreviousAction(): Boolean = onPreviousRequested != null

    fun requestNext() {
        onNextRequested?.invoke()
    }

    fun requestPrevious() {
        onPreviousRequested?.invoke()
    }

    fun stopAndClear() {
        onStopRequested?.invoke()
        clearSession()
    }

    fun clearSession(url: String? = null) {
        if (url != null && currentSession?.url != url) return
        currentSession = null
        miniSession = null
        onPlayPauseRequested = null
        onStopRequested = null
        onNextRequested = null
        onPreviousRequested = null
        BackgroundPlaybackController.requestRefresh()
        PipActionsController.requestRefresh()
    }

    fun release() {
        stopAndClear()
        clearSession()
    }
}

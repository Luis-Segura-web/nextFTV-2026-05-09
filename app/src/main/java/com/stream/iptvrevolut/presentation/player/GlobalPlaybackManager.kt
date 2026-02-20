package com.stream.iptvrevolut.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

data class MiniPlayerSession(
    val url: String,
    val title: String,
    val isLive: Boolean
)

object GlobalPlaybackManager {
    private var player: ExoPlayer? = null
    private var currentUrl: String? = null
    private var onNextRequested: (() -> Unit)? = null
    private var onPreviousRequested: (() -> Unit)? = null
    private var currentTitle: String = "Reproduccion activa"
    private var currentIsLive: Boolean = false

    var miniSession: MiniPlayerSession? by mutableStateOf(null)
        private set

    fun getPlayer(context: Context): ExoPlayer {
        player?.let { return it }
        val appContext = context.applicationContext
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15000, 30000, 1000, 2000)
            .setBackBuffer(0, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(mapOf("Connection" to "close"))
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        return ExoPlayer.Builder(appContext)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(appContext, httpFactory)))
            .build()
            .apply {
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
            .also { player = it }
    }

    fun ensurePlaying(context: Context, url: String, title: String, isLive: Boolean) {
        val exoPlayer = getPlayer(context)
        if (currentUrl != url) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
        }
        exoPlayer.play()
        currentUrl = url
        currentTitle = title
        currentIsLive = isLive
    }

    fun getCurrentTitle(): String = currentTitle

    fun isCurrentLive(): Boolean = currentIsLive

    fun showMiniPlayer(url: String, title: String, isLive: Boolean) {
        miniSession = MiniPlayerSession(url = url, title = title, isLive = isLive)
    }

    fun hideMiniPlayer() {
        miniSession = null
    }

    fun hasActivePlayback(): Boolean {
        val exoPlayer = player ?: return false
        return exoPlayer.currentMediaItem != null
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun togglePlayPause() {
        val exoPlayer = player ?: return
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun setNavigationCallbacks(
        onNext: (() -> Unit)?,
        onPrevious: (() -> Unit)?
    ) {
        onNextRequested = onNext
        onPreviousRequested = onPrevious
    }

    fun clearNavigationCallbacks() {
        onNextRequested = null
        onPreviousRequested = null
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
        player?.stop()
        player?.clearMediaItems()
        currentUrl = null
        miniSession = null
        currentTitle = "Reproduccion activa"
        currentIsLive = false
        clearNavigationCallbacks()
    }

    fun release() {
        player?.release()
        player = null
        currentUrl = null
        miniSession = null
        currentTitle = "Reproduccion activa"
        currentIsLive = false
        clearNavigationCallbacks()
    }
}

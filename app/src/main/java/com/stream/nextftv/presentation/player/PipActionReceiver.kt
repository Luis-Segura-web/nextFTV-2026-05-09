package com.stream.nextftv.presentation.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class PipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> GlobalPlaybackManager.togglePlayPause()
            ACTION_NEXT -> GlobalPlaybackManager.requestNext()
            ACTION_PREVIOUS -> GlobalPlaybackManager.requestPrevious()
            ACTION_STOP -> {
                GlobalPlaybackManager.stopAndClear()
                context?.let { PlaybackBackgroundService.stop(it) }
            }
        }
        PipActionsController.requestRefresh()
        BackgroundPlaybackController.requestRefresh()
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.stream.nextftv.pip.PLAY_PAUSE"
        const val ACTION_NEXT = "com.stream.nextftv.pip.NEXT"
        const val ACTION_PREVIOUS = "com.stream.nextftv.pip.PREVIOUS"
        const val ACTION_STOP = "com.stream.nextftv.pip.STOP"
    }
}

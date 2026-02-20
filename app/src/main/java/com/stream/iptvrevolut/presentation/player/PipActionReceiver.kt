package com.stream.iptvrevolut.presentation.player

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
                context?.let { NotificationManagerCompat.from(it).cancel(BG_NOTIFICATION_ID) }
            }
        }
        PipActionsController.requestRefresh()
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.stream.iptvrevolut.pip.PLAY_PAUSE"
        const val ACTION_NEXT = "com.stream.iptvrevolut.pip.NEXT"
        const val ACTION_PREVIOUS = "com.stream.iptvrevolut.pip.PREVIOUS"
        const val ACTION_STOP = "com.stream.iptvrevolut.pip.STOP"
        const val BG_NOTIFICATION_ID = 9031
    }
}

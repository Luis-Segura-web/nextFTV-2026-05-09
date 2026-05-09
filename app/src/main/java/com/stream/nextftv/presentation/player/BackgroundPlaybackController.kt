package com.stream.nextftv.presentation.player

object BackgroundPlaybackController {
    var refreshNotification: (() -> Unit)? = null

    fun requestRefresh() {
        refreshNotification?.invoke()
    }
}

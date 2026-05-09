package com.stream.nextftv.presentation.player

object PipActionsController {
    var refreshActions: (() -> Unit)? = null

    fun requestRefresh() {
        refreshActions?.invoke()
    }
}

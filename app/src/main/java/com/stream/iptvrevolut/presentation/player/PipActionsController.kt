package com.stream.iptvrevolut.presentation.player

object PipActionsController {
    var refreshActions: (() -> Unit)? = null

    fun requestRefresh() {
        refreshActions?.invoke()
    }
}

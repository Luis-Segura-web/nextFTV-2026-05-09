package com.stream.iptvrevolut.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Activity.setFullScreen(enable: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    
    if (enable) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

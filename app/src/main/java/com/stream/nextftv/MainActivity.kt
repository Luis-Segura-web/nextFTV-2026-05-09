package com.stream.nextftv

import android.Manifest
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.graphics.drawable.Icon
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.res.Configuration
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.stream.nextftv.presentation.navigation.AppNavigation
import com.stream.nextftv.presentation.player.BackgroundPlaybackPreferences
import com.stream.nextftv.presentation.player.BackgroundPlaybackController
import com.stream.nextftv.presentation.player.GlobalPlaybackManager
import com.stream.nextftv.presentation.player.PlaybackBackgroundService
import com.stream.nextftv.presentation.player.PlaybackCachePreferences
import com.stream.nextftv.presentation.player.PlaybackEnginePreferences
import com.stream.nextftv.presentation.player.PipActionsController
import com.stream.nextftv.presentation.player.PipActionReceiver
import com.stream.nextftv.presentation.player.PipModeState
import com.stream.nextftv.presentation.player.PipPreferences
import com.stream.nextftv.presentation.player.PlayerFullscreenOrientationMode
import com.stream.nextftv.presentation.player.PlayerFullscreenPreferences
import com.stream.nextftv.presentation.player.PlayerFullscreenState
import com.stream.nextftv.presentation.theme.NextFTVTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private companion object {
        const val TAG = "MainActivity"
    }

    private var hadPipSession = false

    private fun supportsPictureInPicture(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun canEnterPictureInPicture(): Boolean {
        return PipPreferences.isEnabled &&
            supportsPictureInPicture() &&
            GlobalPlaybackManager.hasActivePlayback() &&
            !isInPictureInPictureMode
    }

    private fun buildPictureInPictureParams(
        hasActivePlayback: Boolean = GlobalPlaybackManager.hasActivePlayback()
    ): android.app.PictureInPictureParams {
        val canAutoEnterPip = PipPreferences.isEnabled && hasActivePlayback
        return android.app.PictureInPictureParams.Builder().apply {
            setActions(if (hasActivePlayback) buildPipActions() else emptyList())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAutoEnterEnabled(canAutoEnterPip)
            }
        }.build()
    }

    private fun applyOrientationPolicy() {
        requestedOrientation = if (PlayerFullscreenState.isFullscreen) {
            when (PlayerFullscreenPreferences.fullscreenOrientationMode) {
                PlayerFullscreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                PlayerFullscreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                PlayerFullscreenOrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        PipPreferences.onChanged = { enabled ->
            if (!enabled) {
                exitPipIfNeeded()
            }
            refreshPipActions()
        }
        PipPreferences.initialize(this)
        BackgroundPlaybackPreferences.initialize(this)
        PlaybackCachePreferences.initialize(this)
        PlaybackEnginePreferences.initialize(this)
        PlayerFullscreenState.onChanged = { applyOrientationPolicy() }
        PlayerFullscreenPreferences.onChanged = { applyOrientationPolicy() }
        PlayerFullscreenPreferences.initialize(this)
        applyOrientationPolicy()
        BackgroundPlaybackController.refreshNotification = {
            if (isInPictureInPictureMode || !BackgroundPlaybackPreferences.isEnabled || !GlobalPlaybackManager.hasActivePlayback()) {
                PlaybackBackgroundService.stop(this)
            } else {
                PlaybackBackgroundService.startOrUpdate(this)
            }
        }
        BackgroundPlaybackPreferences.onChanged = { enabled ->
            if (!enabled && !isInPictureInPictureMode) {
                GlobalPlaybackManager.stopAndClear()
                PlaybackBackgroundService.stop(this)
            }
        }
        PipActionsController.refreshActions = { refreshPipActions() }
        setContent {
            NextFTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyOrientationPolicy()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    fun enterPipMode() {
        if (!canEnterPictureInPicture()) return
        enterPictureInPictureMode(buildPictureInPictureParams())
    }

    private fun refreshPipActions() {
        if (!supportsPictureInPicture()) return
        val hasActivePlayback = GlobalPlaybackManager.hasActivePlayback()
        setPictureInPictureParams(buildPictureInPictureParams(hasActivePlayback))
    }

    private fun exitPipIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
            )
        }
    }

    private fun buildPipActions(): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()

        if (GlobalPlaybackManager.hasPreviousAction()) {
            actions += RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                "Anterior",
                "Anterior",
                PendingIntent.getBroadcast(
                    this,
                    1001,
                    Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_PREVIOUS),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val isPlaying = GlobalPlaybackManager.isPlaying()
        actions += RemoteAction(
            Icon.createWithResource(
                this,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (isPlaying) "Pausar" else "Reproducir",
            if (isPlaying) "Pausar" else "Reproducir",
            PendingIntent.getBroadcast(
                this,
                1002,
                Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        if (GlobalPlaybackManager.hasNextAction()) {
            actions += RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_next),
                "Siguiente",
                "Siguiente",
                PendingIntent.getBroadcast(
                    this,
                    1003,
                    Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_NEXT),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        return actions
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (canEnterPictureInPicture()) {
                setPictureInPictureParams(buildPictureInPictureParams())
            }
            return
        }
        if (canEnterPictureInPicture()) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipModeState.isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            hadPipSession = true
            return
        }
        if (hadPipSession) {
            lifecycleScope.launch {
                delay(350)
                val appReturnedToForeground = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
                if (!isInPictureInPictureMode && !appReturnedToForeground) {
                    GlobalPlaybackManager.stopAndClear()
                    PlaybackBackgroundService.stop(this@MainActivity)
                    hadPipSession = false
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations && !isInPictureInPictureMode) {
            if (BackgroundPlaybackPreferences.isEnabled && GlobalPlaybackManager.hasActivePlayback()) {
                Log.i(
                    TAG,
                    "onPause start_background_service_early hasPlayback=${GlobalPlaybackManager.hasActivePlayback()}"
                )
                PlaybackBackgroundService.startOrUpdate(this)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isInPictureInPictureMode) {
            if (BackgroundPlaybackPreferences.isEnabled && GlobalPlaybackManager.hasActivePlayback()) {
                Log.i(
                    TAG,
                    "onStop keep_playback_in_background hasPlayback=${GlobalPlaybackManager.hasActivePlayback()}"
                )
                PlaybackBackgroundService.startOrUpdate(this)
            } else {
                Log.w(
                    TAG,
                    "onStop stopAndClear reason=activity_backgrounded backgroundPlayback=${BackgroundPlaybackPreferences.isEnabled} hasPlayback=${GlobalPlaybackManager.hasActivePlayback()}"
                )
                GlobalPlaybackManager.stopAndClear()
                PlaybackBackgroundService.stop(this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isInPictureInPictureMode) {
            hadPipSession = false
        }
        refreshPipActions()
        PlaybackBackgroundService.stop(this)
    }

    override fun onDestroy() {
        PipActionsController.refreshActions = null
        BackgroundPlaybackController.refreshNotification = null
        PipPreferences.onChanged = null
        BackgroundPlaybackPreferences.onChanged = null
        super.onDestroy()
        if (isFinishing) {
            GlobalPlaybackManager.release()
        }
    }

}

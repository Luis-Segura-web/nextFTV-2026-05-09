package com.stream.iptvrevolut

import android.Manifest
import android.app.PendingIntent
import android.app.RemoteAction
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.graphics.drawable.Icon
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.rememberNavController
import com.stream.iptvrevolut.presentation.navigation.AppNavigation
import com.stream.iptvrevolut.presentation.player.BackgroundPlaybackPreferences
import com.stream.iptvrevolut.presentation.player.GlobalPlaybackManager
import com.stream.iptvrevolut.presentation.player.PipActionsController
import com.stream.iptvrevolut.presentation.player.PipActionReceiver
import com.stream.iptvrevolut.presentation.player.PlayerEnginePreferences
import com.stream.iptvrevolut.presentation.player.PipModeState
import com.stream.iptvrevolut.presentation.player.PipPreferences
import com.stream.iptvrevolut.presentation.theme.IPTVRevolutTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var hadPipSession = false
    private val backgroundChannelId = "playback_background_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        PipPreferences.onChanged = { enabled ->
            if (!enabled) {
                exitPipIfNeeded()
            }
        }
        PipPreferences.initialize(this)
        BackgroundPlaybackPreferences.initialize(this)
        PlayerEnginePreferences.initialize(this)
        BackgroundPlaybackPreferences.onChanged = { enabled ->
            if (!enabled && !isInPictureInPictureMode) {
                GlobalPlaybackManager.stopAndClear()
                hideBackgroundPlaybackNotification()
            }
        }
        createBackgroundPlaybackChannel()
        PipActionsController.refreshActions = { refreshPipActions() }
        setContent {
            IPTVRevolutTheme {
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    fun enterPipMode() {
        if (!PipPreferences.isEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = android.app.PictureInPictureParams.Builder()
            builder.setActions(buildPipActions())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
            }
            val params = builder.build()
            enterPictureInPictureMode(params)
        }
    }

    private fun refreshPipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            val params = android.app.PictureInPictureParams.Builder()
                .setActions(buildPipActions())
                .build()
            setPictureInPictureParams(params)
        }
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
        if (
            PipPreferences.isEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            GlobalPlaybackManager.hasActivePlayback() &&
            !isInPictureInPictureMode
        ) {
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
                    hideBackgroundPlaybackNotification()
                    hadPipSession = false
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (hadPipSession && !isInPictureInPictureMode && !isChangingConfigurations) {
            GlobalPlaybackManager.stopAndClear()
            hadPipSession = false
            hideBackgroundPlaybackNotification()
            return
        }
        if (!isChangingConfigurations && !isInPictureInPictureMode) {
            if (BackgroundPlaybackPreferences.isEnabled && GlobalPlaybackManager.hasActivePlayback()) {
                showBackgroundPlaybackNotification()
            } else {
                GlobalPlaybackManager.stopAndClear()
                hideBackgroundPlaybackNotification()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isInPictureInPictureMode) {
            hadPipSession = false
        }
        hideBackgroundPlaybackNotification()
    }

    override fun onDestroy() {
        PipActionsController.refreshActions = null
        PipPreferences.onChanged = null
        BackgroundPlaybackPreferences.onChanged = null
        super.onDestroy()
        if (isFinishing) {
            GlobalPlaybackManager.release()
        }
    }

    private fun createBackgroundPlaybackChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                backgroundChannelId,
                "Reproduccion en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Control de reproduccion cuando la app esta en segundo plano"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showBackgroundPlaybackNotification() {
        val currentTitle = GlobalPlaybackManager.getCurrentTitle()
        val currentSubtitle = if (GlobalPlaybackManager.isCurrentLive()) {
            "Canal en segundo plano"
        } else {
            "Contenido en segundo plano"
        }

        val playPauseAction = NotificationCompat.Action(
            if (GlobalPlaybackManager.isPlaying()) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (GlobalPlaybackManager.isPlaying()) "Pausar" else "Reproducir",
            PendingIntent.getBroadcast(
                this,
                2001,
                Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val closeAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Cerrar",
            PendingIntent.getBroadcast(
                this,
                2002,
                Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val notification = NotificationCompat.Builder(this, backgroundChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(currentSubtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(currentTitle))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    2003,
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(playPauseAction)
            .addAction(closeAction)
            .build()

        NotificationManagerCompat.from(this).notify(PipActionReceiver.BG_NOTIFICATION_ID, notification)
    }

    private fun hideBackgroundPlaybackNotification() {
        NotificationManagerCompat.from(this).cancel(PipActionReceiver.BG_NOTIFICATION_ID)
    }
}

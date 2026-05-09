package com.stream.nextftv.presentation.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stream.nextftv.MainActivity
import com.stream.nextftv.R

class PlaybackBackgroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                releaseLocks()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_OR_UPDATE, null -> {
                // A foreground-service launch must promote itself immediately, even if playback
                // disappears between the caller check and service startup.
                startForeground(NOTIFICATION_ID, buildNotification())
                if (!BackgroundPlaybackPreferences.isEnabled || !GlobalPlaybackManager.hasActivePlayback()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    releaseLocks()
                    stopSelf()
                    return START_NOT_STICKY
                }
                acquireLocks()
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val currentTitle = GlobalPlaybackManager.getCurrentTitle()
        val isPlaying = GlobalPlaybackManager.isPlaying()
        val currentSubtitle = if (GlobalPlaybackManager.isCurrentLive()) {
            if (isPlaying) "Canal reproduciendose en segundo plano" else "Canal pausado en segundo plano"
        } else {
            if (isPlaying) "Contenido reproduciendose en segundo plano" else "Contenido pausado en segundo plano"
        }
        val currentStatus = if (isPlaying) "Segundo plano activo" else "Segundo plano en pausa"

        val previousAction = if (GlobalPlaybackManager.hasPreviousAction()) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                "Anterior",
                PendingIntent.getBroadcast(
                    this,
                    2000,
                    Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_PREVIOUS),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            null
        }

        val playPauseAction = NotificationCompat.Action(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pausar" else "Reanudar",
            PendingIntent.getBroadcast(
                this,
                2001,
                Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val nextAction = if (GlobalPlaybackManager.hasNextAction()) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                "Siguiente",
                PendingIntent.getBroadcast(
                    this,
                    2004,
                    Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_NEXT),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            null
        }

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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(currentSubtitle)
            .setSubText(currentStatus)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
            .apply {
                previousAction?.let(::addAction)
                addAction(playPauseAction)
                nextAction?.let(::addAction)
                addAction(closeAction)
            }
            .build()
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (wakeLock == null && powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$packageName:playback_background"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } else if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiLock == null && wifiManager != null) {
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "$packageName:playback_background"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } else if (wifiLock?.isHeld == false) {
            wifiLock?.acquire()
        }
    }

    private fun releaseLocks() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null

        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
        }
        wifiLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproduccion en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Control de reproduccion cuando la app esta en segundo plano"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "playback_background_channel"
        const val NOTIFICATION_ID = 9031
        private const val ACTION_START_OR_UPDATE = "com.stream.nextftv.playback.START_OR_UPDATE"
        private const val ACTION_STOP = "com.stream.nextftv.playback.STOP"

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, PlaybackBackgroundService::class.java).setAction(ACTION_START_OR_UPDATE)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PlaybackBackgroundService::class.java).setAction(ACTION_STOP)
            context.stopService(intent)
        }
    }
}

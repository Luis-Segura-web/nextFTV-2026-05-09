package com.stream.nextftv.presentation.player

import android.content.Context
import android.net.TrafficStats
import android.net.Uri
import android.os.Message
import android.view.Surface
import androidx.annotation.Nullable
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.video.PlaceholderSurface
import com.shuyu.gsyvideoplayer.cache.ICacheManager
import com.shuyu.gsyvideoplayer.model.GSYModel
import com.shuyu.gsyvideoplayer.model.VideoOptionModel
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import tv.danmaku.ijk.media.player.IMediaPlayer

class PatchedExo2PlayerManager : Exo2PlayerManager() {

    private var contextRef: Context? = null
    private var mediaPlayerRef: PatchedIjkExo2MediaPlayer? = null
    private var surfaceRef: Surface? = null
    private var dummySurface: PlaceholderSurface? = null
    private var lastTotalRxBytes = 0L
    private var lastTimeStamp = 0L

    override fun getMediaPlayer(): IMediaPlayer? = mediaPlayerRef

    override fun initVideoPlayer(
        context: Context,
        msg: Message,
        optionModelList: List<VideoOptionModel>?,
        cacheManager: ICacheManager?
    ) {
        contextRef = context.applicationContext
        mediaPlayerRef = PatchedIjkExo2MediaPlayer(context)
        if (dummySurface == null) {
            dummySurface = PlaceholderSurface.newInstanceV17(context, false)
        }
        val gsyModel = msg.obj as GSYModel
        try {
            mediaPlayerRef?.apply {
                setLooping(gsyModel.isLooping)
                setPreview(!gsyModel.mapHeadData.isNullOrEmpty())
                if (gsyModel.isCache && cacheManager != null) {
                    cacheManager.doCacheLogic(
                        context,
                        this,
                        gsyModel.url,
                        gsyModel.mapHeadData,
                        gsyModel.cachePath
                    )
                } else {
                    setCache(gsyModel.isCache)
                    setCacheDir(gsyModel.cachePath)
                    setOverrideExtension(gsyModel.overrideExtension)
                    setDataSource(context, Uri.parse(gsyModel.url), gsyModel.mapHeadData)
                }
                if (gsyModel.speed != 1f && gsyModel.speed > 0f) {
                    setSpeed(gsyModel.speed, 1f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initSuccess(gsyModel)
    }

    override fun showDisplay(msg: Message) {
        val mediaPlayer = mediaPlayerRef ?: return
        if (msg.obj == null) {
            mediaPlayer.setSurface(dummySurface)
        } else {
            val holder = msg.obj as Surface
            surfaceRef = holder
            mediaPlayer.setSurface(holder)
        }
    }

    override fun setSpeed(speed: Float, soundTouch: Boolean) {
        mediaPlayerRef?.runCatching { setSpeed(speed, 1f) }
    }

    override fun setNeedMute(needMute: Boolean) {
        if (needMute) {
            mediaPlayerRef?.setVolume(0f, 0f)
        } else {
            mediaPlayerRef?.setVolume(1f, 1f)
        }
    }

    override fun setVolume(left: Float, right: Float) {
        mediaPlayerRef?.setVolume(left, right)
    }

    override fun releaseSurface() {
        surfaceRef = null
    }

    override fun release() {
        mediaPlayerRef?.apply {
            setSurface(null)
            release()
        }
        mediaPlayerRef = null
        dummySurface?.release()
        dummySurface = null
        lastTotalRxBytes = 0L
        lastTimeStamp = 0L
    }

    override fun getBufferedPercentage(): Int = mediaPlayerRef?.bufferedPercentage ?: 0

    override fun getNetSpeed(): Long = if (mediaPlayerRef != null) getNetSpeed(contextRef) else 0L

    override fun setSpeedPlaying(speed: Float, soundTouch: Boolean) = Unit

    override fun start() {
        mediaPlayerRef?.start()
    }

    override fun stop() {
        mediaPlayerRef?.stop()
    }

    override fun pause() {
        mediaPlayerRef?.pause()
    }

    override fun getVideoWidth(): Int = mediaPlayerRef?.videoWidth ?: 0

    override fun getVideoHeight(): Int = mediaPlayerRef?.videoHeight ?: 0

    override fun isPlaying(): Boolean = mediaPlayerRef?.isPlaying ?: false

    override fun seekTo(time: Long) {
        mediaPlayerRef?.seekTo(time)
    }

    override fun getCurrentPosition(): Long = mediaPlayerRef?.currentPosition ?: 0L

    override fun getDuration(): Long = mediaPlayerRef?.duration ?: 0L

    override fun getVideoSarNum(): Int = mediaPlayerRef?.videoSarNum ?: 1

    override fun getVideoSarDen(): Int = mediaPlayerRef?.videoSarDen ?: 1

    override fun isSurfaceSupportLockCanvas(): Boolean = false

    override fun setSeekParameter(@Nullable seekParameters: SeekParameters?) {
        seekParameters ?: return
        mediaPlayerRef?.setSeekParameter(seekParameters)
    }

    private fun getNetSpeed(context: Context?): Long {
        context ?: return 0L
        val nowTotalRxBytes = if (TrafficStats.getUidRxBytes(context.applicationInfo.uid) == TrafficStats.UNSUPPORTED.toLong()) {
            0L
        } else {
            TrafficStats.getTotalRxBytes() / 1024
        }
        val nowTimeStamp = System.currentTimeMillis()
        val calculationTime = nowTimeStamp - lastTimeStamp
        if (calculationTime == 0L) {
            return 0L
        }
        val speed = (nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime
        lastTimeStamp = nowTimeStamp
        lastTotalRxBytes = nowTotalRxBytes
        return speed
    }
}

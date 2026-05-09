package com.stream.nextftv.presentation.player

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer
import com.stream.nextftv.utils.findActivity
import tv.danmaku.ijk.media.player.misc.IjkMediaFormat
import tv.danmaku.ijk.media.player.misc.IMediaFormat
import tv.danmaku.ijk.media.player.misc.ITrackInfo

data class GsyTrackOption(
    val streamIndex: Int,
    val trackType: Int,
    val label: String,
    val selected: Boolean,
    val exoRendererIndex: Int? = null,
    val exoGroupIndex: Int? = null,
    val exoTrackIndex: Int? = null
)

enum class ExoCodecCompatibility {
    SUPPORTED,
    EXCEEDS_CAPABILITIES,
    UNSUPPORTED,
    UNKNOWN
}

data class ExoTrackCompatibility(
    val trackType: Int,
    val label: String,
    val sampleMimeType: String?,
    val codecs: String?,
    val selected: Boolean,
    val compatibility: ExoCodecCompatibility
)

data class ExoPlaybackCompatibility(
    val videoTracks: List<ExoTrackCompatibility>,
    val audioTracks: List<ExoTrackCompatibility>
) {
    fun hasUnsupportedUnselectedVideoTrack(): Boolean {
        return videoTracks.any {
            !it.selected && (it.compatibility == ExoCodecCompatibility.UNSUPPORTED || it.compatibility == ExoCodecCompatibility.UNKNOWN)
        } && videoTracks.none { it.selected }
    }

    fun hasSupportedSelectedVideoTrack(): Boolean {
        return videoTracks.any {
            it.selected && (it.compatibility == ExoCodecCompatibility.SUPPORTED || it.compatibility == ExoCodecCompatibility.EXCEEDS_CAPABILITIES)
        }
    }
}

data class PlayerSubtitleStyle(
    val horizontalMarginDp: Int = 20,
    val bottomMarginDp: Int = 8,
    val textSizeSp: Float = 17f,
    val minTextSizeSp: Float = 14f,
    val maxTextSizeSp: Float = 24f,
    val maxLines: Int = 3,
    val textGravity: Int = Gravity.CENTER,
    val textAlignment: Int = View.TEXT_ALIGNMENT_CENTER,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0x80000000.toInt(),
    val backgroundCornerRadiusDp: Int = 14,
    val paddingHorizontalDp: Int = 10,
    val paddingVerticalDp: Int = 4,
    val shadowColor: Int = 0xFF000000.toInt(),
    val shadowRadius: Float = 10f,
    val shadowDx: Float = 0f,
    val shadowDy: Float = 3f,
    val isBold: Boolean = true
)

private data class ScreenAdjustOption(
    val showType: Int,
    val labelResId: Int
)

open class IptvGsyPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseStandardGsyPlayerView(context, attrs) {

    private companion object {
        const val TRACKS_TAG = "IptvTrackDebug"
        const val FULLSCREEN_TRANSITION_TAG = "IptvFullscreenTransition"
        const val SEEK_BACK_MS = 5_000L
        const val SEEK_FORWARD_MS = 15_000L
    }

    private var skipNextFullscreenOrientationReset = false

    fun suppressNextFullscreenOrientationReset() {
        skipNextFullscreenOrientationReset = true
    }

    var subtitleStyle: PlayerSubtitleStyle = PlayerSubtitleStyle()
        set(value) {
            field = value
            applySubtitleStyle()
        }

    private fun stripSubtitleMarkup(text: String): String {
        if (text.isEmpty()) return ""
        val builder = StringBuilder(text.length)
        var insideTag = false
        text.forEach { char ->
            when {
                char == '{' -> insideTag = true
                char == '}' -> insideTag = false
                !insideTag -> builder.append(char)
            }
        }
        return builder.toString().trim()
    }

    var hasVideoRenderingStarted: Boolean = false
        private set

    private var selectedScreenShowType: Int = GSYVideoType.SCREEN_TYPE_DEFAULT
    private val hideScreenAdjustChipRunnable = Runnable {
        screenAdjustChip?.visibility = View.GONE
    }

    fun resetVideoRenderingState() {
        hasVideoRenderingStarted = false
    }

    private fun enforcePipUiVisibility() {
        if (!isInSystemPipMode) return
        titleTextView?.visibility = View.GONE
        backButton?.visibility = View.GONE
        fullscreenButton?.visibility = View.GONE
        pipContainer?.visibility = View.GONE
        pipButton?.visibility = View.GONE
        subtitleContainer?.visibility = View.GONE
        subtitleButton?.visibility = View.GONE
        audioTrackContainer?.visibility = View.GONE
        audioTrackButton?.visibility = View.GONE
        orientationModeContainer?.visibility = View.GONE
        playbackEngineContainer?.visibility = View.GONE
        playbackEngineButton?.visibility = View.GONE
        screenAdjustContainer?.visibility = View.GONE
        screenAdjustButton?.visibility = View.GONE
        mCurrentTimeTextView?.visibility = View.GONE
        mTotalTimeTextView?.visibility = View.GONE
        mProgressBar?.visibility = View.GONE
        mBottomProgressBar?.visibility = View.GONE
        mTopContainer?.visibility = View.GONE
        mBottomContainer?.visibility = View.GONE
        mLoadingProgressBar?.visibility = View.GONE
        mThumbImageViewLayout?.visibility = View.GONE
        mLockScreen?.visibility = View.GONE
        mStartButton?.visibility = View.GONE
        seekBackButton?.visibility = View.GONE
        seekForwardButton?.visibility = View.GONE
        seekBackLabel?.visibility = View.GONE
        seekForwardLabel?.visibility = View.GONE
    }

    private fun updateCenterSeekButtonsVisibility() {
        val shouldShow = !isInSystemPipMode &&
            playbackContentType != PlaybackContentType.LIVE &&
            mBottomContainer?.visibility == View.VISIBLE
        seekBackButton?.visibility = if (shouldShow) View.VISIBLE else View.GONE
        seekForwardButton?.visibility = if (shouldShow) View.VISIBLE else View.GONE
        seekBackLabel?.visibility = if (shouldShow) View.VISIBLE else View.GONE
        seekForwardLabel?.visibility = if (shouldShow) View.VISIBLE else View.GONE
        if (shouldShow) {
            requestCenterSeekButtonsLayout()
        }
    }

    private fun requestCenterSeekButtonsLayout() {
        val parent = seekBackButton?.parent as? ViewGroup ?: return
        parent.post {
            seekBackButton?.requestLayout()
            seekForwardButton?.requestLayout()
            seekBackLabel?.requestLayout()
            seekForwardLabel?.requestLayout()
            parent.requestLayout()
            parent.invalidate()
        }
    }

    private fun enforceContentUiVisibility() {
        if (isInSystemPipMode) {
            enforcePipUiVisibility()
            return
        }
        updateCenterSeekButtonsVisibility()
        if (playbackContentType != PlaybackContentType.LIVE) return
        mCurrentTimeTextView?.visibility = View.GONE
        mTotalTimeTextView?.visibility = View.GONE
        mProgressBar?.visibility = View.GONE
        mBottomProgressBar?.visibility = View.GONE
        bottomSeekRow?.visibility = View.GONE
    }

    var onCloseRequested: (() -> Unit)? = null
    var onPipRequested: (() -> Unit)? = null
    var onFullscreenRequested: (() -> Unit)? = null
    var onPlaybackEngineToggleRequested: (() -> Unit)? = null
    var playbackEngine: PlaybackEngine = PlaybackEngine.EXO
        set(value) {
            field = value
            updatePlaybackEngineButton()
        }
    var isInSystemPipMode: Boolean = false
        set(value) {
            field = value
            applyContentTypeUi()
        }
    var playbackContentType: PlaybackContentType = PlaybackContentType.MOVIE
        set(value) {
            field = value
            applyContentTypeUi()
        }
    private val pipButton: ImageView?
        get() = findViewById(com.stream.nextftv.R.id.pip)
    private val pipContainer: View?
        get() = pipButton?.parent as? View
    private val subtitleButton: ImageView?
        get() = findViewById(com.stream.nextftv.R.id.subtitle)
    private val subtitleContainer: View?
        get() = subtitleButton?.parent as? View
    private val audioTrackButton: ImageView?
        get() = findViewById(com.stream.nextftv.R.id.audio_track)
    private val audioTrackContainer: View?
        get() = audioTrackButton?.parent as? View
    private val subtitleDisplay: SubtitleBackgroundTextView?
        get() = findViewById(com.stream.nextftv.R.id.subtitle_display)
    private val screenAdjustChip: TextView?
        get() = findViewById(com.stream.nextftv.R.id.screen_adjust_chip)
    private val screenAdjustButton: ImageView?
        get() = findViewById(com.stream.nextftv.R.id.screen_adjust)
    private val screenAdjustContainer: View?
        get() = findViewById(com.stream.nextftv.R.id.screen_adjust_container)
    private val playbackEngineButton: TextView?
        get() = findViewById(com.stream.nextftv.R.id.playback_engine_toggle)
    private val playbackEngineContainer: View?
        get() = findViewById(com.stream.nextftv.R.id.playback_engine_container)
    private val orientationModeButton: ImageView?
        get() = findViewById(com.stream.nextftv.R.id.orientation_mode)
    private val orientationModeContainer: View?
        get() = findViewById(com.stream.nextftv.R.id.orientation_mode_container)
    private val bottomActionsSpacer: View?
        get() = findViewById(com.stream.nextftv.R.id.bottom_actions_spacer)
    private val bottomControlsRow: LinearLayout?
        get() = findViewById(com.stream.nextftv.R.id.layout_bottom_controls_row)
    private val bottomSeekRow: LinearLayout?
        get() = findViewById(com.stream.nextftv.R.id.layout_bottom_seek_row)
    private val seekBackButton: ImageView?
        get() = findViewById(com.stream.nextftv.R.id.seek_back)
    private val seekForwardButton: ImageView?
        get() = findViewById(com.stream.nextftv.R.id.seek_forward)
    private val seekBackLabel: TextView?
        get() = findViewById(com.stream.nextftv.R.id.seek_back_label)
    private val seekForwardLabel: TextView?
        get() = findViewById(com.stream.nextftv.R.id.seek_forward_label)
    override fun getEnlargeImageRes(): Int = com.stream.nextftv.R.drawable.ic_player_fullscreen_enter

    override fun getShrinkImageRes(): Int = com.stream.nextftv.R.drawable.ic_player_fullscreen_exit

    private fun availableScreenAdjustOptions(): List<ScreenAdjustOption> = listOf(
        ScreenAdjustOption(
            GSYVideoType.SCREEN_TYPE_DEFAULT,
            com.stream.nextftv.R.string.player_screen_adjust_default
        ),
        ScreenAdjustOption(
            GSYVideoType.SCREEN_TYPE_16_9,
            com.stream.nextftv.R.string.player_screen_adjust_16_9
        ),
        ScreenAdjustOption(
            GSYVideoType.SCREEN_TYPE_4_3,
            com.stream.nextftv.R.string.player_screen_adjust_4_3
        ),
        ScreenAdjustOption(
            GSYVideoType.SCREEN_TYPE_FULL,
            com.stream.nextftv.R.string.player_screen_adjust_fill_crop
        ),
        ScreenAdjustOption(
            GSYVideoType.SCREEN_MATCH_FULL,
            com.stream.nextftv.R.string.player_screen_adjust_stretch
        )
    )

    private fun applyScreenShowType(showType: Int) {
        selectedScreenShowType = showType
        GSYVideoType.setShowType(showType)
        updateScreenAdjustButtonLabel()
        changeTextureViewShowType()
        onVideoSizeChanged()
        invalidate()
    }

    private fun currentScreenAdjustOption(): ScreenAdjustOption? {
        return availableScreenAdjustOptions().firstOrNull { it.showType == selectedScreenShowType }
    }

    private fun updateScreenAdjustButtonLabel() {
        val labelResId = currentScreenAdjustOption()?.labelResId
            ?: com.stream.nextftv.R.string.player_screen_adjust_default
        screenAdjustButton?.contentDescription =
            "${context.getString(com.stream.nextftv.R.string.player_screen_adjust)}: ${context.getString(labelResId)}"
    }

    private fun updatePlaybackEngineButton() {
        val label = when (playbackEngine) {
            PlaybackEngine.EXO -> "HW"
            PlaybackEngine.IJK -> "SW"
        }
        playbackEngineButton?.text = label
        playbackEngineButton?.contentDescription =
            "${context.getString(com.stream.nextftv.R.string.player_playback_engine)}: $label"
    }

    private fun showScreenAdjustChip(labelResId: Int) {
        val chip = screenAdjustChip ?: return
        chip.removeCallbacks(hideScreenAdjustChipRunnable)
        chip.animate().cancel()
        chip.text = context.getString(labelResId)
        chip.alpha = 1f
        chip.visibility = View.VISIBLE
        chip.postDelayed(hideScreenAdjustChipRunnable, 1400L)
    }

    private fun cycleScreenAdjustOption() {
        val options = availableScreenAdjustOptions()
        if (options.isEmpty()) return
        val currentIndex = options.indexOfFirst { it.showType == selectedScreenShowType }.coerceAtLeast(0)
        val nextOption = options[(currentIndex + 1) % options.size]
        applyScreenShowType(nextOption.showType)
        showScreenAdjustChip(nextOption.labelResId)
    }

    private fun nextOrientationMode(mode: PlayerFullscreenOrientationMode): PlayerFullscreenOrientationMode {
        return when (mode) {
            PlayerFullscreenOrientationMode.PORTRAIT -> PlayerFullscreenOrientationMode.LANDSCAPE
            PlayerFullscreenOrientationMode.LANDSCAPE -> PlayerFullscreenOrientationMode.AUTO
            PlayerFullscreenOrientationMode.AUTO -> PlayerFullscreenOrientationMode.PORTRAIT
        }
    }

    private fun orientationModeLabelResId(mode: PlayerFullscreenOrientationMode): Int {
        return when (mode) {
            PlayerFullscreenOrientationMode.PORTRAIT -> com.stream.nextftv.R.string.player_orientation_mode_portrait
            PlayerFullscreenOrientationMode.LANDSCAPE -> com.stream.nextftv.R.string.player_orientation_mode_landscape
            PlayerFullscreenOrientationMode.AUTO -> com.stream.nextftv.R.string.player_orientation_mode_auto
        }
    }

    private fun cycleFullscreenOrientationMode() {
        if (!isIfCurrentIsFullscreen) return
        val nextMode = nextOrientationMode(PlayerFullscreenPreferences.fullscreenOrientationMode)
        PlayerFullscreenPreferences.updateFullscreenOrientationMode(context, nextMode)
        applyFullscreenPortraitInsets()
        showScreenAdjustChip(orientationModeLabelResId(nextMode))
    }

    private fun handleFullscreenClick() {
        if (isIfCurrentIsFullscreen) {
            onBackFullscreen()
        } else {
            onFullscreenRequested?.invoke()
                ?: startWindowFullscreen(getActivityContext() ?: context, true, true)
        }
    }

    private fun restartControlsVisibilityTimeout() {
        if (isInSystemPipMode) return
        if (mBottomContainer?.visibility != View.VISIBLE && mTopContainer?.visibility != View.VISIBLE) return
        startDismissControlViewTimer()
    }

    private fun shouldRestartControlsTimeout(clickedView: View?): Boolean {
        return clickedView != null && clickedView !is SubtitleBackgroundTextView
    }

    private fun shouldRespectStatusBarInFullscreen(): Boolean {
        return isIfCurrentIsFullscreen &&
            PlayerFullscreenPreferences.fullscreenOrientationMode == PlayerFullscreenOrientationMode.PORTRAIT
    }

    private fun applyFullscreenStatusBarPolicy() {
        val activity = context.findActivity() ?: (getActivityContext() as? android.app.Activity) ?: return
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (shouldRespectStatusBarInFullscreen()) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        } else if (isIfCurrentIsFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    private fun applyFullscreenPortraitInsets() {
        if (!isIfCurrentIsFullscreen) {
            ViewCompat.setOnApplyWindowInsetsListener(this, null)
            setPadding(paddingLeft, 0, paddingRight, paddingBottom)
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val topInset = if (shouldRespectStatusBarInFullscreen()) {
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            } else {
                0
            }
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(this)
        applyFullscreenStatusBarPolicy()
    }

    fun configureBaseUi() {
        val isLiveContent = playbackContentType == PlaybackContentType.LIVE
        titleTextView?.visibility = View.VISIBLE
        backButton?.visibility = View.VISIBLE
        setIsTouchWiget(!isLiveContent)
        setIsTouchWigetFull(!isLiveContent)
        setAutoFullWithSize(true)
        setLockLand(true)
        setNeedLockFull(true)
        setEnlargeImageRes(com.stream.nextftv.R.drawable.ic_player_fullscreen_enter)
        setShrinkImageRes(com.stream.nextftv.R.drawable.ic_player_fullscreen_exit)
        applySubtitleStyle()
        updatePlaybackEngineButton()
        pipButton?.setOnClickListener {
            onPipRequested?.invoke()
            restartControlsVisibilityTimeout()
        }
        subtitleButton?.setOnClickListener {
            showSubtitleTrackDialog()
            restartControlsVisibilityTimeout()
        }
        audioTrackButton?.setOnClickListener {
            showAudioTrackDialog()
            restartControlsVisibilityTimeout()
        }
        playbackEngineButton?.setOnClickListener {
            onPlaybackEngineToggleRequested?.invoke()
            restartControlsVisibilityTimeout()
        }
        screenAdjustButton?.setOnClickListener {
            cycleScreenAdjustOption()
            restartControlsVisibilityTimeout()
        }
        orientationModeButton?.setOnClickListener {
            cycleFullscreenOrientationMode()
            restartControlsVisibilityTimeout()
        }
        seekBackButton?.setOnClickListener(this)
        seekForwardButton?.setOnClickListener(this)
        applyScreenShowType(selectedScreenShowType)
        if (!isIfCurrentIsFullscreen) {
            backButton?.setOnClickListener { onCloseRequested?.invoke() }
            fullscreenButton?.setOnClickListener { handleFullscreenClick() }
        }
        applyFullscreenPortraitInsets()
        applyContentTypeUi()
    }

    override fun onPrepared() {
        resetVideoRenderingState()
        super.onPrepared()
    }

    override fun onInfo(what: Int, extra: Int) {
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            hasVideoRenderingStarted = true
        }
        super.onInfo(what, extra)
    }

    private fun applyContentTypeUi() {
        applySubtitleStyle()
        if (isInSystemPipMode) {
            screenAdjustChip?.visibility = View.GONE
            enforcePipUiVisibility()
            return
        }

        val isLiveContent = playbackContentType == PlaybackContentType.LIVE
        setIsTouchWiget(!isLiveContent)
        setIsTouchWigetFull(!isLiveContent)
        mTopContainer?.visibility = View.VISIBLE
        mBottomContainer?.visibility = View.VISIBLE
        mCurrentTimeTextView?.visibility = if (isLiveContent) View.GONE else View.VISIBLE
        mTotalTimeTextView?.visibility = if (isLiveContent) View.GONE else View.VISIBLE
        mProgressBar?.visibility = if (isLiveContent) View.GONE else View.VISIBLE
        mBottomProgressBar?.visibility = if (isLiveContent) View.GONE else View.VISIBLE
        playbackEngineContainer?.visibility = View.VISIBLE
        playbackEngineButton?.visibility = View.VISIBLE
        fullscreenButton?.visibility = View.VISIBLE
        orientationModeContainer?.visibility = if (isIfCurrentIsFullscreen) View.VISIBLE else View.GONE
        screenAdjustContainer?.visibility = View.VISIBLE
        screenAdjustButton?.visibility = View.VISIBLE
        pipContainer?.visibility = if (onPipRequested != null && !isInSystemPipMode) View.VISIBLE else View.GONE
        pipButton?.visibility = if (onPipRequested != null && !isInSystemPipMode) View.VISIBLE else View.GONE
        refreshTrackButtonsVisibility()
        bottomActionsSpacer?.visibility = View.VISIBLE
        bottomSeekRow?.visibility = if (isLiveContent) View.GONE else View.VISIBLE
        bottomControlsRow?.gravity =
            if (isLiveContent) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.END
        (mBottomContainer?.layoutParams as? RelativeLayout.LayoutParams)?.apply {
            marginStart = 0
            marginEnd = 0
            mBottomContainer?.layoutParams = this
        }
        val bottomPaddingHorizontal = context.resources.getDimensionPixelSize(
            if (isLiveContent) com.stream.nextftv.R.dimen.player_bottom_bar_live_horizontal_padding
            else com.stream.nextftv.R.dimen.player_bottom_bar_default_horizontal_padding
        )
        bottomSeekRow?.setPaddingRelative(
            bottomPaddingHorizontal,
            bottomSeekRow?.paddingTop ?: 0,
            bottomPaddingHorizontal,
            bottomSeekRow?.paddingBottom ?: 0
        )
        bottomControlsRow?.setPaddingRelative(
            bottomPaddingHorizontal,
            bottomControlsRow?.paddingTop ?: 0,
            bottomPaddingHorizontal,
            bottomControlsRow?.paddingBottom ?: 0
        )
        titleTextView?.apply {
            gravity = Gravity.TOP or Gravity.START
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            alpha = 1f
            maxLines = 3
        }
        (titleTextView?.layoutParams as? LinearLayout.LayoutParams)?.apply {
            marginStart = if (isLiveContent) context.resources.getDimensionPixelSize(com.stream.nextftv.R.dimen.player_title_margin_start_live)
            else context.resources.getDimensionPixelSize(com.stream.nextftv.R.dimen.player_title_margin_start_default)
            marginEnd = context.resources.getDimensionPixelSize(com.stream.nextftv.R.dimen.player_title_margin_end_default)
            titleTextView?.layoutParams = this
        }
        mTopContainer?.setBackgroundResource(
            if (isLiveContent) com.stream.nextftv.R.drawable.video_title_bg_live
            else com.stream.nextftv.R.drawable.video_title_bg_vod
        )
        (mBottomContainer?.layoutParams as? RelativeLayout.LayoutParams)?.height =
            if (isLiveContent) context.resources.getDimensionPixelSize(com.stream.nextftv.R.dimen.player_bottom_bar_live_height)
            else RelativeLayout.LayoutParams.WRAP_CONTENT
        backButton?.setImageResource(
            if (isIfCurrentIsFullscreen) com.stream.nextftv.R.drawable.ic_player_back
            else com.stream.nextftv.R.drawable.ic_player_close
        )
        fullscreenButton?.setImageResource(if (isIfCurrentIsFullscreen) getShrinkImageRes() else getEnlargeImageRes())
        updateCenterSeekButtonsVisibility()
        mBottomContainer?.requestLayout()
    }

    private fun refreshTrackButtonsVisibility() {
        val (audioTracks, subtitleTracks) = refreshTrackOptions()
        val shouldShowAudioButton = audioTracks.size > 1
        val shouldShowSubtitleButton = subtitleTracks.isNotEmpty()

        audioTrackContainer?.visibility = if (shouldShowAudioButton) View.VISIBLE else View.GONE
        audioTrackButton?.visibility = if (shouldShowAudioButton) View.VISIBLE else View.GONE
        subtitleContainer?.visibility = if (shouldShowSubtitleButton) View.VISIBLE else View.GONE
        subtitleButton?.visibility = if (shouldShowSubtitleButton) View.VISIBLE else View.GONE
    }

    private fun showAudioTrackDialog() {
        val (audioTracks, _) = refreshTrackOptions()
        if (audioTracks.size <= 1) return
        logTrackState("showAudioTrackDialog")

        val labels = audioTracks.map { it.label }.toTypedArray()
        val selectedIndex = audioTracks.indexOfFirst { it.selected }.coerceAtLeast(0)

        AlertDialog.Builder(getActivityContext() ?: context)
            .setTitle("Pista de audio")
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                audioTracks.getOrNull(which)?.let(::selectTrackOption)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSubtitleTrackDialog() {
        val (_, subtitleTracks) = refreshTrackOptions()
        if (subtitleTracks.isEmpty()) return
        logTrackState("showSubtitleTrackDialog")

        val labels = buildList {
            add("Sin subtitulos")
            addAll(subtitleTracks.map { it.label })
        }.toTypedArray()
        val selectedTrackIndex = subtitleTracks.indexOfFirst { it.selected }
        val selectedIndex = if (selectedTrackIndex >= 0) selectedTrackIndex + 1 else 0

        AlertDialog.Builder(getActivityContext() ?: context)
            .setTitle("Subtitulos")
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                if (which == 0) {
                    disableSelectedSubtitles()
                } else {
                    subtitleTracks.getOrNull(which - 1)?.let(::selectTrackOption)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun changeUiToNormal() {
        super.changeUiToNormal()
        enforceContentUiVisibility()
    }

    override fun changeUiToPreparingShow() {
        super.changeUiToPreparingShow()
        enforceContentUiVisibility()
    }

    override fun changeUiToPlayingShow() {
        super.changeUiToPlayingShow()
        enforceContentUiVisibility()
    }

    override fun changeUiToPauseShow() {
        super.changeUiToPauseShow()
        enforceContentUiVisibility()
    }

    override fun changeUiToPlayingBufferingShow() {
        super.changeUiToPlayingBufferingShow()
        enforceContentUiVisibility()
    }

    override fun changeUiToCompleteShow() {
        super.changeUiToCompleteShow()
        enforceContentUiVisibility()
    }

    override fun changeUiToError() {
        super.changeUiToError()
        enforceContentUiVisibility()
    }

    override fun changeUiToPrepareingClear() {
        super.changeUiToPrepareingClear()
        enforceContentUiVisibility()
    }

    override fun changeUiToPlayingClear() {
        super.changeUiToPlayingClear()
        enforceContentUiVisibility()
    }

    override fun changeUiToPauseClear() {
        super.changeUiToPauseClear()
        enforceContentUiVisibility()
    }

    override fun changeUiToPlayingBufferingClear() {
        super.changeUiToPlayingBufferingClear()
        enforceContentUiVisibility()
    }

    override fun changeUiToClear() {
        super.changeUiToClear()
        enforceContentUiVisibility()
    }

    override fun changeUiToCompleteClear() {
        super.changeUiToCompleteClear()
        enforceContentUiVisibility()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            com.stream.nextftv.R.id.seek_back -> {
                seekBy(-SEEK_BACK_MS)
                restartControlsVisibilityTimeout()
                return
            }
            com.stream.nextftv.R.id.seek_forward -> {
                seekBy(SEEK_FORWARD_MS)
                restartControlsVisibilityTimeout()
                return
            }
        }
        super.onClick(v)
        if (shouldRestartControlsTimeout(v)) {
            restartControlsVisibilityTimeout()
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val handled = super.onTouch(v, event)
        if (event?.actionMasked == MotionEvent.ACTION_UP && shouldRestartControlsTimeout(v)) {
            restartControlsVisibilityTimeout()
        }
        return handled
    }

    override fun hideAllWidget() {
        super.hideAllWidget()
        enforceContentUiVisibility()
    }

    private fun seekBy(offsetMs: Long) {
        if (playbackContentType == PlaybackContentType.LIVE) return
        if (mCurrentState == CURRENT_STATE_ERROR || mCurrentState == CURRENT_STATE_NORMAL) return

        val durationMs = duration.takeIf { it > 0L } ?: 0L
        val currentPositionMs = currentPositionWhenPlaying.coerceAtLeast(0L)
        val seekTarget = if (durationMs > 0L) {
            (currentPositionMs + offsetMs).coerceIn(0L, durationMs)
        } else {
            (currentPositionMs + offsetMs).coerceAtLeast(0L)
        }
        seekTo(seekTarget)
    }

    fun setPlaybackSpeedCompat(speed: Float) {
        setSpeedPlaying(speed, true)
    }

    fun refreshTrackOptions(): Pair<List<GsyTrackOption>, List<GsyTrackOption>> {
        val playerManager = getGSYVideoManager().player
        return when (playerManager) {
            is IjkPlayerManager -> refreshIjkTrackOptions(playerManager)
            is Exo2PlayerManager -> refreshExoTrackOptions(playerManager)
            else -> emptyList<GsyTrackOption>() to emptyList()
        }
    }

    fun selectTrackOption(option: GsyTrackOption) {
        val manager = getGSYVideoManager().player
        Log.d(
            TRACKS_TAG,
            "selectTrackOption index=${option.streamIndex} type=${trackTypeName(option.trackType)} label=${option.label}"
        )
        when (manager) {
            is IjkPlayerManager -> {
                val currentPosition = currentPositionWhenPlaying.coerceAtLeast(0L)
                manager.selectTrack(option.streamIndex)
                stabilizeIjkTrackSwitch(option, currentPosition)
            }
            is Exo2PlayerManager -> selectExoTrackOption(manager, option)
            else -> return
        }
        logTrackState("afterSelectTrack")
    }

    fun disableSelectedSubtitles() {
        when (val manager = getGSYVideoManager().player) {
            is IjkPlayerManager -> {
                val (_, subtitles) = refreshTrackOptions()
                subtitles.filter { it.selected }.forEach { manager.deselectTrack(it.streamIndex) }
            }
            is Exo2PlayerManager -> disableExoSubtitles(manager)
            else -> return
        }
        updateSubtitleText(null)
        logTrackState("afterDisableSubtitles")
    }

    fun updateSubtitleText(text: String?) {
        val subtitleView = subtitleDisplay ?: return
        Log.d(TRACKS_TAG, "timedText=${text ?: "<null>"}")
        subtitleView.post {
            val cleanedText = stripSubtitleMarkup(text.orEmpty())
            subtitleView.text = cleanedText
            subtitleView.translationZ = 0f
            subtitleView.requestLayout()
            subtitleView.invalidate()
            subtitleView.visibility = if (cleanedText.isBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun applySubtitleStyle() {
        val subtitleView = subtitleDisplay ?: return
        val style = subtitleStyle
        val horizontalMarginPx = dp(style.horizontalMarginDp)
        val bottomMarginPx = calculateSubtitleBottomMarginPx(style)
        val horizontalPaddingPx = dp(style.paddingHorizontalDp)
        val verticalPaddingPx = dp(style.paddingVerticalDp)
        val cornerRadiusPx = dp(style.backgroundCornerRadiusDp).toFloat()
        subtitleView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            calculateSubtitleTextSizeSp(style)
        )
        subtitleView.maxLines = style.maxLines
        subtitleView.gravity = style.textGravity
        subtitleView.textAlignment = style.textAlignment
        subtitleView.setTextColor(style.textColor)
        subtitleView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        subtitleView.setPaddingRelative(0, 0, 0, 0)
        subtitleView.setLineBackgroundStyle(
            color = style.backgroundColor,
            cornerRadiusPx = cornerRadiusPx,
            horizontalPaddingPx = horizontalPaddingPx,
            verticalPaddingPx = verticalPaddingPx
        )
        subtitleView.setShadowLayer(
            style.shadowRadius,
            style.shadowDx,
            style.shadowDy,
            style.shadowColor
        )
        subtitleView.setTypeface(subtitleView.typeface, if (style.isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        subtitleView.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = horizontalMarginPx
            marginEnd = horizontalMarginPx
            bottomMargin = bottomMarginPx
        }
        subtitleView.translationZ = 0f
        subtitleView.requestLayout()
    }

    private fun calculateSubtitleTextSizeSp(style: PlayerSubtitleStyle): Float {
        val viewHeight = if (height > 0) height else resources.displayMetrics.heightPixels
        val density = resources.displayMetrics.density * resources.configuration.fontScale
        val proportionalSp = when {
            isInSystemPipMode -> (viewHeight * 0.060f) / density
            isIfCurrentIsFullscreen -> (viewHeight * 0.045f) / density
            else -> (viewHeight * 0.035f) / density
        }
        return proportionalSp
            .coerceAtLeast(style.minTextSizeSp)
            .coerceAtMost(style.maxTextSizeSp)
    }

    private fun calculateSubtitleBottomMarginPx(style: PlayerSubtitleStyle): Int {
        val viewHeight = if (height > 0) height else resources.displayMetrics.heightPixels
        val proportionalPx = when {
            isInSystemPipMode -> (viewHeight * 0.008f).toInt()
            isIfCurrentIsFullscreen -> (viewHeight * 0.006f).toInt()
            else -> (viewHeight * 0.004f).toInt()
        }
        return proportionalPx.coerceAtLeast(dp(style.bottomMarginDp))
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    fun logTrackState(reason: String) {
        when (val manager = getGSYVideoManager().player) {
            is IjkPlayerManager -> logIjkTrackState(reason, manager)
            is Exo2PlayerManager -> logExoTrackState(reason, manager)
            else -> Unit
        }
    }

    private fun trackTypeName(trackType: Int): String = when (trackType) {
        ITrackInfo.MEDIA_TRACK_TYPE_VIDEO -> "video"
        ITrackInfo.MEDIA_TRACK_TYPE_AUDIO -> "audio"
        ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE -> "subtitle"
        ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT -> "timedtext"
        ITrackInfo.MEDIA_TRACK_TYPE_METADATA -> "metadata"
        else -> "unknown($trackType)"
    }

    private fun buildTrackLabel(
        info: ITrackInfo,
        index: Int,
        fallbackPrefix: String,
        ordinals: MutableMap<String, Int>
    ): String {
        val format = info.format
        val baseLabel = buildTrackDisplayName(
            rawLabel = info.infoInline?.takeIf { it.isNotBlank() },
            language = info.language,
            fallbackPrefix = fallbackPrefix,
            index = index
        )
        val codec = format?.getString(IjkMediaFormat.KEY_IJK_CODEC_NAME_UI).orEmpty().trim()
        val bitrate = format?.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI).orEmpty().trim()
        val trackNumber = ordinals.getOrDefault(baseLabel, 0) + 1
        ordinals[baseLabel] = trackNumber
        val suffixParts = mutableListOf<String>()
        if (info.trackType == ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
            if (codec.isNotBlank() && codec != "N/A") suffixParts += codec.uppercase()
            if (bitrate.isNotBlank() && bitrate != "N/A") suffixParts += bitrate
        }
        val numberedBase = if (trackNumber > 1 || fallbackPrefix == "Subtitulo") {
            "$baseLabel $trackNumber"
        } else {
            baseLabel
        }
        return if (suffixParts.isEmpty()) numberedBase else "$numberedBase · ${suffixParts.joinToString(" · ")}"
    }

    private fun buildTrackDisplayName(
        rawLabel: String?,
        language: String?,
        fallbackPrefix: String,
        index: Int
    ): String {
        val normalizedLabel = normalizeTrackLabel(rawLabel)
        if (normalizedLabel.isNotBlank()) return normalizedLabel
        val normalizedLanguage = localizeTrackLanguage(language)
        if (normalizedLanguage.isNotBlank()) return normalizedLanguage
        return "$fallbackPrefix ${index + 1}"
    }

    private fun normalizeTrackLabel(rawLabel: String?): String {
        val value = rawLabel?.trim().orEmpty()
        if (value.isBlank()) return ""
        return when (value.lowercase()) {
            "spanish" -> "Español"
            "spanish - audio description" -> "Español (Audiodescripción)"
            "european spanish" -> "Español (Europa)"
            "japanese [original]" -> "Japonés (Original)"
            "japanese - audio description" -> "Japonés (Audiodescripción)"
            "english" -> "Inglés"
            "german" -> "Alemán"
            "french" -> "Francés"
            "italian" -> "Italiano"
            "czech" -> "Checo"
            "polish" -> "Polaco"
            "brazilian portuguese" -> "Portugués (Brasil)"
            "brazilian portuguese - audio description" -> "Portugués (Brasil, Audiodescripción)"
            "thai" -> "Tailandés"
            "turkish" -> "Turco"
            "indonesian" -> "Indonesio"
            "hindi" -> "Hindi"
            "hungarian" -> "Húngaro"
            "arabic" -> "Árabe"
            "danish" -> "Danés"
            "greek" -> "Griego"
            "hebrew" -> "Hebreo"
            "finnish" -> "Finés"
            "croatian" -> "Croata"
            "romanian" -> "Rumano"
            "russian" -> "Ruso"
            "swedish" -> "Sueco"
            "ukrainian" -> "Ucraniano"
            "vietnamese" -> "Vietnamita"
            "chinese" -> "Chino"
            "korean" -> "Coreano"
            "malay" -> "Malayo"
            else -> value
        }
    }

    private fun localizeTrackLanguage(language: String?): String {
        return when (language?.trim()?.lowercase()) {
            "spa", "es", "esp" -> "Español"
            "eng", "en" -> "Inglés"
            "por", "pt" -> "Portugués"
            "fra", "fr" -> "Francés"
            "deu", "ger", "de" -> "Alemán"
            "ita", "it" -> "Italiano"
            "jpn", "ja" -> "Japonés"
            "kor", "ko" -> "Coreano"
            "cs", "cze" -> "Checo"
            "hi", "hin" -> "Hindi"
            "hu", "hun" -> "Húngaro"
            "ms-ind" -> "Indonesio"
            "ms", "may" -> "Malayo"
            "pl", "pol" -> "Polaco"
            "th", "tha" -> "Tailandés"
            "tr", "tur" -> "Turco"
            "ar", "ara" -> "Árabe"
            "da", "dan" -> "Danés"
            "el", "gre" -> "Griego"
            "fi", "fin" -> "Finés"
            "he", "heb" -> "Hebreo"
            "hbs-hrv", "hr", "hrv" -> "Croata"
            "no-nob", "nob" -> "Noruego Bokmal"
            "nl", "dut" -> "Neerlandés"
            "ro", "rum" -> "Rumano"
            "ru", "rus" -> "Ruso"
            "sv", "swe" -> "Sueco"
            "uk", "ukr" -> "Ucraniano"
            "vi", "vie" -> "Vietnamita"
            "zh", "chi" -> "Chino"
            "und", "", null -> ""
            else -> language
        }
    }

    private fun refreshIjkTrackOptions(manager: IjkPlayerManager): Pair<List<GsyTrackOption>, List<GsyTrackOption>> {
        val trackInfo = manager.trackInfo ?: emptyArray()
        val selectedAudio = manager.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
        val selectedTimedText = manager.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
        val audioOrdinals = mutableMapOf<String, Int>()
        val subtitleOrdinals = mutableMapOf<String, Int>()

        val audios = trackInfo.mapIndexedNotNull { index, info ->
            if (info.trackType != ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) return@mapIndexedNotNull null
            val label = buildTrackLabel(
                info = info,
                index = index,
                fallbackPrefix = "Audio",
                ordinals = audioOrdinals
            )
            GsyTrackOption(
                streamIndex = index,
                trackType = info.trackType,
                label = label,
                selected = selectedAudio == index
            )
        }

        val subtitles = trackInfo.mapIndexedNotNull { index, info ->
            val type = info.trackType
            if (type != ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE &&
                type != ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
            ) {
                return@mapIndexedNotNull null
            }
            val label = buildTrackLabel(
                info = info,
                index = index,
                fallbackPrefix = "Subtitulo",
                ordinals = subtitleOrdinals
            )
            GsyTrackOption(
                streamIndex = index,
                trackType = type,
                label = label,
                selected = selectedTimedText == index
            )
        }

        return audios to subtitles
    }

    private fun refreshExoTrackOptions(manager: Exo2PlayerManager): Pair<List<GsyTrackOption>, List<GsyTrackOption>> {
        val exoPlayer = manager.mediaPlayer as? IjkExo2MediaPlayer ?: return emptyList<GsyTrackOption>() to emptyList()
        val mappedTrackInfo = exoPlayer.trackSelector?.currentMappedTrackInfo ?: return emptyList<GsyTrackOption>() to emptyList()
        val currentTracks = exoPlayer.currentTracks
        val audioOrdinals = mutableMapOf<String, Int>()
        val subtitleOrdinals = mutableMapOf<String, Int>()
        val audios = mutableListOf<GsyTrackOption>()
        val subtitles = mutableListOf<GsyTrackOption>()

        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val rendererType = mappedTrackInfo.getRendererType(rendererIndex)
            val rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            for (groupIndex in 0 until rendererTrackGroups.length) {
                val trackGroup = rendererTrackGroups[groupIndex]
                val currentGroup = currentTracks?.groups?.firstOrNull {
                    it.type == rendererType && it.mediaTrackGroup == trackGroup
                }
                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIndex)
                    val option = GsyTrackOption(
                        streamIndex = trackIndex,
                        trackType = when (rendererType) {
                            C.TRACK_TYPE_AUDIO -> ITrackInfo.MEDIA_TRACK_TYPE_AUDIO
                            C.TRACK_TYPE_TEXT -> ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
                            else -> rendererType
                        },
                        label = buildExoTrackLabel(
                            format = format,
                            fallbackPrefix = if (rendererType == C.TRACK_TYPE_AUDIO) "Audio" else "Subtitulo",
                            ordinals = if (rendererType == C.TRACK_TYPE_AUDIO) audioOrdinals else subtitleOrdinals,
                            index = trackIndex
                        ),
                        selected = currentGroup?.isTrackSelected(trackIndex) == true,
                        exoRendererIndex = rendererIndex,
                        exoGroupIndex = groupIndex,
                        exoTrackIndex = trackIndex
                    )
                    when (rendererType) {
                        C.TRACK_TYPE_AUDIO -> audios += option
                        C.TRACK_TYPE_TEXT -> subtitles += option
                    }
                }
            }
        }

        return audios to subtitles
    }

    fun hasExoVideoTrack(): Boolean {
        val manager = getGSYVideoManager().player as? Exo2PlayerManager ?: return false
        val exoPlayer = manager.mediaPlayer as? IjkExo2MediaPlayer ?: return false
        val mappedTrackInfo = exoPlayer.trackSelector?.currentMappedTrackInfo ?: return false
        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            if (mappedTrackInfo.getRendererType(rendererIndex) != C.TRACK_TYPE_VIDEO) continue
            val rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            if (rendererTrackGroups.length > 0) {
                return true
            }
        }
        return false
    }

    fun hasUnsupportedUnselectedExoVideoTrack(): Boolean {
        return getExoPlaybackCompatibility()?.hasUnsupportedUnselectedVideoTrack() == true
    }

    fun shouldAllowVisibleVideoFallback(): Boolean {
        val compatibility = getExoPlaybackCompatibility() ?: return false
        return compatibility.hasUnsupportedUnselectedVideoTrack() && !compatibility.hasSupportedSelectedVideoTrack()
    }

    fun getExoPlaybackCompatibility(): ExoPlaybackCompatibility? {
        val manager = getGSYVideoManager().player as? Exo2PlayerManager ?: return null
        val exoPlayer = manager.mediaPlayer as? IjkExo2MediaPlayer ?: return null
        val mappedTrackInfo = exoPlayer.trackSelector?.currentMappedTrackInfo ?: return null
        val currentTracks = exoPlayer.currentTracks ?: return null
        val videoTracks = mutableListOf<ExoTrackCompatibility>()
        val audioTracks = mutableListOf<ExoTrackCompatibility>()
        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val rendererType = mappedTrackInfo.getRendererType(rendererIndex)
            if (rendererType != C.TRACK_TYPE_VIDEO && rendererType != C.TRACK_TYPE_AUDIO) continue
            val rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            for (groupIndex in 0 until rendererTrackGroups.length) {
                val trackGroup = rendererTrackGroups[groupIndex]
                val currentGroup = currentTracks.groups.firstOrNull {
                    it.type == rendererType && it.mediaTrackGroup == trackGroup
                }
                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIndex)
                    val trackCompatibility = ExoTrackCompatibility(
                        trackType = rendererType,
                        label = buildExoTrackLabel(
                            format = format,
                            fallbackPrefix = if (rendererType == C.TRACK_TYPE_VIDEO) "Video" else "Audio",
                            ordinals = mutableMapOf(),
                            index = trackIndex
                        ),
                        sampleMimeType = format.sampleMimeType,
                        codecs = format.codecs,
                        selected = currentGroup?.isTrackSelected(trackIndex) == true,
                        compatibility = resolveExoTrackCompatibility(
                            format = format,
                            support = mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex)
                        )
                    )
                    when (rendererType) {
                        C.TRACK_TYPE_VIDEO -> videoTracks += trackCompatibility
                        C.TRACK_TYPE_AUDIO -> audioTracks += trackCompatibility
                    }
                }
            }
        }
        return ExoPlaybackCompatibility(videoTracks = videoTracks, audioTracks = audioTracks)
    }

    fun logExoPlaybackCompatibility(reason: String) {
        val compatibility = getExoPlaybackCompatibility() ?: return
        val summary = buildList {
            compatibility.videoTracks.forEach { track ->
                add(
                    "video[label=${track.label}, mime=${track.sampleMimeType}, codecs=${track.codecs}, selected=${track.selected}, support=${track.compatibility}]"
                )
            }
            compatibility.audioTracks.forEach { track ->
                add(
                    "audio[label=${track.label}, mime=${track.sampleMimeType}, codecs=${track.codecs}, selected=${track.selected}, support=${track.compatibility}]"
                )
            }
        }.joinToString(separator = "; ")
        Log.d(TRACKS_TAG, "exoCompatibility reason=$reason $summary")
    }

    private fun resolveExoTrackCompatibility(
        format: Format,
        support: Int
    ): ExoCodecCompatibility {
        return when (support) {
            C.FORMAT_HANDLED -> ExoCodecCompatibility.SUPPORTED
            C.FORMAT_EXCEEDS_CAPABILITIES -> ExoCodecCompatibility.EXCEEDS_CAPABILITIES
            C.FORMAT_UNSUPPORTED_TYPE,
            C.FORMAT_UNSUPPORTED_SUBTYPE,
            C.FORMAT_UNSUPPORTED_DRM -> {
                if (format.sampleMimeType == null || format.sampleMimeType == MimeTypes.VIDEO_UNKNOWN || format.sampleMimeType == "video/x-unknown") {
                    ExoCodecCompatibility.UNKNOWN
                } else {
                    ExoCodecCompatibility.UNSUPPORTED
                }
            }
            else -> ExoCodecCompatibility.UNKNOWN
        }
    }

    private fun selectExoTrackOption(manager: Exo2PlayerManager, option: GsyTrackOption) {
        val exoPlayer = manager.mediaPlayer as? IjkExo2MediaPlayer ?: return
        val trackSelector = exoPlayer.trackSelector ?: return
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
        val rendererIndex = option.exoRendererIndex ?: return
        val groupIndex = option.exoGroupIndex ?: return
        val trackIndex = option.exoTrackIndex ?: return
        val rendererType = mappedTrackInfo.getRendererType(rendererIndex)
        val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
        if (groupIndex !in 0 until trackGroups.length) return
        val trackGroup = trackGroups[groupIndex]
        if (trackIndex !in 0 until trackGroup.length) return

        val builder = trackSelector.parameters.buildUpon()
            .clearOverridesOfType(rendererType)
            .setTrackTypeDisabled(rendererType, false)
            .setOverrideForType(TrackSelectionOverride(trackGroup, trackIndex))

        trackSelector.parameters = builder.build()
    }

    private fun disableExoSubtitles(manager: Exo2PlayerManager) {
        val exoPlayer = manager.mediaPlayer as? IjkExo2MediaPlayer ?: return
        val trackSelector = exoPlayer.trackSelector ?: return
        trackSelector.parameters = trackSelector.parameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    private fun logIjkTrackState(reason: String, manager: IjkPlayerManager) {
        val trackInfo = manager.trackInfo ?: emptyArray()
        val selectedAudio = manager.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
        val selectedTimedText = manager.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
        Log.d(
            TRACKS_TAG,
            "trackState reason=$reason selectedAudio=$selectedAudio selectedTimedText=$selectedTimedText total=${trackInfo.size}"
        )
        trackInfo.forEachIndexed { index, info ->
            val format = info.format
            val formatSummary = if (format != null) {
                buildString {
                    append("mime=").append(format.getString(IMediaFormat.KEY_MIME))
                    append(", codec=").append(format.getString(IjkMediaFormat.KEY_IJK_CODEC_NAME_UI))
                    append(", codecLong=").append(format.getString(IjkMediaFormat.KEY_IJK_CODEC_LONG_NAME_UI))
                    append(", bitrate=").append(format.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI))
                    append(", channels=").append(format.getString(IjkMediaFormat.KEY_IJK_CHANNEL_UI))
                    append(", sampleRate=").append(format.getString(IjkMediaFormat.KEY_IJK_SAMPLE_RATE_UI))
                    append(", resolution=").append(format.getString(IjkMediaFormat.KEY_IJK_RESOLUTION_UI))
                    append(", frameRate=").append(format.getString(IjkMediaFormat.KEY_IJK_FRAME_RATE_UI))
                }
            } else {
                "null"
            }
            Log.d(
                TRACKS_TAG,
                "track index=$index type=${trackTypeName(info.trackType)} language=${info.language} info=${info.infoInline} format={$formatSummary}"
            )
        }
    }

    private fun logExoTrackState(reason: String, manager: Exo2PlayerManager) {
        val exoPlayer = manager.mediaPlayer as? IjkExo2MediaPlayer ?: return
        val currentTracks = exoPlayer.currentTracks ?: return
        Log.d(TRACKS_TAG, "trackState reason=$reason exoGroups=${currentTracks.groups.size}")
        currentTracks.groups.forEachIndexed { groupIndex, group ->
            for (trackIndex in 0 until group.mediaTrackGroup.length) {
                val format = group.mediaTrackGroup.getFormat(trackIndex)
                Log.d(
                    TRACKS_TAG,
                    "exo group=$groupIndex type=${rendererTypeName(group.type)} track=$trackIndex selected=${group.isTrackSelected(trackIndex)} format={$format}"
                )
            }
        }
    }

    private fun rendererTypeName(type: Int): String = when (type) {
        C.TRACK_TYPE_AUDIO -> "audio"
        C.TRACK_TYPE_TEXT -> "text"
        C.TRACK_TYPE_VIDEO -> "video"
        else -> "other($type)"
    }

    private fun buildExoTrackLabel(
        format: Format,
        fallbackPrefix: String,
        ordinals: MutableMap<String, Int>,
        index: Int
    ): String {
        val baseLabel = buildTrackDisplayName(
            rawLabel = format.label,
            language = format.language,
            fallbackPrefix = fallbackPrefix,
            index = index
        )
        val trackNumber = ordinals.getOrDefault(baseLabel, 0) + 1
        ordinals[baseLabel] = trackNumber
        val suffixParts = mutableListOf<String>()
        if (fallbackPrefix == "Audio") {
            format.codecs?.takeIf { it.isNotBlank() }?.let { suffixParts += it.uppercase() }
            format.bitrate.takeIf { it > 0 }?.let { suffixParts += "${it / 1000} kbps" }
            format.channelCount.takeIf { it > 0 }?.let { suffixParts += "${it}ch" }
        } else if (fallbackPrefix == "Subtitulo") {
            suffixParts += buildSubtitleFlagParts(format)
        }
        val numberedBase = if (trackNumber > 1 || fallbackPrefix == "Subtitulo") {
            "$baseLabel $trackNumber"
        } else {
            baseLabel
        }
        return if (suffixParts.isEmpty()) numberedBase else "$numberedBase · ${suffixParts.joinToString(" · ")}"
    }

    private fun buildSubtitleFlagParts(format: Format): List<String> {
        val parts = linkedSetOf<String>()
        if ((format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) {
            parts += "Forzado"
        }
        if ((format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0) {
            parts += "Predeterminado"
        }
        if ((format.selectionFlags and C.SELECTION_FLAG_AUTOSELECT) != 0) {
            parts += "Auto"
        }
        if ((format.roleFlags and C.ROLE_FLAG_CAPTION) != 0) {
            parts += "CC"
        }
        if ((format.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) != 0) {
            parts += "SDH"
        }
        if ((format.roleFlags and C.ROLE_FLAG_EASY_TO_READ) != 0) {
            parts += "Lectura fácil"
        }
        return parts.toList()
    }

    private fun stabilizeIjkTrackSwitch(option: GsyTrackOption, currentPosition: Long) {
        if (playbackContentType == PlaybackContentType.LIVE) return

        if (option.trackType == ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE ||
            option.trackType == ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
        ) {
            updateSubtitleText(null)
        }

        postDelayed({
            val seekTarget = currentPosition.coerceAtLeast(0L)
            if (seekTarget > 0L) {
                seekTo(seekTarget)
            }
        }, 120L)

        if (option.trackType == ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE ||
            option.trackType == ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
        ) {
            postDelayed({
                val seekTarget = currentPosition.coerceAtLeast(0L)
                if (seekTarget > 0L) {
                    seekTo(seekTarget)
                }
            }, 320L)
        }
    }

    override fun backToNormal() {
        val shouldSkipAnimation = mShowFullAnimation &&
            (mListItemRect == null || mListItemSize == null)

        if (shouldSkipAnimation) {
            val previousAnimationFlag = mShowFullAnimation
            mShowFullAnimation = false
            super.backToNormal()
            mShowFullAnimation = previousAnimationFlag
            return
        }

        super.backToNormal()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            applySubtitleStyle()
        }
    }

    override fun startWindowFullscreen(
        context: Context?,
        actionBar: Boolean,
        statusBar: Boolean
    ): GSYBaseVideoPlayer {
        val shouldHideStatusBar =
            statusBar && PlayerFullscreenPreferences.fullscreenOrientationMode != PlayerFullscreenOrientationMode.PORTRAIT
        val fullscreenPlayer = super.startWindowFullscreen(context, actionBar, shouldHideStatusBar)
        (fullscreenPlayer as? IptvGsyPlayerView)?.let { player ->
            player.playbackContentType = playbackContentType
            player.onCloseRequested = onCloseRequested
            player.onPipRequested = onPipRequested
            player.onFullscreenRequested = onFullscreenRequested
            player.onPlaybackEngineToggleRequested = onPlaybackEngineToggleRequested
            player.playbackEngine = playbackEngine
            player.isInSystemPipMode = isInSystemPipMode
            player.selectedScreenShowType = selectedScreenShowType
            player.skipNextFullscreenOrientationReset = skipNextFullscreenOrientationReset
            player.configureBaseUi()
            player.applyFullscreenPortraitInsets()
        }
        return fullscreenPlayer
    }

    override fun clearFullscreenLayout() {
        if (!mFullAnimEnd) {
            return
        }
        mIfCurrentIsFullscreen = false

        val shouldSkipOrientationReset =
            skipNextFullscreenOrientationReset && currentState == CURRENT_STATE_AUTO_COMPLETE
        skipNextFullscreenOrientationReset = false

        var delay = 0
        if (mOrientationUtils != null) {
            if (shouldSkipOrientationReset) {
                Log.d(
                    FULLSCREEN_TRANSITION_TAG,
                    "skip_back_to_portrait_on_autonext state=$currentState"
                )
            } else {
                delay = mOrientationUtils.backToProtVideo()
            }
            mOrientationUtils.setEnable(false)
            mOrientationUtils.releaseListener()
            mOrientationUtils = null
        }

        if (!mShowFullAnimation) {
            delay = 0
        }

        val vp = getViewGroup()
        val oldFullscreenView = vp.findViewById<View>(fullId)
        if (oldFullscreenView is com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer) {
            oldFullscreenView.isIfCurrentIsFullscreen = false
        }

        mInnerHandler.postDelayed(
            { backToNormal() },
            delay.toLong()
        )
    }
}

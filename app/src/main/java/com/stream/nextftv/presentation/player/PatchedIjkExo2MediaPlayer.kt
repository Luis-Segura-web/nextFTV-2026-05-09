package com.stream.nextftv.presentation.player

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer
import tv.danmaku.ijk.media.player.IjkTimedText

class PatchedIjkExo2MediaPlayer(context: Context) : IjkExo2MediaPlayer(context) {

    override fun onCues(cueGroup: CueGroup) {
        val subtitleText = cueGroup.cues
            .mapNotNull(Cue::text)
            .map { it.toString().trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

        Log.d(
            SUBTITLE_TAG,
            "exo_onCues count=${cueGroup.cues.size} text=${if (subtitleText.isBlank()) "<blank>" else subtitleText}"
        )

        if (subtitleText.isBlank()) {
            notifyOnTimedText(null)
            return
        }

        notifyOnTimedText(IjkTimedText(Rect(0, 0, 1, 1), subtitleText))
    }

    private companion object {
        const val SUBTITLE_TAG = "IptvTrackDebug"
    }
}

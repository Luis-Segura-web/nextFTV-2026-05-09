package com.stream.iptvrevolut.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stream.iptvrevolut.MainActivity
import com.stream.iptvrevolut.presentation.screens.livetv.components.VideoPlayer
import com.stream.iptvrevolut.utils.findActivity

@Composable
fun MiniPlayerOverlay(
    modifier: Modifier = Modifier
) {
    val session = GlobalPlaybackManager.miniSession ?: return
    val activity = LocalContext.current.findActivity() as? MainActivity

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .aspectRatio(16 / 9f)
        ) {
            VideoPlayer(
                url = session.url,
                title = session.title,
                useOriginalMedia3Controller = true,
                isFullScreen = false,
                isLive = session.isLive,
                isSmall = true,
                onPipRequested = { activity?.enterPipMode() },
                onClose = { GlobalPlaybackManager.stopAndClear() }
            )
        }
    }
}

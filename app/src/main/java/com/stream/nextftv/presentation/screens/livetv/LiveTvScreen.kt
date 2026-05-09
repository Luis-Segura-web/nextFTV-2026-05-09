package com.stream.nextftv.presentation.screens.livetv

import com.stream.nextftv.MainActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.stream.nextftv.presentation.player.GlobalPlaybackManager
import com.stream.nextftv.presentation.player.PipModeState
import com.stream.nextftv.presentation.theme.StreamingBackground
import com.stream.nextftv.utils.findActivity
import com.shuyu.gsyvideoplayer.GSYVideoManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    onBack: () -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val categories by viewModel.categories.collectAsState()
    val streams = viewModel.pagedStreams.collectAsLazyPagingItems()
    val currentStream = viewModel.currentPlayingStream
    val activity = remember(context) { context.findActivity() }
    val isInPipMode = PipModeState.isInPipMode
    var isFullScreen by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val currentStreamState by rememberUpdatedState(currentStream)

    fun closeLivePlayerIfNeeded() {
        isFullScreen = false
        if (viewModel.currentPlayingStream != null) {
            viewModel.currentPlayingStream = null
        }
        GlobalPlaybackManager.stopAndClear()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (activity != null && GSYVideoManager.isFullState(activity)) {
                return@onDispose
            }
            if (currentStreamState != null) {
                viewModel.currentPlayingStream = null
            }
            GlobalPlaybackManager.stopAndClear()
        }
    }

    // Recordar el estado de filtrado
    var lastFilterKey by androidx.compose.runtime.saveable.rememberSaveable { 
        mutableStateOf("${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}") 
    }

    LaunchedEffect(viewModel.selectedCategoryId, viewModel.sortOrder, viewModel.searchQuery) {
        val currentKey = "${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}"
        if (currentKey != lastFilterKey) {
            listState.scrollToItem(0)
            lastFilterKey = currentKey
        }
    }

    LaunchedEffect(viewModel.isSearchActive) {
        if (viewModel.isSearchActive) {
            delay(150)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.selectedCategoryId in setOf("all", "favorites", "recents")) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(currentStream?.streamId, viewModel.isPlayerLoading) {
        val stream = currentStream ?: return@LaunchedEffect
        GlobalPlaybackManager.updateSessionMetadata(
            url = "${viewModel.activeProfile?.url}live/${viewModel.activeProfile?.username}/${viewModel.activeProfile?.password}/${stream.streamId}.ts",
            title = stream.name,
            isLive = true
        )
    }

    fun enterFullscreen() {
        if (isFullScreen) return
        isFullScreen = true
    }

    fun exitFullscreen() {
        if (!isFullScreen) return
        isFullScreen = false
    }

    BackHandler {
        if (isFullScreen) {
            val activity = context.findActivity()
            if (activity == null || !GSYVideoManager.backFromWindowFull(activity)) {
                exitFullscreen()
            }
        } else if (viewModel.isSearchActive) {
            viewModel.isSearchActive = false
            viewModel.searchQuery = ""
        } else {
            closeLivePlayerIfNeeded()
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                if (!isInPipMode) {
                    LiveTvTopBar(
                        isSearchActive = viewModel.isSearchActive,
                        searchQuery = viewModel.searchQuery,
                        sortMenuExpanded = sortMenuExpanded,
                        focusRequester = focusRequester,
                        onSearchQueryChange = { viewModel.searchQuery = it },
                        onSearchToggle = {
                            viewModel.isSearchActive = !viewModel.isSearchActive
                            if (!viewModel.isSearchActive) {
                                viewModel.searchQuery = ""
                            }
                        },
                        onDismissSearch = {
                            viewModel.isSearchActive = false
                            viewModel.searchQuery = ""
                        },
                        onBack = {
                            closeLivePlayerIfNeeded()
                            onBack()
                        },
                        onSortMenuExpandedChange = { sortMenuExpanded = it },
                        onSortOrderSelected = {
                            viewModel.sortOrder = it
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        ) { innerPadding ->
            LiveTvMainContent(
                categories = categories,
                streams = streams,
                currentStream = currentStream,
                isInPipMode = isInPipMode,
                isFullScreen = isFullScreen,
                innerPadding = innerPadding,
                listState = listState,
                searchQuery = viewModel.searchQuery,
                selectedCategoryId = viewModel.selectedCategoryId,
                viewModel = viewModel,
                onEnterFullscreen = ::enterFullscreen,
                onExitFullscreen = ::exitFullscreen,
                onPipRequested = { (context.findActivity() as? MainActivity)?.enterPipMode() },
                onClosePlayer = ::closeLivePlayerIfNeeded
            )
        }
    }
}

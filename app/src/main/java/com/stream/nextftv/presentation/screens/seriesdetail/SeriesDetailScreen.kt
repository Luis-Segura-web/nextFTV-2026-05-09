package com.stream.nextftv.presentation.screens.seriesdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stream.nextftv.MainActivity
import com.stream.nextftv.data.remote.SeriesEpisodeDto
import com.stream.nextftv.presentation.player.GlobalPlaybackManager
import com.stream.nextftv.presentation.player.PipModeState
import com.stream.nextftv.presentation.screens.detail.components.formatElapsed
import com.stream.nextftv.presentation.screens.detail.components.SourceSelectionSheet
import com.stream.nextftv.presentation.theme.StreamingBackground
import com.stream.nextftv.utils.findActivity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    initialEpisodeId: String? = null,
    autoPlay: Boolean = false,
    sourceCategoryId: String? = null,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSeriesClick: (Int, String?) -> Unit,
    onActorClick: (Int, String?, String?) -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val isInPipMode = PipModeState.isInPipMode
    var isFullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(seriesId, sourceCategoryId) {
        viewModel.loadSeries(seriesId, initialEpisodeId, autoPlay, sourceCategoryId)
    }

    val series = viewModel.seriesStream
    val details = viewModel.seriesDetails
    val episodes = viewModel.episodes
    val currentEpisode = viewModel.currentEpisode
    val profile = viewModel.activeProfile
    val selectedSeason = viewModel.selectedSeason
    val recommendedSeries = viewModel.recommendedSeries
    val similarSeries = viewModel.similarSeries
    val variationItems = viewModel.variationItems
    val isFavorite by viewModel.isFavorite().collectAsState(initial = false)
    val episodeDownloads by viewModel.episodeDownloads.collectAsState(initial = emptyMap())
    val currentPlaybackSource = remember(currentEpisode, profile, episodeDownloads) {
        resolveEpisodePlaybackSource(
            episode = currentEpisode,
            profileUrl = profile?.url,
            profileUsername = profile?.username,
            profilePassword = profile?.password,
            download = currentEpisode?.id?.let(episodeDownloads::get)
        )
    }
    val currentLocalEpisodePath = currentPlaybackSource?.takeIf { it.isLocal }?.url
    var lastRecentRegisteredUrl by remember(seriesId) { mutableStateOf<String?>(null) }
    var lastPlayerPositionMs by remember(seriesId) { mutableLongStateOf(0L) }
    var lastPlayerDurationMs by remember(seriesId) { mutableLongStateOf(0L) }
    var pendingResumeEpisode by remember(seriesId) { mutableStateOf<SeriesEpisodeDto?>(null) }
    var showSecondarySections by remember(seriesId) { mutableStateOf(false) }
    val currentEpisodeState by rememberUpdatedState(currentEpisode)
    val playerActiveState by rememberUpdatedState(viewModel.isPlayerActive)
    val latestPositionState by rememberUpdatedState(lastPlayerPositionMs)
    val latestDurationState by rememberUpdatedState(lastPlayerDurationMs)

    LaunchedEffect(series?.seriesId, viewModel.isLoading) {
        if (series == null || viewModel.isLoading) {
            showSecondarySections = false
            return@LaunchedEffect
        }
        delay(350)
        showSecondarySections = true
    }

    fun closeSeriesPlayerIfNeeded() {
        val episode = currentEpisode ?: run {
            viewModel.isPlayerActive = false
            GlobalPlaybackManager.stopAndClear()
            return
        }
        if (viewModel.isPlayerActive) {
            val persistedProgress = viewModel.episodeProgressById[episode.id] ?: 0L
            val resolvedPositionMs = maxOf(lastPlayerPositionMs, persistedProgress)
            val resolvedDurationMs = lastPlayerDurationMs
                .takeIf { it > 0L }
                ?: viewModel.episodeDurationById[episode.id].orEmptyDuration()
            viewModel.onPlayerClosed(episode, resolvedPositionMs, resolvedDurationMs)
            viewModel.isPlayerActive = false
        }
        GlobalPlaybackManager.stopAndClear()
    }

    DisposableEffect(Unit) {
        onDispose {
            val episodeOnExit = currentEpisodeState
            if (playerActiveState && episodeOnExit != null) {
                val persistedProgress = viewModel.episodeProgressById[episodeOnExit.id] ?: 0L
                val resolvedPositionMs = maxOf(latestPositionState, persistedProgress)
                val resolvedDurationMs = latestDurationState
                    .takeIf { it > 0L }
                    ?: viewModel.episodeDurationById[episodeOnExit.id].orEmptyDuration()
                viewModel.onPlayerClosed(episodeOnExit, resolvedPositionMs, resolvedDurationMs)
                viewModel.isPlayerActive = false
            }
            GlobalPlaybackManager.stopAndClear()
        }
    }

    if (viewModel.showVariationSelector) {
        SourceSelectionSheet(
            title = viewModel.variationSelectorTitle,
            sources = viewModel.variations,
            onSourceClick = { variation ->
                viewModel.showVariationSelector = false
                onSeriesClick(variation.source.seriesId, sourceCategoryId)
            },
            onDismiss = { viewModel.showVariationSelector = false },
            sourceName = { it.source.name },
            isCurrentSource = { it.isCurrent }
        )
    }

    pendingResumeEpisode?.let { episode ->
        val resumeMs = viewModel.episodeProgressById[episode.id] ?: 0L
        AlertDialog(
            onDismissRequest = { pendingResumeEpisode = null },
            title = { Text("Reanudar episodio") },
            text = {
                Text("Este episodio tiene avance guardado (${formatElapsed(resumeMs)}). ¿Deseas continuar o empezar desde cero?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingResumeEpisode = null
                        viewModel.onEpisodeClick(episode)
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingResumeEpisode = null
                        viewModel.onEpisodeClickFromStart(episode)
                    }
                ) {
                    Text("Desde cero")
                }
            }
        )
    }

    val coverUrl = series?.cover
    val xtreamBackdrop = series?.backdropPath?.firstOrNull()
    val detailsBackdropPath = details?.backdropPath
    val seriesBackdropUrl = remember(series, details) {
        when {
            !xtreamBackdrop.isNullOrBlank() && xtreamBackdrop != coverUrl -> xtreamBackdrop
            !detailsBackdropPath.isNullOrBlank() -> "https://image.tmdb.org/t/p/w780$detailsBackdropPath"
            else -> coverUrl
        }
    }

    LaunchedEffect(currentEpisode?.id, viewModel.isPlayerActive) {
        val episode = currentEpisode ?: return@LaunchedEffect
        if (!viewModel.isPlayerActive) return@LaunchedEffect
        val streamUrl = resolveEpisodePlaybackSource(
            episode = episode,
            profileUrl = profile?.url,
            profileUsername = profile?.username,
            profilePassword = profile?.password,
            download = episodeDownloads[episode.id]
        )?.url
        val title = buildEpisodeDisplayTitle(episode, series?.name)
        if (streamUrl != null) {
            GlobalPlaybackManager.updateSessionMetadata(
                url = streamUrl,
                title = title,
                isLive = false
            )
        }
    }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            closeSeriesPlayerIfNeeded()
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                if (!isFullScreen && !isInPipMode) {
                    SeriesDetailTopBar(
                        title = series?.name.orEmpty(),
                        onBack = {
                            closeSeriesPlayerIfNeeded()
                            onBack()
                        },
                        onHome = {
                            closeSeriesPlayerIfNeeded()
                            onHome()
                        }
                    )
                }
            }
        ) { innerPadding ->
            if (series != null) {
                var selectedDetailTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("INFO", "EPISODIOS")

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isFullScreen || isInPipMode) PaddingValues(0.dp) else innerPadding)
                ) {
                // 1. HEADER DINÁMICO
                val playerModifier = if (isFullScreen || isInPipMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                }

                val resumeMs = currentEpisode?.let { viewModel.episodeProgressById[it.id] ?: 0L } ?: 0L
                val streamUrl = if (viewModel.isPlayerActive && currentEpisode != null && profile != null) {
                    currentLocalEpisodePath
                        ?: "${profile.url}series/${profile.username}/${profile.password}/${currentEpisode.id}.${currentEpisode.containerExtension ?: "mp4"}"
                } else {
                    null
                }
                LaunchedEffect(streamUrl, currentEpisode?.id) {
                    if (streamUrl != null) {
                        lastPlayerPositionMs = resumeMs.coerceAtLeast(0L)
                        lastPlayerDurationMs = currentEpisode?.id
                            ?.let { episodeId -> viewModel.episodeDurationById[episodeId] }
                            ?.coerceAtLeast(0L)
                            ?: 0L
                    }
                }
                SeriesDetailPlayerSection(
                    modifier = playerModifier,
                    isPlayerActive = viewModel.isPlayerActive,
                    currentEpisode = currentEpisode,
                    profileUrl = profile?.url,
                    profileUsername = profile?.username,
                    profilePassword = profile?.password,
                    episodeDownload = currentEpisode?.id?.let(episodeDownloads::get),
                    seriesName = series.name,
                    seriesBackdropUrl = seriesBackdropUrl,
                    seriesPosterUrl = coverUrl,
                    isFullScreen = isFullScreen,
                    isInPipMode = isInPipMode,
                    resumeMs = resumeMs,
                    onLoading = { loading ->
                        viewModel.isPlayerLoading = loading
                        if (!loading && streamUrl != null && lastRecentRegisteredUrl != streamUrl) {
                            viewModel.onPlaybackStarted(series.seriesId)
                            lastRecentRegisteredUrl = streamUrl
                        }
                    },
                    onProgress = { pos ->
                        currentEpisode?.let { viewModel.updateSeriesProgress(it, pos) }
                    },
                    onProgressSnapshot = { pos, duration ->
                        lastPlayerPositionMs = pos
                        lastPlayerDurationMs = duration
                        currentEpisode?.let { viewModel.updateEpisodePlaybackSnapshot(it, pos, duration) }
                    },
                    onPlaybackCompleted = { pos, duration ->
                        lastPlayerPositionMs = pos
                        lastPlayerDurationMs = duration
                        currentEpisode?.let { viewModel.onPlaybackCompleted(it, pos, duration) }
                    },
                    onEnterFullscreen = {
                        isFullScreen = true
                    },
                    onQuitFullscreen = { isFullScreen = false },
                    onNext = { viewModel.onNextEpisode() },
                    onPrevious = { viewModel.onPreviousEpisode() },
                    shouldKeepFullscreenOnAutoComplete = {
                        isFullScreen && viewModel.willAutoPlayNextEpisode(currentEpisode)
                    },
                    onClose = {
                        closeSeriesPlayerIfNeeded()
                        isFullScreen = false
                    },
                    onPipRequested = { (context.findActivity() as? MainActivity)?.enterPipMode() }
                )

                // 2. TABS DE NAVEGACIÓN (Solo si no es FullScreen)
                if (!isFullScreen && !isInPipMode) {
                    SeriesDetailMainContent(
                        selectedDetailTab = selectedDetailTab,
                        onTabSelected = { selectedDetailTab = it },
                        tabs = tabs,
                        series = series,
                        details = details,
                        episodes = episodes,
                        selectedSeason = selectedSeason,
                        currentEpisode = currentEpisode,
                        variationItems = if (showSecondarySections) variationItems else emptyList(),
                        recommendedSeries = if (showSecondarySections) recommendedSeries else emptyList(),
                        similarSeries = if (showSecondarySections) similarSeries else emptyList(),
                        isFavorite = isFavorite,
                        lastEpisodePositionMs = viewModel.lastEpisodePositionMs,
                        episodeDownloads = episodeDownloads,
                        seriesBackdropUrl = seriesBackdropUrl,
                        onPlayPrimary = {
                            selectedDetailTab = 1
                            viewModel.playLastSeenEpisodeOrFallback()
                            viewModel.isPlayerActive = true
                        },
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onVariationClick = { item ->
                            if (!item.isActual) {
                                onSeriesClick(item.localId, sourceCategoryId)
                            }
                        },
                        onRecommendedClick = { item ->
                            viewModel.checkAndNavigateToSeries(item.id, item.tvName ?: item.title) { seriesId ->
                                onSeriesClick(seriesId, sourceCategoryId)
                            }
                        },
                        onSimilarClick = { item ->
                            viewModel.checkAndNavigateToSeries(item.id, item.tvName ?: item.title) { seriesId ->
                                onSeriesClick(seriesId, sourceCategoryId)
                            }
                        },
                        onActorClick = { actor ->
                            onActorClick(actor.id, actor.name, actor.profilePath)
                        },
                        onSeasonSelect = { viewModel.onSeasonSelect(it) },
                        onEpisodeClick = { episode ->
                            val savedProgress = viewModel.episodeProgressById[episode.id] ?: 0L
                            val durationMs = viewModel.episodeDurationById[episode.id].orEmptyDuration()
                            val isCompletedAt100 = durationMs > 0L && savedProgress >= durationMs
                            val hasResumeToAsk = savedProgress > 0L && !isCompletedAt100

                            when {
                                hasResumeToAsk -> pendingResumeEpisode = episode
                                isCompletedAt100 -> viewModel.onEpisodeClickFromStart(episode)
                                else -> viewModel.onEpisodeClick(episode)
                            }
                        },
                        onEpisodeDownloadClick = { episode ->
                            viewModel.handleEpisodeDownloadClick(episode)
                        },
                        primaryActionMode = viewModel.primaryActionMode,
                        lastEpisodeSeason = viewModel.lastEpisodeSeason,
                        lastEpisodeNumber = viewModel.lastEpisodeNumber,
                        episodeProgressById = viewModel.episodeProgressById,
                        episodeDurationById = viewModel.episodeDurationById,
                        tmdbEpisodeTitleByKey = viewModel.tmdbEpisodeTitleByKey
                    )
                }
                }
            } else if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun Long?.orEmptyDuration(): Long = this?.coerceAtLeast(0L) ?: 0L

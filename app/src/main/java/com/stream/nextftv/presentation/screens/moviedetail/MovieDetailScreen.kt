package com.stream.nextftv.presentation.screens.moviedetail

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
import com.stream.nextftv.presentation.player.GlobalPlaybackManager
import com.stream.nextftv.presentation.player.PipModeState
import com.stream.nextftv.presentation.screens.detail.components.parseDurationToMs
import com.stream.nextftv.presentation.screens.detail.components.SourceSelectionSheet
import com.stream.nextftv.presentation.theme.StreamingBackground
import com.stream.nextftv.utils.findActivity
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    streamId: Int,
    autoPlay: Boolean = false,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onActorClick: (Int, String?, String?) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val isInPipMode = PipModeState.isInPipMode
    var isFullScreen by remember { mutableStateOf(false) }
    var lastPlayerPositionMs by remember(streamId) { mutableLongStateOf(0L) }
    var lastPlayerDurationMs by remember(streamId) { mutableLongStateOf(0L) }
    var showSecondarySections by remember(streamId) { mutableStateOf(false) }

    LaunchedEffect(streamId) {
        viewModel.loadMovie(streamId, autoPlay)
    }

    val movie = viewModel.vodStream
    val xtream = viewModel.xtreamInfo
    val tmdb = viewModel.movieDetails
    val collectionItems = viewModel.collectionItems
    val collectionTitle = viewModel.collectionTitle
    val profile = viewModel.activeProfile
    val recommendedMovies = viewModel.recommendedMovies
    val similarMovies = viewModel.similarMovies
    val isFavorite by viewModel.isFavorite().collectAsState(initial = false)
    val downloadState by viewModel.downloadState.collectAsState(initial = null)
    val currentLocalFilePath = remember(downloadState?.filePath, downloadState?.status) {
        val download = downloadState
        if (
            download?.status == "completed" &&
            File(download.filePath).exists() &&
            File(download.filePath).length() > 0L
        ) {
            download.filePath
        } else {
            null
        }
    }

    LaunchedEffect(movie?.streamId, viewModel.isLoading) {
        if (movie == null || viewModel.isLoading) {
            showSecondarySections = false
            return@LaunchedEffect
        }
        delay(350)
        showSecondarySections = true
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentMovieState by rememberUpdatedState(movie)
    val playerActiveState by rememberUpdatedState(viewModel.isPlayerActive)
    val latestPositionState by rememberUpdatedState(lastPlayerPositionMs)
    val latestDurationState by rememberUpdatedState(lastPlayerDurationMs)

    fun closeMoviePlayerIfNeeded() {
        val activeMovie = movie
        if (activeMovie == null) {
            viewModel.isPlayerActive = false
            GlobalPlaybackManager.stopAndClear()
            return
        }
        if (viewModel.isPlayerActive) {
            viewModel.onPlayerClosed(
                activeMovie.streamId,
                lastPlayerPositionMs,
                lastPlayerDurationMs
            )
            viewModel.isPlayerActive = false
        }
        GlobalPlaybackManager.stopAndClear()
    }

    DisposableEffect(Unit) {
        onDispose {
            val movieOnExit = currentMovieState
            if (playerActiveState && movieOnExit != null) {
                viewModel.onPlayerClosed(
                    movieOnExit.streamId,
                    latestPositionState,
                    latestDurationState
                )
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
                onMovieClick(variation.source.streamId)
            },
            onDismiss = { viewModel.showVariationSelector = false },
            sourceName = { it.source.name },
            isCurrentSource = { it.isCurrent }
        )
    }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            closeMoviePlayerIfNeeded()
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isFullScreen && !isInPipMode) {
                    MovieDetailTopBar(
                        title = movie?.name.orEmpty(),
                        onBack = {
                            closeMoviePlayerIfNeeded()
                            onBack()
                        },
                        onHome = {
                            closeMoviePlayerIfNeeded()
                            onHome()
                        }
                    )
                }
            }
        ) { innerPadding ->
            if (movie != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isFullScreen || isInPipMode) PaddingValues(0.dp) else innerPadding)
                ) {
                // 1. HEADER (DINÁMICO SEGÚN FULLSCREEN)
                val playerModifier = if (isFullScreen || isInPipMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                }

                val posterUrl = movie.streamIcon
                val xtreamBackdrop = movie.backdropUrl
                val xtreamInfoImage = xtream?.info?.movieImage
                val tmdbBackdropPath = tmdb?.backdropPath
                val movieThumbUrl = when {
                    !xtreamBackdrop.isNullOrBlank() && xtreamBackdrop != posterUrl -> xtreamBackdrop
                    !tmdbBackdropPath.isNullOrBlank() -> "https://image.tmdb.org/t/p/w1280$tmdbBackdropPath"
                    !xtreamInfoImage.isNullOrBlank() && xtreamInfoImage != posterUrl -> xtreamInfoImage
                    else -> posterUrl
                }
                val streamUrl = if (viewModel.isPlayerActive && profile != null) {
                    currentLocalFilePath
                        ?: "${profile.url}movie/${profile.username}/${profile.password}/${movie.streamId}.${movie.containerExtension ?: "mp4"}"
                } else {
                    null
                }
                LaunchedEffect(streamUrl, viewModel.lastMoviePositionMs, viewModel.lastMovieDurationMs) {
                    if (streamUrl != null) {
                        lastPlayerPositionMs = viewModel.lastMoviePositionMs.coerceAtLeast(0L)
                        lastPlayerDurationMs = viewModel.lastMovieDurationMs.coerceAtLeast(0L)
                    }
                }
                MovieDetailPlayerSection(
                    modifier = playerModifier,
                    isPlayerActive = viewModel.isPlayerActive,
                    profileUrl = profile?.url,
                    profileUsername = profile?.username,
                    profilePassword = profile?.password,
                    localFilePath = currentLocalFilePath,
                    movieStreamId = movie.streamId,
                    movieName = movie.name,
                    containerExtension = movie.containerExtension,
                    movieThumbUrl = movieThumbUrl,
                    moviePosterUrl = posterUrl,
                    isFullScreen = isFullScreen,
                    isInPipMode = isInPipMode,
                    resumePositionMs = viewModel.lastMoviePositionMs,
                    onLoading = { loading ->
                        if (!loading) viewModel.onPlaybackStarted(movie.streamId)
                    },
                    onProgress = { pos -> viewModel.updateMovieProgress(movie.streamId, pos) },
                    onProgressSnapshot = { pos, duration ->
                        lastPlayerPositionMs = pos
                        lastPlayerDurationMs = duration
                        viewModel.updateMovieDuration(movie.streamId, duration)
                    },
                    onEnterFullscreen = { isFullScreen = true },
                    onQuitFullscreen = { isFullScreen = false },
                    onClose = {
                        closeMoviePlayerIfNeeded()
                        isFullScreen = false
                    },
                    onPipRequested = { (context.findActivity() as? MainActivity)?.enterPipMode() }
                )

                // 2. CONTENIDO SCROLLABLE (Solo visible si NO es FullScreen)
                if (!isFullScreen && !isInPipMode) {
                    MovieDetailScrollableContent(
                        movieName = movie.name,
                        tmdbYear = tmdb?.releaseDate?.take(4),
                        xtreamYear = xtream?.info?.releaseDate?.take(4),
                        runtime = xtream?.info?.runtime,
                        voteAverage = tmdb?.voteAverage,
                        lastMoviePositionMs = viewModel.lastMoviePositionMs,
                        lastMovieDurationMs = viewModel.lastMovieDurationMs,
                        plot = tmdb?.overview?.takeIf { it.isNotBlank() } ?: xtream?.info?.plot,
                        cast = if (showSecondarySections) tmdb?.credits?.cast.orEmpty() else emptyList(),
                        collectionTitle = if (showSecondarySections) collectionTitle else "",
                        collectionItems = if (showSecondarySections) collectionItems else emptyList(),
                        recommendedMovies = if (showSecondarySections) recommendedMovies else emptyList(),
                        similarMovies = if (showSecondarySections) similarMovies else emptyList(),
                        isFavorite = isFavorite,
                        downloadState = downloadState,
                        onPlay = { viewModel.isPlayerActive = true },
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onDownload = {
                            viewModel.handleDownloadClick()
                            if (downloadState == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Descarga añadida a la cola")
                                }
                            }
                        },
                        onCollectionMovieClick = { item ->
                            viewModel.onCollectionItemClick(item) { id -> onMovieClick(id) }
                        },
                        onRecommendedMovieClick = { item ->
                            viewModel.checkAndNavigateToMovie(item.id, item.title) { id -> onMovieClick(id) }
                        },
                        onSimilarMovieClick = { item ->
                            viewModel.checkAndNavigateToMovie(item.id, item.title) { id -> onMovieClick(id) }
                        },
                        onActorClick = { actor ->
                            onActorClick(actor.id, actor.name, actor.profilePath)
                        }
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

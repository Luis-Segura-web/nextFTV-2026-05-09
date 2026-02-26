package com.stream.iptvrevolut.presentation.screens.moviedetail

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.stream.iptvrevolut.R
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieShortDto
import com.stream.iptvrevolut.presentation.player.PipModeState
import com.stream.iptvrevolut.presentation.screens.livetv.components.VideoPlayer
import com.stream.iptvrevolut.presentation.components.download.CircularDownloadButton
import com.stream.iptvrevolut.presentation.components.selector.SourceSelectionSheet
import com.stream.iptvrevolut.presentation.theme.spacing
import com.stream.iptvrevolut.presentation.theme.shimmerLoading
import com.stream.iptvrevolut.utils.findActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    streamId: Int,
    autoPlay: Boolean = false,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val isInPipMode = PipModeState.isInPipMode
    var isFullScreen by remember { mutableStateOf(false) }
    var lastPlayerPositionMs by remember(streamId) { mutableLongStateOf(0L) }
    var lastPlayerDurationMs by remember(streamId) { mutableLongStateOf(0L) }

    LaunchedEffect(streamId) {
        viewModel.loadMovie(streamId, autoPlay)
    }

    // Manejo de rotación inteligente para Películas
    DisposableEffect(isFullScreen) {
        if (isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (viewModel.showVariationSelector) {
        SourceSelectionSheet(
            title = movie?.name ?: "Seleccionar versión",
            sources = viewModel.variations,
            onSourceClick = { variation ->
                viewModel.showVariationSelector = false
                onMovieClick(variation.streamId)
            },
            onDismiss = { viewModel.showVariationSelector = false },
            sourceName = { it.name }
        )
    }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else if (viewModel.isPlayerActive && movie != null) {
            viewModel.onPlayerClosed(movie.streamId, lastPlayerPositionMs, lastPlayerDurationMs)
            viewModel.isPlayerActive = false
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isFullScreen && !isInPipMode) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = movie?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = onHome) {
                            Icon(Icons.Default.Home, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    ) { innerPadding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (movie != null) {
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

                Box(
                    modifier = playerModifier.background(Color.Black)
                ) {
                    if (viewModel.isPlayerActive && profile != null) {
                        val streamUrl = viewModel.localFilePath ?: "${profile.url}movie/${profile.username}/${profile.password}/${movie.streamId}.${movie.containerExtension ?: "mp4"}"
                        LaunchedEffect(streamUrl, viewModel.lastMoviePositionMs, viewModel.lastMovieDurationMs) {
                            lastPlayerPositionMs = viewModel.lastMoviePositionMs.coerceAtLeast(0L)
                            lastPlayerDurationMs = viewModel.lastMovieDurationMs.coerceAtLeast(0L)
                        }
                        key(streamUrl) {
                            VideoPlayer(
                                url = streamUrl,
                                title = movie.name,
                                useOriginalMedia3Controller = true,
                                isFullScreen = isFullScreen,
                                resumePositionMs = viewModel.lastMoviePositionMs,
                                onLoading = { loading -> if (!loading) viewModel.onPlaybackStarted(movie.streamId) },
                                onProgress = { pos -> viewModel.updateMovieProgress(movie.streamId, pos) },
                                onProgressSnapshot = { pos, duration ->
                                    lastPlayerPositionMs = pos
                                    lastPlayerDurationMs = duration
                                    viewModel.updateMovieDuration(movie.streamId, duration)
                                },
                                onFullScreenClick = { isFullScreen = !isFullScreen },
                                onClose = { 
                                    viewModel.onPlayerClosed(movie.streamId, lastPlayerPositionMs, lastPlayerDurationMs)
                                    isFullScreen = false
                                    viewModel.isPlayerActive = false 
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        var isImageLoading by remember { mutableStateOf(true) }
                        
                        val posterUrl = movie.streamIcon
                        val xtreamBackdrop = movie.backdropUrl
                        val xtreamInfoImage = xtream?.info?.movieImage

                        val backdropUrl = when {
                            !xtreamBackdrop.isNullOrBlank() && xtreamBackdrop != posterUrl -> xtreamBackdrop
                            !tmdb?.backdropPath.isNullOrBlank() -> "https://image.tmdb.org/t/p/w1280${tmdb?.backdropPath}"
                            !xtreamInfoImage.isNullOrBlank() && xtreamInfoImage != posterUrl -> xtreamInfoImage
                            else -> posterUrl
                        }

                        AsyncImage(
                            model = backdropUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().shimmerLoading(isImageLoading),
                            contentScale = ContentScale.Crop,
                            onSuccess = { isImageLoading = false },
                            onError = { isImageLoading = false }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                    )
                                )
                        )
                    }
                }

                // 2. CONTENIDO SCROLLABLE (Solo visible si NO es FullScreen)
                if (!isFullScreen && !isInPipMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(MaterialTheme.spacing.medium)
                    ) {
                        Text(
                            text = movie.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        val year = tmdb?.releaseDate?.take(4) ?: xtream?.info?.releaseDate?.take(4)
                        val duration = xtream?.info?.runtime ?: ""
                        
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!year.isNullOrBlank()) InfoChip(text = year)
                            if (duration.isNotBlank()) InfoChip(text = "$duration min")
                            tmdb?.voteAverage?.let { if (it > 0) RatingChip(rating = it) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.isPlayerActive = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                val hasProgress = viewModel.lastMoviePositionMs > 0L
                                val parsedRuntimeMs = parseMovieRuntimeMs(xtream?.info?.runtime)
                                val durationForProgress = when {
                                    viewModel.lastMovieDurationMs > 0L -> viewModel.lastMovieDurationMs
                                    parsedRuntimeMs > 0L -> parsedRuntimeMs
                                    else -> 0L
                                }
                                val progressFraction = if (hasProgress && durationForProgress > 0L) {
                                    (viewModel.lastMoviePositionMs.toFloat() / durationForProgress.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }

                                Box(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (hasProgress) "CONTINUAR" else "REPRODUCIR",
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp
                                            )
                                            if (hasProgress) {
                                                Text(
                                                    text = formatElapsed(viewModel.lastMoviePositionMs),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }

                                    if (hasProgress && progressFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .background(Color.Black.copy(alpha = 0.28f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(progressFraction)
                                                    .background(Color.White.copy(alpha = 0.95f))
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                onClick = { viewModel.toggleFavorite() },
                                modifier = Modifier.size(54.dp),
                                color = if (isFavorite) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                border = if (isFavorite) BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            val downloadState by viewModel.downloadState.collectAsState(initial = null)
                            CircularDownloadButton(
                                download = downloadState,
                                onClick = { 
                                    viewModel.handleDownloadClick()
                                    if (downloadState == null) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Descarga añadida a la cola")
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val plot = tmdb?.overview?.takeIf { it.isNotBlank() } ?: xtream?.info?.plot
                        if (!plot.isNullOrBlank()) {
                            Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = plot,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                        }

                        tmdb?.credits?.cast?.let { castList ->
                            if (castList.isNotEmpty()) {
                                HorizontalListSection("Reparto Principal") {
                                    items(castList.take(10)) { actor ->
                                        ActorItem(name = actor.name, role = actor.character, photoUrl = actor.profilePath)
                                    }
                                }
                            }
                        }

                        if (collectionItems.isNotEmpty()) {
                            HorizontalListSection(collectionTitle) {
                                items(collectionItems) { item ->
                                    MovieShortItem(
                                        movie = item.tmdbMovie,
                                        isActual = item.isActual,
                                        isFound = item.isFound,
                                        onClick = { 
                                            if (item.isActual) return@MovieShortItem
                                            if (item.isFound) {
                                                viewModel.checkAndNavigateToMovie(item.tmdbMovie.id, item.tmdbMovie.title, onMovieClick)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (recommendedMovies.isNotEmpty()) {
                            HorizontalListSection("Películas Recomendadas") {
                                items(recommendedMovies) { (item, _) ->
                                    MovieShortItem(item, onClick = { 
                                        viewModel.checkAndNavigateToMovie(item.id, item.title, onMovieClick)
                                    })
                                }
                            }
                        }

                        if (similarMovies.isNotEmpty()) {
                            HorizontalListSection("Películas Similares") {
                                items(similarMovies) { (item, _) ->
                                    MovieShortItem(item, onClick = { 
                                        viewModel.checkAndNavigateToMovie(item.id, item.title, onMovieClick)
                                    })
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalListSection(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp),
            content = content
        )
    }
}

@Composable
fun MovieShortItem(
    movie: TmdbMovieShortDto, 
    isActual: Boolean = false,
    isFound: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(modifier = Modifier.width(100.dp).clickable(enabled = isFound) { onClick() }) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w342${movie.posterPath}",
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isFound) 1f else 0.4f
            )
            
            if (isActual) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "ACTUAL",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else if (!isFound) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "NO DISPONIBLE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = movie.title ?: movie.tvName ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            color = if (isFound) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun CircleActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.4f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RatingChip(rating: Double) {
    Surface(
        color = Color(0xFFFFD700).copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(String.format("%.1f", rating), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        }
    }
}

@Composable
fun ActorItem(name: String, role: String, photoUrl: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w185$photoUrl",
            contentDescription = name,
            modifier = Modifier.size(70.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        Text(role, style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

private fun parseMovieRuntimeMs(raw: String?): Long {
    val input = raw?.trim().orEmpty()
    if (input.isBlank()) return 0L

    val hhMmSs = Regex("""^(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?$""").matchEntire(input)
    if (hhMmSs != null) {
        val a = hhMmSs.groupValues[1].toLongOrNull() ?: return 0L
        val b = hhMmSs.groupValues[2].toLongOrNull() ?: return 0L
        val c = hhMmSs.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
        val totalSeconds = if (hhMmSs.groupValues[3].isNotBlank()) (a * 3600L) + (b * 60L) + c else (a * 60L) + b
        return totalSeconds * 1000L
    }

    val numeric = Regex("""(\d+(?:[.,]\d+)?)""").find(input)?.value
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?: return 0L

    return when {
        input.contains("h", ignoreCase = true) -> (numeric * 60.0 * 60.0 * 1000.0).toLong()
        input.contains("min", ignoreCase = true) || input.contains("m", ignoreCase = true) ->
            (numeric * 60.0 * 1000.0).toLong()
        else -> (numeric * 60.0 * 1000.0).toLong()
    }
}

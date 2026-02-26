package com.stream.iptvrevolut.presentation.screens.seriesdetail

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.stream.iptvrevolut.data.remote.SeriesEpisodeDto
import com.stream.iptvrevolut.presentation.player.PipModeState
import com.stream.iptvrevolut.presentation.screens.livetv.components.VideoPlayer
import com.stream.iptvrevolut.presentation.screens.moviedetail.CircleActionButton
import com.stream.iptvrevolut.presentation.screens.moviedetail.InfoChip
import com.stream.iptvrevolut.presentation.screens.moviedetail.RatingChip
import com.stream.iptvrevolut.presentation.screens.moviedetail.ActorItem
import com.stream.iptvrevolut.presentation.components.download.CircularDownloadButton
import com.stream.iptvrevolut.presentation.components.selector.SourceSelectionSheet
import com.stream.iptvrevolut.presentation.theme.dimens
import com.stream.iptvrevolut.presentation.theme.spacing
import com.stream.iptvrevolut.presentation.theme.shimmerLoading
import com.stream.iptvrevolut.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    initialEpisodeId: String? = null,
    autoPlay: Boolean = false,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSeriesClick: (Int) -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val isInPipMode = PipModeState.isInPipMode
    var isFullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(seriesId) {
        viewModel.loadSeries(seriesId, initialEpisodeId, autoPlay)
    }

    // Manejo de rotación inteligente para Series
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
    var lastRecentRegisteredUrl by remember(seriesId) { mutableStateOf<String?>(null) }
    var lastPlayerPositionMs by remember(seriesId) { mutableLongStateOf(0L) }
    var lastPlayerDurationMs by remember(seriesId) { mutableLongStateOf(0L) }
    var pendingResumeEpisode by remember(seriesId) { mutableStateOf<SeriesEpisodeDto?>(null) }

    if (viewModel.showVariationSelector) {
        SourceSelectionSheet(
            title = series?.name ?: "Seleccionar versión",
            sources = viewModel.variations,
            onSourceClick = { variation ->
                viewModel.showVariationSelector = false
                onSeriesClick(variation.seriesId)
            },
            onDismiss = { viewModel.showVariationSelector = false },
            sourceName = { it.name }
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
    val seriesBackdropUrl = remember(series, details) {
        when {
            !xtreamBackdrop.isNullOrBlank() && xtreamBackdrop != coverUrl -> xtreamBackdrop
            !details?.backdropPath.isNullOrBlank() -> "https://image.tmdb.org/t/p/w780${details?.backdropPath}"
            else -> coverUrl
        }
    }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else if (viewModel.isPlayerActive) {
            currentEpisode?.let { episode ->
                val persistedProgress = viewModel.episodeProgressById[episode.id] ?: 0L
                val resolvedPositionMs = maxOf(lastPlayerPositionMs, persistedProgress)
                val resolvedDurationMs = lastPlayerDurationMs
                    .takeIf { it > 0L }
                    ?: parseEpisodeDurationMs(episode.info?.duration)
                viewModel.onPlayerClosed(episode, resolvedPositionMs, resolvedDurationMs)
            }
            viewModel.isPlayerActive = false
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isFullScreen && !isInPipMode) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = series?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (viewModel.isPlayerActive) {
                                    currentEpisode?.let { episode ->
                                        val persistedProgress = viewModel.episodeProgressById[episode.id] ?: 0L
                                        val resolvedPositionMs = maxOf(lastPlayerPositionMs, persistedProgress)
                                        val resolvedDurationMs = lastPlayerDurationMs
                                            .takeIf { it > 0L }
                                            ?: parseEpisodeDurationMs(episode.info?.duration)
                                        viewModel.onPlayerClosed(episode, resolvedPositionMs, resolvedDurationMs)
                                    }
                                    viewModel.isPlayerActive = false
                                } else {
                                    onBack()
                                }
                            }
                        ) {
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
        } else if (series != null) {
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

                Box(modifier = playerModifier.background(Color.Black)) {
                    if (viewModel.isPlayerActive && currentEpisode != null && profile != null) {
                        val streamUrl = viewModel.localEpisodePaths[currentEpisode.id] ?: "${profile.url}series/${profile.username}/${profile.password}/${currentEpisode.id}.${currentEpisode.containerExtension ?: "mp4"}"
                        val resumeMs = viewModel.episodeProgressById[currentEpisode.id] ?: 0L
                        LaunchedEffect(streamUrl, resumeMs) {
                            lastPlayerPositionMs = resumeMs.coerceAtLeast(0L)
                            lastPlayerDurationMs = 0L
                        }
                        key(streamUrl) {
                            VideoPlayer(
                                url = streamUrl,
                                title = "${series.name} - S${currentEpisode.season?.toString()?.padStart(2, '0')}E${currentEpisode.episodeNum?.toString()?.padStart(2, '0')} - ${currentEpisode.title}",
                                useOriginalMedia3Controller = true,
                                isFullScreen = isFullScreen,
                                resumePositionMs = resumeMs,
                                onLoading = { loading ->
                                    viewModel.isPlayerLoading = loading
                                    if (!loading && lastRecentRegisteredUrl != streamUrl) {
                                        viewModel.onPlaybackStarted(series.seriesId)
                                        lastRecentRegisteredUrl = streamUrl
                                    }
                                },
                                onProgress = { pos ->
                                    currentEpisode?.let { ep ->
                                        viewModel.updateSeriesProgress(ep, pos)
                                    }
                                },
                                onProgressSnapshot = { pos, duration ->
                                    lastPlayerPositionMs = pos
                                    lastPlayerDurationMs = duration
                                },
                                onFullScreenClick = { isFullScreen = !isFullScreen },
                                onNext = { viewModel.onNextEpisode() },
                                onPrevious = { viewModel.onPreviousEpisode() },
                                onClose = {
                                    currentEpisode?.let { episode ->
                                        val persistedProgress = viewModel.episodeProgressById[episode.id] ?: 0L
                                        val resolvedPositionMs = maxOf(lastPlayerPositionMs, persistedProgress)
                                        val resolvedDurationMs = lastPlayerDurationMs
                                            .takeIf { it > 0L }
                                            ?: parseEpisodeDurationMs(episode.info?.duration)
                                        viewModel.onPlayerClosed(episode, resolvedPositionMs, resolvedDurationMs)
                                    }
                                    isFullScreen = false
                                    viewModel.isPlayerActive = false
                                },
                                isLive = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        var isImageLoading by remember { mutableStateOf(true) }
                        val coverUrl = series.cover
                        val xtreamBackdrop = series.backdropPath?.firstOrNull()

                        val backdropUrl = when {
                            !xtreamBackdrop.isNullOrBlank() && xtreamBackdrop != coverUrl -> xtreamBackdrop
                            !details?.backdropPath.isNullOrBlank() -> "https://image.tmdb.org/t/p/w1280${details?.backdropPath}"
                            else -> coverUrl
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

                // 2. TABS DE NAVEGACIÓN (Solo si no es FullScreen)
                if (!isFullScreen && !isInPipMode) {
                    TabRow(
                        selectedTabIndex = selectedDetailTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedDetailTab == index,
                                onClick = { selectedDetailTab = index },
                                text = { 
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selectedDetailTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    // 3. CONTENIDO SEGÚN TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxl)
                    ) {
                        if (selectedDetailTab == 0) {
                            // PESTAÑA INFO
                            item {
                                Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                                    Text(
                                        text = series.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    val year = details?.releaseDate?.take(4) ?: series.releaseDate?.take(4)
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (!year.isNullOrBlank()) InfoChip(text = year)
                                        if (episodes != null) InfoChip(text = "${episodes.size} Temporadas")
                                        details?.voteAverage?.let { if (it > 0) RatingChip(rating = it) }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { 
                                                selectedDetailTab = 1
                                                viewModel.playLastSeenEpisodeOrFallback()
                                                viewModel.isPlayerActive = true 
                                            },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                val actionMode = viewModel.primaryActionMode
                                                val hasEpisodeTarget = viewModel.lastEpisodeSeason != null && viewModel.lastEpisodeNumber != null
                                                val actionText = when (actionMode) {
                                                    SeriesDetailViewModel.PrimaryActionMode.PLAY -> "REPRODUCIR"
                                                    SeriesDetailViewModel.PrimaryActionMode.CONTINUE -> "CONTINUAR"
                                                    SeriesDetailViewModel.PrimaryActionMode.NEXT -> "SIGUIENTE EPISODIO"
                                                }
                                                Text(
                                                    text = actionText,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                )
                                                if (hasEpisodeTarget) {
                                                    val season = viewModel.lastEpisodeSeason ?: 0
                                                    val episode = viewModel.lastEpisodeNumber ?: 0
                                                    val subtitle = when (actionMode) {
                                                        SeriesDetailViewModel.PrimaryActionMode.CONTINUE ->
                                                            "S${season}E${episode} - ${formatElapsed(viewModel.lastEpisodePositionMs)}"
                                                        SeriesDetailViewModel.PrimaryActionMode.NEXT ->
                                                            "S${season}E${episode}"
                                                        SeriesDetailViewModel.PrimaryActionMode.PLAY ->
                                                            "S${season}E${episode}"
                                                    }
                                                    Text(
                                                        text = subtitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.9f)
                                                    )
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
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    val plot = details?.overview?.takeIf { it.isNotBlank() } ?: series.plot
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
                                }
                            }

                            if (variationItems.isNotEmpty()) {
                                item {
                                    HorizontalSeriesListSection("Versiones Disponibles") {
                                        items(variationItems) { item ->
                                            SeriesShortItem(
                                                series = item.tmdbItem,
                                                isActual = item.isActual,
                                                onClick = {
                                                    if (!item.isActual) {
                                                        viewModel.checkAndNavigateToSeries(item.tmdbItem.id, item.tmdbItem.tvName, onSeriesClick)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            details?.credits?.cast?.let { castList ->
                                if (castList.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                                            Text("Reparto Principal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                items(castList.take(10)) { actor ->
                                                    ActorItem(name = actor.name, role = actor.character, photoUrl = actor.profilePath)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (recommendedSeries.isNotEmpty()) {
                                item {
                                    HorizontalSeriesListSection("Series Recomendadas") {
                                        items(recommendedSeries) { (item, _) ->
                                            SeriesShortItem(item, onClick = { 
                                                viewModel.checkAndNavigateToSeries(item.id, item.tvName ?: item.title, onSeriesClick)
                                            })
                                        }
                                    }
                                }
                            }

                            if (similarSeries.isNotEmpty()) {
                                item {
                                    HorizontalSeriesListSection("Series Similares") {
                                        items(similarSeries) { (item, _) ->
                                            SeriesShortItem(item, onClick = { 
                                                viewModel.checkAndNavigateToSeries(item.id, item.tvName ?: item.title, onSeriesClick)
                                            })
                                        }
                                    }
                                }
                            }
                        } else {
                            // PESTAÑA EPISODIOS
                            if (!episodes.isNullOrEmpty()) {
                                item {
                                    ScrollableTabRow(
                                        selectedTabIndex = episodes.keys.indexOf(selectedSeason.toString()).coerceAtLeast(0),
                                        containerColor = MaterialTheme.colorScheme.background,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        edgePadding = MaterialTheme.spacing.medium,
                                        divider = {}
                                    ) {
                                        episodes.keys.sortedBy { it.toIntOrNull() ?: 0 }.forEach { season ->
                                            Tab(
                                                selected = selectedSeason.toString() == season,
                                                onClick = { viewModel.onSeasonSelect(season.toInt()) },
                                                text = { 
                                                    Text(
                                                        text = "Temp. $season",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (selectedSeason.toString() == season) FontWeight.Bold else FontWeight.Normal
                                                    ) 
                                                }
                                            )
                                        }
                                    }
                                }

                                val seasonEpisodes = episodes[selectedSeason.toString()] ?: emptyList()
                                items(seasonEpisodes) { episode ->
                                    EpisodeItem(
                                        episode = episode,
                                        isPlaying = currentEpisode?.id == episode.id,
                                        progressMs = viewModel.episodeProgressById[episode.id] ?: 0L,
                                        download = episodeDownloads[episode.id],
                                        fallbackUrl = seriesBackdropUrl,
                                        onClick = {
                                            val savedProgress = viewModel.episodeProgressById[episode.id] ?: 0L
                                            val durationMs = parseEpisodeDurationMs(episode.info?.duration)
                                            val isCompletedAt100 = durationMs > 0L && savedProgress >= durationMs
                                            val hasResumeToAsk = savedProgress > 0L && !isCompletedAt100

                                            when {
                                                hasResumeToAsk -> pendingResumeEpisode = episode
                                                isCompletedAt100 -> viewModel.onEpisodeClickFromStart(episode)
                                                else -> viewModel.onEpisodeClick(episode)
                                            }
                                        },
                                        onDownload = { viewModel.handleEpisodeDownloadClick(episode) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: SeriesEpisodeDto,
    isPlaying: Boolean,
    progressMs: Long = 0L,
    download: com.stream.iptvrevolut.data.local.entity.download.DownloadEntity?,
    fallbackUrl: String? = null,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val durationMs = remember(episode.info?.duration) { parseEpisodeDurationMs(episode.info?.duration) }
    val progressFraction = remember(progressMs, durationMs) {
        when {
            progressMs <= 0L -> 0f
            durationMs > 0L -> (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            else -> (progressMs.toFloat() / (45L * 60L * 1000L).toFloat()).coerceIn(0.04f, 0.95f)
        }
    }
    val hasProgress = progressMs > 0L
    val isCompleted = durationMs > 0L && progressFraction >= 0.98f

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.small,
        border = if (isPlaying) 
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) 
        else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isEpThumbLoading by remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(16 / 9f)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerLoading(isEpThumbLoading)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = if (episode.info?.movieImage.isNullOrBlank()) fallbackUrl else episode.info?.movieImage
                
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { isEpThumbLoading = false },
                    onError = { isEpThumbLoading = false }
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow, 
                        contentDescription = null, 
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                if (hasProgress) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(
                                    if (isCompleted) Color(0xFF2ECC71) else Color(0xFF00C2FF)
                                )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episodeNum}. ${episode.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (progressMs > 0L) {
                    Text(
                        text = "Progreso: ${formatElapsed(progressMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!episode.info?.duration.isNullOrBlank()) {
                    Text(
                        text = "${episode.info?.duration} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            CircularDownloadButton(
                download = download,
                onClick = onDownload,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

private fun parseEpisodeDurationMs(raw: String?): Long {
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

@Composable
fun HorizontalSeriesListSection(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 24.dp, horizontal = MaterialTheme.spacing.medium)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun SeriesShortItem(
    series: com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieShortDto, 
    isActual: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp).clickable { onClick() }) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w342${series.posterPath}",
                contentDescription = series.title ?: series.tvName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
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
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = series.tvName ?: series.title ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

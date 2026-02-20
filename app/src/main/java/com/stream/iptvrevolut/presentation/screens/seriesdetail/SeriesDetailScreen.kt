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
                            maxLines = 1,
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
                        key(streamUrl) {
                            VideoPlayer(
                                url = streamUrl,
                                title = "${series.name} - S${currentEpisode.season?.toString()?.padStart(2, '0')}E${currentEpisode.episodeNum?.toString()?.padStart(2, '0')} - ${currentEpisode.title}",
                                useOriginalMedia3Controller = true,
                                isFullScreen = isFullScreen,
                                onLoading = { loading ->
                                    viewModel.isPlayerLoading = loading
                                    if (!loading && lastRecentRegisteredUrl != streamUrl) {
                                        viewModel.onPlaybackStarted(series.seriesId)
                                        lastRecentRegisteredUrl = streamUrl
                                    }
                                },
                                onFullScreenClick = { isFullScreen = !isFullScreen },
                                onNext = { viewModel.onNextEpisode() },
                                onPrevious = { viewModel.onPreviousEpisode() },
                                onClose = { 
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
                                                if (currentEpisode == null && episodes != null) {
                                                    episodes[selectedSeason.toString()]?.firstOrNull()?.let {
                                                        viewModel.onEpisodeClick(it)
                                                    }
                                                }
                                                viewModel.isPlayerActive = true 
                                            },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("REPRODUCIR", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
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
                                        download = episodeDownloads[episode.id],
                                        fallbackUrl = seriesBackdropUrl,
                                        onClick = { viewModel.onEpisodeClick(episode) },
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
    download: com.stream.iptvrevolut.data.local.entity.download.DownloadEntity?,
    fallbackUrl: String? = null,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
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

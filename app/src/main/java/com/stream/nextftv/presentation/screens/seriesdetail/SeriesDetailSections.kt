package com.stream.nextftv.presentation.screens.seriesdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.data.utils.PlaybackCacheLocator
import com.stream.nextftv.data.utils.PlaybackCacheCleanupUtils
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.remote.SeriesEpisodeDto
import com.stream.nextftv.data.remote.tmdb.TmdbCastDto
import com.stream.nextftv.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto
import com.stream.nextftv.presentation.player.PlaybackContentType
import com.stream.nextftv.presentation.player.VideoPlayer
import com.stream.nextftv.presentation.screens.detail.components.ActorItem
import com.stream.nextftv.presentation.screens.detail.components.DetailBackdropPoster
import com.stream.nextftv.presentation.screens.detail.components.DetailIconActionButton
import com.stream.nextftv.presentation.screens.detail.components.DetailPrimaryActionButton
import com.stream.nextftv.presentation.screens.detail.components.EpisodeItem
import com.stream.nextftv.presentation.screens.detail.components.ExpandableSynopsis
import com.stream.nextftv.presentation.screens.detail.components.formatElapsed
import com.stream.nextftv.presentation.screens.detail.components.HorizontalSeriesListSection
import com.stream.nextftv.presentation.screens.detail.components.InfoChip
import com.stream.nextftv.presentation.screens.detail.components.RatingChip
import com.stream.nextftv.presentation.screens.detail.components.SeriesShortItem
import com.stream.nextftv.presentation.screens.detail.components.parseDurationToMs
import com.stream.nextftv.presentation.theme.spacing
import com.stream.nextftv.presentation.theme.shimmerLoading

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SeriesDetailTopBar(
    title: String,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
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

@Composable
internal fun SeriesDetailPlayerSection(
    modifier: Modifier,
    isPlayerActive: Boolean,
    currentEpisode: SeriesEpisodeDto?,
    profileUrl: String?,
    profileUsername: String?,
    profilePassword: String?,
    episodeDownload: DownloadEntity?,
    seriesName: String?,
    seriesBackdropUrl: String?,
    seriesPosterUrl: String?,
    isFullScreen: Boolean,
    isInPipMode: Boolean,
    resumeMs: Long,
    onLoading: (Boolean) -> Unit,
    onProgress: (Long) -> Unit,
    onProgressSnapshot: (Long, Long) -> Unit,
    onPlaybackCompleted: (Long, Long) -> Unit,
    onEnterFullscreen: () -> Unit,
    onQuitFullscreen: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    shouldKeepFullscreenOnAutoComplete: () -> Boolean,
    onClose: () -> Unit,
    onPipRequested: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.background(Color.Black)) {
        if (isPlayerActive && currentEpisode != null) {
            val playbackSource = resolveEpisodePlaybackSource(
                episode = currentEpisode,
                profileUrl = profileUrl,
                profileUsername = profileUsername,
                profilePassword = profilePassword,
                download = episodeDownload
            )
            val remoteUrl = profileUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { "${it}series/${profileUsername}/${profilePassword}/${currentEpisode.id}.${currentEpisode.containerExtension ?: "mp4"}" }
            LaunchedEffect(playbackSource?.url, remoteUrl) {
                if (playbackSource?.isLocal == true && !remoteUrl.isNullOrBlank()) {
                    PlaybackCacheCleanupUtils.clearPlaybackCaches(
                        context,
                        PlaybackCacheLocator.playTagFor(PlaybackContentType.SERIES),
                        remoteUrl
                    )
                }
            }
            val streamUrl = playbackSource?.url
            val episodeDisplayTitle = remember(
                currentEpisode.id,
                currentEpisode.title,
                currentEpisode.season,
                currentEpisode.episodeNum,
                seriesName
            ) {
                buildEpisodeDisplayTitle(currentEpisode, seriesName)
            }
            if (streamUrl != null) key(currentEpisode.id, streamUrl, playbackSource.isLocal) {
                VideoPlayer(
                    url = streamUrl,
                    title = episodeDisplayTitle,
                    isFullScreen = isFullScreen,
                    isSmall = isInPipMode,
                    resumePositionMs = resumeMs,
                    thumbUrl = seriesBackdropUrl,
                    contentType = PlaybackContentType.SERIES,
                    onLoading = onLoading,
                    onProgress = onProgress,
                    onProgressSnapshot = onProgressSnapshot,
                    onPlaybackCompleted = onPlaybackCompleted,
                    onEnterFullscreen = onEnterFullscreen,
                    onQuitFullscreen = onQuitFullscreen,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    shouldKeepFullscreenOnAutoComplete = shouldKeepFullscreenOnAutoComplete,
                    onClose = onClose,
                    onPipRequested = onPipRequested,
                    isLive = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            var isImageLoading by remember { mutableStateOf(true) }
            coil3.compose.AsyncImage(
                model = seriesBackdropUrl,
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
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            DetailBackdropPoster(
                posterUrl = seriesPosterUrl,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
internal fun SeriesDetailMainContent(
    selectedDetailTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    series: SeriesStreamEntity,
    details: TmdbMovieDetailsDto?,
    episodes: Map<String, List<SeriesEpisodeDto>>?,
    selectedSeason: Int,
    currentEpisode: SeriesEpisodeDto?,
    variationItems: List<SeriesDetailViewModel.SeriesVariationItem>,
    recommendedSeries: List<Pair<TmdbMovieShortDto, Int>>,
    similarSeries: List<Pair<TmdbMovieShortDto, Int>>,
    isFavorite: Boolean,
    lastEpisodePositionMs: Long,
    episodeDownloads: Map<String, DownloadEntity>,
    seriesBackdropUrl: String?,
    onPlayPrimary: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVariationClick: (SeriesDetailViewModel.SeriesVariationItem) -> Unit,
    onRecommendedClick: (TmdbMovieShortDto) -> Unit,
    onSimilarClick: (TmdbMovieShortDto) -> Unit,
    onActorClick: (TmdbCastDto) -> Unit,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeClick: (SeriesEpisodeDto) -> Unit,
    onEpisodeDownloadClick: (SeriesEpisodeDto) -> Unit,
    primaryActionMode: SeriesDetailViewModel.PrimaryActionMode,
    lastEpisodeSeason: Int?,
    lastEpisodeNumber: Int?,
    episodeProgressById: Map<String, Long>,
    episodeDurationById: Map<String, Long>,
    tmdbEpisodeTitleByKey: Map<String, String>
) {
    PrimaryTabRow(
        selectedTabIndex = selectedDetailTab,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedDetailTab == index,
                onClick = { onTabSelected(index) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxl)
    ) {
        if (selectedDetailTab == 0) {
            item {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                    Text(
                        text = series.name.orEmpty(),
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
                        val hasEpisodeTarget = lastEpisodeSeason != null && lastEpisodeNumber != null
                        val title = when (primaryActionMode) {
                            SeriesDetailViewModel.PrimaryActionMode.PLAY -> "Reproducir"
                            SeriesDetailViewModel.PrimaryActionMode.CONTINUE -> "Continuar"
                            SeriesDetailViewModel.PrimaryActionMode.NEXT -> "Siguiente episodio"
                        }
                        val supportingText = if (hasEpisodeTarget) {
                            val season = lastEpisodeSeason.toString().padStart(2, '0')
                            val episode = lastEpisodeNumber.toString().padStart(2, '0')
                            when (primaryActionMode) {
                                SeriesDetailViewModel.PrimaryActionMode.CONTINUE ->
                                    "S${season}E${episode} · ${formatElapsed(lastEpisodePositionMs)}"
                                SeriesDetailViewModel.PrimaryActionMode.NEXT,
                                SeriesDetailViewModel.PrimaryActionMode.PLAY ->
                                    "S${season}E${episode}"
                            }
                        } else {
                            null
                        }

                        DetailPrimaryActionButton(
                            onClick = onPlayPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            icon = Icons.Default.PlayArrow,
                            title = title,
                            supportingText = supportingText
                        )

                        DetailIconActionButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(54.dp),
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            active = isFavorite
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val plot = details?.overview?.takeIf { it.isNotBlank() } ?: series.plot
                    if (!plot.isNullOrBlank()) {
                        Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ExpandableSynopsis(text = plot)
                    }
                }
            }

            details?.credits?.cast?.let { castList ->
                if (castList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Actores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(castList.take(10)) { actor ->
                                    ActorItem(
                                        name = actor.name,
                                        role = actor.character,
                                        photoUrl = actor.profilePath,
                                        onClick = { onActorClick(actor) }
                                    )
                                }
                            }
                        }
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
                                onClick = { onVariationClick(item) }
                            )
                        }
                    }
                }
            }

            if (recommendedSeries.isNotEmpty()) {
                item {
                    HorizontalSeriesListSection("Series Recomendadas") {
                        items(recommendedSeries) { (item, _) ->
                            SeriesShortItem(item, onClick = { onRecommendedClick(item) })
                        }
                    }
                }
            }

            if (similarSeries.isNotEmpty()) {
                item {
                    HorizontalSeriesListSection("Series Similares") {
                        items(similarSeries) { (item, _) ->
                            SeriesShortItem(item, onClick = { onSimilarClick(item) })
                        }
                    }
                }
            }

        } else {
            if (!episodes.isNullOrEmpty()) {
                item {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = episodes.keys.indexOf(selectedSeason.toString()).coerceAtLeast(0),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = MaterialTheme.spacing.medium,
                        divider = {}
                    ) {
                        episodes.keys.sortedBy { it.toIntOrNull() ?: 0 }.forEach { season ->
                            Tab(
                                selected = selectedSeason.toString() == season,
                                onClick = { onSeasonSelect(season.toInt()) },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val tmdbEpisodeTitle = episode.season
                        ?.let { season -> episode.episodeNum?.let { number -> tmdbEpisodeTitleByKey["$season:$number"] } }
                    EpisodeItem(
                        episode = episode,
                        isPlaying = currentEpisode?.id == episode.id,
                        progressMs = episodeProgressById[episode.id] ?: 0L,
                        videoDurationMs = episodeDurationById[episode.id] ?: 0L,
                        download = episodeDownloads[episode.id],
                        seriesName = series.name,
                        preferredTitle = tmdbEpisodeTitle,
                        fallbackUrl = seriesBackdropUrl,
                        onClick = { onEpisodeClick(episode) },
                        onDownload = { onEpisodeDownloadClick(episode) }
                    )
                }
            }
        }
    }
}

package com.stream.nextftv.presentation.screens.moviedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import coil3.compose.AsyncImage
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.data.utils.PlaybackCacheLocator
import com.stream.nextftv.data.utils.PlaybackCacheCleanupUtils
import com.stream.nextftv.data.remote.tmdb.TmdbCastDto
import com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto
import com.stream.nextftv.presentation.player.PlaybackContentType
import com.stream.nextftv.presentation.player.VideoPlayer
import com.stream.nextftv.presentation.screens.detail.components.ActorItem
import com.stream.nextftv.presentation.screens.detail.components.CircularDownloadButton
import com.stream.nextftv.presentation.screens.detail.components.DetailBackdropPoster
import com.stream.nextftv.presentation.screens.detail.components.DetailIconActionButton
import com.stream.nextftv.presentation.screens.detail.components.DetailPrimaryActionButton
import com.stream.nextftv.presentation.screens.detail.components.ExpandableSynopsis
import com.stream.nextftv.presentation.screens.detail.components.formatElapsed
import com.stream.nextftv.presentation.screens.detail.components.HorizontalListSection
import com.stream.nextftv.presentation.screens.detail.components.InfoChip
import com.stream.nextftv.presentation.screens.detail.components.MovieShortItem
import com.stream.nextftv.presentation.screens.detail.components.parseDurationToMs
import com.stream.nextftv.presentation.screens.detail.components.RatingChip
import com.stream.nextftv.presentation.theme.spacing
import com.stream.nextftv.presentation.theme.shimmerLoading

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun MovieDetailTopBar(
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
internal fun MovieDetailPlayerSection(
    modifier: Modifier,
    isPlayerActive: Boolean,
    profileUrl: String?,
    profileUsername: String?,
    profilePassword: String?,
    localFilePath: String?,
    movieStreamId: Int,
    movieName: String,
    containerExtension: String?,
    movieThumbUrl: String?,
    moviePosterUrl: String?,
    isFullScreen: Boolean,
    isInPipMode: Boolean,
    resumePositionMs: Long,
    onLoading: (Boolean) -> Unit,
    onProgress: (Long) -> Unit,
    onProgressSnapshot: (Long, Long) -> Unit,
    onEnterFullscreen: () -> Unit,
    onQuitFullscreen: () -> Unit,
    onClose: () -> Unit,
    onPipRequested: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.background(Color.Black)) {
        if (
            isPlayerActive &&
            !profileUrl.isNullOrBlank() &&
            !profileUsername.isNullOrBlank() &&
            !profilePassword.isNullOrBlank()
        ) {
            val remoteUrl = "${profileUrl}movie/${profileUsername}/${profilePassword}/${movieStreamId}.${containerExtension ?: "mp4"}"
            LaunchedEffect(localFilePath, remoteUrl) {
                if (!localFilePath.isNullOrBlank()) {
                    PlaybackCacheCleanupUtils.clearPlaybackCaches(
                        context,
                        PlaybackCacheLocator.playTagFor(PlaybackContentType.MOVIE),
                        remoteUrl
                    )
                }
            }
            val streamUrl = localFilePath
                ?: remoteUrl
            key(streamUrl) {
                VideoPlayer(
                    url = streamUrl,
                    title = movieName,
                    isFullScreen = isFullScreen,
                    isSmall = isInPipMode,
                    resumePositionMs = resumePositionMs,
                    thumbUrl = movieThumbUrl,
                    contentType = PlaybackContentType.MOVIE,
                    onLoading = onLoading,
                    onProgress = onProgress,
                    onProgressSnapshot = onProgressSnapshot,
                    onEnterFullscreen = onEnterFullscreen,
                    onQuitFullscreen = onQuitFullscreen,
                    onClose = onClose,
                    onPipRequested = onPipRequested,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            var isImageLoading by remember { mutableStateOf(true) }

            AsyncImage(
                model = movieThumbUrl,
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
                posterUrl = moviePosterUrl,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
internal fun MovieDetailScrollableContent(
    movieName: String,
    tmdbYear: String?,
    xtreamYear: String?,
    runtime: String?,
    voteAverage: Double?,
    lastMoviePositionMs: Long,
    lastMovieDurationMs: Long,
    plot: String?,
    cast: List<TmdbCastDto>,
    collectionTitle: String,
    collectionItems: List<MovieDetailViewModel.CollectionItem>,
    recommendedMovies: List<Pair<TmdbMovieShortDto, *>>,
    similarMovies: List<Pair<TmdbMovieShortDto, *>>,
    isFavorite: Boolean,
    downloadState: DownloadEntity?,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onCollectionMovieClick: (MovieDetailViewModel.CollectionItem) -> Unit,
    onRecommendedMovieClick: (TmdbMovieShortDto) -> Unit,
    onSimilarMovieClick: (TmdbMovieShortDto) -> Unit,
    onActorClick: (TmdbCastDto) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.medium)
    ) {
        Text(
            text = movieName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        val year = tmdbYear ?: xtreamYear
        val duration = runtime.orEmpty()

        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!year.isNullOrBlank()) InfoChip(text = year)
            if (duration.isNotBlank()) InfoChip(text = "$duration min")
            voteAverage?.let { if (it > 0) RatingChip(rating = it) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasProgress = lastMoviePositionMs > 0L
            val parsedRuntimeMs = parseDurationToMs(runtime)
            val durationForProgress = when {
                lastMovieDurationMs > 0L -> lastMovieDurationMs
                parsedRuntimeMs > 0L -> parsedRuntimeMs
                else -> 0L
            }
            val progressFraction = if (hasProgress && durationForProgress > 0L) {
                (lastMoviePositionMs.toFloat() / durationForProgress.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            DetailPrimaryActionButton(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                icon = Icons.Default.PlayArrow,
                title = if (hasProgress) "Continuar" else "Reproducir",
                supportingText = if (hasProgress) {
                    buildString {
                        append(formatElapsed(lastMoviePositionMs))
                        if (durationForProgress > 0L) {
                            append(" de ")
                            append(formatElapsed(durationForProgress))
                        }
                    }
                } else {
                    runtime?.takeIf { it.isNotBlank() }?.let { "$it min" }
                },
                progressFraction = progressFraction,
                onClick = onPlay
            )

            DetailIconActionButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(54.dp),
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                active = isFavorite
            )

            CircularDownloadButton(download = downloadState, onClick = onDownload)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!plot.isNullOrBlank()) {
            Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ExpandableSynopsis(text = plot)
        }

        if (cast.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            HorizontalListSection("Actores") {
                items(cast.take(10)) { actor ->
                    ActorItem(
                        name = actor.name,
                        role = actor.character,
                        photoUrl = actor.profilePath,
                        onClick = { onActorClick(actor) }
                    )
                }
            }
        }

        if (collectionItems.isNotEmpty()) {
            HorizontalListSection(collectionTitle) {
                items(collectionItems) { item ->
                    MovieShortItem(
                        movie = item.tmdbMovie,
                        isActual = item.isActual,
                        isFound = item.localVariants.isNotEmpty(),
                        onClick = { onCollectionMovieClick(item) }
                    )
                }
            }
        }

        if (recommendedMovies.isNotEmpty()) {
            HorizontalListSection("Películas Recomendadas") {
                items(recommendedMovies) { (item, _) ->
                    MovieShortItem(item, onClick = { onRecommendedMovieClick(item) })
                }
            }
        }

        if (similarMovies.isNotEmpty()) {
            HorizontalListSection("Películas Similares") {
                items(similarMovies) { (item, _) ->
                    MovieShortItem(item, onClick = { onSimilarMovieClick(item) })
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
    }
}

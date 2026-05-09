package com.stream.nextftv.presentation.screens.actor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.stream.nextftv.presentation.screens.detail.components.SourceSelectionSheet
import com.stream.nextftv.presentation.theme.StreamingBackground
import com.stream.nextftv.presentation.theme.spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ActorDetailScreen(
    actorId: Int,
    fallbackName: String?,
    fallbackPhotoPath: String?,
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onSeriesClick: (Int) -> Unit,
    viewModel: ActorDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(actorId) {
        viewModel.loadActor(actorId)
    }

    val actor = viewModel.actor
    val actorName = actor?.name ?: fallbackName.orEmpty()
    val actorPhoto = actor?.profilePath ?: fallbackPhotoPath
    var selectedTab by remember(actorId) { mutableIntStateOf(0) }
    val tabs = listOf("Películas", "Series")

    if (viewModel.showMovieVariationSelector) {
        SourceSelectionSheet(
            title = viewModel.variationSelectorTitle,
            sources = viewModel.movieVariations,
            onSourceClick = { option ->
                viewModel.dismissMovieVariationSelector()
                onMovieClick(option.source.streamId)
            },
            onDismiss = viewModel::dismissMovieVariationSelector,
            sourceName = { it.source.name },
            isCurrentSource = { it.isCurrent }
        )
    }

    if (viewModel.showSeriesVariationSelector) {
        SourceSelectionSheet(
            title = viewModel.variationSelectorTitle,
            sources = viewModel.seriesVariations,
            onSourceClick = { option ->
                viewModel.dismissSeriesVariationSelector()
                onSeriesClick(option.source.seriesId)
            },
            onDismiss = viewModel::dismissSeriesVariationSelector,
            sourceName = { it.source.name },
            isCurrentSource = { it.isCurrent }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = androidx.compose.foundation.isSystemInDarkTheme())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = actorName.ifBlank { "Actor" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        androidx.compose.material3.IconButton(onClick = onBack) {
                            androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { innerPadding ->
            when {
                viewModel.isLoading && actor == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxl),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MaterialTheme.spacing.medium),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = actorPhoto?.let { "https://image.tmdb.org/t/p/w342$it" },
                                    contentDescription = actorName,
                                    modifier = Modifier
                                        .size(124.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = actorName.ifBlank { "Actor" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                actor?.biography?.takeIf { it.isNotBlank() }?.let { bio ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ExpandableText(
                                        text = bio,
                                        collapsedMaxLines = 4,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        item {
                            PrimaryTabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = { Text(title) }
                                    )
                                }
                            }
                        }

                        if (selectedTab == 0) {
                            if (viewModel.movieCredits.isEmpty()) {
                                item {
                                    EmptyFilmographyMessage("No hay películas disponibles en tu catálogo para este actor.")
                                }
                            } else {
                                items(viewModel.movieCredits, key = { it.streamId }) { credit ->
                                    FilmographyListItem(
                                        title = credit.tmdbItem.title ?: credit.tmdbItem.tvName.orEmpty(),
                                        year = credit.year,
                                        posterPath = credit.tmdbItem.posterPath,
                                        character = credit.character,
                                        overview = credit.overview,
                                        onClick = {
                                            viewModel.onMovieCreditClick(credit) { streamId ->
                                                onMovieClick(streamId)
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            if (viewModel.seriesCredits.isEmpty()) {
                                item {
                                    EmptyFilmographyMessage("No hay series disponibles en tu catálogo para este actor.")
                                }
                            } else {
                                items(viewModel.seriesCredits, key = { it.seriesId }) { credit ->
                                    FilmographyListItem(
                                        title = credit.tmdbItem.tvName ?: credit.tmdbItem.title.orEmpty(),
                                        year = credit.year,
                                        posterPath = credit.tmdbItem.posterPath,
                                        character = credit.character,
                                        overview = credit.overview,
                                        onClick = {
                                            viewModel.onSeriesCreditClick(credit) { seriesId ->
                                                onSeriesClick(seriesId)
                                            }
                                        }
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
private fun EmptyFilmographyMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FilmographyListItem(
    title: String,
    year: String?,
    posterPath: String?,
    character: String?,
    overview: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncImage(
            model = posterPath?.let {
                if (it.startsWith("http://") || it.startsWith("https://")) it else "https://image.tmdb.org/t/p/w342$it"
            },
            contentDescription = title,
            modifier = Modifier
                .width(92.dp)
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            year?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            character?.takeIf { it.isNotBlank() }?.let { role ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            overview?.takeIf { it.isNotBlank() }?.let { summary ->
                Spacer(modifier = Modifier.height(8.dp))
                ExpandableText(
                    text = summary,
                    collapsedMaxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandableText(
    text: String,
    collapsedMaxLines: Int,
    textStyle: androidx.compose.ui.text.TextStyle,
    textColor: androidx.compose.ui.graphics.Color
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var canExpand by remember(text) { mutableStateOf(false) }

    Column {
        Text(
            text = text,
            style = textStyle,
            color = textColor,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                canExpand = layoutResult.hasVisualOverflow
            }
        )
        if (canExpand || expanded) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (expanded) "Mostrar menos" else "Mostrar más")
            }
        }
    }
}

package com.stream.nextftv.presentation.screens.movies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.remote.tmdb.TmdbPersonSearchResultDto
import com.stream.nextftv.presentation.theme.StreamingBackground
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val categories by viewModel.categories.collectAsState()
    val movies = viewModel.pagedMovies.collectAsLazyPagingItems()
    val enhancedSearchResults = viewModel.enhancedSearchResults
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    // Recordar el estado de filtrado para evitar resetear scroll al volver de detalles
    var lastFilterKey by androidx.compose.runtime.saveable.rememberSaveable { 
        mutableStateOf("${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}") 
    }

    LaunchedEffect(viewModel.selectedCategoryId, viewModel.sortOrder, viewModel.searchQuery) {
        val currentKey = "${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}"
        if (currentKey != lastFilterKey) {
            gridState.scrollToItem(0) // Salto instantáneo
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
            gridState.scrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                MoviesTopBar(
                    isSearchActive = viewModel.isSearchActive,
                    searchQuery = viewModel.searchQuery,
                    sortMenuExpanded = sortMenuExpanded,
                    focusRequester = focusRequester,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearchToggle = {
                        viewModel.isSearchActive = !viewModel.isSearchActive
                        if (!viewModel.isSearchActive) {
                            viewModel.onSearchQueryChange("")
                        }
                    },
                    onDismissSearch = {
                        viewModel.isSearchActive = false
                        viewModel.onSearchQueryChange("")
                    },
                    onBack = onBack,
                    onSortMenuExpandedChange = { sortMenuExpanded = it },
                    onSortOrderSelected = {
                        viewModel.sortOrder = it
                        sortMenuExpanded = false
                    }
                )
            }
        ) { innerPadding ->
            MoviesContent(
                categories = categories,
                movies = movies,
                enhancedSearchResults = enhancedSearchResults,
                selectedCategoryId = viewModel.selectedCategoryId,
                searchQuery = viewModel.searchQuery,
                isEnhancedSearchLoading = viewModel.isEnhancedSearchLoading,
                gridState = gridState,
                innerPadding = innerPadding,
                viewModel = viewModel,
                onMovieClick = onMovieClick
            )
        }
    }
}

@Composable
fun ActorSearchItem(
    actor: TmdbPersonSearchResultDto,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape),
            shape = CircleShape
        ) {
            AsyncImage(
                model = actor.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" },
                contentDescription = actor.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = actor.name.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MovieItem(
    movie: VodStreamEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = movie.streamIcon,
                    contentDescription = movie.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Surface(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp),
                    shape = RoundedCornerShape(bottomStart = 16.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color.Red else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = movie.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

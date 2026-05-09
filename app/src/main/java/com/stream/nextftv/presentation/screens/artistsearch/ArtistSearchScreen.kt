package com.stream.nextftv.presentation.screens.artistsearch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stream.nextftv.presentation.screens.movies.ActorSearchItem
import com.stream.nextftv.presentation.theme.StreamingBackground
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ArtistSearchScreen(
    onBack: () -> Unit,
    onActorClick: (Int, String?, String?) -> Unit,
    viewModel: ArtistSearchViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val query = viewModel.searchQuery
    val actors = viewModel.actors
    val isLoading = viewModel.isLoading

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    LaunchedEffect(query) {
        gridState.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 0.dp,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
                        ) {
                            TextField(
                                value = query,
                                onValueChange = viewModel::onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        text = "Buscar artistas en TMDB",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (query.isNotBlank()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { innerPadding ->
            when {
                isLoading && actors.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                query.isBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Escribe el nombre de un artista",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                actors.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron artistas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(92.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 48.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(
                            count = actors.size,
                            key = { index -> actors[index].id }
                        ) { index ->
                            val actor = actors[index]
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ActorSearchItem(actor = actor) {
                                    onActorClick(actor.id, actor.name, actor.profilePath)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

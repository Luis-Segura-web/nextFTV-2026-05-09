package com.stream.iptvrevolut.presentation.screens.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.stream.iptvrevolut.R
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import com.stream.iptvrevolut.domain.model.SortOrder
import com.stream.iptvrevolut.presentation.screens.livetv.CategoryChip
import com.stream.iptvrevolut.presentation.theme.dimens
import com.stream.iptvrevolut.presentation.theme.spacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSeriesClick: (Int) -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val series = viewModel.pagedSeries.collectAsLazyPagingItems()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    // Recordar el estado de filtrado
    var lastFilterKey by androidx.compose.runtime.saveable.rememberSaveable { 
        mutableStateOf("${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}") 
    }

    LaunchedEffect(viewModel.selectedCategoryId, viewModel.sortOrder, viewModel.searchQuery) {
        val currentKey = "${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}"
        if (currentKey != lastFilterKey) {
            gridState.scrollToItem(0)
            lastFilterKey = currentKey
        }
    }

    LaunchedEffect(viewModel.isSearchActive) {
        if (viewModel.isSearchActive) {
            delay(150)
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (viewModel.isSearchActive) {
                        TextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { 
                                Text(
                                    text = stringResource(R.string.series_search_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
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
                    } else {
                        Text(
                            text = stringResource(R.string.series_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.isSearchActive) {
                            viewModel.isSearchActive = false
                            viewModel.onSearchQueryChange("")
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.isSearchActive = !viewModel.isSearchActive 
                        if (!viewModel.isSearchActive) {
                            viewModel.onSearchQueryChange("")
                        }
                    }) {
                        Icon(
                            imageVector = if (viewModel.isSearchActive) Icons.Default.Close else Icons.Default.Search, 
                            contentDescription = null
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_default)) },
                                onClick = { 
                                    viewModel.sortOrder = SortOrder.DEFAULT
                                    sortMenuExpanded = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_az)) },
                                onClick = { 
                                    viewModel.sortOrder = SortOrder.A_Z
                                    sortMenuExpanded = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_za)) },
                                onClick = { 
                                    viewModel.sortOrder = SortOrder.Z_A
                                    sortMenuExpanded = false 
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.small),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                item {
                    CategoryChip(
                        name = stringResource(R.string.live_category_all),
                        isSelected = viewModel.selectedCategoryId == "all",
                        onClick = { viewModel.onCategorySelect("all") }
                    )
                }
                item {
                    CategoryChip(
                        name = stringResource(R.string.live_category_favorites),
                        isSelected = viewModel.selectedCategoryId == "favorites",
                        onClick = { viewModel.onCategorySelect("favorites") }
                    )
                }
                item {
                    CategoryChip(
                        name = stringResource(R.string.live_category_recents),
                        isSelected = viewModel.selectedCategoryId == "recents",
                        onClick = { viewModel.onCategorySelect("recents") }
                    )
                }
                items(categories) { category ->
                    CategoryChip(
                        name = category.categoryName,
                        isSelected = viewModel.selectedCategoryId == category.categoryId,
                        onClick = { viewModel.onCategorySelect(category.categoryId) }
                    )
                }
            }

            if (series.itemCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (viewModel.searchQuery.isNotEmpty()) stringResource(R.string.search_no_results) else stringResource(R.string.series_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(100.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    items(series.itemCount, key = { index -> series[index]?.seriesId ?: index }) { index ->
                        val item = series[index]
                        if (item != null) {
                            val isFavorite by viewModel.isFavorite(item.seriesId).collectAsState(initial = false)
                            SeriesItem(
                                series = item,
                                isFavorite = isFavorite,
                                onClick = { onSeriesClick(item.seriesId) },
                                onToggleFavorite = { viewModel.onToggleFavorite(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesItem(
    series: SeriesStreamEntity,
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
                    model = series.cover,
                    contentDescription = series.name,
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
            text = series.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

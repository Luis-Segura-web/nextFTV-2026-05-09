package com.stream.nextftv.presentation.screens.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.stream.nextftv.R
import com.stream.nextftv.data.local.entity.series.SeriesCategoryEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.domain.model.SortOrder
import com.stream.nextftv.presentation.screens.livetv.CategoryChip
import com.stream.nextftv.presentation.theme.spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SeriesTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    sortMenuExpanded: Boolean,
    focusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onDismissSearch: () -> Unit,
    onBack: () -> Unit,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                    )
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
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
                }
            } else {
                Text(
                    text = stringResource(R.string.series_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { if (isSearchActive) onDismissSearch() else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = null
                )
            }

            Box {
                IconButton(onClick = { onSortMenuExpandedChange(true) }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { onSortMenuExpandedChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_default)) },
                        onClick = { onSortOrderSelected(SortOrder.DEFAULT) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_az)) },
                        onClick = { onSortOrderSelected(SortOrder.A_Z) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_za)) },
                        onClick = { onSortOrderSelected(SortOrder.Z_A) }
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

@Composable
internal fun SeriesContent(
    categories: List<SeriesCategoryEntity>,
    series: LazyPagingItems<SeriesStreamEntity>,
    enhancedSearchResults: List<SeriesStreamEntity>,
    selectedCategoryId: String,
    searchQuery: String,
    isEnhancedSearchLoading: Boolean,
    gridState: LazyGridState,
    innerPadding: PaddingValues,
    viewModel: SeriesViewModel,
    onSeriesClick: (Int, String?) -> Unit
) {
    val isEnhancedSearchMode = searchQuery.isNotBlank()
    val displayedCount = if (isEnhancedSearchMode) enhancedSearchResults.size else series.itemCount
    val hasAnySearchResults = displayedCount > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
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
                    isSelected = selectedCategoryId == "all",
                    onClick = { viewModel.onCategorySelect("all") }
                )
            }
            item {
                CategoryChip(
                    name = stringResource(R.string.live_category_favorites),
                    isSelected = selectedCategoryId == "favorites",
                    onClick = { viewModel.onCategorySelect("favorites") }
                )
            }
            item {
                CategoryChip(
                    name = stringResource(R.string.live_category_recents),
                    isSelected = selectedCategoryId == "recents",
                    onClick = { viewModel.onCategorySelect("recents") }
                )
            }
            items(categories) { category ->
                CategoryChip(
                    name = category.categoryName,
                    isSelected = selectedCategoryId == category.categoryId,
                    onClick = { viewModel.onCategorySelect(category.categoryId) }
                )
            }
        }

        if (isEnhancedSearchLoading && !hasAnySearchResults) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!hasAnySearchResults) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isNotEmpty()) stringResource(R.string.search_no_results) else stringResource(R.string.series_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(100.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.medium,
                    top = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.xxl
                ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                if (isEnhancedSearchMode) {
                    items(
                        count = enhancedSearchResults.size,
                        key = { index -> enhancedSearchResults[index].seriesId }
                    ) { index ->
                        val item = enhancedSearchResults[index]
                        val isFavorite by viewModel.isFavorite(item.seriesId).collectAsState(initial = false)
                        SeriesItem(
                            series = item,
                            isFavorite = isFavorite,
                            onClick = { onSeriesClick(item.seriesId, selectedCategoryId) },
                            onToggleFavorite = { viewModel.onToggleFavorite(item) }
                        )
                    }
                } else {
                    items(series.itemCount, key = { index -> series[index]?.seriesId ?: index }) { index ->
                        val item = series[index]
                        if (item != null) {
                            val isFavorite by viewModel.isFavorite(item.seriesId).collectAsState(initial = false)
                            SeriesItem(
                                series = item,
                                isFavorite = isFavorite,
                                onClick = { onSeriesClick(item.seriesId, selectedCategoryId) },
                                onToggleFavorite = { viewModel.onToggleFavorite(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.stream.nextftv.presentation.screens.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.stream.nextftv.data.local.entity.live.LiveCategoryEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamEntity
import com.stream.nextftv.domain.model.SortOrder
import com.stream.nextftv.presentation.player.PlaybackContentType
import com.stream.nextftv.presentation.player.VideoPlayer
import com.stream.nextftv.presentation.theme.spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LiveTvTopBar(
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
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.live_search_placeholder),
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
                    text = stringResource(R.string.live_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSearchActive) onDismissSearch() else onBack()
                }
            ) {
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
internal fun LiveTvMainContent(
    categories: List<LiveCategoryEntity>,
    streams: LazyPagingItems<LiveStreamEntity>,
    currentStream: LiveStreamEntity?,
    isInPipMode: Boolean,
    isFullScreen: Boolean,
    innerPadding: PaddingValues,
    listState: LazyListState,
    searchQuery: String,
    selectedCategoryId: String,
    viewModel: LiveTvViewModel,
    onEnterFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onPipRequested: () -> Unit,
    onClosePlayer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isInPipMode) PaddingValues(0.dp) else innerPadding)
            .imePadding()
    ) {
        LiveTvPlayerSection(
            currentStream = currentStream,
            isInPipMode = isInPipMode,
            isFullScreen = isFullScreen,
            viewModel = viewModel,
            onEnterFullscreen = onEnterFullscreen,
            onExitFullscreen = onExitFullscreen,
            onPipRequested = onPipRequested,
            onClosePlayer = onClosePlayer
        )

        if (!isInPipMode) {
            LiveTvCategoryRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelect = viewModel::onCategorySelect
            )

            LiveTvChannelSection(
                streams = streams,
                currentStream = currentStream,
                listState = listState,
                searchQuery = searchQuery,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun LiveTvPlayerSection(
    currentStream: LiveStreamEntity?,
    isInPipMode: Boolean,
    isFullScreen: Boolean,
    viewModel: LiveTvViewModel,
    onEnterFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onPipRequested: () -> Unit,
    onClosePlayer: () -> Unit
) {
    Box(
        modifier = if (isInPipMode) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
        }.background(Color.Black)
    ) {
        if (currentStream != null && viewModel.activeProfile != null) {
            val profile = viewModel.activeProfile!!
            val streamUrl = "${profile.url}live/${profile.username}/${profile.password}/${currentStream.streamId}.ts"

            key(streamUrl) {
                VideoPlayer(
                    url = streamUrl,
                    title = currentStream.name,
                    isFullScreen = isFullScreen,
                    isSmall = isInPipMode,
                    contentType = PlaybackContentType.LIVE,
                    onLoading = { loading ->
                        viewModel.isPlayerLoading = loading
                        if (!loading && viewModel.currentPlayingStream != null) {
                            viewModel.onPlaybackStarted()
                        }
                    },
                    onError = { viewModel.playerError = it },
                    onEnterFullscreen = onEnterFullscreen,
                    onQuitFullscreen = onExitFullscreen,
                    onNext = { viewModel.onNextChannel() },
                    onPrevious = { viewModel.onPreviousChannel() },
                    onPipRequested = onPipRequested,
                    onClose = onClosePlayer,
                    isLive = true
                )
            }
        } else if (!isInPipMode) {
            Text(
                text = stringResource(R.string.live_select_channel),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun LiveTvCategoryRow(
    categories: List<LiveCategoryEntity>,
    selectedCategoryId: String,
    onCategorySelect: (String) -> Unit
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
                onClick = { onCategorySelect("all") }
            )
        }
        item {
            CategoryChip(
                name = stringResource(R.string.live_category_favorites),
                isSelected = selectedCategoryId == "favorites",
                onClick = { onCategorySelect("favorites") }
            )
        }
        item {
            CategoryChip(
                name = stringResource(R.string.live_category_recents),
                isSelected = selectedCategoryId == "recents",
                onClick = { onCategorySelect("recents") }
            )
        }
        items(categories) { category ->
            CategoryChip(
                name = category.categoryName,
                isSelected = selectedCategoryId == category.categoryId,
                onClick = { onCategorySelect(category.categoryId) }
            )
        }
    }
}

@Composable
private fun ColumnScope.LiveTvChannelSection(
    streams: LazyPagingItems<LiveStreamEntity>,
    currentStream: LiveStreamEntity?,
    listState: LazyListState,
    searchQuery: String,
    viewModel: LiveTvViewModel
) {
    if (streams.itemCount == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    stringResource(R.string.search_no_results)
                } else {
                    stringResource(R.string.live_no_streams)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .imePadding(),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxl)
    ) {
        items(streams.itemCount, key = { index -> streams[index]?.streamId ?: index }) { index ->
            val stream = streams[index]
            if (stream != null) {
                val isFavorite by viewModel.isFavorite(stream.streamId).collectAsState(initial = false)
                ChannelItem(
                    stream = stream,
                    isSelected = currentStream?.streamId == stream.streamId,
                    isFavorite = isFavorite,
                    onClick = { viewModel.onChannelClick(stream) },
                    onToggleFavorite = { viewModel.onToggleFavorite(stream) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
        }
    }
}

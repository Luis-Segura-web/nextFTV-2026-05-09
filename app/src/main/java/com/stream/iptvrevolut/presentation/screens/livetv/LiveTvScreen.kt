package com.stream.iptvrevolut.presentation.screens.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.stream.iptvrevolut.R
import com.stream.iptvrevolut.data.local.entity.live.LiveStreamEntity
import com.stream.iptvrevolut.domain.model.SortOrder
import com.stream.iptvrevolut.presentation.player.PipModeState
import com.stream.iptvrevolut.presentation.screens.livetv.components.VideoPlayer
import com.stream.iptvrevolut.presentation.theme.dimens
import com.stream.iptvrevolut.presentation.theme.spacing
import com.stream.iptvrevolut.utils.findActivity
import com.stream.iptvrevolut.utils.setFullScreen
import android.content.pm.ActivityInfo
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    onBack: () -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val streams = viewModel.pagedStreams.collectAsLazyPagingItems()
    val currentStream = viewModel.currentPlayingStream
    val isInPipMode = PipModeState.isInPipMode
    var isFullScreen by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Recordar el estado de filtrado
    var lastFilterKey by androidx.compose.runtime.saveable.rememberSaveable { 
        mutableStateOf("${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}") 
    }

    LaunchedEffect(viewModel.selectedCategoryId, viewModel.sortOrder, viewModel.searchQuery) {
        val currentKey = "${viewModel.selectedCategoryId}-${viewModel.searchQuery}-${viewModel.sortOrder}"
        if (currentKey != lastFilterKey) {
            listState.scrollToItem(0)
            lastFilterKey = currentKey
        }
    }

    LaunchedEffect(viewModel.isSearchActive) {
        if (viewModel.isSearchActive) {
            delay(150)
            focusRequester.requestFocus()
        }
    }

    // UNIFICACIÓN DE ORIENTACIÓN: Solo Landscape en Fullscreen
    DisposableEffect(isFullScreen) {
        val activity = context.findActivity()
        if (isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val toggleFullScreen: () -> Unit = {
        isFullScreen = !isFullScreen
        context.findActivity()?.setFullScreen(isFullScreen)
    }

    BackHandler {
        if (isFullScreen) {
            toggleFullScreen()
        } else if (viewModel.isSearchActive) {
            viewModel.isSearchActive = false
            viewModel.searchQuery = ""
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
                    title = {
                        if (viewModel.isSearchActive) {
                            TextField(
                                value = viewModel.searchQuery,
                                onValueChange = { viewModel.searchQuery = it },
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
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold 
                            ) 
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (viewModel.isSearchActive) {
                                viewModel.isSearchActive = false
                                viewModel.searchQuery = ""
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
                                viewModel.searchQuery = ""
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen || isInPipMode) PaddingValues(0.dp) else innerPadding)
        ) {
            Box(
                modifier = if (isFullScreen || isInPipMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                }
                .background(Color.Black)
            ) {
                if (currentStream != null && viewModel.activeProfile != null) {
                    val profile = viewModel.activeProfile!!
                    val streamUrl = "${profile.url}live/${profile.username}/${profile.password}/${currentStream.streamId}.ts"
                    
                    key(streamUrl) {
                        VideoPlayer(
                            url = streamUrl,
                            title = currentStream.name,
                            useOriginalMedia3Controller = true,
                            isFullScreen = isFullScreen,
                            onLoading = { loading ->
                                viewModel.isPlayerLoading = loading
                                if (!loading && viewModel.currentPlayingStream != null) {
                                    viewModel.onPlaybackStarted()
                                }
                            },
                            onError = { viewModel.playerError = it },
                            onFullScreenClick = toggleFullScreen,
                            onNext = { viewModel.onNextChannel() },
                            onPrevious = { viewModel.onPreviousChannel() },
                            onClose = { 
                                isFullScreen = false
                                viewModel.currentPlayingStream = null 
                            },
                            isLive = true
                        )
                    }
                } else if (!isFullScreen && !isInPipMode) {
                    Text(
                        text = stringResource(R.string.live_select_channel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (!isFullScreen && !isInPipMode) {
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

                if (streams.itemCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (viewModel.searchQuery.isNotEmpty()) stringResource(R.string.search_no_results) else stringResource(R.string.live_no_streams),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium)
                    ) {
                        items(streams.itemCount, key = { index -> streams[index]?.streamId ?: index }) { index ->
                            val stream = streams[index]
                            if (stream != null) {
                                val isFavorite by viewModel.isFavorite(stream.streamId).collectAsState(initial = false)
                                ChannelItem(
                                    stream = stream,
                                    isSelected = currentStream?.streamId == stream.streamId,
                                    isFavorite = isFavorite,
                                    onClick = { 
                                        viewModel.onChannelClick(stream)
                                    },
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
            }
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(MaterialTheme.dimens.categoryChipHeight)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ChannelItem(
    stream: LiveStreamEntity,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.dimens.channelItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = stream.streamIcon,
            contentDescription = null,
            modifier = Modifier
                .size(MaterialTheme.dimens.channelLogoSize)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

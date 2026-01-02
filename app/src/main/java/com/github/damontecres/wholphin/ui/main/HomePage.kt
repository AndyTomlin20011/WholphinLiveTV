package com.github.damontecres.wholphin.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.jellyfin.sdk.model.api.ImageType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.abbreviateNumber
import com.github.damontecres.wholphin.ui.cards.BannerCard
import com.github.damontecres.wholphin.ui.cards.ItemRow
import com.github.damontecres.wholphin.ui.components.CircularProgress
import com.github.damontecres.wholphin.ui.components.DialogParams
import com.github.damontecres.wholphin.ui.components.DialogPopup
import com.github.damontecres.wholphin.ui.components.EpisodeQuickDetails
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.components.MovieQuickDetails
import com.github.damontecres.wholphin.ui.components.SeriesQuickDetails
import com.github.damontecres.wholphin.ui.data.AddPlaylistViewModel
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.data.RowColumnSaver
import com.github.damontecres.wholphin.ui.detail.MoreDialogActions
import com.github.damontecres.wholphin.ui.detail.PlaylistDialog
import com.github.damontecres.wholphin.ui.detail.PlaylistLoadingState
import com.github.damontecres.wholphin.ui.detail.buildMoreDialogItemsForHome
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.playback.isPlayKeyUp
import com.github.damontecres.wholphin.ui.playback.playable
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.AppColors
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import com.github.damontecres.wholphin.util.LoadingState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import timber.log.Timber
import java.util.UUID

@Composable
fun HomePage(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: AddPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var firstLoad by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        viewModel.init(preferences).join()
        firstLoad = false
    }
    val loading by viewModel.loadingState.observeAsState(LoadingState.Loading)
    val refreshing by viewModel.refreshState.observeAsState(LoadingState.Loading)
    val watchingRows by viewModel.watchingRows.observeAsState(listOf())
    val sportsRows by viewModel.sportsRows.observeAsState(listOf())
    val latestRows by viewModel.latestRows.observeAsState(listOf())
    val heroItems by viewModel.heroItems.observeAsState(listOf())
    LaunchedEffect(loading) {
        val state = loading
        if (!firstLoad && state is LoadingState.Error) {
            // After the first load, refreshes occur in the background and an ErrorMessage won't show
            // So send a Toast on errors instead
            Toast
                .makeText(
                    context,
                    "Home refresh error: ${state.localizedMessage}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    when (val state = loading) {
        is LoadingState.Error -> {
            ErrorMessage(state)
        }

        LoadingState.Loading,
        LoadingState.Pending,
        -> {
            LoadingPage()
        }

        LoadingState.Success -> {
            var dialog by remember { mutableStateOf<DialogParams?>(null) }
            var showPlaylistDialog by remember { mutableStateOf<UUID?>(null) }
            val playlistState by playlistViewModel.playlistState.observeAsState(PlaylistLoadingState.Pending)
            HomePageContent(
                heroItems = heroItems,
                watchingRows + sportsRows + latestRows,
                onClickItem = { position, item ->
                    viewModel.navigationManager.navigateTo(item.destination())
                },
                onLongClickItem = { position, item ->
                    val dialogItems =
                        buildMoreDialogItemsForHome(
                            context = context,
                            item = item,
                            seriesId = item.data.seriesId,
                            playbackPosition = item.playbackPosition,
                            watched = item.played,
                            favorite = item.favorite,
                            actions =
                                MoreDialogActions(
                                    navigateTo = viewModel.navigationManager::navigateTo,
                                    onClickWatch = { itemId, played ->
                                        viewModel.setWatched(itemId, played)
                                    },
                                    onClickFavorite = { itemId, favorite ->
                                        viewModel.setFavorite(itemId, favorite)
                                    },
                                    onClickAddPlaylist = { itemId ->
                                        playlistViewModel.loadPlaylists(MediaType.VIDEO)
                                        showPlaylistDialog = itemId
                                    },
                                ),
                        )
                    dialog =
                        DialogParams(
                            title = item.title ?: "",
                            fromLongClick = true,
                            items = dialogItems,
                        )
                },
                onClickHeroItem = { item ->
                    viewModel.navigationManager.navigateTo(item.destination())
                },
                onLongClickHeroItem = { item ->
                    val dialogItems =
                        buildMoreDialogItemsForHome(
                            context = context,
                            item = item,
                            seriesId = item.data.seriesId,
                            playbackPosition = item.playbackPosition,
                            watched = item.played,
                            favorite = item.favorite,
                            actions =
                                MoreDialogActions(
                                    navigateTo = viewModel.navigationManager::navigateTo,
                                    onClickWatch = { itemId, played ->
                                        viewModel.setWatched(itemId, played)
                                    },
                                    onClickFavorite = { itemId, favorite ->
                                        viewModel.setFavorite(itemId, favorite)
                                    },
                                    onClickAddPlaylist = { itemId ->
                                        playlistViewModel.loadPlaylists(MediaType.VIDEO)
                                        showPlaylistDialog = itemId
                                    },
                                ),
                        )
                    dialog =
                        DialogParams(
                            title = item.title ?: "",
                            fromLongClick = true,
                            items = dialogItems,
                        )
                },
                onClickPlay = { _, item ->
                    val destination =
                        when (item.type) {
                            BaseItemKind.PROGRAM,
                            BaseItemKind.TV_PROGRAM,
                            BaseItemKind.LIVE_TV_PROGRAM,
                            ->
                                item.data.channelId?.let { channelId ->
                                    Destination.Playback(channelId, 0)
                                } ?: Destination.Playback(item)

                            else -> Destination.Playback(item)
                        }
                    viewModel.navigationManager.navigateTo(destination)
                },
                loadingState = refreshing,
                showClock = preferences.appPreferences.interfacePreferences.showClock,
                onUpdateBackdrop = viewModel::updateBackdrop,
                modifier = modifier,
            )
            dialog?.let { params ->
                DialogPopup(
                    params = params,
                    onDismissRequest = { dialog = null },
                )
            }
            showPlaylistDialog?.let { itemId ->
                PlaylistDialog(
                    title = stringResource(R.string.add_to_playlist),
                    state = playlistState,
                    onDismissRequest = { showPlaylistDialog = null },
                    onClick = {
                        playlistViewModel.addToPlaylist(it.id, itemId)
                        showPlaylistDialog = null
                    },
                    createEnabled = true,
                    onCreatePlaylist = {
                        playlistViewModel.createPlaylistAndAddItem(it, itemId)
                        showPlaylistDialog = null
                    },
                    elevation = 3.dp,
                )
            }
        }
    }
}

@Composable
fun HomePageContent(
    heroItems: List<BaseItem>,
    homeRows: List<HomeRowLoadingState>,
    onClickItem: (RowColumn, BaseItem) -> Unit,
    onLongClickItem: (RowColumn, BaseItem) -> Unit,
    onClickHeroItem: (BaseItem) -> Unit,
    onLongClickHeroItem: (BaseItem) -> Unit,
    onClickPlay: (RowColumn, BaseItem) -> Unit,
    showClock: Boolean,
    onUpdateBackdrop: (BaseItem) -> Unit,
    modifier: Modifier = Modifier,
    onFocusPosition: ((RowColumn) -> Unit)? = null,
    loadingState: LoadingState? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val heroCardHeight = Cards.height2x3 * 1.7f
    val heroOffset = if (heroItems.isNotEmpty()) 1 else 0
    val firstRow =
        remember {
            homeRows
                .indexOfFirst {
                    when (it) {
                        is HomeRowLoadingState.Error -> false
                        is HomeRowLoadingState.Loading -> true
                        is HomeRowLoadingState.Pending -> true
                        is HomeRowLoadingState.Success -> it.items.isNotEmpty()
                    }
                }.coerceAtLeast(0)
        }
    var position by rememberSaveable(stateSaver = RowColumnSaver) {
        mutableStateOf(RowColumn(firstRow, 0))
    }
    val focusedItem =
        remember(position) {
            position.let {
                (homeRows.getOrNull(it.row) as? HomeRowLoadingState.Success)?.items?.getOrNull(it.column)
            }
        }

    val listState = rememberLazyListState()
    val rowFocusRequesters = remember(homeRows.size) { List(homeRows.size) { FocusRequester() } }
    val heroFocusRequester = remember { FocusRequester() }
    var focused by rememberSaveable { mutableStateOf(false) }
    val headerVisible by
        remember(heroItems, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
            derivedStateOf {
                if (heroItems.isEmpty()) {
                    true
                } else {
                    listState.firstVisibleItemIndex > 0 ||
                        listState.firstVisibleItemScrollOffset >
                        with(density) { (heroCardHeight / 2).roundToPx() }
                }
            }
        }
    LaunchedEffect(homeRows, heroItems) {
        if (!focused) {
            if (heroItems.isNotEmpty()) {
                heroFocusRequester.tryRequestFocus()
                focused = true
            } else {
                homeRows
                    .indexOfFirst { it is HomeRowLoadingState.Success && it.items.isNotEmpty() }
                    .takeIf { it >= 0 }
                    ?.let {
                        rowFocusRequesters[it].tryRequestFocus()
                        delay(50)
                        val maxIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        val targetIndex =
                            if (heroItems.isNotEmpty() && position.row == 0) {
                                0
                            } else {
                                (position.row + heroOffset).coerceAtMost(maxIndex)
                            }
                        listState.animateScrollToItem(targetIndex)
                        focused = true
                    }
            }
        } else {
            rowFocusRequesters.getOrNull(position.row)?.tryRequestFocus()
        }
    }
    LaunchedEffect(position, heroOffset, listState.layoutInfo.totalItemsCount) {
        val targetIndex =
            if (heroItems.isNotEmpty() && position.row == 0) {
                0
            } else {
                position.row + heroOffset
            }
        val maxIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
        listState.animateScrollToItem(targetIndex.coerceAtMost(maxIndex))
    }
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = headerVisible) {
                HomePageHeader(
                    item = focusedItem,
                    modifier =
                        Modifier
                            .padding(top = 48.dp, bottom = 24.dp, start = 32.dp, end = 32.dp)
                            .fillMaxHeight(.33f),
                )
            }
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = Cards.height2x3,
                    ),
                modifier =
                    Modifier
                        .focusRestorer(),
            ) {
                if (heroItems.isNotEmpty()) {
                    item {
                        HomeHeroCarousel(
                            items = heroItems,
                            onClick = onClickHeroItem,
                            onLongClick = onLongClickHeroItem,
                            onUpdateBackdrop = onUpdateBackdrop,
                            focusRequester = heroFocusRequester,
                            cardHeight = heroCardHeight,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                itemsIndexed(homeRows) { rowIndex, row ->
                    when (val r = row) {
                        is HomeRowLoadingState.Loading,
                        is HomeRowLoadingState.Pending,
                        -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.animateItem(),
                            ) {
                                Text(
                                    text = r.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = stringResource(R.string.loading),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }

                        is HomeRowLoadingState.Error -> {
                            var focused by remember { mutableStateOf(false) }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier =
                                    Modifier
                                        .onFocusChanged {
                                            focused = it.isFocused
                                        }.focusable()
                                        .background(
                                            if (focused) {
                                                // Just so the user can tell it has focus
                                                MaterialTheme.colorScheme.border.copy(alpha = .25f)
                                            } else {
                                                Color.Unspecified
                                            },
                                        ).animateItem(),
                            ) {
                                Text(
                                    text = r.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = r.localizedMessage,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        is HomeRowLoadingState.Success -> {
                            if (row.items.isNotEmpty()) {
                                ItemRow(
                                    title = row.title,
                                    items = row.items,
                                    onClickItem = { index, item ->
                                        onClickItem.invoke(RowColumn(rowIndex, index), item)
                                    },
                                    onLongClickItem = { index, item ->
                                        onLongClickItem.invoke(RowColumn(rowIndex, index), item)
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(rowFocusRequesters[rowIndex])
                                            .animateItem(),
                                    cardContent = { index, item, cardModifier, onClick, onLongClick ->
                                        val isProgramItem =
                                            remember(item) {
                                                when (item?.type) {
                                                    BaseItemKind.PROGRAM,
                                                    BaseItemKind.TV_PROGRAM,
                                                    BaseItemKind.LIVE_TV_PROGRAM,
                                                    -> true

                                                    else -> false
                                                }
                                            }
                                        val rowCardOverlay =
                                            remember(item, isProgramItem) {
                                                val overlayText =
                                                    if (isProgramItem) {
                                                        null
                                                    } else {
                                                        item?.data?.indexNumber?.let { "E$it" }
                                                            ?: item
                                                                ?.data
                                                                ?.userData
                                                                ?.unplayedItemCount
                                                                ?.takeIf { it > 0 }
                                                                ?.let { abbreviateNumber(it) }
                                                    }
                                                val overlayLogoId =
                                                    if (isProgramItem) {
                                                        item?.data?.channelId
                                                    } else {
                                                        null
                                                    }
                                                Triple(
                                                    overlayText,
                                                    overlayLogoId,
                                                    if (isProgramItem) ImageType.PRIMARY else ImageType.LOGO,
                                                )
                                            }
                                        BannerCard(
                                            name = item?.data?.seriesName ?: item?.name,
                                            item = item,
                                            aspectRatio =
                                                if (isProgramItem) {
                                                    AspectRatios.WIDE
                                                } else {
                                                    AspectRatios.TALL
                                                },
                                            cornerText = rowCardOverlay.first,
                                            cornerImageItemId = rowCardOverlay.second,
                                            cornerImageType = rowCardOverlay.third,
                                            played = item?.data?.userData?.played ?: false,
                                            favorite = item?.favorite ?: false,
                                            playPercent =
                                                item?.data?.userData?.playedPercentage
                                                    ?: 0.0,
                                            onClick = onClick,
                                            onLongClick = onLongClick,
                                            modifier =
                                                cardModifier
                                                    .onFocusChanged {
                                                        if (it.isFocused) {
                                                            position = RowColumn(rowIndex, index)
                                                            item?.let(onUpdateBackdrop)
                                                        }
                                                        if (it.isFocused && onFocusPosition != null) {
                                                            val nonEmptyRowBefore =
                                                                homeRows
                                                                    .subList(0, rowIndex)
                                                                    .count {
                                                                        it is HomeRowLoadingState.Success && it.items.isEmpty()
                                                                    }
                                                            onFocusPosition.invoke(
                                                                RowColumn(
                                                                    rowIndex - nonEmptyRowBefore,
                                                                    index,
                                                                ),
                                                            )
                                                        }
                                                    }.onKeyEvent {
                                                        if (isPlayKeyUp(it) && item?.type?.playable == true) {
                                                            Timber.v("Clicked play on ${item.id}")
                                                            onClickPlay.invoke(position, item)
                                                            return@onKeyEvent true
                                                        }
                                                        return@onKeyEvent false
                                                    },
                                            interactionSource = null,
                                            cardHeight = Cards.height2x3,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        when (loadingState) {
            LoadingState.Pending,
            LoadingState.Loading,
            -> {
                Box(
                    modifier =
                        Modifier
                            .padding(if (showClock) 40.dp else 20.dp)
                            .size(40.dp)
                            .align(Alignment.TopEnd),
                ) {
                    CircularProgress(Modifier.fillMaxSize())
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun HomeHeroCarousel(
    items: List<BaseItem>,
    onClick: (BaseItem) -> Unit,
    onLongClick: (BaseItem) -> Unit,
    onUpdateBackdrop: (BaseItem) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 280.dp,
) {
    val focusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val imageUrlService = LocalImageUrlService.current
    val density = LocalDensity.current
    val heroAspectRatio = 16f / 9f
    val gradientStartY = remember(cardHeight, density) { with(density) { cardHeight.toPx() * 0.2f } }
    BoxWithConstraints(modifier = modifier) {
        val heroWidth = remember(cardHeight) { cardHeight * heroAspectRatio }
        val horizontalPadding = remember(maxWidth, heroWidth) {
            val availableSpace = maxWidth - heroWidth
            if (availableSpace > 0.dp) availableSpace / 2 else 0.dp
        }
        val horizontalPaddingPx = remember(horizontalPadding, density) {
            with(density) { horizontalPadding.roundToPx() }
        }
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items) { index, item ->
                val requester = focusRequesters.getOrNull(index)
                var isHeroCardFocused by remember { mutableStateOf(false) }
                val logoUrl =
                    remember(item) {
                        imageUrlService.getItemImageUrl(
                            itemId = item.id,
                            imageType = ImageType.LOGO,
                            maxWidth = 480,
                            maxHeight = 180,
                        )
                            ?: imageUrlService.getItemImageUrl(
                                itemId = item.id,
                                imageType = ImageType.PRIMARY,
                                maxHeight = 180,
                                maxWidth = 480,
                            )
                    }
                BannerCard(
                    name = item.data.seriesName ?: item.name,
                    item = item,
                    imageType = ImageType.BACKDROP,
                    onClick = { onClick(item) },
                    onLongClick = { onLongClick(item) },
                    played = item.data.userData?.played ?: false,
                    favorite = item.favorite ?: false,
                    playPercent = item.data.userData?.playedPercentage ?: 0.0,
                    cardHeight = cardHeight,
                    aspectRatio = heroAspectRatio,
                    overlayContent = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    Color.Transparent,
                                                    AppColors.TransparentBlack75,
                                                ),
                                            startY = gradientStartY,
                                        ),
                                    ),
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                                    .fillMaxWidth(.65f),
                        ) {
                            if (logoUrl != null) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.height(64.dp),
                                )
                            } else {
                                Text(
                                    text = item.title ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            item.data.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                                Text(
                                    text = overview,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    isFocused = isHeroCardFocused,
                    modifier =
                        Modifier
                            .focusRequester(if (index == 0) focusRequester else requester ?: focusRequester)
                            .onFocusChanged {
                                isHeroCardFocused = it.isFocused
                                if (it.isFocused) {
                                    onUpdateBackdrop(item)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(
                                            index = index,
                                            scrollOffset = -horizontalPaddingPx,
                                        )
                                    }
                                }
                            },
                )
            }
        }
    }
}

@Composable
fun HomePageHeader(
    item: BaseItem?,
    modifier: Modifier = Modifier,
) {
    item?.let {
        val dto = item.data
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier,
        ) {
            item.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(.75f),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxWidth(.6f)
                        .fillMaxHeight(),
            ) {
                val isEpisode = item.type == BaseItemKind.EPISODE
                val subtitle = if (isEpisode) dto.name else null
                val overview = dto.overview
                subtitle?.let {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when (item.type) {
                    BaseItemKind.EPISODE -> EpisodeQuickDetails(dto, Modifier)
                    BaseItemKind.SERIES -> SeriesQuickDetails(dto, Modifier)
                    else -> MovieQuickDetails(dto, Modifier)
                }
                val overviewModifier =
                    Modifier
                        .padding(0.dp)
                        .height(48.dp + if (!isEpisode) 12.dp else 0.dp)
                        .width(400.dp)
                if (overview.isNotNullOrBlank()) {
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isEpisode) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = overviewModifier,
                    )
                } else {
                    Spacer(overviewModifier)
                }
            }
        }
    }
}

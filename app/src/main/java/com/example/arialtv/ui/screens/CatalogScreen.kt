package com.example.arialtv.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.data.model.CatalogFilterOption
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.ui.components.ContinueWatchingRail
import com.example.arialtv.ui.components.TvHeroBanner
import com.example.arialtv.ui.components.TvMediaCard
import com.example.arialtv.ui.components.TvMediaRail
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.FocusBorderWhite
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SelectedBorderRed
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SignalRedSoft
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.theme.SurfaceTonal
import com.example.arialtv.ui.viewmodel.CatalogTabState
import kotlinx.coroutines.delay

private val QUICK_SORT_FILTERS = listOf(
    "Hottest" to "Trending",
    "Latest" to "Latest",
)

private val QUICK_YEAR_FILTERS = listOf("2026", "2025")

private val FALLBACK_GENRES = listOf(
    "Action",
    "Sci-Fi",
    "Comedy",
    "Thriller",
    "Horror",
    "Drama",
    "Animation",
    "Adventure",
    "Crime",
)

@Composable
fun CatalogScreen(
    tabState: CatalogTabState,
    watchHistory: List<WatchProgress>,
    watchlist: List<MediaItem>,
    showPersonalRails: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onPlayNow: (MediaItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onResumeProgress: (WatchProgress) -> Unit,
    onItemFocused: (MediaItem, String) -> Unit,
    onApplyFilter: (genre: String, year: String, sort: String, country: String) -> Unit,
    onLoadMoreFiltered: () -> Unit,
    onClearFilter: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalog = tabState.catalog
    when {
        tabState.isLoading && catalog == null -> {
            MinimalShimmerSkeleton(modifier = modifier)
        }
        tabState.error != null && catalog == null -> {
            CleanErrorState(
                message = tabState.error,
                onRetry = onRetry,
                onOpenSettings = onOpenSettings,
                modifier = modifier,
            )
        }
        catalog != null && catalog.rows.isEmpty() && catalog.featured.isEmpty() -> {
            CleanErrorState(
                message = "Nothing to show right now. Try refreshing in a moment.",
                onRetry = onRetry,
                onOpenSettings = onOpenSettings,
                modifier = modifier,
            )
        }
        catalog != null -> {
            val rawHeroItem = tabState.focusedItem
                ?: catalog.featured.firstOrNull()
                ?: catalog.rows.firstOrNull()?.items?.firstOrNull()

            // Debounce hero backdrop updates by 160ms so holding D-pad arrows stays silky 60fps
            var displayedHeroItem by remember { mutableStateOf(rawHeroItem) }
            var displayedCategory by remember { mutableStateOf(tabState.focusedCategory) }
            LaunchedEffect(rawHeroItem?.id, tabState.focusedCategory) {
                if (displayedHeroItem == null) {
                    displayedHeroItem = rawHeroItem
                    displayedCategory = tabState.focusedCategory
                } else if (rawHeroItem != null) {
                    delay(160L)
                    displayedHeroItem = rawHeroItem
                    displayedCategory = tabState.focusedCategory
                }
            }

            // Build genre pills from live MovieBox API `FILTER` chips + clean defaults
            val genreOptions = remember(catalog.filters) {
                buildList<CatalogFilterOption> {
                    catalog.filters.forEach { opt ->
                        if (opt.genre != "All" && none { it.genre.equals(opt.genre, ignoreCase = true) }) {
                            add(opt)
                        }
                    }
                    FALLBACK_GENRES.forEach { g ->
                        if (none { it.genre.equals(g, ignoreCase = true) }) {
                            add(CatalogFilterOption(label = g, genre = g))
                        }
                    }
                }.take(12)
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(CanvasBlack),
            ) {
                // Hero banner (compact when actively viewing a filtered grid so more posters fit)
                val hero = displayedHeroItem ?: rawHeroItem
                if (hero != null && !tabState.isFilterActive) {
                    TvHeroBanner(
                        item = hero,
                        categoryLabel = displayedCategory,
                        isInWatchlist = watchlist.any { it.id == hero.id },
                        onPlayClick = { onPlayNow(hero) },
                        onDetailsClick = { onItemClick(hero) },
                        onToggleWatchlist = { onToggleWatchlist(hero) },
                    )
                }

                // Live MovieBox API Filter Bar (All · Sort · Year · API Genres)
                LiveFilterBar(
                    tabState = tabState,
                    genreOptions = genreOptions,
                    onApplyFilter = onApplyFilter,
                    onClearFilter = onClearFilter,
                )

                if (tabState.isFilterActive) {
                    // Filtered Results Grid powered live by MovieBox `/subject-api/list`
                    FilteredCatalogGrid(
                        tabState = tabState,
                        onItemClick = onItemClick,
                        onItemFocused = { item ->
                            onItemFocused(item, tabState.focusedCategory)
                        },
                        onLoadMore = onLoadMoreFiltered,
                        onClearFilter = onClearFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(CanvasBlack),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (showPersonalRails && watchHistory.isNotEmpty()) {
                            item(key = "continue_watching", contentType = "cw_rail") {
                                ContinueWatchingRail(
                                    entries = watchHistory,
                                    onEntryClick = onResumeProgress,
                                    onEntryFocused = { progress ->
                                        onItemFocused(progress.item, "Continue Watching")
                                    },
                                )
                            }
                        }

                        if (showPersonalRails && watchlist.isNotEmpty()) {
                            item(key = "my_list_home", contentType = "media_rail") {
                                TvMediaRail(
                                    title = "My List",
                                    items = watchlist,
                                    onItemClick = onItemClick,
                                    onItemFocused = { item ->
                                        onItemFocused(item, "My List")
                                    },
                                )
                            }
                        }

                        itemsIndexed(
                            items = catalog.rows,
                            key = { idx, row -> "row_${idx}_${row.category}" },
                            contentType = { _, _ -> "media_rail" },
                        ) { idx, row ->
                            TvMediaRail(
                                title = row.category,
                                items = row.items,
                                indexPrefix = idx + 1,
                                onItemClick = onItemClick,
                                onItemFocused = { focused ->
                                    onItemFocused(focused, row.category)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveFilterBar(
    tabState: CatalogTabState,
    genreOptions: List<CatalogFilterOption>,
    onApplyFilter: (genre: String, year: String, sort: String, country: String) -> Unit,
    onClearFilter: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(CanvasBlack),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "flt_all") {
            CatalogFilterChip(
                label = "All",
                selected = !tabState.isFilterActive,
                onClick = onClearFilter,
            )
        }

        items(QUICK_SORT_FILTERS, key = { "sort_${it.first}" }) { (sortKey, label) ->
            val selected = tabState.activeSort == sortKey
            CatalogFilterChip(
                label = label,
                selected = selected,
                onClick = {
                    val nextSort = if (selected) "ForYou" else sortKey
                    onApplyFilter(tabState.activeGenre, tabState.activeYear, nextSort, tabState.activeCountry)
                },
            )
        }

        items(QUICK_YEAR_FILTERS, key = { "yr_$it" }) { yr ->
            val selected = tabState.activeYear == yr
            CatalogFilterChip(
                label = yr,
                selected = selected,
                onClick = {
                    val nextYear = if (selected) "All" else yr
                    onApplyFilter(tabState.activeGenre, nextYear, tabState.activeSort, tabState.activeCountry)
                },
            )
        }

        items(genreOptions, key = { "gen_${it.genre}" }) { option ->
            val selected = tabState.activeGenre.equals(option.genre, ignoreCase = true)
            CatalogFilterChip(
                label = option.label,
                selected = selected,
                onClick = {
                    val nextGenre = if (selected) "All" else option.genre
                    onApplyFilter(nextGenre, tabState.activeYear, tabState.activeSort, option.country)
                },
            )
        }
    }
}

@Composable
private fun CatalogFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else SurfaceTonal,
            contentColor = if (selected) PureWhite else InkSecondary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(1.5.dp, SelectedBorderRed), shape = shape) else Border.None,
            focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun FilteredCatalogGrid(
    tabState: CatalogTabState,
    onItemClick: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit,
    onLoadMore: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSummary = remember(tabState.activeGenre, tabState.activeYear, tabState.activeSort) {
        buildList {
            if (tabState.activeGenre != "All") add(tabState.activeGenre.uppercase())
            if (tabState.activeYear != "All") add(tabState.activeYear)
            if (tabState.activeSort != "ForYou") {
                add(if (tabState.activeSort == "Hottest") "TRENDING" else tabState.activeSort.uppercase())
            }
        }.joinToString("  ·  ")
    }

    Column(
        modifier = modifier
            .background(CanvasBlack)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (tabState.filteredItems.isNotEmpty()) {
                    "$activeSummary  ·  ${tabState.filteredItems.size} TITLES"
                } else {
                    activeSummary
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary,
            )

            val pillShape = RoundedCornerShape(50)
            Surface(
                onClick = onClearFilter,
                shape = ClickableSurfaceDefaults.shape(pillShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = SurfaceTonal,
                    contentColor = InkSecondary,
                    focusedContainerColor = PureWhite,
                    focusedContentColor = InkOnWhite,
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border.None,
                    focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = pillShape),
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Reset Filter", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        when {
            tabState.isFiltering && tabState.filteredItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "LOADING TITLES...",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary,
                    )
                }
            }

            tabState.filterError != null && tabState.filteredItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = tabState.filterError,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SignalRed,
                    )
                }
            }

            tabState.filteredItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No titles matched this filter combination.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary,
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 132.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = tabState.filteredItems,
                        key = { "flt_${it.id}" },
                        contentType = { "media_card" },
                    ) { item ->
                        TvMediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onFocused = { onItemFocused(item) },
                            cardWidth = 132.dp,
                            posterHeight = 192.dp,
                        )
                    }

                    if (tabState.filterHasMore) {
                        item(key = "flt_load_more", span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val btnShape = RoundedCornerShape(50)
                                Surface(
                                    onClick = onLoadMore,
                                    shape = ClickableSurfaceDefaults.shape(btnShape),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = SurfaceSelected,
                                        contentColor = PureWhite,
                                        focusedContainerColor = PureWhite,
                                        focusedContentColor = InkOnWhite,
                                    ),
                                    border = ClickableSurfaceDefaults.border(
                                        border = Border(BorderStroke(1.dp, SelectedBorderRed), shape = btnShape),
                                        focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = btnShape),
                                    ),
                                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                ) {
                                    Text(
                                        text = if (tabState.isFiltering) "Loading..." else "Load More",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
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
private fun MinimalShimmerSkeleton(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val pulse by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .padding(horizontal = 32.dp, vertical = 14.dp)
            .alpha(pulse),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceTonal)
                .padding(24.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .width(120.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceElevated),
                )
                Box(
                    Modifier
                        .width(280.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceElevated),
                    )
                    Box(
                        Modifier
                            .width(108.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceElevated),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .width(150.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceTonal),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(6) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(
                            modifier = Modifier
                                .width(128.dp)
                                .height(186.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceTonal),
                        )
                        Box(
                            modifier = Modifier
                                .width(92.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceTonal),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanErrorState(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceTonal)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SignalRedSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = SignalRed,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "NO CONNECTION",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = InkPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val pillShape = RoundedCornerShape(50)
                Surface(
                    onClick = onRetry,
                    shape = ClickableSurfaceDefaults.shape(pillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = PureWhite,
                        contentColor = InkOnWhite,
                        focusedContainerColor = SignalRed,
                        focusedContentColor = PureWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border.None,
                        focusedBorder = Border(BorderStroke(2.dp, FocusBorderWhite), shape = pillShape),
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Try Again", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    onClick = onOpenSettings,
                    shape = ClickableSurfaceDefaults.shape(pillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SurfaceElevated,
                        contentColor = InkPrimary,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border.None,
                        focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = pillShape),
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Settings", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

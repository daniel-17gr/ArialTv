package com.example.arialtv.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.data.model.CatalogFilterOption
import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.ui.components.ContinueWatchingRail
import com.example.arialtv.ui.components.TvHeroBanner
import com.example.arialtv.ui.components.TvMediaCard
import com.example.arialtv.ui.components.TvMediaRail
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkOnSelected
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
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
    recommendedRow: CatalogRow? = null,
    showPersonalRails: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onPlayNow: (MediaItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onResumeProgress: (WatchProgress) -> Unit,
    onItemFocused: (MediaItem, String) -> Unit,
    onNextFeatured: (() -> Unit)? = null,
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
            // Keep focused hero state local to CatalogScreen so D-pad browsing NEVER recomposes ArialTvApp!
            val defaultHero = remember(catalog) {
                catalog.featured.firstOrNull() ?: catalog.rows.firstOrNull()?.items?.firstOrNull()
            }
            var pendingHeroItem by remember(catalog) { mutableStateOf(tabState.focusedItem ?: defaultHero) }
            var pendingCategory by remember(catalog) { mutableStateOf(tabState.focusedCategory) }
            var displayedHeroItem by remember(catalog) { mutableStateOf(tabState.focusedItem ?: defaultHero) }
            var displayedCategory by remember(catalog) { mutableStateOf(tabState.focusedCategory) }

            LaunchedEffect(tabState.focusedItem?.id) {
                val vmFocus = tabState.focusedItem
                if (vmFocus != null && vmFocus.id != pendingHeroItem?.id) {
                    pendingHeroItem = vmFocus
                    pendingCategory = tabState.focusedCategory
                }
            }

            LaunchedEffect(pendingHeroItem?.id, pendingCategory) {
                if (displayedHeroItem == null) {
                    displayedHeroItem = pendingHeroItem
                    displayedCategory = pendingCategory
                } else if (pendingHeroItem != null) {
                    delay(180L)
                    displayedHeroItem = pendingHeroItem
                    displayedCategory = pendingCategory
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

            val watchlistIds = remember(watchlist) {
                watchlist.mapTo(HashSet()) { it.id }
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(CanvasBlack),
            ) {
                // Full-bleed Hero starts right at y = 0 behind the transparent floating top bar!
                val hero = displayedHeroItem ?: defaultHero
                if (hero != null && !tabState.isFilterActive) {
                    TvHeroBanner(
                        item = hero,
                        categoryLabel = displayedCategory,
                        isInWatchlist = hero.id in watchlistIds,
                        onPlayClick = { onPlayNow(hero) },
                        onDetailsClick = { onItemClick(hero) },
                        onToggleWatchlist = { onToggleWatchlist(hero) },
                        onNextFeatured = if (catalog.featured.size > 1) {
                            {
                                val feat = catalog.featured
                                val curIdx = feat.indexOfFirst { it.id == hero.id }
                                val next = feat[(curIdx + 1) % feat.size]
                                pendingHeroItem = next
                                pendingCategory = "Featured"
                                displayedHeroItem = next
                                displayedCategory = "Featured"
                                onNextFeatured?.invoke()
                            }
                        } else null,
                    )
                }

                // Live MovieBox API Filter Bar (clears transparent top bar when Hero is hidden)
                LiveFilterBar(
                    tabState = tabState,
                    genreOptions = genreOptions,
                    onApplyFilter = onApplyFilter,
                    onClearFilter = onClearFilter,
                    modifier = if (tabState.isFilterActive) Modifier.padding(top = 56.dp) else Modifier,
                )

                if (tabState.isFilterActive) {
                    FilteredCatalogGrid(
                        tabState = tabState,
                        onItemClick = onItemClick,
                        onItemFocused = { item ->
                            pendingHeroItem = item
                            pendingCategory = tabState.focusedCategory
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
                        contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (showPersonalRails && watchHistory.isNotEmpty()) {
                            item(key = "continue_watching", contentType = "cw_rail") {
                                ContinueWatchingRail(
                                    entries = watchHistory,
                                    onEntryClick = onResumeProgress,
                                    onEntryFocused = { progress ->
                                        pendingHeroItem = progress.item
                                        pendingCategory = "Continue Watching"
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
                                        pendingHeroItem = item
                                        pendingCategory = "My List"
                                    },
                                )
                            }
                        }

                        if (showPersonalRails && recommendedRow != null && recommendedRow.items.isNotEmpty()) {
                            item(key = "because_you_watched", contentType = "media_rail") {
                                TvMediaRail(
                                    title = recommendedRow.category,
                                    items = recommendedRow.items,
                                    onItemClick = onItemClick,
                                    onItemFocused = { item ->
                                        pendingHeroItem = item
                                        pendingCategory = "Recommended"
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
                                    pendingHeroItem = focused
                                    pendingCategory = row.category
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
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasBlack),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "flt_all") {
            CatalogFilterChip(
                label = "All",
                selected = !tabState.isFilterActive,
                onClick = onClearFilter,
            )
        }

        itemsIndexed(QUICK_SORT_FILTERS, key = { idx, it -> "sort_${idx}_${it.first}" }) { _, (sortKey, label) ->
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

        itemsIndexed(QUICK_YEAR_FILTERS, key = { idx, yr -> "yr_${idx}_$yr" }) { _, yr ->
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

        itemsIndexed(genreOptions, key = { idx, opt -> "gen_${idx}_${opt.genre}" }) { _, option ->
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

private val FilterChipShape = RoundedCornerShape(50)

@Composable
private fun CatalogFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(FilterChipShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else SurfaceTonal,
            contentColor = if (selected) InkOnSelected else InkSecondary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
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
        modifier = modifier.background(CanvasBlack),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 4.dp),
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

            Surface(
                onClick = onClearFilter,
                shape = ClickableSurfaceDefaults.shape(FilterChipShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = SurfaceTonal,
                    contentColor = InkSecondary,
                    focusedContainerColor = PureWhite,
                    focusedContentColor = InkOnWhite,
                ),
                border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
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
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = tabState.filteredItems,
                        key = { idx, it -> "flt_${idx}_${it.id}" },
                        contentType = { _, _ -> "media_card" },
                    ) { _, item ->
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
                                Surface(
                                    onClick = onLoadMore,
                                    shape = ClickableSurfaceDefaults.shape(FilterChipShape),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = SurfaceElevated,
                                        contentColor = PureWhite,
                                        focusedContainerColor = PureWhite,
                                        focusedContentColor = InkOnWhite,
                                    ),
                                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
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
                Surface(
                    onClick = onRetry,
                    shape = ClickableSurfaceDefaults.shape(FilterChipShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SurfaceSelected,
                        contentColor = InkOnSelected,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
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
                    shape = ClickableSurfaceDefaults.shape(FilterChipShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SurfaceElevated,
                        contentColor = InkPrimary,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
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

package com.example.arialtv.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Tune
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
import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.CategoryQuery
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.ui.components.ContinueWatchingRail
import com.example.arialtv.ui.components.TvHeroBanner
import com.example.arialtv.ui.components.TvMediaCard
import com.example.arialtv.ui.components.TvMediaRail
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
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

private val SORT_FILTERS = listOf(
    "ForYou" to "For You",
    "Hottest" to "Most Popular",
    "TopRated" to "Top Rated",
    "Latest" to "Newest",
)

private val YEAR_FILTERS = listOf("All", "2026", "2025", "2024", "2023", "2022", "2021", "2020")

private val REGION_FILTERS = listOf(
    "All" to "All Regions",
    "United States" to "USA",
    "United Kingdom" to "UK",
    "Korea" to "South Korea",
    "Japan" to "Japan",
    "France" to "France",
    "Spain" to "Spain",
    "India" to "India",
    "China" to "China",
)

private val VERIFIED_DEFAULT_GENRES = listOf(
    "Action",
    "Sci-Fi",
    "Thriller",
    "Crime",
    "Comedy",
    "Drama",
    "Horror",
    "Animation",
    "Anime",
    "Adventure",
    "Mystery",
    "Family",
    "History",
    "War",
    "Biography",
    "Documentary",
)

@Composable
fun CatalogScreen(
    tabState: CatalogTabState,
    watchHistory: List<WatchProgress>,
    watchlist: List<MediaItem>,
    recommendedRow: CatalogRow? = null,
    showPersonalRails: Boolean,
    showTypeToggle: Boolean = false,
    onItemClick: (MediaItem) -> Unit,
    onPlayNow: (MediaItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onResumeProgress: (WatchProgress) -> Unit,
    onItemFocused: (MediaItem, String) -> Unit,
    onNextFeatured: (() -> Unit)? = null,
    onApplyFilter: (genre: String, year: String, sort: String, country: String, channelId: Int) -> Unit,
    onLoadMoreFiltered: () -> Unit,
    onClearFilter: () -> Unit,
    onExpandCategory: (CategoryQuery) -> Unit,
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

            val genreOptions = remember(catalog.filters) {
                if (catalog.filters.isNotEmpty()) {
                    catalog.filters
                } else {
                    VERIFIED_DEFAULT_GENRES.map { CatalogFilterOption(label = it, genre = it) }
                }
            }

            val watchlistIds = remember(watchlist) {
                watchlist.mapTo(HashSet()) { it.id }
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(CanvasBlack),
            ) {
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

                LiveFilterBar(
                    tabState = tabState,
                    genreOptions = genreOptions,
                    showTypeToggle = showTypeToggle,
                    onApplyFilter = onApplyFilter,
                    onClearFilter = onClearFilter,
                    modifier = if (tabState.isFilterActive) Modifier.padding(top = 56.dp) else Modifier,
                )

                if (tabState.isFilterActive) {
                    FilteredCatalogGrid(
                        tabState = tabState,
                        showTypeToggle = showTypeToggle,
                        onItemClick = onItemClick,
                        onItemFocused = { item ->
                            pendingHeroItem = item
                            pendingCategory = tabState.focusedCategory
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
                                        pendingHeroItem = item
                                        pendingCategory = "My List"
                                        onItemFocused(item, "My List")
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
                                        onItemFocused(item, "Recommended")
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
                                previewLimit = 12,
                                onExpandCategory = row.expandQuery?.let { query ->
                                    { onExpandCategory(query) }
                                },
                                onItemClick = onItemClick,
                                onItemFocused = { focused ->
                                    pendingHeroItem = focused
                                    pendingCategory = row.category
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
    showTypeToggle: Boolean,
    onApplyFilter: (genre: String, year: String, sort: String, country: String, channelId: Int) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRefinePanel by remember { mutableStateOf(false) }

    val refineCount = remember(
        tabState.activeSort,
        tabState.activeYear,
        tabState.activeCountry,
        tabState.activeChannelId,
        showTypeToggle,
    ) {
        (if (tabState.activeSort != "ForYou") 1 else 0) +
            (if (tabState.activeYear != "All") 1 else 0) +
            (if (tabState.activeCountry != "All") 1 else 0) +
            (if (showTypeToggle && tabState.activeChannelId == 2) 1 else 0)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasBlack),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item(key = "flt_all") {
                CatalogFilterChip(
                    label = "All",
                    selected = !tabState.isFilterActive,
                    onClick = {
                        showRefinePanel = false
                        onClearFilter()
                    },
                )
            }

            item(key = "flt_refine_btn") {
                val label = if (refineCount > 0) "Refine · $refineCount" else "Refine"
                Surface(
                    onClick = { showRefinePanel = !showRefinePanel },
                    shape = ClickableSurfaceDefaults.shape(FilterChipShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (showRefinePanel || refineCount > 0) SurfaceSelected else SurfaceElevated,
                        contentColor = if (showRefinePanel || refineCount > 0) InkOnSelected else InkPrimary,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            itemsIndexed(genreOptions, key = { idx, opt -> "gen_${idx}_${opt.genre}" }) { _, option ->
                val selected = tabState.activeGenre.equals(option.genre, ignoreCase = true)
                CatalogFilterChip(
                    label = option.label,
                    selected = selected,
                    onClick = {
                        val nextGenre = if (selected) "All" else option.genre
                        onApplyFilter(
                            nextGenre,
                            tabState.activeYear,
                            tabState.activeSort,
                            tabState.activeCountry,
                            tabState.activeChannelId,
                        )
                    },
                )
            }
        }

        AnimatedVisibility(visible = showRefinePanel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceTonal)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (showTypeToggle) {
                    RefineRow(label = "TYPE") {
                        listOf(1 to "Films", 2 to "Series").forEach { (chId, label) ->
                            val selected = tabState.activeChannelId == chId && tabState.isFilterActive
                            CatalogFilterChip(
                                label = label,
                                selected = selected,
                                compact = true,
                                onClick = {
                                    val nextSort = if (!tabState.isFilterActive) "Hottest" else tabState.activeSort
                                    onApplyFilter(
                                        tabState.activeGenre,
                                        tabState.activeYear,
                                        nextSort,
                                        tabState.activeCountry,
                                        chId,
                                    )
                                },
                            )
                        }
                    }
                }

                RefineRow(label = "SORT") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(SORT_FILTERS, key = { idx, it -> "rf_s_${idx}_${it.first}" }) { _, (key, label) ->
                            CatalogFilterChip(
                                label = label,
                                selected = tabState.activeSort.equals(key, ignoreCase = true),
                                compact = true,
                                onClick = {
                                    onApplyFilter(
                                        tabState.activeGenre,
                                        tabState.activeYear,
                                        key,
                                        tabState.activeCountry,
                                        tabState.activeChannelId,
                                    )
                                },
                            )
                        }
                    }
                }

                RefineRow(label = "YEAR") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(YEAR_FILTERS, key = { idx, yr -> "rf_y_${idx}_$yr" }) { _, yr ->
                            CatalogFilterChip(
                                label = if (yr == "All") "All Years" else yr,
                                selected = tabState.activeYear == yr,
                                compact = true,
                                onClick = {
                                    val nextYear = if (tabState.activeYear == yr && yr != "All") "All" else yr
                                    onApplyFilter(
                                        tabState.activeGenre,
                                        nextYear,
                                        tabState.activeSort,
                                        tabState.activeCountry,
                                        tabState.activeChannelId,
                                    )
                                },
                            )
                        }
                    }
                }

                RefineRow(label = "REGION") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(REGION_FILTERS, key = { idx, it -> "rf_c_${idx}_${it.first}" }) { _, (apiVal, label) ->
                            CatalogFilterChip(
                                label = label,
                                selected = tabState.activeCountry.equals(apiVal, ignoreCase = true),
                                compact = true,
                                onClick = {
                                    val nextCountry = if (tabState.activeCountry.equals(apiVal, ignoreCase = true) && apiVal != "All") {
                                        "All"
                                    } else {
                                        apiVal
                                    }
                                    onApplyFilter(
                                        tabState.activeGenre,
                                        tabState.activeYear,
                                        tabState.activeSort,
                                        nextCountry,
                                        tabState.activeChannelId,
                                    )
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
private fun RefineRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
            modifier = Modifier.width(56.dp),
        )
        content()
    }
}

private val FilterChipShape = RoundedCornerShape(50)

@Composable
private fun CatalogFilterChip(
    label: String,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(FilterChipShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else if (compact) SurfaceElevated else SurfaceTonal,
            contentColor = if (selected) InkOnSelected else InkSecondary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 15.dp,
                vertical = if (compact) 6.dp else 7.dp,
            ),
        )
    }
}

@Composable
private fun FilteredCatalogGrid(
    tabState: CatalogTabState,
    showTypeToggle: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit,
    onLoadMore: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSummary = remember(
        tabState.activeChannelId,
        tabState.activeGenre,
        tabState.activeYear,
        tabState.activeCountry,
        tabState.activeSort,
        showTypeToggle,
    ) {
        buildList {
            if (showTypeToggle) {
                add(if (tabState.activeChannelId == 2) "SERIES" else "FILMS")
            }
            if (tabState.activeGenre != "All") add(tabState.activeGenre.uppercase())
            if (tabState.activeYear != "All") add(tabState.activeYear)
            if (tabState.activeCountry != "All") {
                val friendly = REGION_FILTERS.firstOrNull { it.first.equals(tabState.activeCountry, ignoreCase = true) }?.second
                    ?: tabState.activeCountry
                add(friendly.uppercase())
            }
            if (tabState.activeSort != "ForYou") {
                add(
                    when (tabState.activeSort) {
                        "Hottest" -> "POPULAR"
                        "TopRated" -> "TOP RATED"
                        else -> tabState.activeSort.uppercase()
                    },
                )
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

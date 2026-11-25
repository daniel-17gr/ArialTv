package com.example.arialtv.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.ui.components.TvMediaCard
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnSelected
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.theme.SurfaceTonal
import com.example.arialtv.ui.viewmodel.CategoryUiState

private val CatPillShape = RoundedCornerShape(50)
private val CAT_SORT_OPTIONS = listOf(
    "Hottest" to "Popular",
    "TopRated" to "Top Rated",
    "Latest" to "Newest",
)
private val CAT_YEAR_OPTIONS = listOf("All", "2026", "2025", "2024", "2023", "2022", "2021", "2020")
private val CAT_COUNTRY_OPTIONS = listOf(
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

@Composable
fun CategoryScreen(
    categoryState: CategoryUiState,
    onBack: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onUpdateFilters: (sort: String, year: String, country: String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val query = categoryState.query
    var showRefine by remember(query?.title) { mutableStateOf(false) }

    val activeRefineCount = remember(categoryState.activeYear, categoryState.activeCountry) {
        (if (categoryState.activeYear != "All") 1 else 0) +
            (if (categoryState.activeCountry != "All") 1 else 0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header Row: Back + Title + Subtitle + Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    onClick = onBack,
                    shape = ClickableSurfaceDefaults.shape(CatPillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SurfaceTonal,
                        contentColor = PureWhite,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Back", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = query?.title ?: "Category",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                    )
                    val subtitle = query?.subtitle.orEmpty()
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                        )
                    }
                }
            }

            if (categoryState.items.isNotEmpty()) {
                Text(
                    text = "${categoryState.items.size} TITLES",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                )
            }
        }

        // Primary Sort + Refine Toggle Bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(CAT_SORT_OPTIONS, key = { idx, it -> "cs_${idx}_${it.first}" }) { _, (sortKey, label) ->
                val selected = categoryState.activeSort.equals(sortKey, ignoreCase = true)
                CategoryChip(
                    label = label,
                    selected = selected,
                    onClick = {
                        onUpdateFilters(sortKey, categoryState.activeYear, categoryState.activeCountry)
                    },
                )
            }

            item(key = "cat_refine_toggle") {
                val refineLabel = if (activeRefineCount > 0) "Year & Region · $activeRefineCount" else "Year & Region"
                Surface(
                    onClick = { showRefine = !showRefine },
                    shape = ClickableSurfaceDefaults.shape(CatPillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (showRefine || activeRefineCount > 0) SurfaceSelected else SurfaceTonal,
                        contentColor = if (showRefine || activeRefineCount > 0) InkOnSelected else InkSecondary,
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
                        Text(refineLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (activeRefineCount > 0 || (query != null && categoryState.activeSort != query.sort)) {
                item(key = "cat_reset") {
                    CategoryChip(
                        label = "Reset",
                        selected = false,
                        onClick = {
                            onUpdateFilters(query?.sort ?: "Hottest", "All", "All")
                        },
                    )
                }
            }
        }

        // Collapsible Year + Region Refine Rows
        AnimatedVisibility(visible = showRefine) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceTonal)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("YEAR", style = MaterialTheme.typography.labelSmall, color = InkMuted, modifier = Modifier.width(56.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        itemsIndexed(CAT_YEAR_OPTIONS, key = { idx, yr -> "cyr_${idx}_$yr" }) { _, yr ->
                            CategoryChip(
                                label = if (yr == "All") "All Years" else yr,
                                selected = categoryState.activeYear == yr,
                                compact = true,
                                onClick = {
                                    onUpdateFilters(categoryState.activeSort, yr, categoryState.activeCountry)
                                },
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("REGION", style = MaterialTheme.typography.labelSmall, color = InkMuted, modifier = Modifier.width(56.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        itemsIndexed(CAT_COUNTRY_OPTIONS, key = { idx, it -> "cct_${idx}_${it.first}" }) { _, (apiVal, label) ->
                            CategoryChip(
                                label = label,
                                selected = categoryState.activeCountry.equals(apiVal, ignoreCase = true),
                                compact = true,
                                onClick = {
                                    onUpdateFilters(categoryState.activeSort, categoryState.activeYear, apiVal)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Content Grid
        when {
            categoryState.isLoading && categoryState.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "LOADING CATEGORY...",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary,
                    )
                }
            }

            categoryState.error != null && categoryState.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = categoryState.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SignalRed,
                        )
                        Surface(
                            onClick = onRetry,
                            shape = ClickableSurfaceDefaults.shape(CatPillShape),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = SurfaceElevated,
                                contentColor = PureWhite,
                                focusedContainerColor = PureWhite,
                                focusedContentColor = InkOnWhite,
                            ),
                            border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Try Again", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            categoryState.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "No titles found for this combination.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkSecondary,
                        )
                        CategoryChip(
                            label = "Reset Filters",
                            selected = false,
                            onClick = { onUpdateFilters(query?.sort ?: "Hottest", "All", "All") },
                        )
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 132.dp),
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = categoryState.items,
                        key = { idx, it -> "cat_${idx}_${it.id}" },
                        contentType = { _, _ -> "media_card" },
                    ) { _, item ->
                        TvMediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            cardWidth = 132.dp,
                            posterHeight = 192.dp,
                        )
                    }

                    if (categoryState.hasMore) {
                        item(key = "cat_load_more", span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    onClick = onLoadMore,
                                    shape = ClickableSurfaceDefaults.shape(CatPillShape),
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
                                        text = if (categoryState.isLoading) "Loading..." else "Load More",
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
private fun CategoryChip(
    label: String,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CatPillShape),
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

package com.example.arialtv.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.ui.components.TvMediaCard
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SelectedBorderRed
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.theme.SurfaceTonal
import com.example.arialtv.ui.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    searchState: SearchUiState,
    searchHistory: List<String>,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onFilterChange: (Int) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputFocused by remember { mutableStateOf(false) }
    val searchInputFocus = remember { FocusRequester() }
    val filterAllFocus = remember { FocusRequester() }
    val filterFilmsFocus = remember { FocusRequester() }
    val filterSeriesFocus = remember { FocusRequester() }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                onQueryChange(spoken)
                onSearchSubmit(spoken)
            }
        }
    }

    val activeFilterRequester = when (searchState.selectedFilterType) {
        1 -> filterFilmsFocus
        2 -> filterSeriesFocus
        else -> filterAllFocus
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Sleek Borderless YouTube TV Search Bar + Voice Search Mic
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceTonal)
                    .then(
                        if (inputFocused) {
                            Modifier.border(2.dp, SignalRed, RoundedCornerShape(50))
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (inputFocused) PureWhite else InkMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchState.query.isEmpty()) {
                            Text(
                                text = "Search films, series, anime...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = InkMuted,
                            )
                        }
                        BasicTextField(
                            value = searchState.query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = PureWhite),
                            cursorBrush = SolidColor(SignalRed),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { onSearchSubmit(searchState.query) },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchInputFocus)
                                .focusProperties { down = activeFilterRequester }
                                .onFocusChanged { inputFocused = it.isFocused }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                                    ) {
                                        runCatching { activeFilterRequester.requestFocus() }
                                        true
                                    } else {
                                        false
                                    }
                                },
                        )
                    }
                    if (searchState.query.isNotEmpty()) {
                        Surface(
                            onClick = { onQueryChange("") },
                            shape = ClickableSurfaceDefaults.shape(CircleShape),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = SurfaceElevated,
                                contentColor = InkSecondary,
                                focusedContainerColor = PureWhite,
                                focusedContentColor = InkOnWhite,
                            ),
                            border = ClickableSurfaceDefaults.border(
                                border = Border.None,
                                focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = CircleShape),
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                            modifier = Modifier.focusProperties { down = activeFilterRequester },
                        ) {
                            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            // Voice Search Mic button (Android TV remote mic)
            Surface(
                onClick = {
                    runCatching {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a film or series title")
                        }
                        voiceLauncher.launch(intent)
                    }
                },
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = SurfaceElevated,
                    contentColor = InkPrimary,
                    focusedContainerColor = PureWhite,
                    focusedContentColor = InkOnWhite,
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border.None,
                    focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = CircleShape),
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                modifier = Modifier.focusProperties { down = activeFilterRequester },
            ) {
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Voice Search", modifier = Modifier.size(21.dp))
                }
            }

            // Submit pill
            val pillShape = RoundedCornerShape(50)
            Surface(
                onClick = { onSearchSubmit(searchState.query) },
                shape = ClickableSurfaceDefaults.shape(pillShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = SurfaceElevated,
                    contentColor = PureWhite,
                    focusedContainerColor = PureWhite,
                    focusedContentColor = InkOnWhite,
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border.None,
                    focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = pillShape),
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                modifier = Modifier.focusProperties { down = activeFilterRequester },
            ) {
                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Search", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Filter Pills (All / Films / Series) — explicitly D-pad reachable from search bar!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchFilterChip(
                label = "All",
                selected = searchState.selectedFilterType == 0,
                modifier = Modifier
                    .focusRequester(filterAllFocus)
                    .focusProperties {
                        up = searchInputFocus
                        right = filterFilmsFocus
                    },
                onClick = { onFilterChange(0) },
            )
            SearchFilterChip(
                label = "Films",
                selected = searchState.selectedFilterType == 1,
                modifier = Modifier
                    .focusRequester(filterFilmsFocus)
                    .focusProperties {
                        up = searchInputFocus
                        left = filterAllFocus
                        right = filterSeriesFocus
                    },
                onClick = { onFilterChange(1) },
            )
            SearchFilterChip(
                label = "Series",
                selected = searchState.selectedFilterType == 2,
                modifier = Modifier
                    .focusRequester(filterSeriesFocus)
                    .focusProperties {
                        up = searchInputFocus
                        left = filterFilmsFocus
                    },
                onClick = { onFilterChange(2) },
            )
        }

        // Dynamic Body:
        // 1. Live suggestions from API while typing
        // 2. Submitted search results
        // 3. Empty query: Recent Search History + Live API Trending (never hardcoded!)
        val showingLiveSuggestions = searchState.query.trim().length >= 2 &&
            !searchState.hasSearched &&
            (searchState.suggestions.isNotEmpty() || searchState.isSuggesting)

        when {
            searchState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "SEARCHING...",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary,
                    )
                }
            }

            showingLiveSuggestions -> {
                val matchingHistory = searchHistory.filter {
                    it.contains(searchState.query.trim(), ignoreCase = true)
                }.take(3)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 28.dp),
                ) {
                    item(key = "suggest_header") {
                        Text(
                            text = if (searchState.isSuggesting) "FINDING SUGGESTIONS..." else "SUGGESTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }

                    items(matchingHistory, key = { "mh_$it" }, contentType = { "query_row" }) { past ->
                        SuggestionQueryRow(
                            query = past,
                            isHistory = true,
                            onClick = { onSearchSubmit(past) },
                        )
                    }

                    items(searchState.suggestions, key = { "sug_${it.id}" }, contentType = { "title_row" }) { item ->
                        LiveTitleSuggestionRow(
                            item = item,
                            onClick = { onItemClick(item) },
                        )
                    }

                    item(key = "search_all_row") {
                        SuggestionQueryRow(
                            query = "See all results for \"${searchState.query.trim()}\"",
                            isHistory = false,
                            onClick = { onSearchSubmit(searchState.query) },
                        )
                    }
                }
            }

            searchState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(searchState.error, style = MaterialTheme.typography.bodyLarge, color = SignalRed)
                }
            }

            searchState.hasSearched && searchState.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No results for \"${searchState.query}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary,
                    )
                }
            }

            searchState.results.isNotEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "RESULTS  ·  ${searchState.results.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary,
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 132.dp),
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(searchState.results, key = { "sr_${it.id}" }, contentType = { "media_card" }) { item ->
                        TvMediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            cardWidth = 132.dp,
                            posterHeight = 192.dp,
                        )
                    }
                }
            }

            else -> {
                val filteredTrending = remember(searchState.trendingItems, searchState.selectedFilterType) {
                    when (searchState.selectedFilterType) {
                        1 -> searchState.trendingItems.filter { !it.isSeries }
                        2 -> searchState.trendingItems.filter { it.isSeries }
                        else -> searchState.trendingItems
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    if (searchHistory.isNotEmpty()) {
                        item(key = "recent_searches") {
                            Column(
                                modifier = Modifier.padding(horizontal = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "RECENT SEARCHES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSecondary,
                                    )
                                    val clearShape = RoundedCornerShape(50)
                                    Surface(
                                        onClick = onClearHistory,
                                        shape = ClickableSurfaceDefaults.shape(clearShape),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = SurfaceTonal,
                                            contentColor = InkSecondary,
                                            focusedContainerColor = PureWhite,
                                            focusedContentColor = InkOnWhite,
                                        ),
                                        border = ClickableSurfaceDefaults.border(
                                            border = Border.None,
                                            focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = clearShape),
                                        ),
                                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                    ) {
                                        Text(
                                            text = "Clear History",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    searchHistory.take(8).forEach { past ->
                                        RecentHistoryRow(
                                            query = past,
                                            onSelect = { onSearchSubmit(past) },
                                            onRemove = { onRemoveHistoryItem(past) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live API Trending Search Terms (edge-to-edge LazyRow)
                    if (searchState.trendingTerms.isNotEmpty()) {
                        item(key = "api_trending_terms") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "TRENDING SEARCHES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(searchState.trendingTerms, key = { "tt_$it" }) { term ->
                                        SearchFilterChip(label = term, selected = false) {
                                            onSearchSubmit(term)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live API Trending Titles from `/subject-api/search-rank` (edge-to-edge LazyRow + soft right fade)
                    if (filteredTrending.isNotEmpty()) {
                        item(key = "api_trending_titles") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "TRENDING NOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        items(filteredTrending, key = { "tr_${it.id}" }, contentType = { "media_card" }) { item ->
                                            TvMediaCard(
                                                item = item,
                                                onClick = { onItemClick(item) },
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(42.dp)
                                            .height(248.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(Color.Transparent, CanvasBlack.copy(alpha = 0.88f)),
                                                ),
                                            ),
                                    )
                                }
                            }
                        }
                    } else if (searchHistory.isEmpty() && !searchState.isLoadingTrending) {
                        item(key = "empty_search_hint") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Type a title above to search films and series.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkMuted,
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
private fun SearchFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SuggestionQueryRow(
    query: String,
    isHistory: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceTonal,
            contentColor = InkPrimary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (isHistory) Icons.Rounded.History else Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Rounded.NorthWest,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun LiveTitleSuggestionRow(
    item: MediaItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceTonal,
            contentColor = InkPrimary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(SurfaceElevated),
            ) {
                if (item.posterUrl.isNotBlank()) {
                    val req = remember(item.posterUrl) {
                        ImageRequest.Builder(context)
                            .data(item.posterUrl)
                            .size(120, 170)
                            .crossfade(150)
                            .build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildList {
                    add(item.mediaTypeLabel)
                    if (item.year.isNotBlank()) add(item.year)
                    if (item.rating.isNotBlank()) add("★ ${item.rating}")
                }.joinToString("  ·  ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                )
            }
        }
    }
}

@Composable
private fun RecentHistoryRow(
    query: String,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val rowShape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = onSelect,
            shape = ClickableSurfaceDefaults.shape(rowShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = SurfaceTonal,
                contentColor = InkPrimary,
                focusedContainerColor = PureWhite,
                focusedContentColor = InkOnWhite,
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border.None,
                focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = rowShape),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(
            onClick = onRemove,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = SurfaceTonal,
                contentColor = InkMuted,
                focusedContainerColor = SignalRed,
                focusedContentColor = PureWhite,
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border.None,
                focusedBorder = Border(BorderStroke(2.dp, PureWhite), shape = CircleShape),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(17.dp))
            }
        }
    }
}

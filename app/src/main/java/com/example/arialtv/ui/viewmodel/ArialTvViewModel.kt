package com.example.arialtv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.arialtv.data.model.AppMetadata
import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.AspectRatioMode
import com.example.arialtv.data.model.CatalogLanguage
import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.ConnectionMode
import com.example.arialtv.data.model.ConnectionTestResult
import com.example.arialtv.data.model.DubOption
import com.example.arialtv.data.model.EpisodeProgress
import com.example.arialtv.data.model.HomeCatalog
import com.example.arialtv.data.model.MediaDetails
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.PreferredAudioLanguage
import com.example.arialtv.data.model.PreferredCodec
import com.example.arialtv.data.model.PreferredQuality
import com.example.arialtv.data.model.ResolvedPlayback
import com.example.arialtv.data.model.SeasonInfo
import com.example.arialtv.data.model.SubtitleStyle
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.data.repository.MediaRepository
import com.example.arialtv.data.storage.AppPreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class TvNavTab(val label: String) {
    SEARCH("Search"),
    HOME("Home"),
    MOVIES("Films"),
    SERIES("Series"),
    LIBRARY("My List"),
    SETTINGS("Settings"),
}

sealed interface AppDestination {
    data object Browse : AppDestination
    data class Details(val item: MediaItem) : AppDestination
    data class Player(
        val item: MediaItem,
        val season: Int = 0,
        val episode: Int = 0,
        val startPositionMs: Long = 0L,
        val seasonsMeta: List<SeasonInfo> = emptyList(),
        val dubsMeta: List<DubOption> = emptyList(),
        val explicitDubSubjectId: String? = null,
    ) : AppDestination
}

data class CatalogTabState(
    val isLoading: Boolean = false,
    val catalog: HomeCatalog? = null,
    val focusedItem: MediaItem? = null,
    val focusedCategory: String = "Featured",
    val error: String? = null,
    val activeGenre: String = "All",
    val activeYear: String = "All",
    val activeSort: String = "ForYou",
    val activeCountry: String = "All",
    val isFiltering: Boolean = false,
    val filteredItems: List<MediaItem> = emptyList(),
    val filterPage: Int = 1,
    val filterHasMore: Boolean = false,
    val filterError: String? = null,
) {
    val isFilterActive: Boolean
        get() = activeGenre != "All" || activeYear != "All" || activeSort != "ForYou" || activeCountry != "All"
}

data class SearchUiState(
    val query: String = "",
    val selectedFilterType: Int = 0, // 0 = All, 1 = Movies, 2 = Series
    val isLoading: Boolean = false,
    val isSuggesting: Boolean = false,
    val suggestions: List<MediaItem> = emptyList(),
    val results: List<MediaItem> = emptyList(),
    val hasSearched: Boolean = false,
    val error: String? = null,
    val trendingItems: List<MediaItem> = emptyList(),
    val trendingTerms: List<String> = emptyList(),
    val isLoadingTrending: Boolean = false,
    val hasLoadedTrending: Boolean = false,
)

data class DetailUiState(
    val isLoading: Boolean = false,
    val details: MediaDetails? = null,
    val similarItems: List<MediaItem> = emptyList(),
    val selectedSeason: Int = 1,
    val error: String? = null,
)

data class PlayerUiState(
    val isLoading: Boolean = false,
    val playback: ResolvedPlayback? = null,
    val activeStreamIndex: Int = 0,
    val selectedSubtitleId: String? = null,
    val error: String? = null,
)

data class SettingsUiState(
    val isTesting: Boolean = false,
    val testResult: ConnectionTestResult? = null,
    val isCheckingUpdate: Boolean = false,
    val updateStatusMessage: String? = null,
)

data class ArialTvUiState(
    val activeTab: TvNavTab = TvNavTab.HOME,
    val destination: AppDestination = AppDestination.Browse,
    val showQuickMenu: Boolean = false,
    val showExitDialog: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val homeState: CatalogTabState = CatalogTabState(isLoading = true),
    val moviesState: CatalogTabState = CatalogTabState(),
    val seriesState: CatalogTabState = CatalogTabState(),
    val searchState: SearchUiState = SearchUiState(),
    val detailState: DetailUiState = DetailUiState(),
    val playerState: PlayerUiState = PlayerUiState(),
    val settingsState: SettingsUiState = SettingsUiState(),
    val watchHistory: List<WatchProgress> = emptyList(),
    val episodeProgress: Map<String, EpisodeProgress> = emptyMap(),
    val searchHistory: List<String> = emptyList(),
    val watchlist: List<MediaItem> = emptyList(),
    val recommendedRow: CatalogRow? = null,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerExpired: Boolean = false,
    val toastMessage: String? = null,
)

class ArialTvViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val repository = MediaRepository()
    private val safeHandler = CoroutineExceptionHandler { _, _ -> }

    private val _sleepTimerRemainingSec = MutableStateFlow(0)
    val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerRemainingSec.asStateFlow()

    private val _uiState = MutableStateFlow(
        ArialTvUiState(
            settings = preferences.loadSettings(),
            watchHistory = preferences.loadWatchHistory(),
            episodeProgress = preferences.loadEpisodeProgressMap(),
            searchHistory = preferences.loadSearchHistory(),
            watchlist = preferences.loadWatchlist(),
        ),
    )
    val uiState: StateFlow<ArialTvUiState> = _uiState.asStateFlow()

    private var backStack = ArrayDeque<AppDestination>()
    private var detailJob: Job? = null
    private var playerJob: Job? = null
    private var searchJob: Job? = null
    private var suggestJob: Job? = null
    private var trendingJob: Job? = null
    private var recommendationJob: Job? = null
    private var toastJob: Job? = null
    private val filterJobs = mutableMapOf<TvNavTab, Job>()
    private var sleepTimerJob: Job? = null

    init {
        loadCatalogForTab(TvNavTab.HOME, forceRefresh = true)
        loadPersonalRecommendations()
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(2_400L)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    private fun loadPersonalRecommendations() {
        val anchor = _uiState.value.watchHistory.firstOrNull()?.item ?: return
        recommendationJob?.cancel()
        val settings = _uiState.value.settings
        recommendationJob = viewModelScope.launch {
            val similar = runCatching { repository.fetchSimilarItems(settings, anchor) }
                .getOrDefault(emptyList())
            if (similar.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        recommendedRow = CatalogRow(
                            category = "Because You Watched ${anchor.title}",
                            items = similar,
                        ),
                    )
                }
            }
        }
    }

    fun selectTab(tab: TvNavTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                destination = AppDestination.Browse,
                showExitDialog = false,
            )
        }
        backStack.clear()
        when (tab) {
            TvNavTab.HOME -> {
                if (_uiState.value.homeState.catalog == null && !_uiState.value.homeState.isLoading) {
                    loadCatalogForTab(TvNavTab.HOME)
                }
            }
            TvNavTab.MOVIES -> {
                if (_uiState.value.moviesState.catalog == null && !_uiState.value.moviesState.isLoading) {
                    loadCatalogForTab(TvNavTab.MOVIES)
                }
            }
            TvNavTab.SERIES -> {
                if (_uiState.value.seriesState.catalog == null && !_uiState.value.seriesState.isLoading) {
                    loadCatalogForTab(TvNavTab.SERIES)
                }
            }
            TvNavTab.SEARCH -> {
                if (!_uiState.value.searchState.hasLoadedTrending && !_uiState.value.searchState.isLoadingTrending) {
                    loadTrendingSearch()
                }
            }
            TvNavTab.LIBRARY, TvNavTab.SETTINGS -> Unit
        }
    }

    fun refreshCurrentTab() {
        when (val tab = _uiState.value.activeTab) {
            TvNavTab.HOME, TvNavTab.MOVIES, TvNavTab.SERIES -> {
                val tabState = getTabState(tab)
                if (tabState.isFilterActive) {
                    applyCatalogFilter(
                        tab = tab,
                        genre = tabState.activeGenre,
                        year = tabState.activeYear,
                        sort = tabState.activeSort,
                        country = tabState.activeCountry,
                    )
                } else {
                    loadCatalogForTab(tab, forceRefresh = true)
                }
            }
            TvNavTab.SEARCH -> {
                val q = _uiState.value.searchState.query
                if (q.isNotBlank()) performSearch(q) else loadTrendingSearch(force = true)
            }
            TvNavTab.SETTINGS -> runConnectionTest()
            TvNavTab.LIBRARY -> Unit
        }
    }

    private fun getTabState(tab: TvNavTab): CatalogTabState = when (tab) {
        TvNavTab.HOME -> _uiState.value.homeState
        TvNavTab.MOVIES -> _uiState.value.moviesState
        TvNavTab.SERIES -> _uiState.value.seriesState
        else -> _uiState.value.homeState
    }

    fun applyCatalogFilter(
        tab: TvNavTab,
        genre: String = getTabState(tab).activeGenre,
        year: String = getTabState(tab).activeYear,
        sort: String = getTabState(tab).activeSort,
        country: String = getTabState(tab).activeCountry,
    ) {
        val isAll = genre == "All" && year == "All" && sort == "ForYou" && country == "All"
        filterJobs[tab]?.cancel()
        if (isAll) {
            updateTabState(tab) {
                it.copy(
                    activeGenre = "All",
                    activeYear = "All",
                    activeSort = "ForYou",
                    activeCountry = "All",
                    isFiltering = false,
                    filteredItems = emptyList(),
                    filterPage = 1,
                    filterHasMore = false,
                    filterError = null,
                )
            }
            return
        }

        updateTabState(tab) {
            it.copy(
                activeGenre = genre,
                activeYear = year,
                activeSort = sort,
                activeCountry = country,
                isFiltering = true,
                filteredItems = emptyList(),
                filterPage = 1,
                filterHasMore = false,
                filterError = null,
            )
        }

        val settings = _uiState.value.settings
        val channelId = if (tab == TvNavTab.SERIES) 2 else 1
        filterJobs[tab] = viewModelScope.launch {
            try {
                val result = repository.fetchFilteredCatalog(
                    settings = settings,
                    channelId = channelId,
                    genre = genre.takeUnless { it == "All" },
                    year = year.takeUnless { it == "All" },
                    country = country.takeUnless { it == "All" },
                    sort = sort,
                    page = 1,
                    perPage = 20,
                )
                updateTabState(tab) {
                    it.copy(
                        isFiltering = false,
                        filteredItems = result.items,
                        filterPage = 1,
                        filterHasMore = result.hasMore,
                        filterError = null,
                        focusedItem = result.items.firstOrNull() ?: it.focusedItem,
                        focusedCategory = listOfNotNull(
                            genre.takeUnless { g -> g == "All" },
                            year.takeUnless { y -> y == "All" },
                            sort.takeUnless { s -> s == "ForYou" },
                        ).joinToString(" · ").ifBlank { "Filtered" },
                    )
                }
            } catch (_: Exception) {
                updateTabState(tab) {
                    it.copy(
                        isFiltering = false,
                        filterError = "Couldn't load filtered titles. Try again.",
                    )
                }
            }
        }
    }

    fun loadMoreFilteredItems(tab: TvNavTab) {
        val current = getTabState(tab)
        if (!current.isFilterActive || current.isFiltering || !current.filterHasMore) return
        val nextPage = current.filterPage + 1
        val settings = _uiState.value.settings
        val channelId = if (tab == TvNavTab.SERIES) 2 else 1

        updateTabState(tab) { it.copy(isFiltering = true, filterError = null) }
        filterJobs[tab] = viewModelScope.launch {
            try {
                val result = repository.fetchFilteredCatalog(
                    settings = settings,
                    channelId = channelId,
                    genre = current.activeGenre.takeUnless { it == "All" },
                    year = current.activeYear.takeUnless { it == "All" },
                    country = current.activeCountry.takeUnless { it == "All" },
                    sort = current.activeSort,
                    page = nextPage,
                    perPage = 20,
                )
                updateTabState(tab) {
                    val combined = (it.filteredItems + result.items).distinctBy { item -> item.id }
                    it.copy(
                        isFiltering = false,
                        filteredItems = combined,
                        filterPage = nextPage,
                        filterHasMore = result.hasMore,
                        filterError = null,
                    )
                }
            } catch (_: Exception) {
                updateTabState(tab) { it.copy(isFiltering = false) }
            }
        }
    }

    fun clearCatalogFilter(tab: TvNavTab) {
        applyCatalogFilter(tab = tab, genre = "All", year = "All", sort = "ForYou", country = "All")
    }

    fun loadTrendingSearch(force: Boolean = false) {
        if (!force && _uiState.value.searchState.hasLoadedTrending) return
        trendingJob?.cancel()
        _uiState.update {
            it.copy(searchState = it.searchState.copy(isLoadingTrending = true))
        }
        val settings = _uiState.value.settings
        trendingJob = viewModelScope.launch {
            try {
                val rank = repository.fetchTrendingSearch(settings)
                val combined = buildList {
                    val maxLen = maxOf(rank.movies.size, rank.series.size)
                    for (i in 0 until maxLen) {
                        if (i < rank.movies.size) add(rank.movies[i])
                        if (i < rank.series.size) add(rank.series[i])
                    }
                }.distinctBy { it.id }.take(14)

                _uiState.update {
                    it.copy(
                        searchState = it.searchState.copy(
                            trendingItems = combined,
                            trendingTerms = rank.everyoneSearch.take(10),
                            isLoadingTrending = false,
                            hasLoadedTrending = true,
                        ),
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        searchState = it.searchState.copy(
                            isLoadingTrending = false,
                            hasLoadedTrending = true,
                        ),
                    )
                }
            }
        }
    }

    fun loadCatalogForTab(tab: TvNavTab, forceRefresh: Boolean = false) {
        val currentTabState = when (tab) {
            TvNavTab.HOME -> _uiState.value.homeState
            TvNavTab.MOVIES -> _uiState.value.moviesState
            TvNavTab.SERIES -> _uiState.value.seriesState
            else -> return
        }
        if (!forceRefresh && currentTabState.catalog != null) return

        updateTabState(tab) { it.copy(isLoading = true, error = null) }
        val settings = _uiState.value.settings

        viewModelScope.launch {
            try {
                val catalog = when (tab) {
                    TvNavTab.HOME -> repository.fetchHome(settings, tabId = 0, forceRefresh = forceRefresh)
                    TvNavTab.MOVIES -> repository.fetchMoviesCatalog(settings, forceRefresh = forceRefresh)
                    TvNavTab.SERIES -> repository.fetchSeriesCatalog(settings, forceRefresh = forceRefresh)
                    else -> return@launch
                }
                val initialHero = catalog.featured.firstOrNull()
                    ?: catalog.rows.firstOrNull()?.items?.firstOrNull()
                val initialCategory = if (catalog.featured.isNotEmpty()) {
                    "Featured"
                } else {
                    catalog.rows.firstOrNull()?.category ?: "Browse"
                }
                updateTabState(tab) {
                    it.copy(
                        isLoading = false,
                        catalog = catalog,
                        focusedItem = initialHero,
                        focusedCategory = initialCategory,
                        error = null,
                    )
                }
                if (forceRefresh && currentTabState.catalog != null) {
                    showToast("Catalog refreshed")
                }
            } catch (e: Exception) {
                updateTabState(tab) {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't reach catalog. Check your internet connection.",
                    )
                }
            }
        }
    }

    fun cycleHeroFeatured(tab: TvNavTab) {
        val state = getTabState(tab)
        val featured = state.catalog?.featured.orEmpty()
        if (featured.size < 2) return
        val currentIdx = featured.indexOfFirst { it.id == state.focusedItem?.id }
        val next = featured[(currentIdx + 1) % featured.size]
        updateTabState(tab) {
            it.copy(focusedItem = next, focusedCategory = "Featured")
        }
    }

    fun onCardFocused(tab: TvNavTab, item: MediaItem, category: String) {
        updateTabState(tab) {
            it.copy(focusedItem = item, focusedCategory = category)
        }
    }

    // --- Search, Live Suggestions & Search History ---

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchState = it.searchState.copy(
                    query = query,
                    hasSearched = if (query.isBlank()) false else it.searchState.hasSearched,
                    results = if (query.isBlank()) emptyList() else it.searchState.results,
                    suggestions = if (query.trim().length < 2) emptyList() else it.searchState.suggestions,
                    isSuggesting = query.trim().length >= 2,
                ),
            )
        }
        suggestJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) return

        val settings = _uiState.value.settings
        val filterType = _uiState.value.searchState.selectedFilterType
        suggestJob = viewModelScope.launch {
            delay(320L)
            try {
                val items = repository.search(settings, trimmed, subjectType = filterType, perPage = 8)
                _uiState.update {
                    if (it.searchState.query.trim() == trimmed) {
                        it.copy(
                            searchState = it.searchState.copy(
                                suggestions = items.take(6),
                                isSuggesting = false,
                            ),
                        )
                    } else {
                        it
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(searchState = it.searchState.copy(isSuggesting = false))
                }
            }
        }
    }

    fun updateSearchFilter(subjectType: Int) {
        _uiState.update { it.copy(searchState = it.searchState.copy(selectedFilterType = subjectType)) }
        val q = _uiState.value.searchState.query
        if (q.isNotBlank()) {
            performSearch(q, subjectType)
        }
    }

    fun performSearch(
        query: String = _uiState.value.searchState.query,
        filterType: Int = _uiState.value.searchState.selectedFilterType,
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        suggestJob?.cancel()
        searchJob?.cancel()

        val updatedHistory = preferences.addSearchHistory(trimmed)
        _uiState.update {
            it.copy(
                searchHistory = updatedHistory,
                searchState = it.searchState.copy(
                    query = trimmed,
                    selectedFilterType = filterType,
                    isLoading = true,
                    isSuggesting = false,
                    suggestions = emptyList(),
                    hasSearched = true,
                    error = null,
                ),
            )
        }
        val settings = _uiState.value.settings
        searchJob = viewModelScope.launch {
            try {
                val results = repository.search(settings, trimmed, subjectType = filterType)
                _uiState.update {
                    it.copy(
                        searchState = it.searchState.copy(
                            isLoading = false,
                            results = results,
                            error = null,
                        ),
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        searchState = it.searchState.copy(
                            isLoading = false,
                            error = "Search timed out. Try again.",
                        ),
                    )
                }
            }
        }
    }

    fun removeSearchHistoryItem(query: String) {
        val updated = preferences.removeSearchHistory(query)
        _uiState.update { it.copy(searchHistory = updated) }
    }

    fun clearSearchHistory() {
        preferences.clearSearchHistory()
        _uiState.update { it.copy(searchHistory = emptyList()) }
        showToast("Search history cleared")
    }

    // --- Watchlist / My List ---

    fun toggleWatchlist(item: MediaItem) {
        val existed = _uiState.value.watchlist.any { it.id == item.id }
        val updated = preferences.toggleWatchlist(item)
        _uiState.update { it.copy(watchlist = updated) }
        showToast(if (existed) "Removed from My List" else "Added to My List")
    }

    fun clearWatchlist() {
        preferences.clearWatchlist()
        _uiState.update { it.copy(watchlist = emptyList()) }
        showToast("My List cleared")
    }

    // --- Details & Seasons ---

    fun openDetails(item: MediaItem) {
        val current = _uiState.value.destination
        if (current !is AppDestination.Details || current.item.id != item.id) {
            backStack.addLast(current)
        }
        _uiState.update {
            it.copy(
                destination = AppDestination.Details(item),
                detailState = DetailUiState(
                    isLoading = true,
                    details = MediaDetails(item = item),
                    selectedSeason = 1,
                    error = null,
                ),
            )
        }
        loadDetailsFor(item)
    }

    fun switchDub(dubSubjectId: String) {
        val currentDetails = _uiState.value.detailState.details ?: return
        if (currentDetails.item.id == dubSubjectId) return
        val switchedItem = currentDetails.item.copy(id = dubSubjectId)
        _uiState.update {
            it.copy(
                destination = AppDestination.Details(switchedItem),
                detailState = it.detailState.copy(isLoading = true, error = null),
            )
        }
        loadDetailsFor(switchedItem)
    }

    fun selectSeason(seasonNumber: Int) {
        _uiState.update {
            it.copy(detailState = it.detailState.copy(selectedSeason = seasonNumber))
        }
    }

    private fun loadDetailsFor(item: MediaItem) {
        detailJob?.cancel()
        val settings = _uiState.value.settings
        detailJob = viewModelScope.launch {
            try {
                val details = repository.fetchDetails(settings, item)
                val historyEntry = _uiState.value.watchHistory.firstOrNull { it.item.id == details.item.id }
                val defaultSeason = historyEntry?.season?.takeIf { it > 0 }
                    ?: details.seasons.firstOrNull()?.seasonNumber
                    ?: 1
                _uiState.update {
                    it.copy(
                        detailState = DetailUiState(
                            isLoading = false,
                            details = details,
                            similarItems = it.detailState.similarItems,
                            selectedSeason = defaultSeason,
                            error = null,
                        ),
                    )
                }
                // Load live "More Like This" recommendations right after details arrive
                val similar = runCatching { repository.fetchSimilarItems(settings, details.item) }
                    .getOrDefault(emptyList())
                _uiState.update {
                    if (it.detailState.details?.item?.id == details.item.id) {
                        it.copy(detailState = it.detailState.copy(similarItems = similar))
                    } else {
                        it
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        detailState = it.detailState.copy(
                            isLoading = false,
                            error = "Couldn't load extra details right now.",
                        ),
                    )
                }
            }
        }
    }

    // --- Playback & In-Player Episodes ---

    fun startPlayback(
        item: MediaItem,
        season: Int = 0,
        episode: Int = 0,
        resumeFromHistory: Boolean = true,
        seasonsMeta: List<SeasonInfo> = emptyList(),
        requestedResolution: Int? = null,
    ) {
        val history = _uiState.value.watchHistory.firstOrNull { it.item.id == item.id }
        val resolvedSeason = when {
            season > 0 -> season
            item.isSeries && history != null && history.season > 0 -> history.season
            item.isSeries -> seasonsMeta.firstOrNull()?.seasonNumber ?: 1
            else -> 0
        }
        val resolvedEpisode = when {
            episode > 0 -> episode
            item.isSeries && history != null && history.episode > 0 -> history.episode
            item.isSeries -> 1
            else -> 0
        }

        val epKey = AppPreferences.episodeKey(item.id, resolvedSeason, resolvedEpisode)
        val epSaved = _uiState.value.episodeProgress[epKey]
        val startPos = when {
            !resumeFromHistory -> 0L
            item.isSeries && epSaved != null && !epSaved.isWatched -> epSaved.positionMs
            history != null && history.season == resolvedSeason && history.episode == resolvedEpisode && history.progressFraction < 0.94f -> history.positionMs
            else -> 0L
        }

        val currentDest = _uiState.value.destination
        if (currentDest !is AppDestination.Player) {
            backStack.addLast(currentDest)
        }

        val detailSnapshot = _uiState.value.detailState.details?.takeIf { it.item.id == item.id }
        val effectiveSeasons = seasonsMeta.ifEmpty { detailSnapshot?.seasons.orEmpty() }
        val effectiveDubs = detailSnapshot?.dubs.orEmpty()
        // If user explicitly picked a dub on DetailScreen (i.e. item.id matches one of the dubs), honor it
        val explicitFromDetails = detailSnapshot?.dubs?.firstOrNull { it.subjectId == item.id }?.subjectId

        val playerDest = AppDestination.Player(
            item = item,
            season = resolvedSeason,
            episode = resolvedEpisode,
            startPositionMs = startPos,
            seasonsMeta = effectiveSeasons,
            dubsMeta = effectiveDubs,
            explicitDubSubjectId = explicitFromDetails,
        )

        _uiState.update {
            it.copy(
                destination = playerDest,
                sleepTimerExpired = false,
                playerState = PlayerUiState(isLoading = true, playback = null, error = null),
            )
        }

        resolveStreamFor(playerDest, requestedResolution)
    }

    /**
     * Switches to another dubbed audio language directly inside the player while preserving
     * the exact playback timestamp.
     */
    fun switchPlayerAudioDub(dubSubjectId: String, resumePositionMs: Long) {
        val dest = _uiState.value.destination as? AppDestination.Player ?: return
        val currentPlayback = _uiState.value.playerState.playback
        if (currentPlayback?.subjectId == dubSubjectId) return

        val dubs = currentPlayback?.dubs?.ifEmpty { dest.dubsMeta } ?: dest.dubsMeta
        val targetDub = dubs.firstOrNull { it.subjectId == dubSubjectId }

        val updatedDest = dest.copy(
            startPositionMs = resumePositionMs.coerceAtLeast(0L),
            dubsMeta = dubs,
            explicitDubSubjectId = dubSubjectId,
        )
        _uiState.update {
            it.copy(
                destination = updatedDest,
                playerState = it.playerState.copy(isLoading = true, error = null),
            )
        }
        targetDub?.let { showToast("Switching audio to ${it.languageName}") }
        resolveStreamFor(updatedDest, requestedResolution = null)
    }

    fun playEpisodeFromPlayer(season: Int, episode: Int) {
        val dest = _uiState.value.destination as? AppDestination.Player ?: return
        startPlayback(
            item = dest.item,
            season = season,
            episode = episode,
            resumeFromHistory = true,
            seasonsMeta = dest.seasonsMeta,
        )
    }

    fun playPreviousEpisode() {
        val dest = _uiState.value.destination as? AppDestination.Player ?: return
        if (!dest.item.isSeries) return
        val seasons = dest.seasonsMeta
        val (prevSe, prevEp) = when {
            dest.episode > 1 -> dest.season to (dest.episode - 1)
            seasons.any { it.seasonNumber < dest.season } -> {
                val prior = seasons.last { it.seasonNumber < dest.season }
                prior.seasonNumber to prior.episodeCount
            }
            else -> return
        }
        startPlayback(
            item = dest.item,
            season = prevSe,
            episode = prevEp,
            resumeFromHistory = false,
            seasonsMeta = seasons,
        )
    }

    fun playNextEpisode() {
        val dest = _uiState.value.destination as? AppDestination.Player ?: return
        if (!dest.item.isSeries) return
        val seasons = dest.seasonsMeta
        val currentSeasonInfo = seasons.firstOrNull { it.seasonNumber == dest.season }
        val (nextSeason, nextEpisode) = when {
            currentSeasonInfo != null && dest.episode < currentSeasonInfo.episodeCount -> {
                dest.season to (dest.episode + 1)
            }
            seasons.any { it.seasonNumber > dest.season } -> {
                val nextSe = seasons.first { it.seasonNumber > dest.season }
                nextSe.seasonNumber to 1
            }
            else -> dest.season to (dest.episode + 1)
        }
        startPlayback(
            item = dest.item,
            season = nextSeason,
            episode = nextEpisode,
            resumeFromHistory = false,
            seasonsMeta = seasons,
        )
    }

    fun retryPlayback(requestedResolution: Int? = null) {
        val dest = _uiState.value.destination as? AppDestination.Player ?: return
        _uiState.update {
            it.copy(playerState = it.playerState.copy(isLoading = true, error = null))
        }
        resolveStreamFor(dest, requestedResolution)
    }

    fun selectStreamQuality(index: Int) {
        val playback = _uiState.value.playerState.playback ?: return
        if (index in playback.availableStreams.indices) {
            _uiState.update {
                it.copy(
                    playerState = it.playerState.copy(
                        activeStreamIndex = index,
                        playback = playback.copy(selectedStream = playback.availableStreams[index]),
                        error = null,
                    ),
                )
            }
        }
    }

    fun selectSubtitle(subtitleId: String?) {
        _uiState.update {
            it.copy(playerState = it.playerState.copy(selectedSubtitleId = subtitleId))
        }
    }

    private fun resolveStreamFor(dest: AppDestination.Player, requestedResolution: Int?) {
        playerJob?.cancel()
        val settings = _uiState.value.settings
        playerJob = viewModelScope.launch {
            try {
                val seasons = if (dest.item.isSeries && dest.seasonsMeta.isEmpty() && settings.connectionMode == ConnectionMode.STANDALONE_V3) {
                    runCatching { repository.fetchDetails(settings, dest.item).seasons }.getOrDefault(emptyList())
                } else {
                    dest.seasonsMeta
                }

                val resolved = repository.resolvePlayback(
                    settings = settings,
                    item = dest.item,
                    season = dest.season,
                    episode = dest.episode,
                    seasonsMeta = seasons,
                    dubsMeta = dest.dubsMeta,
                    explicitDubSubjectId = dest.explicitDubSubjectId,
                    requestedResolution = requestedResolution,
                )
                val selectedIdx = resolved.availableStreams
                    .indexOfFirst { it.url == resolved.selectedStream.url }
                    .coerceAtLeast(0)

                val preferredLang = settings.language.code
                val defaultSub = resolved.subtitles.firstOrNull {
                    it.languageCode.equals(preferredLang, ignoreCase = true)
                }?.id

                _uiState.update {
                    it.copy(
                        destination = dest.copy(seasonsMeta = seasons, dubsMeta = resolved.dubs),
                        playerState = PlayerUiState(
                            isLoading = false,
                            playback = resolved,
                            activeStreamIndex = selectedIdx,
                            selectedSubtitleId = defaultSub,
                            error = null,
                        ),
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        playerState = PlayerUiState(
                            isLoading = false,
                            playback = null,
                            error = "Couldn't start playback. Try retrying or choosing another quality.",
                        ),
                    )
                }
            }
        }
    }

    fun recordPlaybackProgress(positionMs: Long, durationMs: Long) {
        val dest = _uiState.value.destination as? AppDestination.Player ?: return
        if (positionMs < 3_000L || durationMs <= 0L) return
        val prevTopId = _uiState.value.watchHistory.firstOrNull()?.item?.id
        val updatedHistory = preferences.saveWatchProgress(
            WatchProgress(
                item = dest.item,
                season = dest.season,
                episode = dest.episode,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
        val updatedEpMap = if (dest.season > 0 && dest.episode > 0) {
            preferences.saveEpisodeProgress(dest.item.id, dest.season, dest.episode, positionMs, durationMs)
        } else {
            _uiState.value.episodeProgress
        }
        _uiState.update { it.copy(watchHistory = updatedHistory, episodeProgress = updatedEpMap) }
        if (prevTopId != dest.item.id) {
            loadPersonalRecommendations()
        }
    }

    // --- Sleep Timer / Timeout ---

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSec.value = 0
            _uiState.update {
                it.copy(sleepTimerMinutes = 0, sleepTimerExpired = false)
            }
            showToast("Sleep timer off")
            return
        }
        val totalSec = minutes * 60
        _sleepTimerRemainingSec.value = totalSec
        _uiState.update {
            it.copy(sleepTimerMinutes = minutes, sleepTimerExpired = false)
        }
        showToast("Sleep timer: ${AppMetadata.formatSleepLongLabel(minutes)}")
        sleepTimerJob = viewModelScope.launch(safeHandler) {
            var remaining = totalSec
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                _sleepTimerRemainingSec.value = remaining
            }
            _sleepTimerRemainingSec.value = 0
            _uiState.update {
                it.copy(sleepTimerMinutes = 0, sleepTimerExpired = true)
            }
        }
    }

    fun dismissSleepTimerExpired() {
        _uiState.update { it.copy(sleepTimerExpired = false) }
    }

    // --- Navigation, Remote MENU Key & Exit Confirmation ---

    fun toggleQuickMenu() {
        _uiState.update { it.copy(showQuickMenu = !it.showQuickMenu, showExitDialog = false) }
    }

    fun dismissQuickMenu() {
        _uiState.update { it.copy(showQuickMenu = false) }
    }

    fun requestExitDialog() {
        _uiState.update { it.copy(showQuickMenu = false, showExitDialog = true) }
    }

    fun cycleSleepTimer() {
        val options = AppMetadata.SLEEP_TIMER_OPTIONS
        val idx = options.indexOf(_uiState.value.sleepTimerMinutes)
        val next = options[(idx + 1) % options.size]
        setSleepTimer(next)
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.settings.language == CatalogLanguage.ENGLISH) {
            CatalogLanguage.FRENCH
        } else {
            CatalogLanguage.ENGLISH
        }
        updateLanguage(next)
    }

    fun handleBackPress() {
        val state = _uiState.value
        when {
            state.showQuickMenu -> dismissQuickMenu()
            state.showExitDialog -> dismissExitDialog()
            state.destination !is AppDestination.Browse -> navigateBack()
            state.activeTab != TvNavTab.HOME -> selectTab(TvNavTab.HOME)
            else -> _uiState.update { it.copy(showExitDialog = true) }
        }
    }

    fun dismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }

    fun navigateBack(): Boolean {
        if (_uiState.value.destination is AppDestination.Browse) {
            return false
        }
        playerJob?.cancel()
        val previous = backStack.removeLastOrNull() ?: AppDestination.Browse
        _uiState.update {
            it.copy(
                destination = previous,
                playerState = PlayerUiState(),
            )
        }
        return true
    }

    // --- Settings ---

    fun updateConnectionMode(mode: ConnectionMode) {
        updateSettings { it.copy(connectionMode = mode) }
        invalidateCatalogs()
    }

    fun updateBridgeBaseUrl(url: String) {
        updateSettings { it.copy(bridgeBaseUrl = url) }
    }

    fun updateLanguage(language: CatalogLanguage) {
        updateSettings { it.copy(language = language) }
        showToast("Catalog language: ${language.label}")
        invalidateCatalogs()
    }

    fun updatePreferredAudioLanguage(audioLanguage: PreferredAudioLanguage) {
        updateSettings { it.copy(preferredAudioLanguage = audioLanguage) }
        showToast("Preferred audio: ${audioLanguage.label}")
    }

    fun updatePreferredQuality(quality: PreferredQuality) {
        updateSettings { it.copy(preferredQuality = quality) }
    }

    fun updatePreferredCodec(codec: PreferredCodec) {
        updateSettings { it.copy(preferredCodec = codec) }
    }

    fun updateAutoPlayNextEpisode(enabled: Boolean) {
        updateSettings { it.copy(autoPlayNextEpisode = enabled) }
    }

    fun updateDefaultPlaybackSpeed(speed: Float) {
        updateSettings { it.copy(defaultPlaybackSpeed = speed) }
    }

    fun updateAspectRatioMode(mode: AspectRatioMode) {
        updateSettings { it.copy(aspectRatioMode = mode) }
    }

    fun updateControlsTimeout(seconds: Int) {
        updateSettings { it.copy(controlsTimeoutSeconds = seconds) }
    }

    fun updateNetworkTimeout(seconds: Int) {
        updateSettings { it.copy(networkTimeoutSeconds = seconds) }
    }

    fun updateSubtitleStyle(transform: (SubtitleStyle) -> SubtitleStyle) {
        updateSettings { it.copy(subtitleStyle = transform(it.subtitleStyle)) }
    }

    fun clearWatchHistory() {
        recommendationJob?.cancel()
        preferences.clearWatchHistory()
        _uiState.update {
            it.copy(
                watchHistory = emptyList(),
                episodeProgress = emptyMap(),
                recommendedRow = null,
            )
        }
        showToast("Watch history cleared")
    }

    fun runConnectionTest() {
        _uiState.update {
            it.copy(settingsState = it.settingsState.copy(isTesting = true, testResult = null))
        }
        val settings = _uiState.value.settings
        viewModelScope.launch {
            val result = repository.testConnection(settings)
            _uiState.update {
                it.copy(settingsState = it.settingsState.copy(isTesting = false, testResult = result))
            }
        }
    }

    fun checkForGithubUpdate() {
        if (_uiState.value.settingsState.isCheckingUpdate) return
        _uiState.update {
            it.copy(
                settingsState = it.settingsState.copy(
                    isCheckingUpdate = true,
                    updateStatusMessage = "Checking GitHub releases...",
                ),
            )
        }
        viewModelScope.launch {
            val statusMsg = withContext(Dispatchers.IO) {
                runCatching {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
                    val req = Request.Builder()
                        .url(AppMetadata.GITHUB_RELEASES_API)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "ArialTV/${AppMetadata.VERSION_NAME}")
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        when {
                            resp.code == 404 -> "Current version v${AppMetadata.VERSION_NAME} (no GitHub release published yet)"
                            !resp.isSuccessful -> "Couldn't check updates right now (HTTP ${resp.code})"
                            else -> {
                                val json = JSONObject(resp.body?.string().orEmpty())
                                val latestTag = json.optString("tag_name", "").trim()
                                val cleanLatest = latestTag.removePrefix("v").removePrefix("V")
                                if (cleanLatest.isEmpty() || cleanLatest == AppMetadata.VERSION_NAME) {
                                    "You're up to date (v${AppMetadata.VERSION_NAME})"
                                } else {
                                    "Update available: $latestTag — download on ${AppMetadata.GITHUB_HANDLE}/ArialTv"
                                }
                            }
                        }
                    }
                }.getOrElse {
                    "Unable to reach GitHub right now."
                }
            }
            _uiState.update {
                it.copy(
                    settingsState = it.settingsState.copy(
                        isCheckingUpdate = false,
                        updateStatusMessage = statusMsg,
                    ),
                )
            }
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_uiState.value.settings)
        preferences.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    private fun invalidateCatalogs() {
        repository.clearCaches()
        _uiState.update {
            it.copy(
                homeState = CatalogTabState(),
                moviesState = CatalogTabState(),
                seriesState = CatalogTabState(),
                searchState = it.searchState.copy(hasLoadedTrending = false, trendingItems = emptyList(), trendingTerms = emptyList()),
            )
        }
        loadCatalogForTab(TvNavTab.HOME, forceRefresh = true)
        loadPersonalRecommendations()
    }

    private fun updateTabState(tab: TvNavTab, transform: (CatalogTabState) -> CatalogTabState) {
        _uiState.update { state ->
            when (tab) {
                TvNavTab.HOME -> state.copy(homeState = transform(state.homeState))
                TvNavTab.MOVIES -> state.copy(moviesState = transform(state.moviesState))
                TvNavTab.SERIES -> state.copy(seriesState = transform(state.seriesState))
                else -> state
            }
        }
    }
}

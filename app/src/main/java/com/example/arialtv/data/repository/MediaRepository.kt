package com.example.arialtv.data.repository

import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.CatalogFilterOption
import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.ConnectionMode
import com.example.arialtv.data.model.ConnectionTestResult
import com.example.arialtv.data.model.DubOption
import com.example.arialtv.data.model.FilteredCatalogPage
import com.example.arialtv.data.model.HomeCatalog
import com.example.arialtv.data.model.MediaDetails
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.PreferredAudioLanguage
import com.example.arialtv.data.model.PreferredCodec
import com.example.arialtv.data.model.ResolvedPlayback
import com.example.arialtv.data.model.SeasonInfo
import com.example.arialtv.data.model.StreamResource
import com.example.arialtv.data.model.TrendingSearchRank
import com.example.arialtv.data.moviebox.MovieBoxApiException
import com.example.arialtv.data.moviebox.MovieBoxBridgeClient
import com.example.arialtv.data.moviebox.MovieBoxV3Client
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class MediaRepository(
    private val v3Client: MovieBoxV3Client = MovieBoxV3Client(),
    private val bridgeClient: MovieBoxBridgeClient = MovieBoxBridgeClient(),
) {
    private data class CacheEntry<T>(val value: T, val storedAtMs: Long)

    private val catalogCache = ConcurrentHashMap<String, CacheEntry<HomeCatalog>>()
    private val detailsCache = ConcurrentHashMap<String, CacheEntry<MediaDetails>>()
    private val similarCache = ConcurrentHashMap<String, CacheEntry<List<MediaItem>>>()
    private val trendingCache = ConcurrentHashMap<String, CacheEntry<TrendingSearchRank>>()

    fun clearCaches() {
        catalogCache.clear()
        detailsCache.clear()
        similarCache.clear()
        trendingCache.clear()
    }

    private fun <T> getFresh(map: ConcurrentHashMap<String, CacheEntry<T>>, key: String): T? {
        val entry = map[key] ?: return null
        return if (System.currentTimeMillis() - entry.storedAtMs <= CACHE_TTL_MS) entry.value else null
    }

    private fun <T> putFresh(map: ConcurrentHashMap<String, CacheEntry<T>>, key: String, value: T) {
        if (map.size > 16) map.clear()
        map[key] = CacheEntry(value, System.currentTimeMillis())
    }

    private fun syncClientConfig(settings: AppSettings) {
        v3Client.setLanguage(settings.language)
        v3Client.setTimeoutSeconds(settings.networkTimeoutSeconds)
    }

    private fun List<MediaItem>.cleanPlayable(): List<MediaItem> =
        filter { it.id.isNotBlank() && it.title.isNotBlank() && it.posterUrl.isNotBlank() && it.hasResource }
            .distinctBy { it.id }

    suspend fun fetchHome(
        settings: AppSettings,
        tabId: Int = 0,
        forceRefresh: Boolean = false,
    ): HomeCatalog {
        syncClientConfig(settings)
        val cacheKey = "home_${settings.connectionMode.id}_${settings.language.code}_$tabId"
        if (!forceRefresh) {
            getFresh(catalogCache, cacheKey)?.let { return it }
        }

        val result = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> coroutineScope {
                val primaryDef = async { runCatching { v3Client.fetchHomepage(tabId = tabId) }.getOrNull() }
                val moviesTabDef = async {
                    if (tabId == 0) runCatching { v3Client.fetchHomepage(tabId = 2) }.getOrNull() else null
                }
                val rankDef = async { runCatching { v3Client.fetchSearchRank() }.getOrNull() }
                val topRatedDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 1, sort = "Hottest", page = 1, perPage = 20).items
                    }.getOrDefault(emptyList())
                }
                val latestMoviesDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 1, sort = "Latest", page = 1, perPage = 20).items
                    }.getOrDefault(emptyList())
                }
                val actionSciFiDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Action", sort = "Hottest", page = 1, perPage = 18).items
                    }.getOrDefault(emptyList())
                }

                val primary = primaryDef.await()
                val moviesTab = moviesTabDef.await()
                val rank = rankDef.await()
                val hottest = topRatedDef.await().cleanPlayable()
                val latest = latestMoviesDef.await().cleanPlayable()
                val actionHits = actionSciFiDef.await().cleanPlayable()

                if (primary == null && moviesTab == null && hottest.isEmpty() && rank == null) {
                    // Surface real error if offline
                    return@coroutineScope v3Client.fetchHomepage(tabId = tabId)
                }

                val seenIdsAcrossCurated = HashSet<String>()
                fun deduplicateRow(items: List<MediaItem>, preferUnique: Boolean = true): List<MediaItem> {
                    val clean = items.cleanPlayable()
                    if (!preferUnique) return clean
                    val fresh = clean.filter { it.id !in seenIdsAcrossCurated }
                    val chosen = if (fresh.size >= 6) fresh else clean
                    chosen.take(10).forEach { seenIdsAcrossCurated.add(it.id) }
                    return chosen
                }

                val curatedRows = buildList {
                    // 1. Live Trending Movies (always high signal!)
                    val trendMovies = rank?.movies.orEmpty().cleanPlayable()
                    if (trendMovies.isNotEmpty()) {
                        add(CatalogRow("Trending Movies", deduplicateRow(trendMovies)))
                    }

                    // 2. Top Rated Cinema (sorted by IMDb score descending)
                    val topRated = hottest
                        .sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
                    if (topRated.isNotEmpty()) {
                        add(CatalogRow("Top Rated Cinema", deduplicateRow(topRated)))
                    }

                    // 3. Trending TV Series
                    val trendSeries = rank?.series.orEmpty().cleanPlayable()
                    if (trendSeries.isNotEmpty()) {
                        add(CatalogRow("Trending Series", deduplicateRow(trendSeries)))
                    }

                    // 4. Primary MovieBox curated rows (skipping empty / live sports noise)
                    val rawApiRows = buildList {
                        primary?.rows?.let { addAll(it) }
                        moviesTab?.rows?.let { addAll(it) }
                    }
                    rawApiRows
                        .filter { row ->
                            val low = row.category.lowercase()
                            !low.contains("football") && !low.contains("live match") && !low.contains("sport live")
                        }
                        .distinctBy { it.category.lowercase() }
                        .forEach { row ->
                            val cleanItems = deduplicateRow(row.items, preferUnique = false)
                            if (cleanItems.size >= 3) {
                                add(CatalogRow(row.category, cleanItems))
                            }
                        }

                    // 5. New Releases
                    if (latest.isNotEmpty()) {
                        add(CatalogRow("New Releases", deduplicateRow(latest)))
                    }

                    // 6. Action & Blockbusters
                    if (actionHits.isNotEmpty()) {
                        add(CatalogRow("Action Blockbusters", deduplicateRow(actionHits)))
                    }
                }.distinctBy { it.category.lowercase() }

                // Curate Hero Spotlight: prioritize high-rated titles with real widescreen backdrops
                val featuredPool = buildList {
                    primary?.featured?.let { addAll(it) }
                    moviesTab?.featured?.let { addAll(it) }
                    addAll(rank?.movies.orEmpty().take(5))
                    addAll(hottest.take(5))
                }.cleanPlayable()
                    .sortedWith(
                        compareByDescending<MediaItem> { if (it.backdropUrl.isNotBlank() && it.backdropUrl != it.posterUrl) 1 else 0 }
                            .thenByDescending { it.rating.toDoubleOrNull() ?: 0.0 },
                    )
                    .take(8)

                val mergedFilters = buildList {
                    primary?.filters?.let { addAll(it) }
                    moviesTab?.filters?.let { addAll(it) }
                }.distinctBy { it.label.lowercase() }

                HomeCatalog(
                    featured = featuredPool.ifEmpty { curatedRows.firstOrNull()?.items?.take(5).orEmpty() },
                    rows = curatedRows,
                    filters = mergedFilters,
                    sourceHost = primary?.sourceHost?.ifBlank { moviesTab?.sourceHost.orEmpty() }
                        ?: moviesTab?.sourceHost.orEmpty(),
                )
            }
            ConnectionMode.CUSTOM_BRIDGE -> bridgeClient.fetchHome(settings.bridgeBaseUrl)
        }

        if (result.rows.isNotEmpty()) {
            putFresh(catalogCache, cacheKey, result)
        }
        return result
    }

    suspend fun fetchMoviesCatalog(
        settings: AppSettings,
        forceRefresh: Boolean = false,
    ): HomeCatalog {
        syncClientConfig(settings)
        val cacheKey = "movies_${settings.connectionMode.id}_${settings.language.code}"
        if (!forceRefresh) {
            getFresh(catalogCache, cacheKey)?.let { return it }
        }

        val result = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> coroutineScope {
                val tabDef = async { runCatching { v3Client.fetchHomepage(tabId = 2) }.getOrNull() }
                val rankDef = async { runCatching { v3Client.fetchSearchRank().movies }.getOrDefault(emptyList()) }
                val sciFiDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Sci-Fi", sort = "Hottest", page = 1, perPage = 16).items
                    }.getOrDefault(emptyList())
                }
                val thrillerDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Thriller", sort = "Hottest", page = 1, perPage = 16).items
                    }.getOrDefault(emptyList())
                }

                val movieTab = tabDef.await()
                val trending = rankDef.await().cleanPlayable()
                val sciFi = sciFiDef.await().cleanPlayable()
                val thriller = thrillerDef.await().cleanPlayable()

                val rows = buildList {
                    if (trending.isNotEmpty()) add(CatalogRow("Trending Films", trending))
                    movieTab?.rows?.forEach { row ->
                        val items = row.items.cleanPlayable()
                        if (items.size >= 3) add(CatalogRow(row.category, items))
                    }
                    if (sciFi.isNotEmpty()) add(CatalogRow("Sci-Fi & Adventure", sciFi))
                    if (thriller.isNotEmpty()) add(CatalogRow("Thrillers & Mystery", thriller))
                }.distinctBy { it.category.lowercase() }

                if (rows.isNotEmpty()) {
                    HomeCatalog(
                        featured = movieTab?.featured?.cleanPlayable()?.ifEmpty { trending.take(6) } ?: trending.take(6),
                        rows = rows,
                        filters = movieTab?.filters.orEmpty(),
                        sourceHost = movieTab?.sourceHost.orEmpty(),
                    )
                } else {
                    val home = fetchHome(settings, tabId = 0, forceRefresh = forceRefresh)
                    filterCatalogByType(home, isSeries = false)
                }
            }
            ConnectionMode.CUSTOM_BRIDGE -> {
                val home = bridgeClient.fetchHome(settings.bridgeBaseUrl)
                filterCatalogByType(home, isSeries = false)
            }
        }

        if (result.rows.isNotEmpty()) {
            putFresh(catalogCache, cacheKey, result)
        }
        return result
    }

    suspend fun fetchSeriesCatalog(
        settings: AppSettings,
        forceRefresh: Boolean = false,
    ): HomeCatalog {
        syncClientConfig(settings)
        val cacheKey = "series_${settings.connectionMode.id}_${settings.language.code}"
        if (!forceRefresh) {
            getFresh(catalogCache, cacheKey)?.let { return it }
        }

        val result = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> coroutineScope {
                val rankDef = async { runCatching { v3Client.fetchSearchRank().series }.getOrDefault(emptyList()) }
                val hottestDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 2, sort = "Hottest", page = 1, perPage = 20).items
                    }.getOrDefault(emptyList())
                }
                val latestDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 2, sort = "Latest", page = 1, perPage = 20).items
                    }.getOrDefault(emptyList())
                }
                val dramaDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 2, genre = "Drama", sort = "Hottest", page = 1, perPage = 16).items
                    }.getOrDefault(emptyList())
                }
                val animeDef = async {
                    runCatching {
                        v3Client.fetchFilteredList(channelId = 2, genre = "Animation", sort = "Hottest", page = 1, perPage = 16).items
                    }.getOrDefault(emptyList())
                }

                val rankSeries = rankDef.await().cleanPlayable()
                val hottestSeries = hottestDef.await().cleanPlayable()
                val latestSeries = latestDef.await().cleanPlayable()
                val dramaSeries = dramaDef.await().cleanPlayable()
                val animeSeries = animeDef.await().cleanPlayable()

                val rows = buildList {
                    if (rankSeries.isNotEmpty()) add(CatalogRow("Trending Series", rankSeries))
                    if (hottestSeries.isNotEmpty()) add(CatalogRow("Most Popular Series", hottestSeries))
                    if (latestSeries.isNotEmpty()) add(CatalogRow("New Releases", latestSeries))
                    if (dramaSeries.isNotEmpty()) add(CatalogRow("Acclaimed Dramas", dramaSeries))
                    if (animeSeries.isNotEmpty()) add(CatalogRow("Animation & Anime", animeSeries))
                }.distinctBy { it.category.lowercase() }

                val defaultSeriesFilters = listOf(
                    CatalogFilterOption("Drama", genre = "Drama"),
                    CatalogFilterOption("Action", genre = "Action"),
                    CatalogFilterOption("Comedy", genre = "Comedy"),
                    CatalogFilterOption("Sci-Fi", genre = "Sci-Fi"),
                    CatalogFilterOption("Crime", genre = "Crime"),
                    CatalogFilterOption("Animation", genre = "Animation"),
                    CatalogFilterOption("Thriller", genre = "Thriller"),
                )

                val featured = (rankSeries + hottestSeries)
                    .distinctBy { it.id }
                    .sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
                    .take(6)

                HomeCatalog(
                    featured = featured,
                    rows = rows,
                    filters = defaultSeriesFilters,
                    sourceHost = v3Client.activeBaseUrl,
                )
            }
            ConnectionMode.CUSTOM_BRIDGE -> {
                val home = bridgeClient.fetchHome(settings.bridgeBaseUrl)
                filterCatalogByType(home, isSeries = true)
            }
        }

        if (result.rows.isNotEmpty()) {
            putFresh(catalogCache, cacheKey, result)
        }
        return result
    }

    suspend fun fetchSimilarItems(
        settings: AppSettings,
        anchor: MediaItem,
    ): List<MediaItem> {
        syncClientConfig(settings)
        if (settings.connectionMode != ConnectionMode.STANDALONE_V3) return emptyList()
        val primaryGenre = anchor.genre
            .split(',', '/', '|')
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
        val channelId = if (anchor.isSeries) 2 else 1
        val cacheKey = "sim_${settings.language.code}_${channelId}_${primaryGenre ?: "hottest"}"

        val pool = getFresh(similarCache, cacheKey) ?: runCatching {
            v3Client.fetchFilteredList(
                channelId = channelId,
                genre = primaryGenre,
                sort = "Hottest",
                page = 1,
                perPage = 16,
            ).items.cleanPlayable()
        }.getOrDefault(emptyList()).also { list ->
            if (list.isNotEmpty()) putFresh(similarCache, cacheKey, list)
        }

        return pool.filter { it.id != anchor.id }.take(12)
    }

    suspend fun fetchTrendingSearch(
        settings: AppSettings,
        forceRefresh: Boolean = false,
    ): TrendingSearchRank {
        syncClientConfig(settings)
        val cacheKey = "trend_${settings.connectionMode.id}_${settings.language.code}"
        if (!forceRefresh) {
            getFresh(trendingCache, cacheKey)?.let { return it }
        }
        val rank = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> v3Client.fetchSearchRank()
            ConnectionMode.CUSTOM_BRIDGE -> TrendingSearchRank()
        }
        if (rank.movies.isNotEmpty() || rank.series.isNotEmpty()) {
            putFresh(trendingCache, cacheKey, rank)
        }
        return rank
    }

    suspend fun fetchFilteredCatalog(
        settings: AppSettings,
        channelId: Int,
        genre: String? = null,
        year: String? = null,
        country: String? = null,
        sort: String = "ForYou",
        page: Int = 1,
        perPage: Int = 20,
    ): FilteredCatalogPage {
        syncClientConfig(settings)
        return when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> v3Client.fetchFilteredList(
                channelId = channelId,
                genre = genre,
                year = year,
                country = country,
                sort = sort,
                page = page,
                perPage = perPage,
            )
            ConnectionMode.CUSTOM_BRIDGE -> FilteredCatalogPage(emptyList(), page = 1, hasMore = false, totalCount = 0)
        }
    }

    private fun filterCatalogByType(catalog: HomeCatalog, isSeries: Boolean): HomeCatalog {
        val filteredRows = catalog.rows.mapNotNull { row ->
            val matching = row.items.filter { it.isSeries == isSeries }
            if (matching.isNotEmpty()) CatalogRow(row.category, matching) else null
        }
        val filteredFeatured = catalog.featured.filter { it.isSeries == isSeries }
            .ifEmpty { filteredRows.firstOrNull()?.items?.take(5).orEmpty() }
        return HomeCatalog(
            featured = filteredFeatured,
            rows = filteredRows.ifEmpty { catalog.rows },
            filters = catalog.filters,
            sourceHost = catalog.sourceHost,
        )
    }

    suspend fun search(
        settings: AppSettings,
        query: String,
        subjectType: Int = 0,
        perPage: Int = 20,
    ): List<MediaItem> {
        syncClientConfig(settings)
        return when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> v3Client.search(query = query, subjectType = subjectType, perPage = perPage)
            ConnectionMode.CUSTOM_BRIDGE -> {
                val all = bridgeClient.search(settings.bridgeBaseUrl, query)
                val filtered = if (subjectType == 0) all else all.filter { it.subjectType == subjectType }
                filtered.take(perPage)
            }
        }
    }

    suspend fun fetchDetails(
        settings: AppSettings,
        item: MediaItem,
        forceRefresh: Boolean = false,
    ): MediaDetails {
        syncClientConfig(settings)
        val cacheKey = "det_${settings.connectionMode.id}_${settings.language.code}_${item.id}"
        if (!forceRefresh) {
            getFresh(detailsCache, cacheKey)?.let { return it }
        }
        val resolved = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> {
                val details = v3Client.fetchItemDetails(item.id, includeSeasons = true)
                val mergedBackdrop = details.item.backdropUrl.ifBlank { item.backdropUrl }
                val mergedPoster = details.item.posterUrl.ifBlank { item.posterUrl }
                details.copy(
                    item = details.item.copy(
                        backdropUrl = mergedBackdrop,
                        posterUrl = mergedPoster,
                    ),
                )
            }
            ConnectionMode.CUSTOM_BRIDGE -> MediaDetails(item = item)
        }
        putFresh(detailsCache, cacheKey, resolved)
        return resolved
    }

    /**
     * Resolves playable streams, available audio dubs, and external subtitles for a movie or episode.
     * Automatically honors the user's preferred audio language when a matching dub exists (unless an
     * explicit dub subjectId was chosen).
     */
    suspend fun resolvePlayback(
        settings: AppSettings,
        item: MediaItem,
        season: Int = 0,
        episode: Int = 0,
        seasonsMeta: List<SeasonInfo> = emptyList(),
        dubsMeta: List<DubOption> = emptyList(),
        explicitDubSubjectId: String? = null,
        requestedResolution: Int? = null,
    ): ResolvedPlayback = coroutineScope {
        syncClientConfig(settings)
        if (settings.connectionMode == ConnectionMode.CUSTOM_BRIDGE) {
            return@coroutineScope bridgeClient.fetchStream(
                baseUrl = settings.bridgeBaseUrl,
                subjectId = explicitDubSubjectId ?: item.id,
                season = season,
                episode = episode,
            )
        }

        // Ensure we know available dubs so both preferred audio language & in-player audio switcher work
        val knownDubs = if (dubsMeta.isNotEmpty()) {
            dubsMeta
        } else {
            val cached = getFresh(detailsCache, "det_${settings.connectionMode.id}_${settings.language.code}_${item.id}")
            if (cached != null && cached.dubs.isNotEmpty()) {
                cached.dubs
            } else {
                runCatching { v3Client.fetchItemDetails(item.id, includeSeasons = false).dubs }
                    .getOrDefault(emptyList())
            }
        }

        // Resolve target subjectId: explicit dub pick > user's PreferredAudioLanguage > item.id
        val preferredDub = if (explicitDubSubjectId != null) {
            knownDubs.firstOrNull { it.subjectId == explicitDubSubjectId }
        } else {
            findMatchingDub(knownDubs, settings.preferredAudioLanguage)
        }
        val activeSubjectId = explicitDubSubjectId
            ?: preferredDub?.subjectId?.takeIf { it.isNotBlank() }
            ?: item.id
        val activeAudioLabel = knownDubs.firstOrNull { it.subjectId == activeSubjectId }?.languageName
            ?: preferredDub?.languageName
            ?: "Original Audio"

        // If we switched to a different dub subjectId for a TV series, refresh seasons if needed
        val effectiveSeasons = if (item.isSeries && activeSubjectId != item.id) {
            runCatching { v3Client.fetchSeasons(activeSubjectId) }.getOrDefault(seasonsMeta)
        } else {
            seasonsMeta
        }

        val targetResolution = requestedResolution ?: settings.preferredQuality.resolution
        val targetSeason = if (item.isSeries && season <= 0) {
            effectiveSeasons.firstOrNull()?.seasonNumber ?: 1
        } else {
            season
        }
        val targetEpisode = if (item.isSeries && episode <= 0) 1 else episode

        val initialPage = if (item.isSeries && targetSeason > 0 && targetEpisode > 0 && effectiveSeasons.isNotEmpty()) {
            computeEpisodePage(effectiveSeasons, targetSeason, targetEpisode, perPage = 20)
        } else {
            1
        }

        val queryRes = targetResolution.coerceAtMost(1080).takeIf { it > 0 } ?: 1080
        var resolvedSubjectId = activeSubjectId
        var firstResult = runCatching {
            v3Client.fetchResources(
                subjectId = activeSubjectId,
                resolution = queryRes,
                page = initialPage,
                perPage = 20,
            )
        }.getOrNull()

        // Fallback gracefully to original item.id if the preferred dub had no streams for this episode
        if ((firstResult == null || firstResult.streams.isEmpty()) && activeSubjectId != item.id) {
            resolvedSubjectId = item.id
            firstResult = v3Client.fetchResources(
                subjectId = item.id,
                resolution = queryRes,
                page = initialPage,
                perPage = 20,
            )
        }
        val safeFirst = firstResult ?: v3Client.fetchResources(
            subjectId = resolvedSubjectId,
            resolution = queryRes,
            page = initialPage,
            perPage = 20,
        )

        val availableResolutions = safeFirst.collectionResolutions
        val collectedStreams = safeFirst.streams.toMutableList()

        val resolutionsToProbe = buildList {
            if (requestedResolution != null && requestedResolution != queryRes && requestedResolution in availableResolutions) {
                add(requestedResolution)
            } else if (settings.preferredQuality.resolution == 2160 && 2160 in availableResolutions) {
                add(2160)
            }
            if (item.isSeries && availableResolutions.size > 1) {
                val secondary = availableResolutions.firstOrNull { it != queryRes && it in listOf(1080, 720, 480) }
                if (secondary != null && secondary !in this) {
                    add(secondary)
                }
            }
        }

        // Probe extra resolutions concurrently for faster playback start
        if (resolutionsToProbe.isNotEmpty()) {
            val extraDeferreds = resolutionsToProbe.take(2).map { extraRes ->
                async {
                    runCatching {
                        v3Client.fetchResources(
                            subjectId = resolvedSubjectId,
                            resolution = extraRes,
                            page = initialPage,
                            perPage = 20,
                        ).streams
                    }.getOrDefault(emptyList())
                }
            }
            extraDeferreds.forEach { collectedStreams.addAll(it.await()) }
        }

        if (item.isSeries && targetSeason > 0 && targetEpisode > 0) {
            val matchedNow = collectedStreams.any { it.season == targetSeason && it.episode == targetEpisode }
            if (!matchedNow && safeFirst.hasMore) {
                var nextPage = safeFirst.nextPage
                var guard = 0
                while (guard < 4) {
                    val pageRes = runCatching {
                        v3Client.fetchResources(
                            subjectId = resolvedSubjectId,
                            resolution = queryRes,
                            page = nextPage,
                            perPage = 20,
                        )
                    }.getOrNull() ?: break
                    collectedStreams.addAll(pageRes.streams)
                    if (pageRes.streams.any { it.season == targetSeason && it.episode == targetEpisode } || !pageRes.hasMore) {
                        break
                    }
                    nextPage = pageRes.nextPage
                    guard++
                }
            }
        }

        val uniqueStreams = collectedStreams.distinctBy { "${it.url}_${it.resolution}_${it.season}_${it.episode}" }
        if (uniqueStreams.isEmpty()) {
            throw MovieBoxApiException("No playable streams were found for \"${item.title}\".")
        }

        val episodeCandidates = if (item.isSeries && targetSeason > 0 && targetEpisode > 0) {
            val exact = uniqueStreams.filter { it.season == targetSeason && it.episode == targetEpisode }
            exact.ifEmpty { uniqueStreams }
        } else {
            uniqueStreams
        }

        val bestStream = pickBestStream(
            candidates = episodeCandidates,
            targetResolution = targetResolution,
            preferredCodec = settings.preferredCodec,
        )

        val subtitles = runCatching {
            v3Client.fetchCaptions(resolvedSubjectId, bestStream.resourceId)
        }.getOrDefault(emptyList())

        val sortedQualities = episodeCandidates
            .sortedWith(
                compareByDescending<StreamResource> { it.resolution }
                    .thenByDescending { if (it.codec.equals("h264", ignoreCase = true)) 1 else 0 },
            )

        val finalAudioLabel = knownDubs.firstOrNull { it.subjectId == resolvedSubjectId }?.languageName
            ?: activeAudioLabel

        ResolvedPlayback(
            subjectId = resolvedSubjectId,
            title = safeFirst.subjectTitle.ifBlank { item.title },
            season = bestStream.season,
            episode = bestStream.episode,
            selectedStream = bestStream,
            availableStreams = sortedQualities,
            subtitles = subtitles,
            sourceLabel = bestStream.qualityLabel,
            dubs = knownDubs,
            activeAudioLabel = finalAudioLabel,
        )
    }

    suspend fun testConnection(settings: AppSettings): ConnectionTestResult {
        syncClientConfig(settings)
        return when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> v3Client.testConnection()
            ConnectionMode.CUSTOM_BRIDGE -> bridgeClient.testConnection(settings.bridgeBaseUrl)
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L

        fun findMatchingDub(
            dubs: List<DubOption>,
            preferred: PreferredAudioLanguage,
        ): DubOption? {
            if (dubs.isEmpty() || preferred == PreferredAudioLanguage.ORIGINAL) return null
            val code = preferred.matchCode.lowercase()
            val label = preferred.label.lowercase()
            return dubs.firstOrNull { dub ->
                val dc = dub.languageCode.lowercase()
                val dn = dub.languageName.lowercase()
                dc == code ||
                    dc.startsWith("$code-") ||
                    dc.startsWith("${code}_") ||
                    dn.contains(label) ||
                    (code == "fr" && (dn.contains("french") || dn.contains("français") || dn.contains("vf"))) ||
                    (code == "es" && (dn.contains("spanish") || dn.contains("español"))) ||
                    (code == "en" && dn.contains("english")) ||
                    (code == "ar" && dn.contains("arab")) ||
                    (code == "hi" && dn.contains("hindi")) ||
                    (code == "pt" && dn.contains("portug"))
            }
        }
        fun computeEpisodePage(
            seasons: List<SeasonInfo>,
            seasonNumber: Int,
            episodeNumber: Int,
            perPage: Int = 20,
        ): Int {
            val priorEpisodes = seasons
                .filter { it.seasonNumber < seasonNumber }
                .sumOf { it.episodeCount }
            val zeroBasedOffset = (priorEpisodes + (episodeNumber - 1)).coerceAtLeast(0)
            return (zeroBasedOffset / perPage) + 1
        }

        fun pickBestStream(
            candidates: List<StreamResource>,
            targetResolution: Int,
            preferredCodec: PreferredCodec,
        ): StreamResource {
            return candidates.maxWith(
                compareBy<StreamResource>(
                    // 1. Prefer closest resolution to requested quality (or highest if Auto Best)
                    { stream ->
                        if (targetResolution >= 2160) {
                            stream.resolution
                        } else {
                            // Exact match highest, otherwise closest below/above
                            10_000 - abs(stream.resolution - targetResolution)
                        }
                    },
                    // 2. Apply codec preference
                    { stream ->
                        val codec = stream.codec.lowercase()
                        when (preferredCodec) {
                            PreferredCodec.H264 -> if (codec == "h264" || codec == "avc") 2 else 0
                            PreferredCodec.HEVC -> if (codec == "hevc" || codec == "h265") 2 else 0
                            PreferredCodec.AUTO_HARDWARE -> if (codec == "h264" || codec == "avc") 1 else 0
                        }
                    },
                    // 3. Higher resolution tie-breaker
                    { it.resolution },
                ),
            )
        }
    }
}

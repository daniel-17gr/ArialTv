package com.example.arialtv.data.repository

import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.CatalogFilterOption
import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.CategoryQuery
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
import kotlinx.coroutines.CancellationException
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

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun List<MediaItem>.cleanPlayable(filterAdult: Boolean): List<MediaItem> =
        ContentFilter.filterSafeList(this, filterAdult)
            .filter {
                it.id.isNotBlank() &&
                    it.title.isNotBlank() &&
                    it.posterUrl.isNotBlank() &&
                    it.hasResource &&
                    (it.subjectType == 1 || it.subjectType == 2)
            }
            .distinctBy { it.id }

    private fun List<MediaItem>.cleanHomeQuality(
        filterAdult: Boolean,
        minRating: Double = 5.5,
    ): List<MediaItem> =
        filter { ContentFilter.isQualityHomeItem(it, filterAdult, minRating = minRating) }
            .distinctBy { it.id }

    private fun findRowsByKeywords(
        rows: List<CatalogRow>,
        vararg keywords: String,
    ): List<MediaItem> {
        return rows
            .filter { row ->
                val cleaned = ContentFilter.cleanCategoryTitle(row.category).lowercase()
                keywords.any { kw -> cleaned.contains(kw.lowercase()) }
            }
            .flatMap { it.items }
    }

    private fun sortByImdbDesc(items: List<MediaItem>): List<MediaItem> =
        items.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }

    suspend fun fetchHome(
        settings: AppSettings,
        tabId: Int = 0,
        forceRefresh: Boolean = false,
    ): HomeCatalog {
        syncClientConfig(settings)
        val filterAdult = settings.adultFilterEnabled
        val cacheKey = "home_${settings.connectionMode.id}_${settings.language.code}_${tabId}_af$filterAdult"
        if (!forceRefresh) {
            getFresh(catalogCache, cacheKey)?.let { return it }
        }

        val result = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> coroutineScope {
                val moviesTabDef = async { safeCall { v3Client.fetchHomepage(tabId = 2) } }
                val seriesTabDef = async { safeCall { v3Client.fetchHomepage(tabId = 5) } }
                val rankDef = async { safeCall { v3Client.fetchSearchRank() } }
                val sciFiDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Sci-Fi", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val thrillerDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Thriller", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val actionDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Action", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val animDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Animation", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val comedyDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Comedy", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val koreaSeriesDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, country = "Korea", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }

                val moviesTab = moviesTabDef.await()
                val seriesTab = seriesTabDef.await()
                val rank = rankDef.await()
                val sciFiList = sciFiDef.await()
                val thrillerList = thrillerDef.await()
                val actionList = actionDef.await()
                val animList = animDef.await()
                val comedyList = comedyDef.await()
                val koreaSeriesList = koreaSeriesDef.await()

                if (moviesTab == null && seriesTab == null && rank == null &&
                    sciFiList.isEmpty() && actionList.isEmpty()
                ) {
                    // Surface real error if offline
                    return@coroutineScope v3Client.fetchHomepage(tabId = 2)
                }

                val movieRows = moviesTab?.rows.orEmpty()
                val seriesRows = seriesTab?.rows.orEmpty()

                val seenAcrossHome = HashSet<String>()
                fun pickFresh(pool: List<MediaItem>, minRating: Double): List<MediaItem> {
                    val quality = pool.cleanHomeQuality(filterAdult, minRating = minRating)
                        .ifEmpty { pool.cleanHomeQuality(filterAdult, minRating = 4.5) }
                    val fresh = quality.filter { it.id !in seenAcrossHome }
                    val chosen = if (fresh.size >= 12) fresh else (fresh + quality).distinctBy { it.id }
                    chosen.take(12).forEach { seenAcrossHome.add(it.id) }
                    return chosen
                }

                fun curatedRow(
                    title: String,
                    subtitle: String,
                    pool: List<MediaItem>,
                    channelId: Int,
                    genre: String = "All",
                    country: String = "All",
                    sort: String = "Hottest",
                    minRating: Double = 5.5,
                    sortDescByImdb: Boolean = false,
                ): CatalogRow? {
                    val base = pickFresh(pool, minRating = minRating)
                    val ordered = if (sortDescByImdb) sortByImdbDesc(base) else base
                    if (ordered.size < 3) return null
                    return CatalogRow(
                        category = title,
                        items = ordered,
                        expandQuery = CategoryQuery(
                            title = title,
                            subtitle = subtitle,
                            channelId = channelId,
                            genre = genre,
                            country = country,
                            sort = sort,
                            minRating = minRating,
                            seedItems = ordered.take(16),
                        ),
                    )
                }

                val boxOffice2026 = findRowsByKeywords(movieRows, "worldwide box office", "trending movie")
                val imdbTop = findRowsByKeywords(movieRows, "imdb top", "all time box office")
                val superheroAction = findRowsByKeywords(movieRows, "marvel", "dc movies", "action")
                val sciFiWorld = findRowsByKeywords(movieRows, "end of the world", "disaster")
                val animEditorial = findRowsByKeywords(movieRows, "animation", "live-action", "epic fantasy")
                val comedyEditorial = findRowsByKeywords(movieRows, "comed")

                val allTimeSeries = findRowsByKeywords(seriesRows, "all time favorites", "trending tv", "hot returnings")
                val crimeThrillerSeries = findRowsByKeywords(seriesRows, "deadly hunt")
                val kDramaSeries = findRowsByKeywords(seriesRows, "k-drama")

                val curatedRows = listOfNotNull(
                    curatedRow(
                        title = "Trending Worldwide",
                        subtitle = "Most watched films globally",
                        pool = boxOffice2026 + rank?.movies.orEmpty(),
                        channelId = 1,
                        sort = "Hottest",
                        minRating = 5.5,
                    ),
                    curatedRow(
                        title = "IMDb Top Cinema",
                        subtitle = "Critically acclaimed masterpieces",
                        pool = imdbTop,
                        channelId = 1,
                        sort = "TopRated",
                        minRating = 7.0,
                        sortDescByImdb = true,
                    ),
                    curatedRow(
                        title = "Top TV Series",
                        subtitle = "Highest-rated bingeable series",
                        pool = allTimeSeries + rank?.series.orEmpty(),
                        channelId = 2,
                        sort = "Hottest",
                        minRating = 6.0,
                    ),
                    curatedRow(
                        title = "Action & Blockbusters",
                        subtitle = "High-octane action & superheroes",
                        pool = superheroAction + actionList,
                        channelId = 1,
                        genre = "Action",
                        sort = "Hottest",
                        minRating = 5.8,
                    ),
                    curatedRow(
                        title = "Sci-Fi & Cyberpunk",
                        subtitle = "Futuristic & dystopian worlds",
                        pool = sciFiWorld + sciFiList,
                        channelId = 1,
                        genre = "Sci-Fi",
                        sort = "Hottest",
                        minRating = 5.8,
                    ),
                    curatedRow(
                        title = "Crime & Mystery Thrillers",
                        subtitle = "Suspense, heists & dark mysteries",
                        pool = thrillerList + crimeThrillerSeries,
                        channelId = 1,
                        genre = "Thriller",
                        sort = "Hottest",
                        minRating = 6.0,
                    ),
                    curatedRow(
                        title = "Animation & Family",
                        subtitle = "Animated hits for everyone",
                        pool = animList + animEditorial,
                        channelId = 1,
                        genre = "Animation",
                        sort = "Hottest",
                        minRating = 6.0,
                    ),
                    curatedRow(
                        title = "K-Drama & World Hits",
                        subtitle = "Acclaimed Korean & international series",
                        pool = kDramaSeries + koreaSeriesList,
                        channelId = 2,
                        country = "Korea",
                        sort = "Hottest",
                        minRating = 6.2,
                    ),
                    curatedRow(
                        title = "Comedy Picks",
                        subtitle = "Smart comedies & crowd-pleasers",
                        pool = comedyEditorial + comedyList,
                        channelId = 1,
                        genre = "Comedy",
                        sort = "Hottest",
                        minRating = 5.8,
                    ),
                ).distinctBy { it.category.lowercase() }

                // High-signal Hero Spotlight with real widescreen backdrops and strong ratings
                val featuredPool = buildList {
                    addAll(boxOffice2026)
                    addAll(imdbTop.take(4))
                    addAll(rank?.movies.orEmpty().take(5))
                    moviesTab?.featured?.let { addAll(it) }
                    seriesTab?.featured?.let { addAll(it) }
                }.cleanHomeQuality(filterAdult, minRating = 6.5)
                    .sortedWith(
                        compareByDescending<MediaItem> {
                            if (it.backdropUrl.isNotBlank() && it.backdropUrl != it.posterUrl) 1 else 0
                        }.thenByDescending { it.rating.toDoubleOrNull() ?: 0.0 },
                    )
                    .take(8)
                    .ifEmpty {
                        curatedRows.firstOrNull()?.items?.take(5).orEmpty()
                    }

                HomeCatalog(
                    featured = featuredPool,
                    rows = curatedRows,
                    filters = DEFAULT_MOVIE_FILTERS,
                    sourceHost = moviesTab?.sourceHost?.ifBlank { v3Client.activeBaseUrl }
                        ?: v3Client.activeBaseUrl,
                )
            }

            ConnectionMode.CUSTOM_BRIDGE -> {
                val raw = bridgeClient.fetchHome(settings.bridgeBaseUrl)
                raw.copy(
                    featured = raw.featured.cleanPlayable(filterAdult),
                    rows = raw.rows.mapNotNull { row ->
                        val items = row.items.cleanPlayable(filterAdult)
                        if (items.isEmpty()) null else row.copy(
                            category = ContentFilter.cleanCategoryTitle(row.category),
                            items = items,
                        )
                    },
                )
            }
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
        val filterAdult = settings.adultFilterEnabled
        val cacheKey = "movies_${settings.connectionMode.id}_${settings.language.code}_af$filterAdult"
        if (!forceRefresh) {
            getFresh(catalogCache, cacheKey)?.let { return it }
        }

        val result = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> coroutineScope {
                val tabDef = async { safeCall { v3Client.fetchHomepage(tabId = 2) } }
                val rankDef = async { safeCall { v3Client.fetchSearchRank().movies }.orEmpty() }
                val actionDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Action", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val sciFiDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Sci-Fi", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val crimeDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Crime", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val horrorDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Horror", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val comedyDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Comedy", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val animDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "Animation", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val historyDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 1, genre = "History", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }

                val movieTab = tabDef.await()
                val trendingRank = rankDef.await()
                val action = actionDef.await()
                val sciFi = sciFiDef.await()
                val crime = crimeDef.await()
                val horror = horrorDef.await()
                val comedy = comedyDef.await()
                val anim = animDef.await()
                val history = historyDef.await()

                val tabRows = movieTab?.rows.orEmpty()
                val boxOffice = findRowsByKeywords(tabRows, "worldwide box office", "trending movie")
                val imdbTop = findRowsByKeywords(tabRows, "imdb top", "all time box office")
                val superhero = findRowsByKeywords(tabRows, "marvel", "dc movies")
                val endWorld = findRowsByKeywords(tabRows, "end of the world")

                fun movieRow(
                    title: String,
                    subtitle: String,
                    pool: List<MediaItem>,
                    genre: String = "All",
                    sort: String = "Hottest",
                    minRating: Double = 4.8,
                    sortDescByImdb: Boolean = false,
                ): CatalogRow? {
                    val cleaned = pool.cleanHomeQuality(filterAdult, minRating = minRating)
                        .ifEmpty { pool.cleanPlayable(filterAdult) }
                    val ordered = if (sortDescByImdb) sortByImdbDesc(cleaned) else cleaned
                    if (ordered.size < 3) return null
                    return CatalogRow(
                        category = title,
                        items = ordered,
                        expandQuery = CategoryQuery(
                            title = title,
                            subtitle = subtitle,
                            channelId = 1,
                            genre = genre,
                            sort = sort,
                            minRating = minRating,
                            seedItems = ordered.take(16),
                        ),
                    )
                }

                val rows = listOfNotNull(
                    movieRow(
                        title = "Box Office & Trending",
                        subtitle = "Top films in theaters & streaming",
                        pool = boxOffice + trendingRank,
                        sort = "Hottest",
                        minRating = 5.2,
                    ),
                    movieRow(
                        title = "IMDb Top 250 Classics",
                        subtitle = "All-time highest-rated films",
                        pool = imdbTop,
                        sort = "TopRated",
                        minRating = 7.0,
                        sortDescByImdb = true,
                    ),
                    movieRow(
                        title = "Action & Superheroes",
                        subtitle = "Blockbusters, stunts & comic worlds",
                        pool = superhero + action,
                        genre = "Action",
                        minRating = 5.0,
                    ),
                    movieRow(
                        title = "Sci-Fi & Dystopian",
                        subtitle = "Space, AI & post-apocalyptic hits",
                        pool = endWorld + sciFi,
                        genre = "Sci-Fi",
                        minRating = 5.0,
                    ),
                    movieRow(
                        title = "Crime & Thrillers",
                        subtitle = "Detectives, heists & psychological tension",
                        pool = crime,
                        genre = "Crime",
                        minRating = 5.0,
                    ),
                    movieRow(
                        title = "Horror & Supernatural",
                        subtitle = "Chills, hauntings & survival horror",
                        pool = horror,
                        genre = "Horror",
                        minRating = 4.5,
                    ),
                    movieRow(
                        title = "Comedy",
                        subtitle = "Standout comedies & feel-good picks",
                        pool = comedy,
                        genre = "Comedy",
                        minRating = 4.8,
                    ),
                    movieRow(
                        title = "Animation & Adventure",
                        subtitle = "Animated features for all ages",
                        pool = anim,
                        genre = "Animation",
                        minRating = 5.2,
                    ),
                    movieRow(
                        title = "History & War",
                        subtitle = "Epic true stories & historical cinema",
                        pool = history,
                        genre = "History",
                        minRating = 5.2,
                    ),
                ).distinctBy { it.category.lowercase() }

                if (rows.isNotEmpty()) {
                    val featured = (boxOffice + movieTab?.featured.orEmpty() + trendingRank)
                        .cleanHomeQuality(filterAdult, minRating = 6.0)
                        .ifEmpty { rows.first().items.take(6) }
                        .take(8)
                    HomeCatalog(
                        featured = featured,
                        rows = rows,
                        filters = DEFAULT_MOVIE_FILTERS,
                        sourceHost = movieTab?.sourceHost?.ifBlank { v3Client.activeBaseUrl }
                            ?: v3Client.activeBaseUrl,
                    )
                } else {
                    val home = fetchHome(settings, tabId = 0, forceRefresh = forceRefresh)
                    filterCatalogByType(home, isSeries = false, filterAdult = filterAdult)
                }
            }

            ConnectionMode.CUSTOM_BRIDGE -> {
                val home = bridgeClient.fetchHome(settings.bridgeBaseUrl)
                filterCatalogByType(home, isSeries = false, filterAdult = filterAdult)
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
        val filterAdult = settings.adultFilterEnabled
        val cacheKey = "series_${settings.connectionMode.id}_${settings.language.code}_af$filterAdult"
        if (!forceRefresh) {
            getFresh(catalogCache, cacheKey)?.let { return it }
        }

        val result = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> coroutineScope {
                val tabDef = async { safeCall { v3Client.fetchHomepage(tabId = 5) } }
                val rankDef = async { safeCall { v3Client.fetchSearchRank().series }.orEmpty() }
                val latestDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, sort = "Latest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val crimeDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, genre = "Crime", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val koreaDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, country = "Korea", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val sciFiDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, genre = "Sci-Fi", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val dramaDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, genre = "Drama", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val animeDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, genre = "Anime", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }
                val comedyDef = async {
                    safeCall {
                        v3Client.fetchFilteredList(channelId = 2, genre = "Comedy", sort = "Hottest", page = 1, perPage = 20).items
                    }.orEmpty()
                }

                val seriesTab = tabDef.await()
                val rankSeries = rankDef.await()
                val latest = latestDef.await()
                val crime = crimeDef.await()
                val korea = koreaDef.await()
                val sciFi = sciFiDef.await()
                val drama = dramaDef.await()
                val anime = animeDef.await()
                val comedy = comedyDef.await()

                val tabRows = seriesTab?.rows.orEmpty()
                val allTime = findRowsByKeywords(tabRows, "all time favorites", "trending tv")
                val hotReturning = findRowsByKeywords(tabRows, "hot returnings", "new releases")
                val deadlyHunt = findRowsByKeywords(tabRows, "deadly hunt")
                val kDramaEditorial = findRowsByKeywords(tabRows, "k-drama")

                fun seriesRow(
                    title: String,
                    subtitle: String,
                    pool: List<MediaItem>,
                    genre: String = "All",
                    country: String = "All",
                    sort: String = "Hottest",
                    minRating: Double = 5.0,
                ): CatalogRow? {
                    val cleaned = pool.cleanHomeQuality(filterAdult, minRating = minRating)
                        .ifEmpty { pool.cleanPlayable(filterAdult) }
                    if (cleaned.size < 3) return null
                    return CatalogRow(
                        category = title,
                        items = cleaned,
                        expandQuery = CategoryQuery(
                            title = title,
                            subtitle = subtitle,
                            channelId = 2,
                            genre = genre,
                            country = country,
                            sort = sort,
                            minRating = minRating,
                            seedItems = cleaned.take(16),
                        ),
                    )
                }

                val rows = listOfNotNull(
                    seriesRow(
                        title = "All-Time Great Series",
                        subtitle = "Essential series with top IMDb scores",
                        pool = allTime + rankSeries,
                        sort = "Hottest",
                        minRating = 6.5,
                    ),
                    seriesRow(
                        title = "Trending & Returning",
                        subtitle = "New seasons and fresh releases",
                        pool = hotReturning + latest,
                        sort = "Latest",
                        minRating = 5.2,
                    ),
                    seriesRow(
                        title = "Crime & Action Thrillers",
                        subtitle = "Espionage, detectives & edge-of-seat action",
                        pool = deadlyHunt + crime,
                        genre = "Crime",
                        minRating = 5.5,
                    ),
                    seriesRow(
                        title = "K-Drama Essentials",
                        subtitle = "Top Korean dramas & thrillers",
                        pool = kDramaEditorial + korea,
                        country = "Korea",
                        minRating = 5.5,
                    ),
                    seriesRow(
                        title = "Sci-Fi & Mystery",
                        subtitle = "Futuristic worlds & mind-bending mysteries",
                        pool = sciFi,
                        genre = "Sci-Fi",
                        minRating = 5.2,
                    ),
                    seriesRow(
                        title = "Acclaimed Dramas",
                        subtitle = "Prestige character-driven series",
                        pool = drama,
                        genre = "Drama",
                        minRating = 5.5,
                    ),
                    seriesRow(
                        title = "Anime Series",
                        subtitle = "Top-rated Japanese anime",
                        pool = anime,
                        genre = "Anime",
                        minRating = 5.5,
                    ),
                    seriesRow(
                        title = "Comedy Series",
                        subtitle = "Sitcoms & witty comedy series",
                        pool = comedy,
                        genre = "Comedy",
                        minRating = 5.0,
                    ),
                ).distinctBy { it.category.lowercase() }

                val featured = (rankSeries + allTime)
                    .cleanHomeQuality(filterAdult, minRating = 6.5)
                    .distinctBy { it.id }
                    .sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
                    .take(8)
                    .ifEmpty { rows.firstOrNull()?.items?.take(6).orEmpty() }

                HomeCatalog(
                    featured = featured,
                    rows = rows,
                    filters = DEFAULT_SERIES_FILTERS,
                    sourceHost = seriesTab?.sourceHost?.ifBlank { v3Client.activeBaseUrl }
                        ?: v3Client.activeBaseUrl,
                )
            }

            ConnectionMode.CUSTOM_BRIDGE -> {
                val home = bridgeClient.fetchHome(settings.bridgeBaseUrl)
                filterCatalogByType(home, isSeries = true, filterAdult = filterAdult)
            }
        }

        if (result.rows.isNotEmpty()) {
            putFresh(catalogCache, cacheKey, result)
        }
        return result
    }

    suspend fun fetchCategoryPage(
        settings: AppSettings,
        query: CategoryQuery,
        sortOverride: String = query.sort,
        yearOverride: String = query.year,
        countryOverride: String = query.country,
        page: Int = 1,
    ): FilteredCatalogPage {
        val pageResult = fetchFilteredCatalog(
            settings = settings,
            channelId = query.channelId,
            genre = query.genre.takeUnless { it.equals("All", ignoreCase = true) },
            year = yearOverride.takeUnless { it.equals("All", ignoreCase = true) },
            country = countryOverride.takeUnless { it.equals("All", ignoreCase = true) },
            sort = sortOverride,
            page = page,
            perPage = 20,
        )

        val qualityFiltered = if (query.minRating > 0.0) {
            val highRated = pageResult.items.filter {
                (it.rating.toDoubleOrNull() ?: 0.0) >= query.minRating
            }
            if (highRated.size >= 4 || (yearOverride == "All" && countryOverride == "All")) {
                highRated.ifEmpty { pageResult.items }
            } else {
                pageResult.items
            }
        } else {
            pageResult.items
        }

        val isDefaultUnfilteredPage1 = page == 1 &&
            sortOverride == query.sort &&
            yearOverride == query.year &&
            countryOverride == query.country &&
            query.seedItems.isNotEmpty()

        val mergedItems = if (isDefaultUnfilteredPage1) {
            val safeSeeds = query.seedItems.cleanPlayable(settings.adultFilterEnabled)
            val combined = if (sortOverride.equals("TopRated", ignoreCase = true)) {
                sortByImdbDesc((safeSeeds + qualityFiltered).distinctBy { it.id })
            } else {
                (safeSeeds + qualityFiltered).distinctBy { it.id }
            }
            combined
        } else {
            qualityFiltered
        }

        return pageResult.copy(items = mergedItems)
    }

    suspend fun fetchSimilarItems(
        settings: AppSettings,
        anchor: MediaItem,
    ): List<MediaItem> {
        syncClientConfig(settings)
        if (settings.connectionMode != ConnectionMode.STANDALONE_V3) return emptyList()
        val filterAdult = settings.adultFilterEnabled
        val primaryGenre = anchor.genre
            .split(',', '/', '|')
            .map { it.trim() }
            .firstOrNull {
                it.isNotBlank() &&
                    !it.equals("All", ignoreCase = true) &&
                    ContentFilter.isSafeSearchTerm(it, filterAdult)
            }
        val channelId = if (anchor.isSeries) 2 else 1
        val cacheKey = "sim_${settings.language.code}_${channelId}_${primaryGenre ?: "hottest"}_af$filterAdult"

        val pool = getFresh(similarCache, cacheKey) ?: safeCall {
            v3Client.fetchFilteredList(
                channelId = channelId,
                genre = primaryGenre,
                sort = "Hottest",
                page = 1,
                perPage = 18,
            ).items.cleanPlayable(filterAdult)
        }.orEmpty().also { list ->
            if (list.isNotEmpty()) putFresh(similarCache, cacheKey, list)
        }

        return pool.filter { it.id != anchor.id }.take(12)
    }

    suspend fun fetchTrendingSearch(
        settings: AppSettings,
        forceRefresh: Boolean = false,
    ): TrendingSearchRank {
        syncClientConfig(settings)
        val filterAdult = settings.adultFilterEnabled
        val cacheKey = "trend_${settings.connectionMode.id}_${settings.language.code}_af$filterAdult"
        if (!forceRefresh) {
            getFresh(trendingCache, cacheKey)?.let { return it }
        }
        val raw = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> v3Client.fetchSearchRank()
            ConnectionMode.CUSTOM_BRIDGE -> TrendingSearchRank()
        }
        val cleaned = TrendingSearchRank(
            movies = raw.movies.cleanPlayable(filterAdult),
            series = raw.series.cleanPlayable(filterAdult),
            everyoneSearch = raw.everyoneSearch.filter { ContentFilter.isSafeSearchTerm(it, filterAdult) },
        )
        if (cleaned.movies.isNotEmpty() || cleaned.series.isNotEmpty()) {
            putFresh(trendingCache, cacheKey, cleaned)
        }
        return cleaned
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
        if (settings.connectionMode == ConnectionMode.CUSTOM_BRIDGE) {
            return FilteredCatalogPage(emptyList(), page = 1, hasMore = false, totalCount = 0)
        }
        val apiSort = if (sort.equals("TopRated", ignoreCase = true)) "Hottest" else sort
        val rawPage = v3Client.fetchFilteredList(
            channelId = channelId,
            genre = genre,
            year = year,
            country = country,
            sort = apiSort,
            page = page,
            perPage = perPage,
        )
        val safeItems = rawPage.items.cleanPlayable(settings.adultFilterEnabled)
        val orderedItems = if (sort.equals("TopRated", ignoreCase = true)) {
            sortByImdbDesc(safeItems)
        } else {
            safeItems
        }
        return rawPage.copy(items = orderedItems)
    }

    private fun filterCatalogByType(
        catalog: HomeCatalog,
        isSeries: Boolean,
        filterAdult: Boolean,
    ): HomeCatalog {
        val filteredRows = catalog.rows.mapNotNull { row ->
            val matching = row.items.cleanPlayable(filterAdult).filter { it.isSeries == isSeries }
            if (matching.isNotEmpty()) row.copy(items = matching) else null
        }
        val filteredFeatured = catalog.featured.cleanPlayable(filterAdult).filter { it.isSeries == isSeries }
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
        val raw = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> v3Client.search(query = query, subjectType = subjectType, perPage = perPage)
            ConnectionMode.CUSTOM_BRIDGE -> {
                val all = bridgeClient.search(settings.bridgeBaseUrl, query)
                val filtered = if (subjectType == 0) all else all.filter { it.subjectType == subjectType }
                filtered.take(perPage)
            }
        }
        return ContentFilter.filterSafeList(raw, settings.adultFilterEnabled)
            .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.posterUrl.isNotBlank() }
            .distinctBy { it.id }
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

        val knownDubs = if (dubsMeta.isNotEmpty()) {
            dubsMeta
        } else {
            val cached = getFresh(detailsCache, "det_${settings.connectionMode.id}_${settings.language.code}_${item.id}")
            if (cached != null && cached.dubs.isNotEmpty()) {
                cached.dubs
            } else {
                safeCall { v3Client.fetchItemDetails(item.id, includeSeasons = false).dubs }.orEmpty()
            }
        }

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

        val effectiveSeasons = if (item.isSeries && activeSubjectId != item.id) {
            safeCall { v3Client.fetchSeasons(activeSubjectId) } ?: seasonsMeta
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
        var firstResult = safeCall {
            v3Client.fetchResources(
                subjectId = activeSubjectId,
                resolution = queryRes,
                page = initialPage,
                perPage = 20,
            )
        }

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

        if (resolutionsToProbe.isNotEmpty()) {
            val extraDeferreds = resolutionsToProbe.take(2).map { extraRes ->
                async {
                    safeCall {
                        v3Client.fetchResources(
                            subjectId = resolvedSubjectId,
                            resolution = extraRes,
                            page = initialPage,
                            perPage = 20,
                        ).streams
                    }.orEmpty()
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
                    val pageRes = safeCall {
                        v3Client.fetchResources(
                            subjectId = resolvedSubjectId,
                            resolution = queryRes,
                            page = nextPage,
                            perPage = 20,
                        )
                    } ?: break
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

        val subtitles = safeCall {
            v3Client.fetchCaptions(resolvedSubjectId, bestStream.resourceId)
        }.orEmpty()

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

        private val DEFAULT_MOVIE_FILTERS = listOf(
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
        ).map { CatalogFilterOption(label = it, genre = it) }

        private val DEFAULT_SERIES_FILTERS = listOf(
            "Drama",
            "Crime",
            "Action",
            "Sci-Fi",
            "Thriller",
            "Comedy",
            "Mystery",
            "Anime",
            "Animation",
            "Adventure",
            "Family",
            "History",
            "Documentary",
        ).map { CatalogFilterOption(label = it, genre = it) }

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
                    { stream ->
                        if (targetResolution >= 2160) {
                            stream.resolution
                        } else {
                            10_000 - abs(stream.resolution - targetResolution)
                        }
                    },
                    { stream ->
                        val codec = stream.codec.lowercase()
                        when (preferredCodec) {
                            PreferredCodec.H264 -> if (codec == "h264" || codec == "avc") 2 else 0
                            PreferredCodec.HEVC -> if (codec == "hevc" || codec == "h265") 2 else 0
                            PreferredCodec.AUTO_HARDWARE -> if (codec == "h264" || codec == "avc") 1 else 0
                        }
                    },
                    { it.resolution },
                ),
            )
        }
    }
}

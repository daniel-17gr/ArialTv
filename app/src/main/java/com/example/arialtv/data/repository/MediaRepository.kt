package com.example.arialtv.data.repository

import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.CatalogFilterOption
import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.ConnectionMode
import com.example.arialtv.data.model.ConnectionTestResult
import com.example.arialtv.data.model.FilteredCatalogPage
import com.example.arialtv.data.model.HomeCatalog
import com.example.arialtv.data.model.MediaDetails
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.PreferredCodec
import com.example.arialtv.data.model.ResolvedPlayback
import com.example.arialtv.data.model.SeasonInfo
import com.example.arialtv.data.model.StreamResource
import com.example.arialtv.data.model.TrendingSearchRank
import com.example.arialtv.data.moviebox.MovieBoxApiException
import com.example.arialtv.data.moviebox.MovieBoxBridgeClient
import com.example.arialtv.data.moviebox.MovieBoxV3Client
import kotlin.math.abs

class MediaRepository(
    private val v3Client: MovieBoxV3Client = MovieBoxV3Client(),
    private val bridgeClient: MovieBoxBridgeClient = MovieBoxBridgeClient(),
) {

    private fun syncClientConfig(settings: AppSettings) {
        v3Client.setLanguage(settings.language)
        v3Client.setTimeoutSeconds(settings.networkTimeoutSeconds)
    }

    suspend fun fetchHome(settings: AppSettings, tabId: Int = 0): HomeCatalog {
        syncClientConfig(settings)
        return when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> {
                val primary = v3Client.fetchHomepage(tabId = tabId)
                // MovieBox sometimes replaces tabId=0 with a single live-event banner (e.g. Football Live).
                // Automatically enrich with tabId=2 when primary has fewer than 2 catalog rows or no filter pills.
                if (tabId == 0 && (primary.rows.size < 2 || primary.filters.isEmpty())) {
                    val fallback = runCatching { v3Client.fetchHomepage(tabId = 2) }.getOrNull()
                    if (fallback != null && fallback.rows.isNotEmpty()) {
                        val mergedRows = if (primary.rows.size < 2) {
                            (primary.rows + fallback.rows).distinctBy { it.category }
                        } else {
                            primary.rows
                        }
                        val mergedFeatured = primary.featured.ifEmpty { fallback.featured }
                        val mergedFilters = (primary.filters + fallback.filters).distinctBy { it.label.lowercase() }
                        return fallback.copy(
                            featured = mergedFeatured,
                            rows = mergedRows,
                            filters = mergedFilters,
                        )
                    }
                }
                primary
            }
            ConnectionMode.CUSTOM_BRIDGE -> bridgeClient.fetchHome(settings.bridgeBaseUrl)
        }
    }

    suspend fun fetchMoviesCatalog(settings: AppSettings): HomeCatalog {
        syncClientConfig(settings)
        return when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> {
                val movieTab = runCatching { v3Client.fetchHomepage(tabId = 2) }.getOrNull()
                if (movieTab != null && movieTab.rows.isNotEmpty()) {
                    movieTab
                } else {
                    val home = fetchHome(settings, tabId = 0)
                    filterCatalogByType(home, isSeries = false)
                }
            }
            ConnectionMode.CUSTOM_BRIDGE -> {
                val home = bridgeClient.fetchHome(settings.bridgeBaseUrl)
                filterCatalogByType(home, isSeries = false)
            }
        }
    }

    suspend fun fetchSeriesCatalog(settings: AppSettings): HomeCatalog {
        syncClientConfig(settings)
        val rawHome = when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> runCatching { v3Client.fetchHomepage(tabId = 0) }.getOrNull()
            ConnectionMode.CUSTOM_BRIDGE -> bridgeClient.fetchHome(settings.bridgeBaseUrl)
        }
        val filtered = rawHome?.let { filterCatalogByType(it, isSeries = true) }
        val hasSeries = filtered != null && filtered.rows.any { row -> row.items.any { it.isSeries } }
        if (hasSeries && filtered!!.rows.size >= 2) {
            return filtered
        }

        // Enrich with MovieBox v3 live TV ranking & filtered lists so Series is always rich
        if (settings.connectionMode == ConnectionMode.STANDALONE_V3) {
            val rankSeries = runCatching { v3Client.fetchSearchRank().series }.getOrDefault(emptyList())
            val hottestSeries = runCatching {
                v3Client.fetchFilteredList(channelId = 2, sort = "Hottest", page = 1, perPage = 20).items
            }.getOrDefault(emptyList())
            val latestSeries = runCatching {
                v3Client.fetchFilteredList(channelId = 2, sort = "Latest", page = 1, perPage = 20).items
            }.getOrDefault(emptyList())

            val extraRows = buildList {
                if (filtered != null) {
                    addAll(filtered.rows.filter { row -> row.items.any { it.isSeries } })
                }
                if (rankSeries.isNotEmpty()) add(CatalogRow("Trending Series", rankSeries))
                if (hottestSeries.isNotEmpty()) add(CatalogRow("Most Popular Series", hottestSeries))
                if (latestSeries.isNotEmpty()) add(CatalogRow("New Releases", latestSeries))
            }.distinctBy { it.category }

            val defaultSeriesFilters = listOf(
                CatalogFilterOption("Drama", genre = "Drama"),
                CatalogFilterOption("Action", genre = "Action"),
                CatalogFilterOption("Comedy", genre = "Comedy"),
                CatalogFilterOption("Sci-Fi", genre = "Sci-Fi"),
                CatalogFilterOption("Crime", genre = "Crime"),
                CatalogFilterOption("Animation", genre = "Animation"),
                CatalogFilterOption("Thriller", genre = "Thriller"),
            )

            if (extraRows.isNotEmpty()) {
                return HomeCatalog(
                    featured = extraRows.first().items.take(5),
                    rows = extraRows,
                    filters = filtered?.filters?.ifEmpty { defaultSeriesFilters } ?: defaultSeriesFilters,
                    sourceHost = rawHome?.sourceHost.orEmpty(),
                )
            }
        }
        return filtered ?: HomeCatalog(featured = emptyList(), rows = emptyList())
    }

    suspend fun fetchTrendingSearch(settings: AppSettings): TrendingSearchRank {
        syncClientConfig(settings)
        return when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> v3Client.fetchSearchRank()
            ConnectionMode.CUSTOM_BRIDGE -> TrendingSearchRank()
        }
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

    suspend fun fetchDetails(settings: AppSettings, item: MediaItem): MediaDetails {
        syncClientConfig(settings)
        return when (settings.connectionMode) {
            ConnectionMode.STANDALONE_V3 -> {
                val details = v3Client.fetchItemDetails(item.id, includeSeasons = true)
                // Preserve high-res banner backdrop if details had none
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
    }

    /**
     * Resolves playable streams & external subtitles for a movie or a specific TV series episode.
     * For TV series, uses season metadata to compute the exact resource page so even deep seasons
     * (e.g. S05E12) resolve in a single API call.
     */
    suspend fun resolvePlayback(
        settings: AppSettings,
        item: MediaItem,
        season: Int = 0,
        episode: Int = 0,
        seasonsMeta: List<SeasonInfo> = emptyList(),
        requestedResolution: Int? = null,
    ): ResolvedPlayback {
        syncClientConfig(settings)
        if (settings.connectionMode == ConnectionMode.CUSTOM_BRIDGE) {
            return bridgeClient.fetchStream(
                baseUrl = settings.bridgeBaseUrl,
                subjectId = item.id,
                season = season,
                episode = episode,
            )
        }

        val targetResolution = requestedResolution ?: settings.preferredQuality.resolution
        val targetSeason = if (item.isSeries && season <= 0) {
            seasonsMeta.firstOrNull()?.seasonNumber ?: 1
        } else {
            season
        }
        val targetEpisode = if (item.isSeries && episode <= 0) 1 else episode

        // Compute page index for TV series using seasons metadata (perPage = 20)
        val initialPage = if (item.isSeries && targetSeason > 0 && targetEpisode > 0 && seasonsMeta.isNotEmpty()) {
            computeEpisodePage(seasonsMeta, targetSeason, targetEpisode, perPage = 20)
        } else {
            1
        }

        // First call using requested resolution (or 1080p as standard query resolution)
        val queryRes = targetResolution.coerceAtMost(1080).takeIf { it > 0 } ?: 1080
        val firstResult = v3Client.fetchResources(
            subjectId = item.id,
            resolution = queryRes,
            page = initialPage,
            perPage = 20,
        )

        val availableResolutions = firstResult.collectionResolutions
        var collectedStreams = firstResult.streams.toMutableList()

        // If the user wants 4K/2160p or another specific resolution exposed in collectionResolutions,
        // fetch that exact resolution too so the quality selector has real choices.
        val resolutionsToProbe = buildList {
            if (requestedResolution != null && requestedResolution != queryRes && requestedResolution in availableResolutions) {
                add(requestedResolution)
            } else if (settings.preferredQuality.resolution == 2160 && 2160 in availableResolutions) {
                add(2160)
            }
            // For TV series, also probe 720p or 480p as fallback quality options if only one resolution was returned
            if (item.isSeries && availableResolutions.size > 1) {
                val secondary = availableResolutions.firstOrNull { it != queryRes && it in listOf(1080, 720, 480) }
                if (secondary != null && secondary !in this) {
                    add(secondary)
                }
            }
        }

        for (extraRes in resolutionsToProbe.take(2)) {
            runCatching {
                v3Client.fetchResources(
                    subjectId = item.id,
                    resolution = extraRes,
                    page = initialPage,
                    perPage = 20,
                ).streams
            }.getOrNull()?.let { extra ->
                collectedStreams.addAll(extra)
            }
        }

        // If series episode wasn't on initialPage (e.g., seasonsMeta wasn't loaded yet), scan next pages
        if (item.isSeries && targetSeason > 0 && targetEpisode > 0) {
            val matchedNow = collectedStreams.any { it.season == targetSeason && it.episode == targetEpisode }
            if (!matchedNow && firstResult.hasMore) {
                var nextPage = firstResult.nextPage
                var guard = 0
                while (guard < 4) {
                    val pageRes = runCatching {
                        v3Client.fetchResources(
                            subjectId = item.id,
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
            v3Client.fetchCaptions(item.id, bestStream.resourceId)
        }.getOrDefault(emptyList())

        val sortedQualities = episodeCandidates
            .sortedWith(
                compareByDescending<StreamResource> { it.resolution }
                    .thenByDescending { if (it.codec.equals("h264", ignoreCase = true)) 1 else 0 },
            )

        return ResolvedPlayback(
            subjectId = item.id,
            title = firstResult.subjectTitle.ifBlank { item.title },
            season = bestStream.season,
            episode = bestStream.episode,
            selectedStream = bestStream,
            availableStreams = sortedQualities,
            subtitles = subtitles,
            sourceLabel = bestStream.qualityLabel,
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

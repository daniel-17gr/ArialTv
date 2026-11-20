package com.example.arialtv

import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.CatalogLanguage
import com.example.arialtv.data.model.ConnectionMode
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.moviebox.MovieBoxConfig
import com.example.arialtv.data.moviebox.MovieBoxV3Client
import com.example.arialtv.data.repository.MediaRepository
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the standalone Kotlin MovieBox v3 client end-to-end against live endpoints:
 * - Movie flow: Homepage -> x-user session token -> Search -> Details -> Stream -> HTTP 206 CDN Range fetch
 * - Series flow: French locale Homepage -> Series Search -> Seasons + Dubs -> S1E1 Stream + SRT Captions -> HTTP 206 + HTTP 200 SRT
 */
class MovieBoxLiveIntegrationTest {

    private val cdnClient = OkHttpClient.Builder().followRedirects(true).build()

    @Test
    fun standaloneMovieFlowWorksEndToEndLive() = runTest {
        val client = MovieBoxV3Client()
        val repository = MediaRepository(v3Client = client)
        val settings = AppSettings(
            connectionMode = ConnectionMode.STANDALONE_V3,
            language = CatalogLanguage.ENGLISH,
        )

        // 1. Live Homepage + x-user token exchange
        val home = repository.fetchHome(settings)
        assertTrue("Expected non-empty catalog rows from MovieBox v3", home.rows.isNotEmpty())
        assertTrue("Expected featured items", home.featured.isNotEmpty())
        assertNotNull("Expected x-user runtime token after homepage call", client.runtimeToken)

        // 2. Live Search
        val searchResults = repository.search(settings, "Inception")
        assertTrue("Expected search results for Inception", searchResults.isNotEmpty())
        val firstMovie = searchResults.firstOrNull { !it.isSeries } ?: searchResults.first()
        assertFalse(firstMovie.id.isBlank())

        // 3. Live Details
        val details = repository.fetchDetails(settings, firstMovie)
        assertFalse(details.item.title.isBlank())

        // 4. Live Stream + Captions resolution
        val resolved = repository.resolvePlayback(settings, details.item)
        assertTrue(
            "Expected HTTP(S) direct stream URL, got: ${resolved.selectedStream.url}",
            resolved.selectedStream.url.startsWith("http"),
        )
        assertTrue("Expected at least one stream quality", resolved.availableStreams.isNotEmpty())

        // 5. Verify direct CDN Range GET succeeds with 206 Partial Content (no Referer header, okhttp User-Agent)
        assertCdnRange206(resolved.selectedStream.url)
    }

    @Test
    fun standaloneSeriesSeasonsAndSubtitlesWorkLive() = runTest {
        val client = MovieBoxV3Client()
        val repository = MediaRepository(v3Client = client)
        val settings = AppSettings(
            connectionMode = ConnectionMode.STANDALONE_V3,
            language = CatalogLanguage.FRENCH,
        )

        // 1. Live Connection Test in French locale
        val conn = repository.testConnection(settings)
        assertTrue("Expected successful live connection test: ${conn.message}", conn.success)
        assertTrue("Expected session token to be active", conn.hasSessionToken)
        assertTrue("Expected catalog sections > 0", conn.sectionCount > 0)

        // 2. Live Series Catalog
        val seriesCatalog = repository.fetchSeriesCatalog(settings)
        val firstSeries = seriesCatalog.rows.flatMap { it.items }.firstOrNull { it.isSeries }
        assertNotNull("Expected at least one TV series in catalog", firstSeries)

        // 3. Fetch Details including Seasons & Dubs
        val details = repository.fetchDetails(settings, firstSeries!!)
        assertTrue("Expected seasons for TV series ${details.item.title}", details.seasons.isNotEmpty())

        // 4. Resolve S1 E1 playable stream + subtitles
        val firstSeason = details.seasons.first().seasonNumber
        val resolved = repository.resolvePlayback(
            settings = settings,
            item = details.item,
            season = firstSeason,
            episode = 1,
            seasonsMeta = details.seasons,
        )
        assertTrue(resolved.selectedStream.url.startsWith("http"))
        assertCdnRange206(resolved.selectedStream.url)

        // 5. If captions exist, verify the SRT URL returns HTTP 200
        val firstSubtitle = resolved.subtitles.firstOrNull()
        if (firstSubtitle != null) {
            val subReq = Request.Builder()
                .url(firstSubtitle.url)
                .header("User-Agent", MovieBoxConfig.CDN_USER_AGENT)
                .get()
                .build()
            cdnClient.newCall(subReq).execute().use { resp ->
                assertEquals("Expected HTTP 200 for subtitle file", 200, resp.code)
                assertTrue("Expected non-empty SRT subtitle content", (resp.body?.string()?.length ?: 0) > 20)
            }
        }
    }

    @Test
    fun liveTrendingSearchAndFiltersWork() = runTest {
        val repository = MediaRepository()
        val settings = AppSettings(
            connectionMode = ConnectionMode.STANDALONE_V3,
            language = CatalogLanguage.ENGLISH,
        )

        // 1. Live Trending Search Rank
        val rank = repository.fetchTrendingSearch(settings)
        assertTrue(
            "Expected trending movies or series from search-rank",
            rank.movies.isNotEmpty() || rank.series.isNotEmpty(),
        )

        // 2. Live API Filtered Catalog (Action Films)
        val filtered = repository.fetchFilteredCatalog(
            settings = settings,
            channelId = 1,
            genre = "Action",
            sort = "Hottest",
            page = 1,
            perPage = 10,
        )
        assertTrue("Expected live filtered Action titles", filtered.items.isNotEmpty())

        // 3. Content Type Badge logic (MOVIE / SERIES / DOC / MUSIC)
        assertEquals("MOVIE", MediaItem(id = "1", title = "A", subjectType = 1, genre = "Action").contentTypeBadge)
        assertEquals("SERIES", MediaItem(id = "2", title = "B", subjectType = 2, genre = "Drama").contentTypeBadge)
        assertEquals("DOC", MediaItem(id = "3", title = "C", subjectType = 1, genre = "Documentary").contentTypeBadge)
        assertEquals("MUSIC", MediaItem(id = "4", title = "D", subjectType = 6, genre = "Pop").contentTypeBadge)
        assertEquals("MUSIC", MediaItem(id = "5", title = "E", subjectType = 1, genre = "Music, Concert").contentTypeBadge)
    }

    private fun assertCdnRange206(streamUrl: String) {
        val rangeReq = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", MovieBoxConfig.CDN_USER_AGENT)
            .header("Range", "bytes=0-1023")
            .get()
            .build()
        cdnClient.newCall(rangeReq).execute().use { resp ->
            assertEquals(
                "CDN stream should return 206 Partial Content without Referer header (url=$streamUrl)",
                206,
                resp.code,
            )
            val ct = resp.header("Content-Type").orEmpty()
            assertTrue(
                "Expected video/* or octet-stream content-type, got: $ct",
                ct.contains("video", ignoreCase = true) || ct.contains("octet-stream", ignoreCase = true),
            )
        }
    }
}

package com.example.arialtv

import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.repository.ContentFilter
import com.example.arialtv.data.update.AppUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying safety filtering, Home suggestion quality gates, category title cleanup,
 * semantic release comparison, and GitHub APK release asset parsing.
 */
class ArialTvReliabilityTest {

    @Test
    fun adultFilterBlocksEroticAdultAiAdultAndExplicitTitles() {
        val adultSamples = listOf(
            MediaItem(
                id = "7487722334959666840",
                title = "Stag",
                year = "2024",
                rating = "6.1",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 1,
                genre = "Erotic, Hot, Romance",
                countryName = "India",
            ),
            MediaItem(
                id = "5858571083980460176",
                title = "Booking",
                year = "2026",
                rating = "6.3",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 1,
                genre = "Adult, Erotic",
                countryName = "Philippines",
            ),
            MediaItem(
                id = "2820985021849295952",
                title = "Dutdot",
                year = "2026",
                rating = "7.0",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 1,
                genre = "Adult, Erotic",
                countryName = "Philippines",
            ),
            MediaItem(
                id = "4861054660217557840",
                title = "Pinoy Sexy Classics 2",
                year = "2026",
                rating = "6.8",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 1,
                genre = "Adult, Erotic",
                countryName = "Philippines",
            ),
            MediaItem(
                id = "900001",
                title = "My Boss’s Personal Pleasure Toy",
                year = "2026",
                rating = "6.7",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 7,
                genre = "AiAdult",
                countryName = "China",
            ),
            MediaItem(
                id = "900002",
                title = "The Shadow King's Pet",
                year = "2026",
                rating = "6.8",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 7,
                genre = "Modern Drama",
                countryName = "United States",
            ),
            MediaItem(
                id = "900003",
                title = "Bigay hilig: Sexual Fetish",
                year = "2024",
                rating = "5.0",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 1,
                genre = "Documentary",
                countryName = "Philippines",
            ),
            MediaItem(
                id = "900004",
                title = "Bible Black",
                year = "2001",
                rating = "6.8",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 2,
                genre = "Anime",
                countryName = "Japan",
            ),
            MediaItem(
                id = "900005",
                title = "I Want Your Sex",
                year = "2026",
                rating = "5.8",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 1,
                genre = "Comedy, Romance",
                countryName = "United States",
            ),
            MediaItem(
                id = "900006",
                title = "Sharing Tiny Hotel Room With Step Sis - She Wanted to Be Fucked So Bad",
                year = "2025",
                rating = "7.0",
                posterUrl = "https://cdn/poster.jpg",
                subjectType = 1,
                genre = "Drama",
            ),
        )

        adultSamples.forEach { sample ->
            assertFalse(
                "Expected adult filter to block '${sample.title}' (${sample.genre}, type=${sample.subjectType})",
                ContentFilter.isSafeContent(sample, filterEnabled = true),
            )
        }
    }

    @Test
    fun adultFilterPreservesLegitimateMainstreamCinema() {
        val safeMovies = listOf(
            MediaItem("1", "The Dark Knight", "2008", "9.1", "Batman faces the Joker.", "https://cdn/p.jpg", subjectType = 1, genre = "Action, Crime, Drama", countryName = "United States"),
            MediaItem("2", "Inception", "2010", "8.8", "A thief steals corporate secrets.", "https://cdn/p.jpg", subjectType = 1, genre = "Action, Sci-Fi, Thriller", countryName = "United States"),
            MediaItem("3", "Dune: Part Two", "2024", "8.5", "Paul Atreides unites with Chani.", "https://cdn/p.jpg", subjectType = 1, genre = "Action, Adventure, Drama", countryName = "United States"),
            MediaItem("4", "Game of Thrones", "2011", "9.2", "Nine noble families fight for control.", "https://cdn/p.jpg", subjectType = 2, genre = "Action, Adventure, Drama", countryName = "United States"),
            MediaItem("5", "The Shawshank Redemption", "1994", "9.3", "Two imprisoned men bond over years.", "https://cdn/p.jpg", subjectType = 1, genre = "Drama", countryName = "United States"),
            MediaItem("6", "Spider-Man: No Way Home", "2021", "8.1", "Peter Parker seeks Doctor Strange's help.", "https://cdn/p.jpg", subjectType = 1, genre = "Action, Adventure, Sci-Fi", countryName = "United States"),
            MediaItem("7", "Hot Fuzz", "2007", "7.8", "A top London cop is transferred.", "https://cdn/p.jpg", subjectType = 1, genre = "Action, Comedy, Mystery", countryName = "United Kingdom"),
            MediaItem("8", "Inside Out", "2015", "8.1", "Young Riley's emotions guide her.", "https://cdn/p.jpg", subjectType = 1, genre = "Animation, Adventure, Comedy", countryName = "United States"),
            MediaItem("9", "Oppenheimer", "2023", "8.3", "The story of American scientist J. Robert Oppenheimer.", "https://cdn/p.jpg", subjectType = 1, genre = "Biography, Drama, History", countryName = "United States"),
            MediaItem("10", "Prison Break", "2005", "8.3", "An engineer installs himself in a prison.", "https://cdn/p.jpg", subjectType = 2, genre = "Action, Crime, Drama", countryName = "United States"),
        )

        safeMovies.forEach { movie ->
            assertTrue(
                "Expected legitimate cinema '${movie.title}' to pass adult filter",
                ContentFilter.isSafeContent(movie, filterEnabled = true),
            )
        }
    }

    @Test
    fun homeQualityGateRejectsNonsenseAndUnratedSpam() {
        val validHighQuality = MediaItem(
            id = "100",
            title = "The Odyssey",
            year = "2026",
            rating = "8.4",
            posterUrl = "https://cdn/p.jpg",
            subjectType = 1,
            genre = "Action, Adventure, Drama",
            hasResource = true,
        )
        assertTrue(ContentFilter.isQualityHomeItem(validHighQuality, filterEnabled = true, minRating = 5.5, currentYear = 2026))

        val unratedSpam = validHighQuality.copy(id = "101", title = "AGAINST ALL ODDS", rating = "0")
        val noPoster = validHighQuality.copy(id = "102", posterUrl = "")
        val camRip = validHighQuality.copy(id = "103", title = "Josephine[CAM]", rating = "8.0")
        val futurePlaceholder = validHighQuality.copy(id = "104", year = "2027", rating = "7.7")
        val unplayable = validHighQuality.copy(id = "105", hasResource = false)
        val shortDrama = validHighQuality.copy(id = "106", subjectType = 7)
        val lowRatedFiller = validHighQuality.copy(id = "107", rating = "3.2")

        listOf(unratedSpam, noPoster, camRip, futurePlaceholder, unplayable, shortDrama, lowRatedFiller).forEach { bad ->
            assertFalse(
                "Expected Home quality gate to reject '${bad.title}'",
                ContentFilter.isQualityHomeItem(bad, filterEnabled = true, minRating = 5.5, currentYear = 2026),
            )
        }

        assertEquals("All Time Favorites", ContentFilter.cleanCategoryTitle("🏆All Time Favorites🏆"))
        assertEquals("The End of The World", ContentFilter.cleanCategoryTitle("The End of The World 🏜️"))
    }

    @Test
    fun semanticVersionComparatorHandlesAllEdgeCases() {
        assertTrue(AppUpdater.isNewerVersion("1.6.0", "1.5.0"))
        assertTrue(AppUpdater.isNewerVersion("v1.10.0", "1.9.0"))
        assertTrue(AppUpdater.isNewerVersion("v2.0.0", "1.9.9"))
        assertTrue(AppUpdater.isNewerVersion("1.6.1", "1.6"))
        assertFalse(AppUpdater.isNewerVersion("1.6.0", "1.6.0"))
        assertFalse(AppUpdater.isNewerVersion("v1.5.0", "1.6.0"))
        assertFalse(AppUpdater.isNewerVersion("", "1.6.0"))
    }

    @Test
    fun githubReleaseJsonParserExtractsApkAsset() {
        val sampleJson = """
            {
              "tag_name": "v1.7.0",
              "html_url": "https://github.com/daniel-17gr/ArialTv/releases/tag/v1.7.0",
              "body": "Faster playback and bug fixes",
              "assets": [
                {
                  "name": "ArialTv-debug.apk",
                  "size": 12000000,
                  "browser_download_url": "https://github.com/daniel-17gr/ArialTv/releases/download/v1.7.0/ArialTv-debug.apk"
                },
                {
                  "name": "ArialTv-release.apk",
                  "size": 9500000,
                  "browser_download_url": "https://github.com/daniel-17gr/ArialTv/releases/download/v1.7.0/ArialTv-release.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = AppUpdater.parseReleaseJson(sampleJson)
        assertEquals("v1.7.0", parsed.tagName)
        assertEquals("1.7.0", parsed.versionName)
        assertEquals("Faster playback and bug fixes", parsed.releaseNotes)
        assertEquals("ArialTv-release.apk", parsed.apkAssetName)
        assertEquals(9_500_000L, parsed.apkSizeBytes)
        assertEquals(
            "https://github.com/daniel-17gr/ArialTv/releases/download/v1.7.0/ArialTv-release.apk",
            parsed.apkDownloadUrl,
        )
    }
}

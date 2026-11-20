package com.example.arialtv

import com.example.arialtv.data.model.PreferredCodec
import com.example.arialtv.data.model.SeasonInfo
import com.example.arialtv.data.model.StreamResource
import com.example.arialtv.data.moviebox.MovieBoxCrypto
import com.example.arialtv.data.repository.MediaRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieBoxCryptoTest {

    @Test
    fun xClientTokenMatchesPythonGoldenVector() {
        val ts = 1795195000000L
        assertEquals(
            "1795195000000,2861e964aa268fb29c04e9a5102c71ef",
            MovieBoxCrypto.generateXClientToken(ts),
        )
    }

    @Test
    fun getRequestSignatureMatchesPythonGoldenVector() {
        val ts = 1795195000000L
        val url = "https://api6.aoneroom.com/wefeed-mobile-bff/tab-operating?page=1&tabId=0&version="
        val sig = MovieBoxCrypto.generateXTrSignature(
            method = "GET",
            accept = "application/json",
            contentType = "application/json",
            url = url,
            body = null,
            useAltKey = false,
            timestampMs = ts,
        )
        assertEquals("1795195000000|2|U8PyvJ7+PJvqA4bytDpP7g==", sig)
    }

    @Test
    fun postRequestSignatureMatchesPythonGoldenVector() {
        val ts = 1795195000000L
        val url = "https://api6.aoneroom.com/wefeed-mobile-bff/subject-api/search"
        val body = """{"keyword":"Inception","page":1,"perPage":20,"subjectType":0}"""
        val sig = MovieBoxCrypto.generateXTrSignature(
            method = "POST",
            accept = "application/json",
            contentType = "application/json; charset=utf-8",
            url = url,
            body = body,
            useAltKey = false,
            timestampMs = ts,
        )
        assertEquals("1795195000000|2|w6UomwhnzkvLAoJg9MU86g==", sig)
    }

    @Test
    fun sortedQueryPreservesEmptyValuesAndSortsAlphabetically() {
        val url = "https://api6.aoneroom.com/path?z=last&version=&a=first"
        assertEquals("a=first&version=&z=last", MovieBoxCrypto.sortedQueryString(url))
    }

    @Test
    fun episodePageComputationHandlesMultiSeasonOffsets() {
        val seasons = listOf(
            SeasonInfo(seasonNumber = 1, episodeCount = 8, availableResolutions = listOf(1080)),
            SeasonInfo(seasonNumber = 2, episodeCount = 10, availableResolutions = listOf(1080)),
            SeasonInfo(seasonNumber = 3, episodeCount = 12, availableResolutions = listOf(1080)),
        )
        assertEquals(1, MediaRepository.computeEpisodePage(seasons, seasonNumber = 1, episodeNumber = 1, perPage = 20))
        assertEquals(1, MediaRepository.computeEpisodePage(seasons, seasonNumber = 2, episodeNumber = 10, perPage = 20))
        assertEquals(1, MediaRepository.computeEpisodePage(seasons, seasonNumber = 3, episodeNumber = 2, perPage = 20))
        assertEquals(2, MediaRepository.computeEpisodePage(seasons, seasonNumber = 3, episodeNumber = 3, perPage = 20))
    }

    @Test
    fun streamPickerRespectsQualityAndCodecPreferences() {
        val streams = listOf(
            StreamResource("1", "https://cdn/360.mp4", "Movie", 360, "hevc", 0, 0, 100, 100),
            StreamResource("2", "https://cdn/1080_hevc.mp4", "Movie", 1080, "hevc", 0, 0, 100, 200),
            StreamResource("3", "https://cdn/1080_h264.mp4", "Movie", 1080, "h264", 0, 0, 100, 300),
        )
        val pickedH264 = MediaRepository.pickBestStream(streams, 1080, PreferredCodec.H264)
        assertEquals("3", pickedH264.resourceId)

        val pickedHevc = MediaRepository.pickBestStream(streams, 1080, PreferredCodec.HEVC)
        assertEquals("2", pickedHevc.resourceId)

        val picked360 = MediaRepository.pickBestStream(streams, 360, PreferredCodec.AUTO_HARDWARE)
        assertEquals("1", picked360.resourceId)
    }
}

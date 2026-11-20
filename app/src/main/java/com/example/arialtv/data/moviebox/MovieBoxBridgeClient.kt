package com.example.arialtv.data.moviebox

import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.ConnectionTestResult
import com.example.arialtv.data.model.HomeCatalog
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.ResolvedPlayback
import com.example.arialtv.data.model.StreamResource
import com.example.arialtv.data.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Optional client for the FastAPI bridge server.py (/api/home, /api/search, /api/stream/{id}).
 * Allows users who self-host server.py on their LAN or cloud to switch seamlessly in Settings.
 */
class MovieBoxBridgeClient(
    private val httpClient: OkHttpClient = MovieBoxV3Client.defaultOkHttpClient(),
) {

    suspend fun testConnection(baseUrl: String): ConnectionTestResult {
        val cleanBase = normalizeBase(baseUrl)
        val startMs = System.currentTimeMillis()
        return try {
            val catalog = fetchHome(cleanBase)
            val elapsed = System.currentTimeMillis() - startMs
            ConnectionTestResult(
                success = true,
                host = cleanBase,
                latencyMs = elapsed,
                hasSessionToken = true,
                sectionCount = catalog.rows.size,
                message = "Bridge reachable (${catalog.rows.size} rows)",
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startMs
            ConnectionTestResult(
                success = false,
                host = cleanBase,
                latencyMs = elapsed,
                hasSessionToken = false,
                sectionCount = 0,
                message = e.message ?: "Could not reach bridge",
            )
        }
    }

    suspend fun fetchHome(baseUrl: String): HomeCatalog = withContext(Dispatchers.IO) {
        val cleanBase = normalizeBase(baseUrl)
        val json = getJson("$cleanBase/api/home")

        val featured = parseItemList(json.optJSONArray("featured"))
        val rows = mutableListOf<CatalogRow>()
        val rowsArr = json.optJSONArray("rows") ?: JSONArray()
        for (i in 0 until rowsArr.length()) {
            val r = rowsArr.optJSONObject(i) ?: continue
            val category = r.optStringClean("category").ifEmpty { r.optStringClean("title") }
            val items = parseItemList(r.optJSONArray("items"))
            if (items.isNotEmpty()) {
                rows.add(CatalogRow(category = category.ifEmpty { "Row ${i + 1}" }, items = items))
            }
        }

        HomeCatalog(
            featured = featured.ifEmpty { rows.firstOrNull()?.items?.take(3).orEmpty() },
            rows = rows,
            sourceHost = cleanBase,
        )
    }

    suspend fun search(baseUrl: String, query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val cleanBase = normalizeBase(baseUrl)
        val url = "$cleanBase/api/search".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("q", query.trim())
            ?.build()
            ?.toString()
            ?: throw IOException("Invalid bridge URL: $cleanBase")
        val json = getJson(url)
        parseItemList(json.optJSONArray("results"))
    }

    suspend fun fetchStream(
        baseUrl: String,
        subjectId: String,
        season: Int = 0,
        episode: Int = 0,
    ): ResolvedPlayback = withContext(Dispatchers.IO) {
        val cleanBase = normalizeBase(baseUrl)
        val url = "$cleanBase/api/stream/$subjectId".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("season", season.toString())
            ?.addQueryParameter("episode", episode.toString())
            ?.build()
            ?.toString()
            ?: throw IOException("Invalid bridge URL: $cleanBase")

        val json = getJson(url)
        val title = json.optStringClean("title")
        // Prefer direct_url on Android TV (ExoPlayer streams straight from CDN without PC bandwidth!)
        val directUrl = json.optStringClean("direct_url")
        val streamUrl = directUrl.ifBlank { json.optStringClean("stream_url") }
        if (streamUrl.isBlank()) {
            throw IOException("Bridge returned empty stream URL")
        }

        val headersMap = mutableMapOf<String, String>()
        val hdrObj = json.optJSONObject("headers")
        if (hdrObj != null) {
            val keys = hdrObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                headersMap[k] = hdrObj.optString(k)
            }
        }

        val available = mutableListOf<StreamResource>()
        val availArr = json.optJSONArray("available_streams")
        if (availArr != null) {
            for (i in 0 until availArr.length()) {
                val s = availArr.optJSONObject(i) ?: continue
                val u = s.optStringClean("url")
                if (u.isNotBlank()) {
                    available.add(
                        StreamResource(
                            resourceId = "${subjectId}_$i",
                            url = u,
                            title = title,
                            resolution = s.optIntSafe("resolution"),
                            codec = s.optStringClean("codec"),
                            season = s.optIntSafe("season"),
                            episode = s.optIntSafe("episode"),
                            durationSeconds = 0,
                            sizeBytes = 0L,
                            headers = headersMap,
                        ),
                    )
                }
            }
        }

        val qualityInt = json.optStringClean("quality").filter { it.isDigit() }.toIntOrNull() ?: 1080
        val selected = available.firstOrNull { it.url == streamUrl } ?: StreamResource(
            resourceId = subjectId,
            url = streamUrl,
            title = title,
            resolution = qualityInt,
            codec = json.optStringClean("codec"),
            season = season,
            episode = episode,
            durationSeconds = 0,
            sizeBytes = 0L,
            headers = headersMap,
        )

        val subtitles = mutableListOf<SubtitleTrack>()
        val capArr = json.optJSONArray("captions")
        if (capArr != null) {
            for (i in 0 until capArr.length()) {
                val c = capArr.optJSONObject(i) ?: continue
                val capUrl = c.optStringClean("url")
                if (capUrl.isNotBlank()) {
                    subtitles.add(
                        SubtitleTrack(
                            id = c.optStringClean("id").ifEmpty { "$i" },
                            languageCode = c.optStringClean("lan"),
                            languageName = c.optStringClean("lanName").ifEmpty { c.optStringClean("lan") },
                            url = capUrl,
                            delayMs = c.optLongSafe("delay"),
                        ),
                    )
                }
            }
        }

        ResolvedPlayback(
            subjectId = subjectId,
            title = title,
            season = season,
            episode = episode,
            selectedStream = selected,
            availableStreams = available.ifEmpty { listOf(selected) },
            subtitles = subtitles,
            sourceLabel = if (directUrl.isNotBlank()) "BRIDGE → DIRECT CDN" else "BRIDGE PROXY",
        )
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Bridge HTTP ${response.code}: ${body.take(120)}")
            }
            return JSONObject(body)
        }
    }

    private fun parseItemList(array: JSONArray?): List<MediaItem> {
        if (array == null) return emptyList()
        val result = mutableListOf<MediaItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optStringClean("id").ifEmpty { obj.optStringClean("subject_id") }
            if (id.isEmpty()) continue
            val poster = obj.optStringClean("poster_url").ifEmpty { obj.optStringClean("poster") }
            val backdrop = obj.optStringClean("backdrop_url").ifEmpty { poster }
            result.add(
                MediaItem(
                    id = id,
                    title = obj.optStringClean("title").ifEmpty { "Untitled" },
                    year = obj.optStringClean("year"),
                    rating = obj.optStringClean("rating"),
                    description = obj.optStringClean("description"),
                    posterUrl = poster,
                    backdropUrl = backdrop,
                    subjectType = obj.optIntSafe("subject_type").takeIf { it > 0 } ?: 1,
                    genre = obj.optStringClean("genre"),
                ),
            )
        }
        return result
    }

    private fun normalizeBase(baseUrl: String): String =
        baseUrl.trim().trimEnd('/').ifEmpty { "http://10.0.2.2:8000" }
}

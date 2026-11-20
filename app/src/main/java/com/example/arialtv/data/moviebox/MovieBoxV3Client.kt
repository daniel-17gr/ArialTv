package com.example.arialtv.data.moviebox

import android.util.Log
import com.example.arialtv.data.model.CatalogFilterOption
import com.example.arialtv.data.model.CatalogLanguage
import com.example.arialtv.data.model.CatalogRow
import com.example.arialtv.data.model.ConnectionTestResult
import com.example.arialtv.data.model.DubOption
import com.example.arialtv.data.model.FilteredCatalogPage
import com.example.arialtv.data.model.HomeCatalog
import com.example.arialtv.data.model.MediaDetails
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.SeasonInfo
import com.example.arialtv.data.model.StaffMember
import com.example.arialtv.data.model.StreamResource
import com.example.arialtv.data.model.SubtitleTrack
import com.example.arialtv.data.model.TrendingSearchRank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class MovieBoxApiException(
    message: String,
    val statusCode: Int = -1,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * Standalone native Kotlin client for MovieBox v3 Android REST API (api3..api6.aoneroom.com).
 * Features:
 * - Automatic host-pool failover across 7 MovieBox API gateways
 * - HMAC-MD5 request signing (X-Client-Token + x-tr-signature)
 * - Automatic x-user bearer token capture and transparent warmup retry on HTTP/code 441
 */
class MovieBoxV3Client(
    baseClient: OkHttpClient = defaultOkHttpClient(),
) {
    @Volatile
    private var httpClient: OkHttpClient = baseClient

    @Volatile
    private var configuredTimeoutSec: Int = 25

    @Volatile
    var activeBaseUrl: String = MovieBoxConfig.HOST_POOL.first()
        private set

    @Volatile
    var runtimeToken: String? = null
        private set

    @Volatile
    private var currentLanguage: CatalogLanguage = CatalogLanguage.ENGLISH

    @Volatile
    private var fingerprint: MovieBoxConfig.ClientFingerprint =
        MovieBoxConfig.createFingerprint(CatalogLanguage.ENGLISH)

    private val warmupMutex = Mutex()

    fun setTimeoutSeconds(timeoutSec: Int) {
        val clamped = timeoutSec.coerceIn(10, 60)
        if (configuredTimeoutSec != clamped) {
            configuredTimeoutSec = clamped
            httpClient = httpClient.newBuilder()
                .connectTimeout(clamped.toLong(), TimeUnit.SECONDS)
                .readTimeout(clamped.toLong(), TimeUnit.SECONDS)
                .writeTimeout(clamped.toLong(), TimeUnit.SECONDS)
                .build()
        }
    }

    fun setLanguage(language: CatalogLanguage) {
        if (currentLanguage != language) {
            currentLanguage = language
            fingerprint = MovieBoxConfig.createFingerprint(
                language = language,
                existingDeviceId = fingerprint.deviceId,
                existingGaid = fingerprint.gaid,
            )
        }
    }

    suspend fun warmUpSession() {
        warmupMutex.withLock {
            if (!runtimeToken.isNullOrBlank()) return
            try {
                getApiData(
                    path = MovieBoxConfig.MAIN_PAGE_PATH,
                    params = linkedMapOf("page" to "1", "tabId" to "0", "version" to ""),
                    allowWarmupRetry = false,
                )
            } catch (_: Exception) {
                // Best-effort warmup; subsequent call surfaces real error if offline
            }
        }
    }

    suspend fun testConnection(): ConnectionTestResult {
        val startMs = System.currentTimeMillis()
        return try {
            val data = getApiData(
                path = MovieBoxConfig.MAIN_PAGE_PATH,
                params = linkedMapOf("page" to "1", "tabId" to "0", "version" to ""),
            )
            val sections = data.optJSONArray("items")?.length() ?: 0
            val elapsed = System.currentTimeMillis() - startMs
            ConnectionTestResult(
                success = true,
                host = activeBaseUrl,
                latencyMs = elapsed,
                hasSessionToken = !runtimeToken.isNullOrBlank(),
                sectionCount = sections,
                message = "Connected to MovieBox v3 ($sections catalog sections)",
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startMs
            ConnectionTestResult(
                success = false,
                host = activeBaseUrl,
                latencyMs = elapsed,
                hasSessionToken = !runtimeToken.isNullOrBlank(),
                sectionCount = 0,
                message = e.message ?: "Connection failed",
            )
        }
    }

    suspend fun fetchHomepage(tabId: Int = 0, page: Int = 1): HomeCatalog {
        val data = getApiData(
            path = MovieBoxConfig.MAIN_PAGE_PATH,
            params = linkedMapOf(
                "page" to page.toString(),
                "tabId" to tabId.toString(),
                "version" to "",
            ),
        )
        return parseHomepage(data)
    }

    suspend fun search(
        query: String,
        subjectType: Int = 0, // 0 = All, 1 = Movies, 2 = TV Series
        page: Int = 1,
        perPage: Int = 20,
    ): List<MediaItem> {
        val payload = JSONObject().apply {
            put("keyword", query.trim())
            put("page", page)
            put("perPage", perPage.coerceIn(1, 20))
            put("subjectType", subjectType)
        }
        val data = postApiData(
            path = MovieBoxConfig.SEARCH_PATH,
            jsonBody = payload.toString(),
        )
        val itemsArray = data.optJSONArray("items") ?: JSONArray()
        val parsed = mutableListOf<MediaItem>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: continue
            val item = parseSubject(obj) ?: continue
            if (subjectType == 0 || item.subjectType == subjectType) {
                parsed.add(item)
            }
        }
        return parsed
    }

    suspend fun fetchSearchRank(): TrendingSearchRank {
        val data = getApiData(
            path = MovieBoxConfig.SEARCH_RANK_PATH,
            params = emptyMap(),
        )
        fun parseList(key: String): List<MediaItem> {
            val arr = data.optJSONArray(key) ?: return emptyList()
            val out = mutableListOf<MediaItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parseSubject(obj)?.let { out.add(it) }
            }
            return out
        }
        val everyone = mutableListOf<String>()
        val evArr = data.optJSONArray("everyoneSearch")
        if (evArr != null) {
            for (i in 0 until evArr.length()) {
                val entry = evArr.opt(i)
                val label = when (entry) {
                    is JSONObject -> entry.optStringClean("title").ifEmpty { entry.optStringClean("word") }
                    is String -> entry.trim()
                    else -> ""
                }
                if (label.isNotEmpty()) everyone.add(label)
            }
        }
        return TrendingSearchRank(
            movies = parseList("movie"),
            series = parseList("tv"),
            everyoneSearch = everyone.distinct(),
        )
    }

    suspend fun fetchFilteredList(
        channelId: Int = 1, // 1 = Movies, 2 = TV Series
        genre: String? = null,
        year: String? = null,
        country: String? = null,
        sort: String = "ForYou",
        page: Int = 1,
        perPage: Int = 20,
    ): FilteredCatalogPage {
        val payload = JSONObject().apply {
            put("page", page.coerceAtLeast(1))
            put("perPage", perPage.coerceIn(1, 20))
            put("channelId", channelId)
            put("sort", sort.ifBlank { "ForYou" })
            if (!genre.isNullOrBlank() && !genre.equals("All", ignoreCase = true)) {
                put("genre", genre)
            }
            if (!year.isNullOrBlank() && !year.equals("All", ignoreCase = true)) {
                put("year", year)
            }
            if (!country.isNullOrBlank() && !country.equals("All", ignoreCase = true)) {
                put("country", country)
            }
        }
        val data = postApiData(
            path = MovieBoxConfig.FILTER_LIST_PATH,
            jsonBody = payload.toString(),
        )
        val itemsArray = data.optJSONArray("items") ?: JSONArray()
        val parsed = mutableListOf<MediaItem>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: continue
            parseSubject(obj)?.let { parsed.add(it) }
        }
        val pager = data.optJSONObject("pager")
        val hasMore = pager?.optBoolean("hasMore", false) ?: false
        val totalCount = pager?.optIntSafe("totalCount") ?: parsed.size
        return FilteredCatalogPage(
            items = parsed,
            page = page,
            hasMore = hasMore,
            totalCount = totalCount,
        )
    }

    suspend fun fetchItemDetails(subjectId: String, includeSeasons: Boolean = true): MediaDetails {
        if (runtimeToken.isNullOrBlank()) {
            warmUpSession()
        }
        val data = getApiData(
            path = MovieBoxConfig.SUBJECT_GET_PATH,
            params = linkedMapOf("subjectId" to subjectId),
        )
        val item = parseSubject(data) ?: MediaItem(id = subjectId, title = "Untitled")

        val dubs = mutableListOf<DubOption>()
        val dubsArr = data.optJSONArray("dubs")
        if (dubsArr != null) {
            for (i in 0 until dubsArr.length()) {
                val d = dubsArr.optJSONObject(i) ?: continue
                val dId = d.optStringClean("subjectId")
                if (dId.isNotEmpty()) {
                    val rawName = d.optStringClean("lanName").ifEmpty { d.optStringClean("lanCode") }
                    val cleanName = if (rawName.lowercase().startsWith("original")) {
                        "Original Audio"
                    } else {
                        rawName
                    }
                    dubs.add(
                        DubOption(
                            subjectId = dId,
                            languageName = cleanName,
                            languageCode = d.optStringClean("lanCode"),
                            isOriginal = d.optBoolean("original", false),
                        ),
                    )
                }
            }
        }

        val staff = mutableListOf<StaffMember>()
        val staffArr = data.optJSONArray("staffList")
        if (staffArr != null) {
            for (i in 0 until staffArr.length()) {
                val s = staffArr.optJSONObject(i) ?: continue
                val name = s.optStringClean("name")
                if (name.isNotEmpty()) {
                    staff.add(
                        StaffMember(
                            id = s.optStringClean("staffId"),
                            name = name,
                            character = s.optStringClean("character"),
                            avatarUrl = s.optStringClean("avatarUrl"),
                        ),
                    )
                }
            }
        }

        val seasons = if (includeSeasons && (item.isSeries || data.optIntSafe("seNum") > 0)) {
            runCatching { fetchSeasons(subjectId) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val availableResolutions = seasons
            .flatMap { it.availableResolutions }
            .distinct()
            .sortedDescending()

        return MediaDetails(
            item = item,
            durationSeconds = data.optIntSafe("durationSeconds").takeIf { it > 0 }
                ?: data.optIntSafe("seconds"),
            totalSeasons = seasons.size.takeIf { it > 0 } ?: data.optIntSafe("seNum"),
            dubs = dubs,
            seasons = seasons,
            staff = staff,
            availableResolutions = availableResolutions,
        )
    }

    suspend fun fetchSeasons(subjectId: String): List<SeasonInfo> {
        if (runtimeToken.isNullOrBlank()) {
            warmUpSession()
        }
        val data = getApiData(
            path = MovieBoxConfig.SEASON_INFO_PATH,
            params = linkedMapOf("subjectId" to subjectId),
        )
        val seasonsArr = data.optJSONArray("seasons") ?: return emptyList()
        val result = mutableListOf<SeasonInfo>()
        for (i in 0 until seasonsArr.length()) {
            val s = seasonsArr.optJSONObject(i) ?: continue
            val se = s.optIntSafe("se")
            val maxEp = s.optIntSafe("maxEp")
            if (se > 0 && maxEp > 0) {
                val resList = mutableListOf<Int>()
                val resArr = s.optJSONArray("resolutions")
                if (resArr != null) {
                    for (j in 0 until resArr.length()) {
                        val rObj = resArr.optJSONObject(j) ?: continue
                        val r = rObj.optIntSafe("resolution")
                        if (r > 0) resList.add(r)
                    }
                }
                result.add(
                    SeasonInfo(
                        seasonNumber = se,
                        episodeCount = maxEp,
                        availableResolutions = resList.distinct().sortedDescending(),
                    ),
                )
            }
        }
        return result.sortedBy { it.seasonNumber }
    }

    data class ResourcePageResult(
        val subjectTitle: String,
        val streams: List<StreamResource>,
        val collectionResolutions: List<Int>,
        val hasMore: Boolean,
        val nextPage: Int,
        val totalCount: Int,
    )

    suspend fun fetchResources(
        subjectId: String,
        resolution: Int = 1080,
        page: Int = 1,
        perPage: Int = 20,
    ): ResourcePageResult {
        if (runtimeToken.isNullOrBlank()) {
            warmUpSession()
        }
        val data = getApiData(
            path = MovieBoxConfig.RESOURCE_PATH,
            params = linkedMapOf(
                "subjectId" to subjectId,
                "resolution" to resolution.toString(),
                "page" to page.toString(),
                "perPage" to perPage.coerceIn(1, 20).toString(),
            ),
        )

        val subjectTitle = data.optStringClean("subjectTitle")
        val listArr = data.optJSONArray("list") ?: JSONArray()
        val streams = mutableListOf<StreamResource>()
        for (i in 0 until listArr.length()) {
            val obj = listArr.optJSONObject(i) ?: continue
            val link = obj.optStringClean("resourceLink")
            if (link.startsWith("http://") || link.startsWith("https://")) {
                streams.add(
                    StreamResource(
                        resourceId = obj.optStringClean("resourceId"),
                        url = link,
                        title = obj.optStringClean("title").ifEmpty { subjectTitle },
                        resolution = obj.optIntSafe("resolution"),
                        codec = obj.optStringClean("codecName"),
                        season = obj.optIntSafe("se"),
                        episode = obj.optIntSafe("ep"),
                        durationSeconds = obj.optIntSafe("duration"),
                        sizeBytes = obj.optLongSafe("size"),
                    ),
                )
            }
        }

        val collectionRes = mutableListOf<Int>()
        val colArr = data.optJSONArray("collectionResolutions")
        if (colArr != null) {
            for (i in 0 until colArr.length()) {
                val r = colArr.optJSONObject(i)?.optIntSafe("resolution") ?: 0
                if (r > 0) collectionRes.add(r)
            }
        }

        val pager = data.optJSONObject("pager")
        val hasMore = pager?.optBoolean("hasMore", false) ?: false
        val nextPage = pager?.optIntSafe("nextPage") ?: (page + 1)
        val totalCount = pager?.optIntSafe("totalCount") ?: streams.size

        return ResourcePageResult(
            subjectTitle = subjectTitle,
            streams = streams,
            collectionResolutions = collectionRes.distinct().sortedDescending(),
            hasMore = hasMore,
            nextPage = nextPage,
            totalCount = totalCount,
        )
    }

    suspend fun fetchCaptions(subjectId: String, resourceId: String): List<SubtitleTrack> {
        if (resourceId.isBlank()) return emptyList()
        if (runtimeToken.isNullOrBlank()) {
            warmUpSession()
        }
        val data = getApiData(
            path = MovieBoxConfig.EXT_CAPTIONS_PATH,
            params = linkedMapOf(
                "subjectId" to subjectId,
                "resourceId" to resourceId,
            ),
        )
        val arr = data.optJSONArray("extCaptions") ?: return emptyList()
        val result = mutableListOf<SubtitleTrack>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.optStringClean("url")
            if (url.startsWith("http://") || url.startsWith("https://")) {
                result.add(
                    SubtitleTrack(
                        id = obj.optStringClean("id").ifEmpty { "$i" },
                        languageCode = obj.optStringClean("lan"),
                        languageName = obj.optStringClean("lanName").ifEmpty {
                            obj.optStringClean("lan").uppercase()
                        },
                        url = url,
                        delayMs = obj.optLongSafe("delay"),
                    ),
                )
            }
        }
        return result
    }

    private suspend fun getApiData(
        path: String,
        params: Map<String, String> = emptyMap(),
        allowWarmupRetry: Boolean = true,
    ): JSONObject = executeSignedApiRequest(
        method = "GET",
        path = path,
        params = params,
        body = null,
        contentType = "application/json",
        allowWarmupRetry = allowWarmupRetry,
    )

    private suspend fun postApiData(
        path: String,
        jsonBody: String,
        params: Map<String, String> = emptyMap(),
        allowWarmupRetry: Boolean = true,
    ): JSONObject = executeSignedApiRequest(
        method = "POST",
        path = path,
        params = params,
        body = jsonBody,
        contentType = "application/json; charset=utf-8",
        allowWarmupRetry = allowWarmupRetry,
    )

    private suspend fun executeSignedApiRequest(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String,
        allowWarmupRetry: Boolean,
    ): JSONObject = withContext(Dispatchers.IO) {
        val orderedHosts = buildList {
            add(activeBaseUrl)
            for (h in MovieBoxConfig.HOST_POOL) {
                if (h != activeBaseUrl) add(h)
            }
        }

        var lastError: Throwable? = null
        var lastStatusCode = -1
        var lastBody = ""

        for (base in orderedHosts) {
            val urlBuilder = (base + path).toHttpUrl().newBuilder()
            for ((k, v) in params) {
                urlBuilder.addQueryParameter(k, v)
            }
            val url = urlBuilder.build()

            val signedHeaders = MovieBoxCrypto.buildSignedHeaders(
                method = method,
                url = url,
                accept = "application/json",
                contentType = contentType,
                body = body,
                authToken = runtimeToken,
                fingerprint = fingerprint,
                language = currentLanguage,
            )

            val requestBuilder = Request.Builder().url(url)
            for ((k, v) in signedHeaders) {
                requestBuilder.header(k, v)
            }

            if (method.equals("POST", ignoreCase = true)) {
                requestBuilder.post((body ?: "").toRequestBody(contentType.toMediaType()))
            } else {
                requestBuilder.get()
            }

            try {
                httpClient.newCall(requestBuilder.build()).execute().use { response ->
                    absorbXUserHeader(response.header("x-user"))
                    lastStatusCode = response.code
                    val responseBody = response.body?.string().orEmpty()
                    lastBody = responseBody

                    if (response.code in MovieBoxConfig.RETRY_STATUS_CODES) {
                        lastError = MovieBoxApiException(
                            "Host $base returned HTTP ${response.code}",
                            statusCode = response.code,
                        )
                        continue
                    }

                    // Handle 441 miss token by warming up session once and retrying
                    if (response.code == 441 && allowWarmupRetry) {
                        runtimeToken = null
                        warmUpSession()
                        return@withContext executeSignedApiRequest(
                            method = method,
                            path = path,
                            params = params,
                            body = body,
                            contentType = contentType,
                            allowWarmupRetry = false,
                        )
                    }

                    activeBaseUrl = base
                    val json = try {
                        JSONObject(responseBody)
                    } catch (e: Exception) {
                        throw MovieBoxApiException(
                            "Invalid JSON from $base (HTTP ${response.code})",
                            statusCode = response.code,
                            cause = e,
                        )
                    }

                    val apiCode = json.optInt("code", -1)
                    if (apiCode == 441 && allowWarmupRetry) {
                        runtimeToken = null
                        warmUpSession()
                        return@withContext executeSignedApiRequest(
                            method = method,
                            path = path,
                            params = params,
                            body = body,
                            contentType = contentType,
                            allowWarmupRetry = false,
                        )
                    }

                    if (apiCode == 0 && json.optString("message").equals("ok", ignoreCase = true)) {
                        val dataObj = json.optJSONObject("data") ?: JSONObject()
                        Log.d("ArialTv", "API OK $base$path -> keys=${dataObj.names()}")
                        return@withContext dataObj
                    }

                    val serverMessage = json.optStringClean("message")
                        .ifEmpty { json.optStringClean("reason") }
                        .ifEmpty { "HTTP ${response.code}" }
                    Log.e("ArialTv", "API error $base$path code=$apiCode msg=$serverMessage")
                    throw MovieBoxApiException(
                        "MovieBox API error ($apiCode): $serverMessage",
                        statusCode = response.code,
                    )
                }
            } catch (e: MovieBoxApiException) {
                if (e.statusCode in MovieBoxConfig.RETRY_STATUS_CODES) {
                    lastError = e
                    continue
                }
                throw e
            } catch (e: IOException) {
                lastError = e
                continue
            }
        }

        throw MovieBoxApiException(
            message = "All MovieBox API hosts failed (last HTTP $lastStatusCode): ${lastError?.message ?: lastBody.take(120)}",
            statusCode = lastStatusCode,
            cause = lastError,
        )
    }

    private fun absorbXUserHeader(headerValue: String?) {
        if (headerValue.isNullOrBlank()) return
        try {
            val payload = JSONObject(headerValue)
            val token = payload.optString("token", "")
            if (token.isNotBlank()) {
                runtimeToken = token
            }
        } catch (_: Exception) {
            // Ignore malformed x-user header
        }
    }

    private fun parseHomepage(data: JSONObject): HomeCatalog {
        val featured = mutableListOf<MediaItem>()
        val rows = mutableListOf<CatalogRow>()
        val filters = mutableListOf<CatalogFilterOption>()
        val sections = data.optJSONArray("items") ?: JSONArray()

        for (i in 0 until sections.length()) {
            val section = sections.optJSONObject(i) ?: continue
            val secType = section.optStringClean("type")
            val title = section.optStringClean("title").ifEmpty { secType.ifEmpty { "Catalog" } }

            if (secType == "BANNER") {
                val banners = section.optJSONObject("banner")?.optJSONArray("banners") ?: continue
                for (bIdx in 0 until banners.length()) {
                    val banner = banners.optJSONObject(bIdx) ?: continue
                    val subjectObj = banner.optJSONObject("subject") ?: continue
                    val bannerImageUrl = banner.optJSONObject("image")?.optStringClean("url").orEmpty()
                    val parsed = parseSubject(subjectObj, backdropOverride = bannerImageUrl.ifBlank { null })
                    if (parsed != null) {
                        featured.add(parsed)
                    }
                }
                continue
            }

            if (secType == "FILTER") {
                val filtersArr = section.optJSONArray("filters") ?: continue
                for (fIdx in 0 until filtersArr.length()) {
                    val fObj = filtersArr.optJSONObject(fIdx) ?: continue
                    val fTitle = fObj.optStringClean("title")
                    if (fTitle.isEmpty() || fObj.optBoolean("isAll", false)) continue
                    val parsedOpt = parseFilterOption(fTitle, fObj.optStringClean("deepLink"))
                    if (parsedOpt != null && filters.none { it.label.equals(parsedOpt.label, ignoreCase = true) }) {
                        filters.add(parsedOpt)
                    }
                }
                continue
            }

            val items = mutableListOf<MediaItem>()
            val subjectsArr = section.optJSONArray("subjects")
            if (subjectsArr != null && subjectsArr.length() > 0) {
                for (j in 0 until subjectsArr.length()) {
                    val sObj = subjectsArr.optJSONObject(j) ?: continue
                    parseSubject(sObj)?.let { items.add(it) }
                }
            } else {
                val groupsArr = section.optJSONArray("groups")
                if (groupsArr != null) {
                    for (gIdx in 0 until groupsArr.length()) {
                        val gSubjs = groupsArr.optJSONObject(gIdx)?.optJSONArray("subjects") ?: continue
                        for (j in 0 until gSubjs.length()) {
                            val sObj = gSubjs.optJSONObject(j) ?: continue
                            parseSubject(sObj)?.let { items.add(it) }
                        }
                    }
                }
            }

            if (items.isNotEmpty()) {
                rows.add(CatalogRow(category = title, items = items))
            }
        }

        val finalFeatured = if (featured.isNotEmpty()) {
            featured
        } else {
            rows.firstOrNull()?.items?.take(5) ?: emptyList()
        }

        return HomeCatalog(
            featured = finalFeatured,
            rows = rows,
            filters = filters,
            sourceHost = activeBaseUrl,
        )
    }

    private fun parseFilterOption(title: String, deepLink: String): CatalogFilterOption? {
        if (deepLink.isBlank()) return CatalogFilterOption(label = title, genre = title)
        return runCatching {
            val idx = deepLink.indexOf("filterType=")
            if (idx < 0) return CatalogFilterOption(label = title, genre = title)
            val rawParam = deepLink.substring(idx + "filterType=".length).substringBefore('&')
            val decoded = URLDecoder.decode(rawParam, "UTF-8")
            val json = JSONObject(decoded)
            val genre = json.optStringClean("genre").ifEmpty { "All" }
            val year = json.optStringClean("year").ifEmpty { "All" }
            val country = json.optStringClean("country").ifEmpty { "All" }
            val sort = json.optStringClean("sort").ifEmpty { "ForYou" }
            CatalogFilterOption(
                label = title,
                genre = genre,
                year = year,
                country = country,
                sort = sort,
            )
        }.getOrElse {
            CatalogFilterOption(label = title, genre = title)
        }
    }

    private fun parseSubject(raw: JSONObject, backdropOverride: String? = null): MediaItem? {
        val id = raw.optStringClean("subjectId").ifEmpty { raw.optStringClean("id") }
        if (id.isEmpty() || id == "0") return null

        val coverUrl = raw.optJSONObject("cover")?.optStringClean("url").orEmpty()
        val stillsUrl = raw.optJSONObject("stills")?.optStringClean("url").orEmpty()
        val releaseDate = raw.optStringClean("releaseDate")
        val year = if (releaseDate.length >= 4) releaseDate.take(4) else releaseDate
        val rating = raw.optStringClean("imdbRatingValue")
            .ifEmpty { raw.optStringClean("imdbRate") }
            .takeIf { it != "0" && it != "0.0" }
            .orEmpty()

        val genreRaw = raw.opt("genre")
        val genre = when (genreRaw) {
            is JSONArray -> buildList {
                for (i in 0 until genreRaw.length()) {
                    val g = genreRaw.optString(i).trim()
                    if (g.isNotEmpty()) add(g)
                }
            }.joinToString(", ")
            is String -> genreRaw.trim()
            else -> ""
        }

        val resolvedBackdrop = when {
            !backdropOverride.isNullOrBlank() -> backdropOverride
            stillsUrl.isNotBlank() -> stillsUrl
            else -> coverUrl
        }

        return MediaItem(
            id = id,
            title = raw.optStringClean("title").ifEmpty { "Untitled" },
            year = year,
            rating = rating,
            description = raw.optStringClean("description"),
            posterUrl = coverUrl,
            backdropUrl = resolvedBackdrop,
            subjectType = raw.optIntSafe("subjectType").takeIf { it > 0 } ?: 1,
            genre = genre,
            countryName = raw.optStringClean("countryName"),
            hasResource = raw.optBoolean("hasResource", true),
        )
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}

internal fun JSONObject.optStringClean(key: String): String {
    if (!has(key) || isNull(key)) return ""
    val value = opt(key) ?: return ""
    return value.toString().trim().takeIf { it != "null" }.orEmpty()
}

internal fun JSONObject.optIntSafe(key: String): Int {
    if (!has(key) || isNull(key)) return 0
    return when (val v = opt(key)) {
        is Number -> v.toInt()
        is String -> v.trim().toIntOrNull() ?: 0
        else -> 0
    }
}

internal fun JSONObject.optLongSafe(key: String): Long {
    if (!has(key) || isNull(key)) return 0L
    return when (val v = opt(key)) {
        is Number -> v.toLong()
        is String -> v.trim().toLongOrNull() ?: 0L
        else -> 0L
    }
}

package com.example.arialtv.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.AspectRatioMode
import com.example.arialtv.data.model.CatalogLanguage
import com.example.arialtv.data.model.ConnectionMode
import com.example.arialtv.data.model.EpisodeProgress
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.PreferredCodec
import com.example.arialtv.data.model.PreferredQuality
import com.example.arialtv.data.model.SubtitleBackground
import com.example.arialtv.data.model.SubtitleColor
import com.example.arialtv.data.model.SubtitlePosition
import com.example.arialtv.data.model.SubtitleSize
import com.example.arialtv.data.model.SubtitleStyle
import com.example.arialtv.data.model.WatchProgress
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings = AppSettings(
        connectionMode = ConnectionMode.fromId(prefs.getString(KEY_MODE, null)),
        bridgeBaseUrl = prefs.getString(KEY_BRIDGE_URL, "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000",
        language = CatalogLanguage.fromCode(prefs.getString(KEY_LANGUAGE, null)),
        preferredQuality = PreferredQuality.fromResolution(prefs.getInt(KEY_QUALITY, 2160)),
        preferredCodec = PreferredCodec.fromId(prefs.getString(KEY_CODEC, null)),
        autoPlayNextEpisode = prefs.getBoolean(KEY_AUTOPLAY_NEXT, true),
        defaultPlaybackSpeed = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f),
        aspectRatioMode = AspectRatioMode.fromId(prefs.getString(KEY_ASPECT_RATIO, null)),
        controlsTimeoutSeconds = prefs.getInt(KEY_CONTROLS_TIMEOUT, 4),
        networkTimeoutSeconds = prefs.getInt(KEY_NETWORK_TIMEOUT, 25),
        subtitleStyle = SubtitleStyle(
            size = SubtitleSize.fromId(prefs.getString(KEY_SUB_SIZE, null)),
            color = SubtitleColor.fromId(prefs.getString(KEY_SUB_COLOR, null)),
            background = SubtitleBackground.fromId(prefs.getString(KEY_SUB_BG, null)),
            position = SubtitlePosition.fromId(prefs.getString(KEY_SUB_POS, null)),
        ),
    )

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_MODE, settings.connectionMode.id)
            .putString(KEY_BRIDGE_URL, settings.bridgeBaseUrl.trim())
            .putString(KEY_LANGUAGE, settings.language.code)
            .putInt(KEY_QUALITY, settings.preferredQuality.resolution)
            .putString(KEY_CODEC, settings.preferredCodec.id)
            .putBoolean(KEY_AUTOPLAY_NEXT, settings.autoPlayNextEpisode)
            .putFloat(KEY_PLAYBACK_SPEED, settings.defaultPlaybackSpeed)
            .putString(KEY_ASPECT_RATIO, settings.aspectRatioMode.id)
            .putInt(KEY_CONTROLS_TIMEOUT, settings.controlsTimeoutSeconds)
            .putInt(KEY_NETWORK_TIMEOUT, settings.networkTimeoutSeconds)
            .putString(KEY_SUB_SIZE, settings.subtitleStyle.size.id)
            .putString(KEY_SUB_COLOR, settings.subtitleStyle.color.id)
            .putString(KEY_SUB_BG, settings.subtitleStyle.background.id)
            .putString(KEY_SUB_POS, settings.subtitleStyle.position.id)
            .apply()
    }

    // --- Watch History ---

    fun loadWatchHistory(): List<WatchProgress> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val item = parseMediaItem(obj.optJSONObject("item")) ?: continue
                    add(
                        WatchProgress(
                            item = item,
                            season = obj.optInt("season", 0),
                            episode = obj.optInt("episode", 0),
                            positionMs = obj.optLong("positionMs", 0L),
                            durationMs = obj.optLong("durationMs", 0L),
                            updatedAtMs = obj.optLong("updatedAtMs", 0L),
                        ),
                    )
                }
            }.sortedByDescending { it.updatedAtMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveWatchProgress(progress: WatchProgress): List<WatchProgress> {
        val current = loadWatchHistory().toMutableList()
        current.removeAll { it.item.id == progress.item.id }
        val finishedMovie = !progress.item.isSeries && progress.progressFraction > 0.95f
        if (!finishedMovie && progress.positionMs > 3_000L) {
            current.add(0, progress.copy(updatedAtMs = System.currentTimeMillis()))
        }
        val trimmed = current.take(MAX_HISTORY_ITEMS)
        val arr = JSONArray()
        for (entry in trimmed) {
            val obj = JSONObject().apply {
                put("item", serializeMediaItem(entry.item))
                put("season", entry.season)
                put("episode", entry.episode)
                put("positionMs", entry.positionMs)
                put("durationMs", entry.durationMs)
                put("updatedAtMs", entry.updatedAtMs)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
        return trimmed
    }

    fun clearWatchHistory() {
        prefs.edit().remove(KEY_HISTORY).remove(KEY_EPISODE_PROGRESS).apply()
    }

    // --- Per-Episode Watched / Progress Map ---

    fun loadEpisodeProgressMap(): Map<String, EpisodeProgress> {
        val raw = prefs.getString(KEY_EPISODE_PROGRESS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = LinkedHashMap<String, EpisodeProgress>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val entry = obj.optJSONObject(k) ?: continue
                result[k] = EpisodeProgress(
                    positionMs = entry.optLong("pos", 0L),
                    durationMs = entry.optLong("dur", 0L),
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveEpisodeProgress(
        subjectId: String,
        season: Int,
        episode: Int,
        positionMs: Long,
        durationMs: Long,
    ): Map<String, EpisodeProgress> {
        if (season <= 0 || episode <= 0 || durationMs <= 0L) return loadEpisodeProgressMap()
        val map = loadEpisodeProgressMap().toMutableMap()
        val key = episodeKey(subjectId, season, episode)
        map[key] = EpisodeProgress(positionMs, durationMs)
        // Cap map size cleanly
        val trimmed = if (map.size > 300) {
            map.entries.toList().takeLast(300).associate { it.key to it.value }
        } else {
            map
        }
        val obj = JSONObject()
        for ((k, v) in trimmed) {
            obj.put(k, JSONObject().put("pos", v.positionMs).put("dur", v.durationMs))
        }
        prefs.edit().putString(KEY_EPISODE_PROGRESS, obj.toString()).apply()
        return trimmed
    }

    // --- Recent Search History ---

    fun loadSearchHistory(): List<String> {
        val raw = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val q = arr.optString(i, "").trim()
                    if (q.isNotEmpty()) add(q)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addSearchHistory(query: String): List<String> {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return loadSearchHistory()
        val list = loadSearchHistory().toMutableList()
        list.removeAll { it.equals(cleaned, ignoreCase = true) }
        list.add(0, cleaned)
        val trimmed = list.take(MAX_SEARCH_HISTORY)
        saveStringList(KEY_SEARCH_HISTORY, trimmed)
        return trimmed
    }

    fun removeSearchHistory(query: String): List<String> {
        val list = loadSearchHistory().filterNot { it.equals(query, ignoreCase = true) }
        saveStringList(KEY_SEARCH_HISTORY, list)
        return list
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    // --- Watchlist / My List ---

    fun loadWatchlist(): List<MediaItem> {
        val raw = prefs.getString(KEY_WATCHLIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    parseMediaItem(arr.optJSONObject(i))?.let { add(it) }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toggleWatchlist(item: MediaItem): List<MediaItem> {
        val current = loadWatchlist().toMutableList()
        val existed = current.removeAll { it.id == item.id }
        if (!existed) {
            current.add(0, item)
        }
        val trimmed = current.take(MAX_WATCHLIST_ITEMS)
        val arr = JSONArray()
        trimmed.forEach { arr.put(serializeMediaItem(it)) }
        prefs.edit().putString(KEY_WATCHLIST, arr.toString()).apply()
        return trimmed
    }

    fun clearWatchlist() {
        prefs.edit().remove(KEY_WATCHLIST).apply()
    }

    private fun saveStringList(key: String, values: List<String>) {
        val arr = JSONArray()
        values.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun serializeMediaItem(item: MediaItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("year", item.year)
        put("rating", item.rating)
        put("description", item.description)
        put("posterUrl", item.posterUrl)
        put("backdropUrl", item.backdropUrl)
        put("subjectType", item.subjectType)
        put("genre", item.genre)
        put("countryName", item.countryName)
    }

    private fun parseMediaItem(itemObj: JSONObject?): MediaItem? {
        if (itemObj == null) return null
        val id = itemObj.optString("id", "")
        if (id.isBlank()) return null
        return MediaItem(
            id = id,
            title = itemObj.optString("title", "Untitled"),
            year = itemObj.optString("year", ""),
            rating = itemObj.optString("rating", ""),
            description = itemObj.optString("description", ""),
            posterUrl = itemObj.optString("posterUrl", ""),
            backdropUrl = itemObj.optString("backdropUrl", ""),
            subjectType = itemObj.optInt("subjectType", 1),
            genre = itemObj.optString("genre", ""),
            countryName = itemObj.optString("countryName", ""),
        )
    }

    companion object {
        private const val PREFS_NAME = "arial_tv_prefs"
        private const val KEY_MODE = "connection_mode"
        private const val KEY_BRIDGE_URL = "bridge_base_url"
        private const val KEY_LANGUAGE = "catalog_language"
        private const val KEY_QUALITY = "preferred_quality"
        private const val KEY_CODEC = "preferred_codec"
        private const val KEY_AUTOPLAY_NEXT = "autoplay_next_ep"
        private const val KEY_PLAYBACK_SPEED = "default_playback_speed"
        private const val KEY_ASPECT_RATIO = "aspect_ratio_mode"
        private const val KEY_CONTROLS_TIMEOUT = "controls_timeout_sec"
        private const val KEY_NETWORK_TIMEOUT = "network_timeout_sec"
        private const val KEY_SUB_SIZE = "sub_size"
        private const val KEY_SUB_COLOR = "sub_color"
        private const val KEY_SUB_BG = "sub_bg"
        private const val KEY_SUB_POS = "sub_pos"
        private const val KEY_HISTORY = "watch_history_v1"
        private const val KEY_EPISODE_PROGRESS = "episode_progress_v1"
        private const val KEY_SEARCH_HISTORY = "search_history_v1"
        private const val KEY_WATCHLIST = "watchlist_v1"
        private const val MAX_HISTORY_ITEMS = 24
        private const val MAX_SEARCH_HISTORY = 15
        private const val MAX_WATCHLIST_ITEMS = 40

        fun episodeKey(subjectId: String, season: Int, episode: Int): String =
            "$subjectId:$season:$episode"
    }
}

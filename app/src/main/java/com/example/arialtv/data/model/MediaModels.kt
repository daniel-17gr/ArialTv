package com.example.arialtv.data.model

import androidx.compose.runtime.Immutable

@Immutable
enum class CatalogLanguage(
    val code: String,
    val label: String,
    val localeTag: String,
    val localeHyphen: String,
    val acceptLanguage: String,
    val country: String,
    val timezone: String,
) {
    ENGLISH(
        code = "en",
        label = "English",
        localeTag = "en_US",
        localeHyphen = "en-US",
        acceptLanguage = "en-US,en;q=0.9",
        country = "US",
        timezone = "UTC",
    ),
    FRENCH(
        code = "fr",
        label = "Français",
        localeTag = "fr_MG",
        localeHyphen = "fr-MG",
        acceptLanguage = "fr-MG,fr-FR;q=0.95,fr;q=0.9,en-US;q=0.6,en;q=0.5",
        country = "MG",
        timezone = "Indian/Antananarivo",
    );

    companion object {
        fun fromCode(code: String?): CatalogLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

@Immutable
enum class ConnectionMode(val id: String, val label: String) {
    STANDALONE_V3("standalone", "Direct"),
    CUSTOM_BRIDGE("bridge", "Custom Server");

    companion object {
        fun fromId(id: String?): ConnectionMode =
            entries.firstOrNull { it.id == id } ?: STANDALONE_V3
    }
}

@Immutable
enum class PreferredQuality(val resolution: Int, val label: String) {
    AUTO_BEST(2160, "Auto"),
    P1080(1080, "1080p"),
    P720(720, "720p"),
    P480(480, "480p"),
    P360(360, "360p");

    companion object {
        fun fromResolution(res: Int): PreferredQuality =
            entries.firstOrNull { it.resolution == res } ?: AUTO_BEST
    }
}

@Immutable
enum class PreferredCodec(val id: String, val label: String) {
    AUTO_HARDWARE("auto", "Auto"),
    H264("h264", "H.264"),
    HEVC("hevc", "HEVC");

    companion object {
        fun fromId(id: String?): PreferredCodec =
            entries.firstOrNull { it.id == id } ?: AUTO_HARDWARE
    }
}

@Immutable
enum class SubtitleSize(val id: String, val label: String, val fraction: Float) {
    SMALL("small", "Small", 0.044f),
    MEDIUM("medium", "Medium", 0.056f),
    LARGE("large", "Large", 0.072f),
    EXTRA_LARGE("xlarge", "Extra Large", 0.088f);

    companion object {
        fun fromId(id: String?): SubtitleSize =
            entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

@Immutable
enum class SubtitleColor(val id: String, val label: String, val argb: Int) {
    WHITE("white", "White", 0xFFFFFFFF.toInt()),
    AMBER("amber", "Yellow", 0xFFFDE047.toInt()),
    ICE("ice", "Ice", 0xFFE0F2FE.toInt()),
    MINT("mint", "Mint", 0xFFBBF7D0.toInt());

    companion object {
        fun fromId(id: String?): SubtitleColor =
            entries.firstOrNull { it.id == id } ?: WHITE
    }
}

@Immutable
enum class SubtitleBackground(val id: String, val label: String, val argb: Int) {
    NONE("none", "Drop Shadow", 0x00000000),
    TRANSLUCENT("translucent", "Soft Box", 0x99000000.toInt()),
    SOLID("solid", "Solid Black", 0xE6000000.toInt());

    companion object {
        fun fromId(id: String?): SubtitleBackground =
            entries.firstOrNull { it.id == id } ?: TRANSLUCENT
    }
}

@Immutable
enum class SubtitlePosition(val id: String, val label: String, val bottomPaddingFraction: Float) {
    BOTTOM("bottom", "Standard", 0.07f),
    RAISED("raised", "Raised", 0.16f);

    companion object {
        fun fromId(id: String?): SubtitlePosition =
            entries.firstOrNull { it.id == id } ?: BOTTOM
    }
}

@Immutable
data class SubtitleStyle(
    val size: SubtitleSize = SubtitleSize.MEDIUM,
    val color: SubtitleColor = SubtitleColor.WHITE,
    val background: SubtitleBackground = SubtitleBackground.TRANSLUCENT,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
)

@Immutable
enum class AspectRatioMode(val id: String, val label: String) {
    FIT("fit", "Fit"),
    ZOOM("zoom", "Zoom"),
    FILL("fill", "Stretch");

    companion object {
        fun fromId(id: String?): AspectRatioMode =
            entries.firstOrNull { it.id == id } ?: FIT
    }
}

@Immutable
enum class PreferredAudioLanguage(
    val id: String,
    val label: String,
    val matchCode: String,
) {
    ORIGINAL("original", "Original", ""),
    ENGLISH("en", "English", "en"),
    FRENCH("fr", "Français", "fr"),
    SPANISH("es", "Español", "es"),
    ARABIC("ar", "Arabic", "ar"),
    HINDI("hi", "Hindi", "hi"),
    PORTUGUESE("pt", "Português", "pt");

    companion object {
        fun fromId(id: String?): PreferredAudioLanguage =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ORIGINAL
    }
}

@Immutable
data class AppSettings(
    val connectionMode: ConnectionMode = ConnectionMode.STANDALONE_V3,
    val bridgeBaseUrl: String = "http://10.0.2.2:8000",
    val language: CatalogLanguage = CatalogLanguage.ENGLISH,
    val preferredAudioLanguage: PreferredAudioLanguage = PreferredAudioLanguage.ORIGINAL,
    val preferredQuality: PreferredQuality = PreferredQuality.AUTO_BEST,
    val preferredCodec: PreferredCodec = PreferredCodec.AUTO_HARDWARE,
    val autoPlayNextEpisode: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val controlsTimeoutSeconds: Int = 4,
    val networkTimeoutSeconds: Int = 25,
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val adultFilterEnabled: Boolean = true,
)

@Immutable
data class MediaItem(
    val id: String,
    val title: String,
    val year: String = "",
    val rating: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val subjectType: Int = 1, // 1 = Movie, 2 = TV Series
    val genre: String = "",
    val countryName: String = "",
    val hasResource: Boolean = true,
) {
    val isSeries: Boolean get() = subjectType == 2
    val contentTypeBadge: String
        get() {
            val g = genre.lowercase()
            return when {
                subjectType == 7 -> "SHORT"
                subjectType == 6 || g.contains("music") -> "MUSIC"
                g.contains("documentary") -> "DOC"
                subjectType == 2 -> "SERIES"
                else -> "MOVIE"
            }
        }
    val mediaTypeLabel: String get() = contentTypeBadge
    val effectiveBackdropUrl: String get() = backdropUrl.ifBlank { posterUrl }
}

@Immutable
data class CatalogFilterOption(
    val label: String,
    val genre: String = "All",
    val year: String = "All",
    val country: String = "All",
    val sort: String = "ForYou",
)

@Immutable
data class CategoryQuery(
    val title: String,
    val subtitle: String = "",
    val channelId: Int = 1, // 1 = Movies, 2 = TV Series
    val genre: String = "All",
    val year: String = "All",
    val country: String = "All",
    val sort: String = "Hottest",
    val minRating: Double = 0.0,
    val seedItems: List<MediaItem> = emptyList(),
)

@Immutable
data class CatalogRow(
    val category: String,
    val items: List<MediaItem>,
    val expandQuery: CategoryQuery? = null,
)

@Immutable
data class HomeCatalog(
    val featured: List<MediaItem>,
    val rows: List<CatalogRow>,
    val filters: List<CatalogFilterOption> = emptyList(),
    val sourceHost: String = "",
)

@Immutable
data class TrendingSearchRank(
    val movies: List<MediaItem> = emptyList(),
    val series: List<MediaItem> = emptyList(),
    val everyoneSearch: List<String> = emptyList(),
)

@Immutable
data class FilteredCatalogPage(
    val items: List<MediaItem>,
    val page: Int,
    val hasMore: Boolean,
    val totalCount: Int,
)

@Immutable
data class DubOption(
    val subjectId: String,
    val languageName: String,
    val languageCode: String,
    val isOriginal: Boolean,
)

@Immutable
data class SeasonInfo(
    val seasonNumber: Int,
    val episodeCount: Int,
    val availableResolutions: List<Int>,
)

@Immutable
data class StaffMember(
    val id: String,
    val name: String,
    val character: String,
    val avatarUrl: String,
)

@Immutable
data class MediaDetails(
    val item: MediaItem,
    val durationSeconds: Int = 0,
    val totalSeasons: Int = 0,
    val dubs: List<DubOption> = emptyList(),
    val seasons: List<SeasonInfo> = emptyList(),
    val staff: List<StaffMember> = emptyList(),
    val availableResolutions: List<Int> = emptyList(),
)

@Immutable
data class StreamResource(
    val resourceId: String,
    val url: String,
    val title: String,
    val resolution: Int,
    val codec: String,
    val season: Int,
    val episode: Int,
    val durationSeconds: Int,
    val sizeBytes: Long,
    val headers: Map<String, String> = emptyMap(),
) {
    val qualityLabel: String
        get() = when {
            resolution >= 2160 -> "4K"
            resolution > 0 -> "${resolution}p"
            else -> "Auto"
        }

    val codecLabel: String
        get() = when (codec.lowercase()) {
            "h264", "avc" -> "H.264"
            "hevc", "h265" -> "HEVC"
            "" -> "HD"
            else -> codec.uppercase()
        }
}

@Immutable
data class SubtitleTrack(
    val id: String,
    val languageCode: String,
    val languageName: String,
    val url: String,
    val delayMs: Long = 0L,
)

@Immutable
data class ResolvedPlayback(
    val subjectId: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val selectedStream: StreamResource,
    val availableStreams: List<StreamResource>,
    val subtitles: List<SubtitleTrack>,
    val sourceLabel: String,
    val dubs: List<DubOption> = emptyList(),
    val activeAudioLabel: String = "",
)

@Immutable
data class EpisodeProgress(
    val positionMs: Long,
    val durationMs: Long,
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isWatched: Boolean get() = progressFraction >= 0.90f
}

@Immutable
data class WatchProgress(
    val item: MediaItem,
    val season: Int = 0,
    val episode: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val episodeTag: String
        get() = if (season > 0 && episode > 0) "S${season} E${episode}" else ""
}

@Immutable
data class ConnectionTestResult(
    val success: Boolean,
    val host: String,
    val latencyMs: Long,
    val hasSessionToken: Boolean,
    val sectionCount: Int,
    val message: String,
)

object AppMetadata {
    const val APP_NAME = "Arial TV"
    const val VERSION_NAME = "1.6.0"
    const val DEVELOPER_NAME = "Auris Daniel"
    const val GITHUB_HANDLE = "github.com/daniel-17gr"
    const val GITHUB_REPO_URL = "https://github.com/daniel-17gr/ArialTv"
    const val GITHUB_RELEASES_API = "https://api.github.com/repos/daniel-17gr/ArialTv/releases/latest"
    val SLEEP_TIMER_OPTIONS: List<Int> = listOf(0, 15, 30, 45, 60, 90, 120, 180, 240)

    fun formatSleepLabel(minutes: Int): String = when {
        minutes <= 0 -> "Off"
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    fun formatSleepLongLabel(minutes: Int): String = when {
        minutes <= 0 -> "Off"
        minutes < 60 -> "$minutes min"
        minutes == 60 -> "1 hour"
        minutes % 60 == 0 -> "${minutes / 60} hours"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

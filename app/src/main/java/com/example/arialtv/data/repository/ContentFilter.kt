package com.example.arialtv.data.repository

import com.example.arialtv.data.model.MediaItem
import java.util.Calendar
import java.util.Locale

/**
 * Multi-layer safety & quality gate grounded in live MovieBox v3 catalog payloads:
 * 1. Strips adult / erotic / softcore / 18+ / NSFW / short-drama-spam titles & genres.
 * 2. Strips low-quality / unrated / cam-rip / future-placeholder items from Home suggestions.
 * 3. Cleans emoji-heavy category titles for crisp 10-foot TV typography.
 */
object ContentFilter {

    private val BLOCKED_GENRE_TOKENS: Set<String> = setOf(
        "adult",
        "aiadult",
        "ai adult",
        "erotic",
        "erotica",
        "hot",
        "18+",
        "nsfw",
        "hentai",
        "ecchi",
        "softcore",
        "porn",
        "porno",
        "pornography",
        "xxx",
        "smut",
        "bdsm",
        "fetish",
        "sex",
        "sexy",
        "nudity",
        "nude",
        "uncensored",
        "jav",
        "pinoy sexy",
        "vivamax",
        "r-18",
        "nc-17",
        "x-rated",
    )

    private val BLOCKED_GENRE_SUBSTRINGS: List<String> = listOf(
        "erotic",
        "adult",
        "hentai",
        "porn",
        "softcore",
        "nsfw",
        "fetish",
        "bdsm",
        "smut",
        "18+",
        "xxx",
    )

    private val EXPLICIT_TITLE_REGEX = Regex(
        pattern = """(?i)(?:\b(?:erotic\w*|porn\w*|xxx|hentai|ecchi|softcore|fetish\w*|prostitut\w*|sexology|pleasure\s+toy|pinoy\s+sexy|vivamax|bible\s+black|f\*+ck\w*|fuck\w*|pussy|step\s*sis\w*|step\s*mom\w*|milf|orgasm\w*|threesome|kama\s*sutra|kamasutra|playboy|stripper|escort|i\s+want\s+your\s+sex|sex\s+drive|bold\s+web|ullu|kooku|altbalaji|bhabhi|nudity|smut|bdsm|nsfw|sexual\s+fetish|dutdot|laplap)\b|(?:^|\W)18\+(?:\W|$))""",
    )

    private val EXPLICIT_DESC_REGEX = Regex(
        pattern = """(?i)\b(erotic\w*|porn\w*|hentai|softcore|sexual\s+fetish|explicit\s+sex|hardcore\s+sex|adult\s+film|adult\s+video|xxx|prostitut\w*|vivamax|pinoy\s+sexy|nsfw)\b""",
    )

    private val SOFTCORE_SPAM_COUNTRIES: Set<String> = setOf("philippines", "india")
    private val SOFTCORE_SPAM_TITLE_REGEX = Regex("""(?i)\b(sexy|seduction|lust|affair)\b""")

    private val BLOCKED_SEARCH_TERM_REGEX = Regex(
        pattern = """(?i)(?:\b(?:adult|erotic\w*|sexy|sex|porn\w*|hentai|nsfw|vivamax|fetish\w*|xxx)\b|(?:^|\W)18\+(?:\W|$))""",
    )

    private val EMOJI_AND_DECOR_REGEX = Regex("""[\p{So}\p{Cn}\uFE0E\uFE0F\u200D]+""")
    private val MULTI_SPACE_REGEX = Regex("""\s{2,}""")

    fun isSafeContent(item: MediaItem, filterEnabled: Boolean = true): Boolean {
        if (item.id.isBlank() || item.title.isBlank()) return false
        if (!filterEnabled) return true

        // MovieBox subjectType 7 = vertical short dramas / AiAdult micro-dramas
        if (item.subjectType == 7) return false

        val normalizedGenre = item.genre.lowercase(Locale.US)
        if (normalizedGenre.isNotBlank()) {
            val tokens = normalizedGenre
                .split(',', '/', '|', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (tokens.any { it in BLOCKED_GENRE_TOKENS }) return false
            if (BLOCKED_GENRE_SUBSTRINGS.any { normalizedGenre.contains(it) }) return false
        }

        val cleanTitle = item.title.replace('\u2019', '\'').replace('\u2018', '\'')
        val cleanDesc = item.description.replace('\u2019', '\'').replace('\u2018', '\'')

        if (EXPLICIT_TITLE_REGEX.containsMatchIn(cleanTitle)) return false
        if (cleanDesc.isNotBlank() && EXPLICIT_DESC_REGEX.containsMatchIn(cleanDesc)) return false

        val normalizedCountry = item.countryName.trim().lowercase(Locale.US)
        if (normalizedCountry in SOFTCORE_SPAM_COUNTRIES &&
            normalizedGenre.contains("romance") &&
            cleanDesc.isBlank() &&
            SOFTCORE_SPAM_TITLE_REGEX.containsMatchIn(cleanTitle)
        ) {
            return false
        }

        return true
    }

    fun filterSafeList(items: List<MediaItem>, filterEnabled: Boolean = true): List<MediaItem> =
        items.filter { isSafeContent(it, filterEnabled) }

    fun isSafeSearchTerm(term: String, filterEnabled: Boolean = true): Boolean {
        val cleaned = term.trim()
        if (cleaned.isEmpty()) return false
        if (!filterEnabled) return true
        return !BLOCKED_SEARCH_TERM_REGEX.containsMatchIn(cleaned) &&
            !EXPLICIT_TITLE_REGEX.containsMatchIn(cleaned)
    }

    fun isQualityHomeItem(
        item: MediaItem,
        filterEnabled: Boolean = true,
        minRating: Double = 5.5,
        currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    ): Boolean {
        if (!isSafeContent(item, filterEnabled)) return false
        if (item.posterUrl.isBlank() || !item.hasResource) return false
        if (item.subjectType != 1 && item.subjectType != 2) return false

        val lowTitle = item.title.lowercase(Locale.US)
        if (lowTitle.contains("[cam]") || lowTitle.contains("(cam)") || lowTitle.contains("hdcam")) {
            return false
        }

        val yearInt = item.year.trim().toIntOrNull()
        if (yearInt != null && yearInt > currentYear) return false

        val score = item.rating.trim().toDoubleOrNull() ?: 0.0
        if (score < minRating) return false

        return true
    }

    fun cleanCategoryTitle(raw: String): String {
        val stripped = EMOJI_AND_DECOR_REGEX.replace(raw, " ")
        return MULTI_SPACE_REGEX.replace(stripped, " ").trim()
    }
}

package com.example.arialtv.data.moviebox

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Shifts SRT and WebVTT cue timestamps cleanly by a user-specified offset (ms)
 * so out-of-sync external subtitles can be fixed live inside the player.
 */
object SubtitleShiftHelper {
    private val rawSubtitleCache = ConcurrentHashMap<String, String>()

    // Matches HH:MM:SS,mmm or HH:MM:SS.mmm or MM:SS.mmm
    private val timestampRegex = Regex("""(\d{1,2}:)?(\d{2}):(\d{2})([,.])(\d{3})""")

    suspend fun getShiftedSubtitleFile(
        context: Context,
        url: String,
        offsetMs: Long,
        httpClient: OkHttpClient,
    ): File? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        val raw = rawSubtitleCache[url] ?: runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", MovieBoxConfig.CDN_USER_AGENT)
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                resp.body?.string()
            }
        }.getOrNull()?.also { text ->
            if (rawSubtitleCache.size > 12) rawSubtitleCache.clear()
            rawSubtitleCache[url] = text
        } ?: return@withContext null

        val shifted = shiftSubtitleText(raw, offsetMs)
        val ext = if (url.contains(".vtt", ignoreCase = true) || raw.trimStart().startsWith("WEBVTT")) "vtt" else "srt"
        val outFile = File(context.cacheDir, "arial_sub_shifted_${url.hashCode()}_${offsetMs}.$ext")
        runCatching {
            outFile.writeText(shifted, Charsets.UTF_8)
            outFile
        }.getOrNull()
    }

    fun shiftSubtitleText(rawText: String, offsetMs: Long): String {
        if (offsetMs == 0L) return rawText
        val lines = rawText.lines()
        val sb = StringBuilder(rawText.length)
        for (idx in lines.indices) {
            val line = lines[idx]
            if (line.contains("-->")) {
                val shiftedLine = timestampRegex.replace(line) { match ->
                    val hoursStr = match.groupValues[1].removeSuffix(":")
                    val hours = hoursStr.toLongOrNull() ?: 0L
                    val mins = match.groupValues[2].toLongOrNull() ?: 0L
                    val secs = match.groupValues[3].toLongOrNull() ?: 0L
                    val sep = match.groupValues[4]
                    val millis = match.groupValues[5].toLongOrNull() ?: 0L

                    val totalMs = ((hours * 3600L + mins * 60L + secs) * 1000L + millis + offsetMs).coerceAtLeast(0L)
                    val newH = totalMs / 3_600_000L
                    val newM = (totalMs % 3_600_000L) / 60_000L
                    val newS = (totalMs % 60_000L) / 1_000L
                    val newMs = totalMs % 1_000L

                    if (hoursStr.isEmpty() && newH == 0L) {
                        String.format(Locale.US, "%02d:%02d%s%03d", newM, newS, sep, newMs)
                    } else {
                        String.format(Locale.US, "%02d:%02d:%02d%s%03d", newH, newM, newS, sep, newMs)
                    }
                }
                sb.append(shiftedLine)
            } else {
                sb.append(line)
            }
            if (idx < lines.lastIndex) sb.append('\n')
        }
        return sb.toString()
    }
}

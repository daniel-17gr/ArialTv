package com.example.arialtv.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import com.example.arialtv.data.model.AppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

@Immutable
data class GithubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val apkAssetName: String?,
    val apkSizeBytes: Long,
    val htmlUrl: String,
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: GithubReleaseInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class NoReleaseYet(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

sealed interface InstallLaunchResult {
    data object InstallerLaunched : InstallLaunchResult
    data object NeedsUnknownSourcesPermission : InstallLaunchResult
    data class Failed(val reason: String) : InstallLaunchResult
}

object AppUpdater {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = parseVersionParts(remote)
        val currentParts = parseVersionParts(current)
        if (remoteParts.isEmpty()) return false
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseVersionParts(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        if (cleaned.isEmpty()) return emptyList()
        return cleaned.split('.').map { segment ->
            segment.trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    fun parseReleaseJson(rawJson: String): GithubReleaseInfo {
        val json = JSONObject(rawJson)
        val tagName = json.optString("tag_name", "").trim()
        val versionName = tagName.removePrefix("v").removePrefix("V")
        val releaseNotes = json.optString("body", "").trim()
        val htmlUrl = json.optString("html_url", AppMetadata.GITHUB_REPO_URL).trim()

        var bestUrl: String? = null
        var bestName: String? = null
        var bestSize = 0L

        val assets = json.optJSONArray("assets")
        if (assets != null) {
            var fallbackUrl: String? = null
            var fallbackName: String? = null
            var fallbackSize = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "").trim()
                val dlUrl = asset.optString("browser_download_url", "").trim()
                val size = asset.optLong("size", 0L)
                if (name.endsWith(".apk", ignoreCase = true) && dlUrl.startsWith("http")) {
                    if (name.contains("release", ignoreCase = true)) {
                        bestUrl = dlUrl
                        bestName = name
                        bestSize = size
                        break
                    } else if (fallbackUrl == null) {
                        fallbackUrl = dlUrl
                        fallbackName = name
                        fallbackSize = size
                    }
                }
            }
            if (bestUrl == null) {
                bestUrl = fallbackUrl
                bestName = fallbackName
                bestSize = fallbackSize
            }
        }

        return GithubReleaseInfo(
            tagName = tagName.ifBlank { "v$versionName" },
            versionName = versionName,
            releaseNotes = releaseNotes,
            apkDownloadUrl = bestUrl,
            apkAssetName = bestName,
            apkSizeBytes = bestSize,
            htmlUrl = htmlUrl,
        )
    }

    suspend fun checkLatestRelease(
        currentVersion: String = AppMetadata.VERSION_NAME,
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(AppMetadata.GITHUB_RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ArialTV/$currentVersion")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                when {
                    resp.code == 404 -> UpdateCheckResult.NoReleaseYet(currentVersion)
                    !resp.isSuccessful -> UpdateCheckResult.Error("Couldn't check updates right now (HTTP ${resp.code})")
                    else -> {
                        val body = resp.body?.string().orEmpty()
                        if (body.isBlank()) {
                            return@use UpdateCheckResult.Error("Empty response from GitHub")
                        }
                        val info = parseReleaseJson(body)
                        if (info.versionName.isBlank()) {
                            UpdateCheckResult.UpToDate(currentVersion)
                        } else if (isNewerVersion(info.versionName, currentVersion)) {
                            UpdateCheckResult.UpdateAvailable(info)
                        } else {
                            UpdateCheckResult.UpToDate(currentVersion)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            UpdateCheckResult.Error("Unable to reach GitHub right now.")
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { old ->
            if (old.isFile && old.name.endsWith(".apk", ignoreCase = true)) {
                runCatching { old.delete() }
            }
        }

        val safeName = fileName.trim()
            .ifBlank { "ArialTv-update.apk" }
            .replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
            .let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
        val destFile = File(updatesDir, safeName)

        val req = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "ArialTV/${AppMetadata.VERSION_NAME}")
            .get()
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("Download failed (HTTP ${resp.code})")
            }
            val body = resp.body ?: throw IOException("Empty APK response")
            val totalBytes = body.contentLength()
            var downloaded = 0L
            var lastPercent = -1

            onProgress(0)
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0L) {
                            val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastPercent) {
                                lastPercent = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.flush()
                }
            }
            onProgress(100)
        }

        if (!isValidApkFile(destFile)) {
            runCatching { destFile.delete() }
            throw IOException("Downloaded update file was incomplete or invalid")
        }
        destFile
    }

    private fun isValidApkFile(file: File): Boolean {
        if (!file.exists() || file.length() <= 100_000L) return false
        return runCatching {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                val read = fis.read(magic)
                read == 4 &&
                    magic[0] == 0x50.toByte() && // 'P'
                    magic[1] == 0x4B.toByte() && // 'K'
                    magic[2] == 0x03.toByte() &&
                    magic[3] == 0x04.toByte()
            }
        }.getOrDefault(false)
    }

    fun launchApkInstaller(context: Context, apkFile: File): InstallLaunchResult {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return InstallLaunchResult.Failed("Update APK file not found")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val opened = runCatching {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
            }.isSuccess
            return if (opened) {
                InstallLaunchResult.NeedsUnknownSourcesPermission
            } else {
                InstallLaunchResult.Failed("Enable Install Unknown Apps for Arial TV in Android Settings")
            }
        }

        return runCatching {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            InstallLaunchResult.InstallerLaunched
        }.getOrElse { err ->
            InstallLaunchResult.Failed(err.message ?: "Unable to launch package installer")
        }
    }
}

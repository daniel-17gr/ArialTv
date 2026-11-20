package com.example.arialtv.data.moviebox

import com.example.arialtv.data.model.CatalogLanguage
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic request signing for MovieBox v3 Android REST API.
 * Mirrors moviebox_api_v3.v3.crypto bit-for-bit.
 */
object MovieBoxCrypto {

    fun md5Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(data)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun base64DecodePadded(value: String): ByteArray {
        val padLen = (4 - value.length % 4) % 4
        return Base64.getDecoder().decode(value + "=".repeat(padLen))
    }

    fun base64Encode(data: ByteArray): String = Base64.getEncoder().encodeToString(data)

    /**
     * Generates X-Client-Token: "<ts>,<md5(reverse(<ts>))>"
     */
    fun generateXClientToken(timestampMs: Long = System.currentTimeMillis()): String {
        val ts = timestampMs.toString()
        val reversed = ts.reversed()
        val hash = md5Hex(reversed.toByteArray(Charsets.UTF_8))
        return "$ts,$hash"
    }

    /**
     * Rebuilds query string with keys sorted alphabetically and decoded values (not percent-encoded),
     * preserving empty query values such as `version=`.
     */
    fun sortedQueryString(url: HttpUrl): String {
        if (url.querySize == 0) return ""
        val parts = mutableListOf<String>()
        for (key in url.queryParameterNames.sorted()) {
            for (value in url.queryParameterValues(key)) {
                parts.add("$key=${value ?: ""}")
            }
        }
        return parts.joinToString("&")
    }

    fun sortedQueryString(url: String): String = sortedQueryString(url.toHttpUrl())

    fun buildCanonicalString(
        method: String,
        accept: String?,
        contentType: String?,
        url: HttpUrl,
        body: String?,
        timestampMs: Long,
    ): String {
        val path = url.encodedPath
        val query = sortedQueryString(url)
        val canonicalUrl = if (query.isNotEmpty()) "$path?$query" else path

        val bodyBytes = body?.toByteArray(Charsets.UTF_8)
        val bodyHash: String
        val bodyLength: String
        if (bodyBytes != null) {
            val truncated = if (bodyBytes.size > MovieBoxConfig.SIGNATURE_BODY_MAX_BYTES) {
                bodyBytes.copyOf(MovieBoxConfig.SIGNATURE_BODY_MAX_BYTES)
            } else {
                bodyBytes
            }
            bodyHash = md5Hex(truncated)
            bodyLength = bodyBytes.size.toString()
        } else {
            bodyHash = ""
            bodyLength = ""
        }

        return buildString {
            append(method.uppercase()).append('\n')
            append(accept.orEmpty()).append('\n')
            append(contentType.orEmpty()).append('\n')
            append(bodyLength).append('\n')
            append(timestampMs).append('\n')
            append(bodyHash).append('\n')
            append(canonicalUrl)
        }
    }

    /**
     * Generates x-tr-signature: "<ts>|2|<base64(hmac-md5(canonical, key))>"
     */
    fun generateXTrSignature(
        method: String,
        accept: String?,
        contentType: String?,
        url: HttpUrl,
        body: String? = null,
        useAltKey: Boolean = false,
        timestampMs: Long = System.currentTimeMillis(),
    ): String {
        val canonical = buildCanonicalString(method, accept, contentType, url, body, timestampMs)
        val secretB64 = if (useAltKey) MovieBoxConfig.SECRET_KEY_ALT else MovieBoxConfig.SECRET_KEY_DEFAULT
        val secretBytes = base64DecodePadded(secretB64)
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(secretBytes, "HmacMD5"))
        val sigB64 = base64Encode(mac.doFinal(canonical.toByteArray(Charsets.UTF_8)))
        return "$timestampMs|2|$sigB64"
    }

    fun generateXTrSignature(
        method: String,
        accept: String?,
        contentType: String?,
        url: String,
        body: String? = null,
        useAltKey: Boolean = false,
        timestampMs: Long = System.currentTimeMillis(),
    ): String = generateXTrSignature(
        method = method,
        accept = accept,
        contentType = contentType,
        url = url.toHttpUrl(),
        body = body,
        useAltKey = useAltKey,
        timestampMs = timestampMs,
    )

    fun buildSignedHeaders(
        method: String,
        url: HttpUrl,
        accept: String = "application/json",
        contentType: String = "application/json",
        body: String? = null,
        includePlayMode: Boolean = false,
        authToken: String? = null,
        fingerprint: MovieBoxConfig.ClientFingerprint,
        language: CatalogLanguage,
        timestampMs: Long = System.currentTimeMillis(),
    ): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        headers["User-Agent"] = fingerprint.userAgent
        headers["Accept"] = accept
        headers["Content-Type"] = contentType
        headers["Accept-Language"] = language.acceptLanguage
        headers["Accept-Country"] = language.country
        headers["Accept-Timezone"] = language.timezone
        headers["X-Language"] = language.code
        headers["X-Locale"] = language.localeHyphen
        headers["X-Region"] = language.country
        headers["X-Country"] = language.country
        headers["Connection"] = "keep-alive"
        headers["X-Client-Token"] = generateXClientToken(timestampMs)
        headers["x-tr-signature"] = generateXTrSignature(
            method = method,
            accept = accept,
            contentType = contentType,
            url = url,
            body = body,
            useAltKey = false,
            timestampMs = timestampMs,
        )
        headers["X-Client-Info"] = fingerprint.clientInfoJson
        headers["X-Client-Status"] = "0"

        if (!authToken.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $authToken"
        }
        if (includePlayMode) {
            headers["X-Play-Mode"] = "2"
        }
        return headers
    }
}

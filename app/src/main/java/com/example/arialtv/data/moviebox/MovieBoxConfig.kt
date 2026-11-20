package com.example.arialtv.data.moviebox

import com.example.arialtv.data.model.CatalogLanguage
import java.security.SecureRandom
import java.util.UUID

object MovieBoxConfig {
    const val SECRET_KEY_DEFAULT = "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O"
    const val SECRET_KEY_ALT = "Xqn2nnO41/L92o1iuXhSLHTbXvY4Z5ZZ62m8mSLA"
    const val SIGNATURE_BODY_MAX_BYTES = 102_400

    val HOST_POOL: List<String> = listOf(
        "https://api6.aoneroom.com",
        "https://api5.aoneroom.com",
        "https://api4.aoneroom.com",
        "https://api4sg.aoneroom.com",
        "https://api3.aoneroom.com",
        "https://api6sg.aoneroom.com",
        "https://api.inmoviebox.com",
    )

    val RETRY_STATUS_CODES: Set<Int> = setOf(403, 407, 429, 500, 502, 503, 504)

    const val MAIN_PAGE_PATH = "/wefeed-mobile-bff/tab-operating"
    const val SEARCH_PATH = "/wefeed-mobile-bff/subject-api/search"
    const val SEARCH_RANK_PATH = "/wefeed-mobile-bff/subject-api/search-rank"
    const val FILTER_LIST_PATH = "/wefeed-mobile-bff/subject-api/list"
    const val SUBJECT_GET_PATH = "/wefeed-mobile-bff/subject-api/get"
    const val SEASON_INFO_PATH = "/wefeed-mobile-bff/subject-api/season-info"
    const val RESOURCE_PATH = "/wefeed-mobile-bff/subject-api/resource"
    const val EXT_CAPTIONS_PATH = "/wefeed-mobile-bff/subject-api/get-ext-captions"

    const val CDN_USER_AGENT = "okhttp/4.12.0"

    private val random = SecureRandom()
    private val hexChars = "0123456789abcdef"

    data class ClientFingerprint(
        val userAgent: String,
        val clientInfoJson: String,
        val deviceId: String,
        val gaid: String,
    )

    fun createFingerprint(
        language: CatalogLanguage,
        existingDeviceId: String? = null,
        existingGaid: String? = null,
    ): ClientFingerprint {
        val deviceId = existingDeviceId ?: randomHex(32)
        val gaid = existingGaid ?: UUID.randomUUID().toString()
        val versionCode = 50020046
        val osVersion = "13"
        val buildId = "TQ2A.230405.003"
        val brand = "Redmi"
        val model = "23078RKD5C"
        val spCode = if (language == CatalogLanguage.FRENCH) "64601" else ""

        val userAgent = "com.community.oneroom/$versionCode " +
            "(Linux; U; Android $osVersion; ${language.localeTag}; " +
            "$model; Build/$buildId; Cronet/135.0.7012.3)"

        val clientInfoJson = buildString {
            append("""{"package_name":"com.community.oneroom","version_name":"3.0.03.0529.03",""")
            append(""""version_code":$versionCode,"os":"android","os_version":"$osVersion",""")
            append(""""install_ch":"ps","device_id":"$deviceId","install_store":"ps",""")
            append(""""gaid":"$gaid","brand":"$brand","model":"$model",""")
            append(""""system_language":"${language.code}","net":"NETWORK_WIFI",""")
            append(""""region":"${language.country}","country":"${language.country}",""")
            append(""""timezone":"${language.timezone}","sp_code":"$spCode",""")
            append(""""language":"${language.code}","locale":"${language.localeTag}",""")
            append(""""preferred_language":"${language.code}","X-Play-Mode":"2"}""")
        }

        return ClientFingerprint(
            userAgent = userAgent,
            clientInfoJson = clientInfoJson,
            deviceId = deviceId,
            gaid = gaid,
        )
    }

    private fun randomHex(length: Int): String = buildString(length) {
        repeat(length) {
            append(hexChars[random.nextInt(hexChars.length)])
        }
    }
}

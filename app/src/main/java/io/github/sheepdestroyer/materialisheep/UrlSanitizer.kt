package io.github.sheepdestroyer.materialisheep

import android.net.Uri
import java.util.Locale

/**
 * Utility for sanitizing URLs by stripping marketing trackers, ad attribution IDs,
 * and analytics query parameters (e.g., utm_*, fbclid, gclid, etc.) to enhance user privacy.
 */
object UrlSanitizer {

    private val TRACKING_PARAMS = setOf(
        // Google & Google Analytics
        "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "_ga", "_gl",
        // Facebook & Meta
        "fbclid", "fbc", "fbp",
        // Twitter / X
        "twclid",
        // Microsoft / Bing
        "msclkid",
        // TikTok
        "ttclid",
        // Reddit
        "rdid",
        // Yandex
        "yclid", "ymclid",
        // Mailchimp & HubSpot & Marketo
        "mc_cid", "mc_eid", "_hsenc", "_hsmi", "hsctatracking", "mkt_tok",
        // General / Affiliate / Social / Video Trackers
        "igshid", "zanpid", "aff_id", "affiliate_id", "si", "ref_src", "ref_url", "feature",
        "vero_id", "vero_conv", "wickedid", "spreportid", "spmailingid", "spuserid", "spjobid"
    )

    private val TRACKING_PREFIXES = listOf(
        "utm_",
        "hsa_",
        "pk_",
        "matomo_",
        "piwik_"
    )


    /**
     * Sanitizes the given URL by removing all known tracking parameters while preserving
     * functional query parameters, path structure, and fragment identifiers.
     *
     * @param url The raw URL string.
     * @return The sanitized URL string, or the original URL if parsing fails or no tracking params exist.
     */
    @JvmStatic
    fun sanitizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url

        return try {
            val uri = Uri.parse(url.trim())
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            if (scheme != "http" && scheme != "https") {
                return url
            }

            val queryParamNames = uri.queryParameterNames
            if (queryParamNames.isNullOrEmpty()) {
                return url
            }

            var hasTrackingParams = false
            val remainingParams = mutableListOf<Pair<String, List<String>>>()

            for (paramName in queryParamNames) {
                if (isTrackingParam(paramName)) {
                    hasTrackingParams = true
                } else {
                    val values = uri.getQueryParameters(paramName)
                    remainingParams.add(Pair(paramName, values))
                }
            }

            if (!hasTrackingParams) {
                return url
            }

            // Build sanitized URI
            val builder = uri.buildUpon().clearQuery()
            for ((key, values) in remainingParams) {
                for (value in values) {
                    builder.appendQueryParameter(key, value)
                }
            }

            builder.build().toString()
        } catch (e: Throwable) {
            url
        }
    }

    /**
     * Returns true if the query parameter name matches known tracking parameters or prefixes.
     */
    @JvmStatic
    fun isTrackingParam(paramName: String?): Boolean {
        if (paramName.isNullOrBlank()) return false
        val lower = paramName.lowercase(Locale.ROOT)

        if (TRACKING_PARAMS.contains(lower)) {
            return true
        }

        for (prefix in TRACKING_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true
            }
        }

        return false
    }
}

package `in`.rahulja.materialhn.widget

/**
 * Normalises article URLs before they reach the WebView. Cleartext stays banned
 * by the network security config, so a plain-http article is retried over https.
 */
object ArticleUrlPolicy {

    private const val HTTP = "http://"
    private const val HTTPS = "https://"

    @JvmStatic
    fun upgradeToHttps(url: String?): String? =
        if (url != null && url.regionMatches(0, HTTP, 0, HTTP.length, ignoreCase = true)) {
            HTTPS + url.substring(HTTP.length)
        } else {
            url
        }
}

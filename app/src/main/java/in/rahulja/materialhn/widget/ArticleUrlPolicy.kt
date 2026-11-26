package `in`.rahulja.materialhn.widget

/**
 * Normalises article URLs before they reach the WebView. Cleartext stays banned
 * by the network security config, so a plain-http article is retried over https.
 */
object ArticleUrlPolicy {

    private const val HTTP = "http://"
    private const val HTTPS = "https://"
    private val PLAINTEXT_PORT = Regex("^(https://[^/?#]+):80(?=[/?#]|$)", RegexOption.IGNORE_CASE)

    @JvmStatic
    fun upgradeToHttps(url: String?): String? {
        if (url == null || !url.regionMatches(0, HTTP, 0, HTTP.length, ignoreCase = true)) {
            return url
        }
        // An explicit :80 is the plaintext default port; https would not answer on it.
        return PLAINTEXT_PORT.replace(HTTPS + url.substring(HTTP.length), "$1")
    }
}

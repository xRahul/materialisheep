package `in`.rahulja.materialhn.widget

import android.webkit.WebSettings

/**
 * Single source of truth for the WebView settings the app applies to remote and
 * local (reader) content. Security flags are never weakened here.
 */
object WebViewSettingsPolicy {

    @JvmStatic
    fun applyRemote(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        applySharedFlags(settings)
    }

    @JvmStatic
    fun applyLocal(settings: WebSettings) {
        settings.javaScriptEnabled = false
        settings.domStorageEnabled = false
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false
        applySharedFlags(settings)
    }

    private fun applySharedFlags(settings: WebSettings) {
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
    }

    /**
     * Online content is always fetched with normal HTTP caching. An offline archive
     * is served from cache only; an offline page without an archive may still come
     * from the HTTP cache.
     */
    @JvmStatic
    fun cacheModeFor(online: Boolean, archiveExists: Boolean): Int = when {
        online -> WebSettings.LOAD_DEFAULT
        archiveExists -> WebSettings.LOAD_CACHE_ONLY
        else -> WebSettings.LOAD_CACHE_ELSE_NETWORK
    }
}

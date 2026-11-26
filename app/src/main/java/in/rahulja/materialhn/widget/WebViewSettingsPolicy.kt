package `in`.rahulja.materialhn.widget

import android.webkit.WebSettings
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Single source of truth for the WebView settings the app applies to remote and
 * local (reader) content. Security flags are never weakened here.
 */
object WebViewSettingsPolicy {

    @JvmStatic
    @JvmOverloads
    fun applyRemote(settings: WebSettings, darkTheme: Boolean = false) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        applySharedFlags(settings)
        applyAlgorithmicDarkening(settings, allow = darkTheme)
    }

    @JvmStatic
    fun applyLocal(settings: WebSettings) {
        settings.javaScriptEnabled = false
        settings.domStorageEnabled = false
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false
        applySharedFlags(settings)
        // Reader colours must stay exactly as designed, even after a remote page
        // was darkened in the same theme.
        applyAlgorithmicDarkening(settings, allow = false)
    }

    private fun applySharedFlags(settings: WebSettings) {
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
    }

    /** Seam around the androidx.webkit feature check so tests can assert the call. */
    internal fun applyAlgorithmicDarkening(
        settings: WebSettings,
        allow: Boolean,
        isSupported: () -> Boolean = {
            WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)
        },
        setAllowed: (WebSettings, Boolean) -> Unit = { target, value ->
            try {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(target, value)
            } catch (e: UnsupportedOperationException) {
                // Some WebView builds advertise the feature and then refuse it.
            }
        }
    ) {
        if (isSupported()) {
            setAllowed(settings, allow)
        }
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

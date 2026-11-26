package `in`.rahulja.materialhn.widget

import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class WebViewSettingsPolicyTest {

    private lateinit var settings: WebSettings

    @Before
    fun setUp() {
        settings = WebView(ApplicationProvider.getApplicationContext()).settings
    }

    @Test
    fun applyRemoteEnablesJavaScriptAndDomStorage() {
        WebViewSettingsPolicy.applyRemote(settings)

        assertTrue(settings.javaScriptEnabled)
        assertTrue(settings.domStorageEnabled)
    }

    @Test
    fun applyRemoteEnablesWideViewportAndOverviewMode() {
        WebViewSettingsPolicy.applyRemote(settings)

        assertTrue(settings.useWideViewPort)
        assertTrue(settings.loadWithOverviewMode)
    }

    @Test
    fun applyRemoteEnablesPinchZoomWithHiddenControls() {
        WebViewSettingsPolicy.applyRemote(settings)

        assertTrue(settings.supportZoom())
        assertTrue(settings.builtInZoomControls)
        assertFalse(settings.displayZoomControls)
    }

    @Test
    fun applyRemoteKeepsFileAndContentAccessOff() {
        WebViewSettingsPolicy.applyRemote(settings)

        assertFalse(settings.allowFileAccess)
        assertFalse(settings.allowContentAccess)
    }

    @Test
    fun applyLocalDisablesJavaScriptAndDomStorage() {
        WebViewSettingsPolicy.applyRemote(settings)
        WebViewSettingsPolicy.applyLocal(settings)

        assertFalse(settings.javaScriptEnabled)
        assertFalse(settings.domStorageEnabled)
    }

    @Test
    fun applyLocalDisablesWideViewportAndOverviewMode() {
        WebViewSettingsPolicy.applyRemote(settings)
        WebViewSettingsPolicy.applyLocal(settings)

        assertFalse(settings.useWideViewPort)
        assertFalse(settings.loadWithOverviewMode)
    }

    @Test
    fun applyLocalKeepsPinchZoomAndFileAccessOff() {
        WebViewSettingsPolicy.applyLocal(settings)

        assertTrue(settings.supportZoom())
        assertTrue(settings.builtInZoomControls)
        assertFalse(settings.displayZoomControls)
        assertFalse(settings.allowFileAccess)
        assertFalse(settings.allowContentAccess)
    }

    @Test
    fun darkThemeAllowsAlgorithmicDarkening() {
        var allowed: Boolean? = null

        WebViewSettingsPolicy.applyAlgorithmicDarkening(
            settings,
            allow = true,
            isSupported = { true },
            setAllowed = { _, value -> allowed = value }
        )

        assertEquals(true, allowed)
    }

    @Test
    fun lightThemeDisallowsAlgorithmicDarkening() {
        var allowed: Boolean? = null

        WebViewSettingsPolicy.applyAlgorithmicDarkening(
            settings,
            allow = false,
            isSupported = { true },
            setAllowed = { _, value -> allowed = value }
        )

        assertEquals(false, allowed)
    }

    @Test
    fun unsupportedFeatureDoesNothing() {
        var called = false

        WebViewSettingsPolicy.applyAlgorithmicDarkening(
            settings,
            allow = true,
            isSupported = { false },
            setAllowed = { _, _ -> called = true }
        )

        assertFalse(called)
    }
}

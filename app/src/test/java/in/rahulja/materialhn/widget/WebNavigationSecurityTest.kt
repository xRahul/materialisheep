package `in`.rahulja.materialhn.widget

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.materialhn.data.FileDownloader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class WebNavigationSecurityTest {

    private fun allowed(url: String) = MaterialWebView.HistoryWebViewClient.isAllowedNavigation(url)

    @Test
    fun `http and https load in the webview`() {
        assertTrue(allowed("https://news.ycombinator.com/item?id=1"))
        assertTrue(allowed("http://example.com/a"))
    }

    @Test
    fun `external handlers are allowed`() {
        assertTrue(allowed("mailto:s*****e@example.com"))
        assertTrue(allowed("tel:+10000000000"))
        assertTrue(allowed("market://details?id=in.rahulja.materialhn"))
    }

    @Test
    fun `local and script schemes are blocked`() {
        assertFalse(allowed("javascript:alert(1)"))
        assertFalse(allowed("file:///data/data/in.rahulja.materialhn/shared_prefs/auth_secure_store.xml"))
        assertFalse(allowed("content://in.rahulja.materialhn.syncprovider/saved"))
        assertFalse(allowed("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(allowed("data:text/html,<script>alert(1)</script>"))
        assertFalse(allowed(""))
    }

    @Test
    fun `pdf bridge rejects files outside the cache directory`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNull(FileDownloader.confineToCache(
            context, "/data/data/in.rahulja.materialhn/shared_prefs/auth_secure_store.xml"))
        assertNull(FileDownloader.confineToCache(context, "../files/secret"))
        val inside = File(context.cacheDir, "download.pdf")
        assertNotNull(FileDownloader.confineToCache(context, inside.absolutePath))
    }
}

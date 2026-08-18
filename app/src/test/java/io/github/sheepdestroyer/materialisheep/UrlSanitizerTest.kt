package io.github.sheepdestroyer.materialisheep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlSanitizerTest {

    @Test
    fun testSanitizeUrl_nullOrBlank_returnsOriginal() {
        assertNull(UrlSanitizer.sanitizeUrl(null))
        assertEquals("", UrlSanitizer.sanitizeUrl(""))
        assertEquals("   ", UrlSanitizer.sanitizeUrl("   "))
    }

    @Test
    fun testSanitizeUrl_noQueryParams_returnsUnchanged() {
        val url = "https://example.com/article/123"
        assertEquals(url, UrlSanitizer.sanitizeUrl(url))
    }

    @Test
    fun testSanitizeUrl_stripsUtmParams() {
        val raw = "https://example.com/post?utm_source=twitter&utm_medium=social&utm_campaign=launch&id=42"
        val sanitized = UrlSanitizer.sanitizeUrl(raw)
        assertEquals("https://example.com/post?id=42", sanitized)
    }

    @Test
    fun testSanitizeUrl_stripsFbclidAndGclid() {
        val raw = "https://example.com/blog?fbclid=IwAR3x_abc123&gclid=CjwKCAjw&page=2"
        val sanitized = UrlSanitizer.sanitizeUrl(raw)
        assertEquals("https://example.com/blog?page=2", sanitized)
    }

    @Test
    fun testSanitizeUrl_allParamsAreTrackers_removesQueryStringEntirely() {
        val raw = "https://example.com/page?utm_source=newsletter&utm_medium=email&fbclid=123"
        val sanitized = UrlSanitizer.sanitizeUrl(raw)
        assertEquals("https://example.com/page", sanitized)
    }

    @Test
    fun testSanitizeUrl_preservesFragment() {
        val raw = "https://example.com/docs?utm_source=share#section-2"
        val sanitized = UrlSanitizer.sanitizeUrl(raw)
        assertEquals("https://example.com/docs#section-2", sanitized)
    }

    @Test
    fun testSanitizeUrl_preservesFunctionalParams() {
        val raw = "https://news.ycombinator.com/item?id=3891234&p=2"
        val sanitized = UrlSanitizer.sanitizeUrl(raw)
        assertEquals(raw, sanitized)
    }

    @Test
    fun testIsTrackingParam() {
        assertTrue(UrlSanitizer.isTrackingParam("utm_source"))
        assertTrue(UrlSanitizer.isTrackingParam("UTM_CAMPAIGN"))
        assertTrue(UrlSanitizer.isTrackingParam("fbclid"))
        assertTrue(UrlSanitizer.isTrackingParam("gclid"))
        assertTrue(UrlSanitizer.isTrackingParam("mc_eid"))
        assertTrue(UrlSanitizer.isTrackingParam("hsa_cam"))
        assertTrue(UrlSanitizer.isTrackingParam("si"))
        assertTrue(UrlSanitizer.isTrackingParam("ref_src"))
        assertTrue(UrlSanitizer.isTrackingParam("pk_campaign"))
        assertTrue(UrlSanitizer.isTrackingParam("matomo_source"))
        assertFalse(UrlSanitizer.isTrackingParam("id"))
        assertFalse(UrlSanitizer.isTrackingParam("page"))
        assertFalse(UrlSanitizer.isTrackingParam("q"))
        assertFalse(UrlSanitizer.isTrackingParam(null))
        assertFalse(UrlSanitizer.isTrackingParam(""))
    }
}


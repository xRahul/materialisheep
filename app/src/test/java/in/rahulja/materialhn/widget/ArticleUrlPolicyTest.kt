package `in`.rahulja.materialhn.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleUrlPolicyTest {

    @Test
    fun httpIsUpgradedToHttps() {
        assertEquals("https://example.com/a", ArticleUrlPolicy.upgradeToHttps("http://example.com/a"))
    }

    @Test
    fun schemeMatchIsCaseInsensitive() {
        assertEquals("https://Example.com", ArticleUrlPolicy.upgradeToHttps("HTTP://Example.com"))
    }

    @Test
    fun httpsIsUnchanged() {
        assertEquals("https://example.com", ArticleUrlPolicy.upgradeToHttps("https://example.com"))
    }

    @Test
    fun otherSchemesAreUnchanged() {
        assertEquals("mailto:a@b.com", ArticleUrlPolicy.upgradeToHttps("mailto:a@b.com"))
    }

    @Test
    fun emptyAndNullAreSafe() {
        assertEquals("", ArticleUrlPolicy.upgradeToHttps(""))
        assertNull(ArticleUrlPolicy.upgradeToHttps(null))
    }
}

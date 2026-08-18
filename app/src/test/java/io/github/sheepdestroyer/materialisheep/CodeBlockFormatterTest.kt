package io.github.sheepdestroyer.materialisheep

import android.text.Html
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CodeBlockFormatterTest {

    @Test
    fun testPreprocessCodeTags() {
        assertNull(CodeBlockFormatter.preprocessCodeTags(null))
        assertEquals("plain text", CodeBlockFormatter.preprocessCodeTags("plain text"))
        assertEquals("<tt>val a = 1</tt>", CodeBlockFormatter.preprocessCodeTags("<pre><code>val a = 1</code></pre>"))
        assertEquals("<tt>let b = 2</tt>", CodeBlockFormatter.preprocessCodeTags("<code>let b = 2</code>"))
    }

    @Test
    fun testFormatCodeBlocks_nullOrEmpty_returnsOriginal() {
        assertNull(CodeBlockFormatter.formatCodeBlocks(null))
        assertEquals("", CodeBlockFormatter.formatCodeBlocks(""))
    }

    @Test
    fun testFormatCodeBlocks_plainTextWithoutCode_noChanges() {
        val raw = Html.fromHtml("Just a standard comment without code", Html.FROM_HTML_MODE_LEGACY)
        val result = CodeBlockFormatter.formatCodeBlocks(raw)
        assertNotNull(result)
        assertEquals("Just a standard comment without code", result.toString().trim())
    }

    @Test
    fun testFormatCodeBlocks_withPreprocessedCodeTag_appliesBackgroundAndKeywordSpans() {
        val html = "<p>Check out this code:</p><pre><code>val x = 42\nif (x &gt; 10) return true</code></pre>"
        val processed = CodeBlockFormatter.preprocessCodeTags(html)
        val spanned = Html.fromHtml(processed, Html.FROM_HTML_MODE_LEGACY)
        val formatted = CodeBlockFormatter.formatCodeBlocks(spanned) as Spanned

        assertNotNull(formatted)

        // Verify background color span applied
        val bgSpans = formatted.getSpans(0, formatted.length, BackgroundColorSpan::class.java)
        assertTrue(bgSpans.isNotEmpty())

        // Verify keyword foreground color spans applied to 'val', 'if', 'return', 'true'
        val fgSpans = formatted.getSpans(0, formatted.length, ForegroundColorSpan::class.java)
        assertTrue(fgSpans.isNotEmpty())

        // Verify bold style spans applied to keywords
        val styleSpans = formatted.getSpans(0, formatted.length, StyleSpan::class.java)
        assertTrue(styleSpans.isNotEmpty())
    }

    @Test
    fun testAppUtilsFromHtml_integratesCodeFormatting() {
        val html = "<pre><code>fun test() = true</code></pre>"
        val result = AppUtils.fromHtml(html)
        assertTrue(result is Spanned)
        val spanned = result as Spanned
        val bgSpans = spanned.getSpans(0, spanned.length, BackgroundColorSpan::class.java)
        assertTrue(bgSpans.isNotEmpty())
        val fgSpans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        assertTrue(fgSpans.isNotEmpty())
    }

    @Test
    fun testFormatCodeBlocks_darkModeVsLightModeColors() {
        val html = "<pre><code>val x = true</code></pre>"
        val processed = CodeBlockFormatter.preprocessCodeTags(html)

        val lightSpanned = Html.fromHtml(processed, Html.FROM_HTML_MODE_LEGACY)
        val lightFormatted = CodeBlockFormatter.formatCodeBlocks(lightSpanned, isDark = false) as Spanned
        val lightFg = lightFormatted.getSpans(0, lightFormatted.length, ForegroundColorSpan::class.java).first()
        assertEquals(0xFF00796B.toInt(), lightFg.foregroundColor)

        val darkSpanned = Html.fromHtml(processed, Html.FROM_HTML_MODE_LEGACY)
        val darkFormatted = CodeBlockFormatter.formatCodeBlocks(darkSpanned, isDark = true) as Spanned
        val darkFg = darkFormatted.getSpans(0, darkFormatted.length, ForegroundColorSpan::class.java).first()
        assertEquals(0xFF4DB6AC.toInt(), darkFg.foregroundColor)
    }
}



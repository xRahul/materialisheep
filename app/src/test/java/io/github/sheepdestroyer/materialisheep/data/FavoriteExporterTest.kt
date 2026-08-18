package io.github.sheepdestroyer.materialisheep.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoriteExporterTest {

    @Test
    fun testToJson_serializesValidJsonArray() {
        val item1 = mock(WebItem::class.java).apply {
            `when`(id).thenReturn("101")
            `when`(displayedTitle).thenReturn("Show HN: New Project")
            `when`(url).thenReturn("https://example.com/project")
        }
        val item2 = mock(WebItem::class.java).apply {
            `when`(id).thenReturn("102")
            `when`(displayedTitle).thenReturn("Ask HN: Best practices?")
            `when`(url).thenReturn("https://example.com/ask")
        }

        val jsonString = FavoriteExporter.toJson(listOf(item1, item2))
        val jsonArray = JSONArray(jsonString)

        assertEquals(2, jsonArray.length())
        assertEquals("101", jsonArray.getJSONObject(0).getString("id"))
        assertEquals("Show HN: New Project", jsonArray.getJSONObject(0).getString("title"))
        assertEquals("https://example.com/project", jsonArray.getJSONObject(0).getString("url"))
        assertEquals("https://news.ycombinator.com/item?id=101", jsonArray.getJSONObject(0).getString("hnUrl"))
    }

    @Test
    fun testToMarkdown_formatsMarkdownLinks() {
        val item = mock(WebItem::class.java).apply {
            `when`(id).thenReturn("202")
            `when`(displayedTitle).thenReturn("Interesting Tech Article")
            `when`(url).thenReturn("https://tech.example.com")
        }

        val markdown = FavoriteExporter.toMarkdown(listOf(item))

        assertTrue(markdown.startsWith("# Saved Stories - Materialisheep"))
        assertTrue(markdown.contains("[Interesting Tech Article](https://tech.example.com)"))
        assertTrue(markdown.contains("[HN Discussion](https://news.ycombinator.com/item?id=202)"))
    }

    @Test
    fun testToNetscapeHtml_formatsValidHtmlBookmarks() {
        val item = mock(WebItem::class.java).apply {
            `when`(id).thenReturn("303")
            `when`(displayedTitle).thenReturn("Title with <special> & characters")
            `when`(url).thenReturn("https://example.org")
        }

        val html = FavoriteExporter.toNetscapeHtml(listOf(item))

        assertTrue(html.contains("<!DOCTYPE NETSCAPE-Bookmark-file-1>"))
        assertTrue(html.contains("<DT><A HREF=\"https://example.org\">Title with &lt;special&gt; &amp; characters</A>"))
    }
}

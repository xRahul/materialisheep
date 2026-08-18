package io.github.sheepdestroyer.materialisheep.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Exporter utility for serializing saved favorites into portable data formats
 * including structured JSON, Markdown for PKM systems (Obsidian/Logseq), and Netscape Bookmark HTML.
 */
object FavoriteExporter {

    @JvmStatic
    fun toJson(favorites: List<WebItem>): String {
        val array = JSONArray()
        for (item in favorites) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.displayedTitle)
                put("url", item.url)
                put("hnUrl", HackerNewsClient.WEB_ITEM_PATH_PREFIX + item.id)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    @JvmStatic
    fun toMarkdown(favorites: List<WebItem>): String {
        val sb = StringBuilder()
        sb.append("# Saved Stories - Materialisheep\n\n")
        for (item in favorites) {
            sb.append("- [")
                .append(item.displayedTitle.replace("[", "\\[").replace("]", "\\]"))
                .append("](")
                .append(item.url)
                .append(") • [HN Discussion](")
                .append(HackerNewsClient.WEB_ITEM_PATH_PREFIX)
                .append(item.id)
                .append(")\n")
        }
        return sb.toString()
    }

    @JvmStatic
    fun toNetscapeHtml(favorites: List<WebItem>): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        sb.append("<TITLE>Materialisheep Bookmarks</TITLE>\n")
        sb.append("<H1>Materialisheep Bookmarks</H1>\n")
        sb.append("<DL><p>\n")
        for (item in favorites) {
            val escapedTitle = item.displayedTitle
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
            sb.append("    <DT><A HREF=\"")
                .append(item.url)
                .append("\">")
                .append(escapedTitle)
                .append("</A>\n")
        }
        sb.append("</DL><p>\n")
        return sb.toString()
    }
}

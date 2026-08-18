package io.github.sheepdestroyer.materialisheep

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import java.util.regex.Pattern

/**
 * Formats code blocks and inline snippets in comments with distinct monospace styling,
 * background container tinting, and syntax highlighting for common developer keywords.
 */
object CodeBlockFormatter {

    // Common programming keywords across Java, Kotlin, JS/TS, Python, Rust, Go, SQL, C/C++
    private val KEYWORD_PATTERN = Pattern.compile(
        "\\b(val|var|fun|function|const|let|if|else|return|class|interface|object|" +
        "import|export|package|def|async|await|struct|impl|pub|fn|type|SELECT|FROM|" +
        "WHERE|INSERT|UPDATE|DELETE|JOIN|public|private|protected|static|final|" +
        "true|false|null|nil|None)\\b"
    )

    // Code background color (subtle dark tint with alpha)
    private const val CODE_BG_COLOR = 0x1A808080 // ~10% transparent gray

    // Keyword highlight colors tailored for WCAG AA ≥ 4.5:1 contrast
    private const val KEYWORD_COLOR_LIGHT = 0xFF00796B.toInt() // Teal 700 (5.32:1 on white)
    private const val KEYWORD_COLOR_DARK = 0xFF4DB6AC.toInt()  // Teal 300 (10.5:1 on black)

    /**
     * Converts <pre><code> and <code> tags to <tt> tags so that Android's Html.fromHtml
     * creates TypefaceSpan("monospace") elements.
     */
    @JvmStatic
    fun preprocessCodeTags(html: String?): String? {
        if (html.isNullOrEmpty()) return html
        if (!html.contains("<pre") && !html.contains("<code")) {
            return html
        }
        return html
            .replace("<pre><code>", "<tt>")
            .replace("</code></pre>", "</tt>")
            .replace("<code>", "<tt>")
            .replace("</code>", "</tt>")
            .replace("<pre>", "<tt>")
            .replace("</pre>", "</tt>")
    }

    /**
     * Formats monospace spans in the given Spanned CharSequence by applying a background
     * tint and keyword syntax highlights.
     *
     * @param spanned The Spanned CharSequence produced by Html.fromHtml.
     * @param isDark  Whether the target surface is a dark/AMOLED theme.
     * @return The styled CharSequence (or original if null/empty).
     */
    @JvmStatic
    @JvmOverloads
    fun formatCodeBlocks(spanned: CharSequence?, isDark: Boolean = false): CharSequence? {
        if (spanned == null || spanned !is Spanned || spanned.isEmpty()) {
            return spanned
        }

        val keywordColor = if (isDark) KEYWORD_COLOR_DARK else KEYWORD_COLOR_LIGHT
        val spannable = if (spanned is Spannable) spanned else SpannableString(spanned)
        val typefaceSpans = spannable.getSpans(0, spannable.length, TypefaceSpan::class.java)

        for (span in typefaceSpans) {
            val family = span.family
            if (family == null || family.equals("monospace", ignoreCase = true)) {
                val start = spannable.getSpanStart(span)
                val end = spannable.getSpanEnd(span)
                if (start in 0 until end) {
                    // Apply subtle background container tint
                    spannable.setSpan(
                        BackgroundColorSpan(CODE_BG_COLOR),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // Apply keyword syntax highlights within the code span
                    val codeContent = spannable.subSequence(start, end).toString()
                    val matcher = KEYWORD_PATTERN.matcher(codeContent)
                    while (matcher.find()) {
                        val keywordStart = start + matcher.start()
                        val keywordEnd = start + matcher.end()
                        spannable.setSpan(
                            ForegroundColorSpan(keywordColor),
                            keywordStart,
                            keywordEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        spannable.setSpan(
                            StyleSpan(Typeface.BOLD),
                            keywordStart,
                            keywordEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }

        return spannable
    }
}


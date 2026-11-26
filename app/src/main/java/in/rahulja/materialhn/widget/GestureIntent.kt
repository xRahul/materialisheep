package `in`.rahulja.materialhn.widget

import kotlin.math.abs

/**
 * Decides when a WebView should keep a horizontal gesture from its parent pager.
 * Only a mostly horizontal gesture that can actually pan the page claims it, so
 * an unzoomed page still swipes between tabs.
 */
object GestureIntent {

    @JvmStatic
    fun claimsHorizontal(
        dx: Float,
        dy: Float,
        canScrollLeft: Boolean,
        canScrollRight: Boolean,
        touchSlop: Int
    ): Boolean {
        if (abs(dx) <= touchSlop || abs(dx) <= abs(dy)) {
            return false
        }
        return if (dx < 0f) canScrollRight else canScrollLeft
    }
}

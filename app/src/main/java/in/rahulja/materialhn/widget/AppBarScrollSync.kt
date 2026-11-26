package `in`.rahulja.materialhn.widget

import com.google.android.material.appbar.AppBarLayout

/**
 * Drives an AppBarLayout from raw WebView scroll deltas, because a WebView that
 * owns its scrolling cannot participate in nested scrolling.
 */
class AppBarScrollSync(
    private val appBar: AppBarLayout?,
    private val thresholdPx: Int
) {
    private var accumulated = 0

    fun onScroll(dy: Int, atTop: Boolean) {
        if (appBar == null) return
        if (atTop) {
            accumulated = 0
            appBar.setExpanded(true, true)
            return
        }
        if (dy == 0) return
        if (accumulated != 0 && dy > 0 != accumulated > 0) {
            accumulated = 0
        }
        accumulated += dy
        when {
            accumulated >= thresholdPx -> {
                accumulated = 0
                appBar.setExpanded(false, true)
            }
            accumulated <= -thresholdPx -> {
                accumulated = 0
                appBar.setExpanded(true, true)
            }
        }
    }

    fun expand() {
        accumulated = 0
        appBar?.setExpanded(true, true)
    }
}

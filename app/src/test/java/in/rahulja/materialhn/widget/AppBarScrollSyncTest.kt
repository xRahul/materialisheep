package `in`.rahulja.materialhn.widget

import com.google.android.material.appbar.AppBarLayout
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class AppBarScrollSyncTest {

    private val appBar: AppBarLayout = mock(AppBarLayout::class.java)

    @Test
    fun scrollingDownPastThresholdCollapses() {
        val sync = AppBarScrollSync(appBar, thresholdPx = 24)

        sync.onScroll(24, atTop = false)

        verify(appBar).setExpanded(false, true)
    }

    @Test
    fun scrollingUpPastThresholdExpands() {
        val sync = AppBarScrollSync(appBar, thresholdPx = 24)
        sync.onScroll(100, atTop = false)

        sync.onScroll(-24, atTop = false)

        verify(appBar).setExpanded(true, true)
    }

    @Test
    fun reachingTopExpands() {
        val sync = AppBarScrollSync(appBar, thresholdPx = 24)
        sync.onScroll(10, atTop = false)

        sync.onScroll(0, atTop = true)

        verify(appBar).setExpanded(true, true)
    }

    @Test
    fun smallJitterDoesNothing() {
        val sync = AppBarScrollSync(appBar, thresholdPx = 24)

        sync.onScroll(5, atTop = false)
        sync.onScroll(5, atTop = false)
        sync.onScroll(-5, atTop = false)

        verifyNoInteractions(appBar)
    }

    @Test
    fun directionChangeResetsAccumulator() {
        val sync = AppBarScrollSync(appBar, thresholdPx = 24)

        sync.onScroll(20, atTop = false)
        sync.onScroll(-20, atTop = false)
        sync.onScroll(10, atTop = false)

        verifyNoInteractions(appBar)
    }

    @Test
    fun nullAppBarIsSafe() {
        val sync = AppBarScrollSync(null, thresholdPx = 24)

        sync.onScroll(100, atTop = false)
        sync.onScroll(0, atTop = true)
        sync.expand()
    }

    @Test
    fun neverExpandsOnEveryEvent() {
        val sync = AppBarScrollSync(appBar, thresholdPx = 24)

        sync.onScroll(1, atTop = false)

        verify(appBar, never()).setExpanded(true, true)
    }
}

package `in`.rahulja.materialhn.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettleTrackerTest {

    @Test
    fun settlesAfterTwoEqualReadings() {
        val tracker = SettleTracker()

        assertFalse(tracker.record(1200, elapsedMs = 400))
        assertTrue(tracker.record(1200, elapsedMs = 800))
    }

    @Test
    fun growingPageNeverSettlesBeforeCap() {
        val tracker = SettleTracker()

        for (i in 1..9) {
            assertFalse(
                "growing page must not settle at ${i * 400}ms",
                tracker.record(i * 100, elapsedMs = i * 400L)
            )
        }
    }

    @Test
    fun capReachedSettles() {
        val tracker = SettleTracker()

        assertTrue(tracker.record(100, elapsedMs = 4000))
    }

    @Test
    fun zeroLengthDoesNotCountAsStable() {
        val tracker = SettleTracker()

        assertFalse(tracker.record(0, elapsedMs = 400))
        assertFalse(tracker.record(0, elapsedMs = 800))
    }
}

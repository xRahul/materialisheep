package `in`.rahulja.materialhn.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureIntentTest {

    private val slop = 8

    @Test
    fun verticalGestureIsNotClaimed() {
        assertFalse(
            GestureIntent.claimsHorizontal(
                dx = 2f, dy = 40f,
                canScrollLeft = true, canScrollRight = true, touchSlop = slop
            )
        )
    }

    @Test
    fun horizontalGestureWithRoomToTheRightIsClaimed() {
        assertTrue(
            GestureIntent.claimsHorizontal(
                dx = -40f, dy = 2f,
                canScrollLeft = true, canScrollRight = true, touchSlop = slop
            )
        )
    }

    @Test
    fun horizontalGestureWithNoRoomToTheRightIsNotClaimed() {
        assertFalse(
            GestureIntent.claimsHorizontal(
                dx = -40f, dy = 2f,
                canScrollLeft = true, canScrollRight = false, touchSlop = slop
            )
        )
    }

    @Test
    fun horizontalGestureWithRoomToTheLeftIsClaimed() {
        assertTrue(
            GestureIntent.claimsHorizontal(
                dx = 40f, dy = 2f,
                canScrollLeft = true, canScrollRight = false, touchSlop = slop
            )
        )
    }

    @Test
    fun horizontalGestureWithNoRoomToTheLeftIsNotClaimed() {
        assertFalse(
            GestureIntent.claimsHorizontal(
                dx = 40f, dy = 2f,
                canScrollLeft = false, canScrollRight = true, touchSlop = slop
            )
        )
    }

    @Test
    fun gestureBelowSlopIsNotClaimed() {
        assertFalse(
            GestureIntent.claimsHorizontal(
                dx = 4f, dy = 0f,
                canScrollLeft = true, canScrollRight = true, touchSlop = slop
            )
        )
    }
}

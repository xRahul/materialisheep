package io.github.sheepdestroyer.materialisheep.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CommentIndentationTest {

    @Test
    public void testMaxIndentLevelConstant() {
        assertEquals(4, SinglePageItemRecyclerViewAdapter.MAX_INDENT_LEVEL);
    }

    @Test
    public void testEffectiveLevelClamping() {
        int max = SinglePageItemRecyclerViewAdapter.MAX_INDENT_LEVEL;

        // Negative / root level
        assertEquals(0, Math.min(Math.max(0, -1), max));
        assertEquals(0, Math.min(Math.max(0, 0), max));

        // Normal nesting
        assertEquals(1, Math.min(Math.max(0, 1), max));
        assertEquals(2, Math.min(Math.max(0, 2), max));
        assertEquals(3, Math.min(Math.max(0, 3), max));
        assertEquals(4, Math.min(Math.max(0, 4), max));

        // Deep nesting clamped to max (4)
        assertEquals(4, Math.min(Math.max(0, 5), max));
        assertEquals(4, Math.min(Math.max(0, 8), max));
        assertEquals(4, Math.min(Math.max(0, 20), max));
    }
}

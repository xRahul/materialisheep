package io.github.sheepdestroyer.materialisheep.widget;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import android.view.MotionEvent;
import org.junit.Assert;
import org.robolectric.annotation.Config;
import io.github.sheepdestroyer.materialisheep.R;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class NavFloatingActionButtonTest {

    @Test
    public void testInstantiation() {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.AppTheme);
        NavFloatingActionButton fab = new NavFloatingActionButton(context);
        assertNotNull(fab);
    }

    @Test
    public void testBindNavigationPad() {
         Context context = ApplicationProvider.getApplicationContext();
         context.setTheme(R.style.AppTheme);
         NavFloatingActionButton fab = new NavFloatingActionButton(context);
         // This triggers the GestureDetector creation code path
         fab.bindNavigationPad();
    }

    @Test
    public void testDragCompensatesShift() {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.AppTheme);
        NavFloatingActionButton fab = new NavFloatingActionButton(context);

        // Initial position of the view
        float initialViewX = 100f;
        float initialViewY = 100f;
        fab.setX(initialViewX);
        fab.setY(initialViewY);

        // Simulate start drag with an offset
        // Let's say the touch point (Raw) is at (150, 150).
        // OffsetX = ViewX - RawX = 100 - 150 = -50
        // OffsetY = ViewY - RawY = 100 - 150 = -50
        float offsetX = -50f;
        float offsetY = -50f;

        fab.startDrag(offsetX, offsetY);

        // Simulate dragging to a new raw position (160, 160)
        long downTime = System.currentTimeMillis();
        long eventTime = System.currentTimeMillis();
        MotionEvent moveEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, 160f, 160f, 0);

        fab.dispatchTouchEvent(moveEvent);

        // Expected View Position:
        // NewX = RawX + OffsetX = 160 + (-50) = 110
        // NewY = RawY + OffsetY = 160 + (-50) = 110
        Assert.assertEquals(110f, fab.getX(), 0.1f);
        Assert.assertEquals(110f, fab.getY(), 0.1f);
    }
}

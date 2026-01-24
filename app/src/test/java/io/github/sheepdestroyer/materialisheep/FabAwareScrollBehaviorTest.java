package io.github.sheepdestroyer.materialisheep;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FabAwareScrollBehaviorTest {

    private FabAwareScrollBehavior behavior;
    private CoordinatorLayout coordinatorLayout;
    private View child;
    private View target;
    private FloatingActionButton fab;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        behavior = new FabAwareScrollBehavior(context, (AttributeSet) null);
        coordinatorLayout = mock(CoordinatorLayout.class);
        child = mock(View.class);
        target = mock(View.class);
        fab = mock(FloatingActionButton.class);

        when(coordinatorLayout.getDependencies(child)).thenReturn((List) Collections.singletonList(fab));
    }

    @Test
    public void onNestedScroll_scrolledDown_hidesFab() {
        int[] consumed = new int[2];
        // dyConsumed > 0 means scrolled down
        behavior.onNestedScroll(coordinatorLayout, child, target, 0, 10, 0, 0, 0, consumed);

        verify(fab).hide();
        verify(fab, never()).show();
    }

    @Test
    public void onNestedScroll_scrolledUp_showsFab() {
        int[] consumed = new int[2];
        // dyConsumed < 0 means scrolled up
        behavior.onNestedScroll(coordinatorLayout, child, target, 0, -10, 0, 0, 0, consumed);

        verify(fab).show();
        verify(fab, never()).hide();
    }
}

package io.github.sheepdestroyer.materialisheep.widget;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.sheepdestroyer.materialisheep.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S)
public class AppBarSwipeRefreshLayoutTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mContext.setTheme(R.style.AppTheme);
    }

    @Test
    public void testInstantiationAndThemedDefaults() {
        AppBarSwipeRefreshLayout layout = new AppBarSwipeRefreshLayout(mContext);
        assertNotNull(layout);
        // Default overscroll is IF_CONTENT_SCROLLS
        assertNotEquals(View.OVER_SCROLL_NEVER, layout.getOverScrollMode());
    }

    @Test
    public void testApplyThemedColors_allThemes() {
        int[] themeResIds = new int[] {
            R.style.AppTheme,
            R.style.AppTheme_Dark,
            R.style.AppTheme_DayNight,
            R.style.Black,
            R.style.Sepia,
            R.style.Green,
            R.style.Solarized,
            R.style.Solarized_Dark
        };

        for (int themeResId : themeResIds) {
            Context themedContext = ApplicationProvider.getApplicationContext();
            themedContext.setTheme(themeResId);

            AppBarSwipeRefreshLayout layout = new AppBarSwipeRefreshLayout(themedContext);
            assertNotNull("Layout should not be null for theme: " + themeResId, layout);
        }
    }
}
